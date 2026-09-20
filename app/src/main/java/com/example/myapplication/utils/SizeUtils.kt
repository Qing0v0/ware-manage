package com.example.myapplication.utils

import com.example.myapplication.model.OrderBatch
import com.example.myapplication.model.ShoeInventory

/**
 * [OrderBatch] 和 [ShoeInventory] 里 34~44 码是 11 个独立字段，
 * 但是汇总、相加减的时候用数组方便，所以统一在这里转换。
 *
 * 下标顺序：0 -> 34 码，1 -> 35 码 …… 10 -> 44 码，
 * 与 OrderRowData.sizes、OrderBatch.size34 ~ size44 完全一致。
 */
object SizeUtils {

    /** 34 ~ 44 一共 11 个尺码 */
    const val SIZE_AMOUNT = 11

    /** 下标对应的码数，如 0 -> 34，用来拼提示语 */
    fun sizeName(index: Int): Int = 34 + index

    /** 取出订单里的 11 个数量 */
    fun sizesOf(orderBatch: OrderBatch): IntArray = intArrayOf(
        orderBatch.size34, orderBatch.size35, orderBatch.size36,
        orderBatch.size37, orderBatch.size38, orderBatch.size39,
        orderBatch.size40, orderBatch.size41, orderBatch.size42,
        orderBatch.size43, orderBatch.size44
    )

    /** 取出一条存量里的 11 个数量 */
    fun sizesOf(inventory: ShoeInventory): IntArray = intArrayOf(
        inventory.size34, inventory.size35, inventory.size36,
        inventory.size37, inventory.size38, inventory.size39,
        inventory.size40, inventory.size41, inventory.size42,
        inventory.size43, inventory.size44
    )

    /** 把 11 个数量写回一条存量 */
    fun applySizes(inventory: ShoeInventory, sizes: IntArray) {
        inventory.size34 = sizes[0]
        inventory.size35 = sizes[1]
        inventory.size36 = sizes[2]
        inventory.size37 = sizes[3]
        inventory.size38 = sizes[4]
        inventory.size39 = sizes[5]
        inventory.size40 = sizes[6]
        inventory.size41 = sizes[7]
        inventory.size42 = sizes[8]
        inventory.size43 = sizes[9]
        inventory.size44 = sizes[10]
    }
}
