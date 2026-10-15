package com.example.myapplication.ui.screens.bill

import com.example.myapplication.model.OrderBatch
import com.example.myapplication.utils.SizeUtils
import java.math.BigDecimal
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal val billZone: ZoneId = ZoneId.of("Asia/Shanghai")
internal val billDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
internal val billSummaryDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM.dd")
internal val billDateLineFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MM-dd").withZone(billZone)
internal val billTimeLineFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm").withZone(billZone)
internal const val HIDE_BILL_HINT = 0

internal data class BillTotals(
    val income: BigDecimal,
    val cost: BigDecimal
) {
    val profit: BigDecimal
        get() = income.subtract(cost)
}

internal fun billOrderTotal(order: OrderBatch): BigDecimal =
    order.price.multiply(BigDecimal(SizeUtils.sizesOf(order).sum()))

internal fun calculateBillTotals(orders: List<OrderBatch>): BillTotals {
    var income = BigDecimal.ZERO
    var cost = BigDecimal.ZERO
    orders.forEach { order ->
        if (order.orderType.increaseStock) {
            cost = cost.add(billOrderTotal(order))
        } else {
            income = income.add(billOrderTotal(order))
        }
    }
    return BillTotals(income, cost)
}

internal fun billAmountText(amount: BigDecimal): String =
    amount.stripTrailingZeros().toPlainString()
