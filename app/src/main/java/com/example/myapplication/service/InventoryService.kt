package com.example.myapplication.service

import android.content.Context
import com.example.myapplication.R
import com.example.myapplication.model.OrderBatch
import com.example.myapplication.model.ShoeInventory
import com.example.myapplication.utils.SizeUtils

/**
 * 存量（[ShoeInventory]）的加减逻辑，入库和出库都走这里。
 *
 * 规则：
 * - 存量按「货号 + 颜色」区分，一个货号一个颜色的 11 个尺码存在一条记录里；
 * - 入库（OrderType.ARTICLE_PURCHASE）：有存量就在原有数量上累加，没有就新增一条；
 * - 出库 / 破损 / 退给经销商：先查存量，某个尺码减完小于 0 就报错；
 * - 订单流水和存量写在同一个数据库事务里；库存校验也在该事务中进行。
 */
class InventoryService(private val inventoryDatabase: InventoryDatabase, private val context: Context) {

    private val inventoryDAO = inventoryDatabase.shoeInventoryDAO()
    private val orderBatchDAO = inventoryDatabase.orderBatchDAO()

    /**
     * 校验库存并同时写订单流水和存量。任意写入失败时，整个事务回滚。
     * 同一批里相同货号 + 颜色的行会先合并校验，避免合计出库超库存。
     */
    fun apply(orderBatches: List<OrderBatch>, imagePaths: List<String?> = emptyList()): List<String> {
        val errors = ArrayList<String>()

        inventoryDatabase.runInTransaction {
            errors.addAll(check(orderBatches))
            if (errors.isEmpty()) {
                orderBatches.forEachIndexed { index, orderBatch ->
                    orderBatchDAO.insert(orderBatch)
                    applyOne(orderBatch, imagePaths.getOrNull(index))
                }
            }
        }

        return errors
    }

    private fun check(orderBatches: List<OrderBatch>): List<String> {
        val errors = ArrayList<String>()

        for ((articleColor, batches) in orderBatches.groupBy { it.articleId to it.color }) {
            val articleId = articleColor.first
            val color = articleColor.second
            val colorName = color.displayName

            val deltaSizes = IntArray(SizeUtils.SIZE_AMOUNT)
            for (orderBatch in batches) {
                val sign = if (orderBatch.orderType.increaseStock) 1 else -1
                val orderSizes = SizeUtils.sizesOf(orderBatch)
                for (index in 0 until SizeUtils.SIZE_AMOUNT) {
                    deltaSizes[index] += sign * orderSizes[index]
                }
            }

            val inventory = inventoryDAO.query(articleId, color)
            if (inventory == null) {
                if (deltaSizes.any { it < 0 }) {
                    errors.add(context.getString(R.string.inventoryNotFound, articleId, colorName))
                }
                continue
            }

            val stockSizes = SizeUtils.sizesOf(inventory)
            for (index in 0 until SizeUtils.SIZE_AMOUNT) {
                if (stockSizes[index] + deltaSizes[index] < 0) {
                    errors.add(
                        context.getString(
                            R.string.inventoryNotEnough, articleId, colorName,
                            SizeUtils.sizeName(index), stockSizes[index], -deltaSizes[index]
                        )
                    )
                }
            }
        }

        return errors
    }

    private fun applyOne(orderBatch: OrderBatch, imagePath: String?) {
        val inventory = inventoryDAO.query(orderBatch.articleId, orderBatch.color)

        if (inventory == null) {
            // 没有存量：入库就补一条新的；出库这种情况 [check] 已经拦下，这里不再处理
            if (orderBatch.orderType.increaseStock) {
                inventoryDAO.insert(buildInventory(orderBatch, imagePath))
            }
            return
        }

        // 有存量：直接加 / 减
        val sizes = SizeUtils.sizesOf(inventory)
        val orderSizes = SizeUtils.sizesOf(orderBatch)
        val sign = if (orderBatch.orderType.increaseStock) 1 else -1
        for (index in 0 until SizeUtils.SIZE_AMOUNT) {
            sizes[index] += sign * orderSizes[index]
        }
        SizeUtils.applySizes(inventory, sizes)

        // 经销商以最近一次填写的为准（出库不填经销商，就保留原来的）
        if (orderBatch.dealer.isNotEmpty()) {
            inventory.dealer = orderBatch.dealer
        }
        // 图片只有入库能改（界面上出库那一格是锁住的），换了就覆盖
        if (orderBatch.orderType.increaseStock && !imagePath.isNullOrEmpty()) {
            inventory.imagePath = imagePath
        }

        inventoryDAO.update(inventory)
    }

    /** 用入库单里的数量新建一条存量 */
    private fun buildInventory(orderBatch: OrderBatch, imagePath: String?): ShoeInventory {
        val inventory = ShoeInventory(
            articleId = orderBatch.articleId,
            dealer = orderBatch.dealer,
            color = orderBatch.color,
            imagePath = imagePath
        )
        SizeUtils.applySizes(inventory, SizeUtils.sizesOf(orderBatch))
        return inventory
    }
}
