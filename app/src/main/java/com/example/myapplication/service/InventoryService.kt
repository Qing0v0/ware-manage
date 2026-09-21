package com.example.myapplication.service

import com.example.myapplication.model.OrderBatch
import com.example.myapplication.model.ShoeInventory
import com.example.myapplication.utils.SizeUtils
import com.example.myapplication.utils.StringUtils

/**
 * 存量（[ShoeInventory]）的加减逻辑，入库和出库都走这里。
 *
 * 规则：
 * - 存量按「货号 + 颜色」区分，一个货号一个颜色的 11 个尺码存在一条记录里；
 * - 入库（OrderType.ARTICLE_PURCHASE）：有存量就在原有数量上累加，没有就新增一条；
 * - 出库 / 破损 / 退给经销商：先查存量，某个尺码减完小于 0 就报错；
 * - 一次“确定”提交的整批订单是整体生效的：先 [check] 全部通过，再 [apply] 写库。
 */
class InventoryService(private val inventoryDatabase: InventoryDatabase) {

    private val inventoryDAO = inventoryDatabase.shoeInventoryDAO()

    /**
     * 校验这一批订单能不能入账，返回错误提示（列表为空表示可以入账）。
     *
     * 同一批里如果有相同的货号 + 颜色（表格里加了两行），会先合并再和存量比较，
     * 避免两行各自都没超、加起来却超了的情况。
     */
    fun check(orderBatches: List<OrderBatch>): List<String> {
        val errors = ArrayList<String>()

        for ((articleColor, batches) in orderBatches.groupBy { it.articleId to it.color }) {
            val articleId = articleColor.first
            val color = articleColor.second
            val colorName = color.displayName

            // 先算出这一批对同一个货号 + 颜色的净变化：入库为正，出库为负
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
                // 没有存量：只有整批都是“加”的时候才会新增，否则就是库存不足
                if (deltaSizes.any { it < 0 }) {
                    errors.add(String.format(StringUtils.inventoryNotFound, articleId, colorName))
                }
                continue
            }

            val stockSizes = SizeUtils.sizesOf(inventory)
            for (index in 0 until SizeUtils.SIZE_AMOUNT) {
                if (stockSizes[index] + deltaSizes[index] < 0) {
                    // 减完小于 0：报错，把存量、需要多少都告诉用户
                    errors.add(
                        String.format(
                            StringUtils.inventoryNotEnough,
                            articleId,
                            colorName,
                            SizeUtils.sizeName(index),
                            stockSizes[index],
                            -deltaSizes[index]
                        )
                    )
                }
            }
        }

        return errors
    }

    /**
     * 把这一批订单应用到存量上，[check] 通过之后再调用。
     * 整批放在一个事务里，中途出错会整批回滚，不会出现改了一半的情况。
     *
     * @param imagePaths 和 orderBatches 一一对应的图片路径（入库时新选的图片），
     *                   出库不会改图片，null 表示这一行不动图片
     */
    fun apply(orderBatches: List<OrderBatch>, imagePaths: List<String?> = emptyList()) {
        inventoryDatabase.runInTransaction {
            orderBatches.forEachIndexed { index, orderBatch ->
                applyOne(orderBatch, imagePaths.getOrNull(index))
            }
        }
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
