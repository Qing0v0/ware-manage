package com.example.myapplication.model

/*
入库：  ARTICLE_PURCHASE                                        加存量
卖出：  ARTICLE_SOLD_ALIPAY / ARTICLE_SOLD_WX / ARTICLE_SOLD_CASH   按收款方式分，减存量
退货：  ARTICLE_RETURN                                          退给经销商，减存量

displayName：界面上显示的文字（出库页"支付方式"下拉、账单页的标签都用它）
increaseStock = true 表示往存量里加（只有入库），false 表示减

ARTICLE_SOLD 是老版本留下的"出库"（没记收款方式），界面上已经不会再生成它，
留着只是为了能读出以前存的老订单。
 */
enum class OrderType(val displayName: String, val increaseStock: Boolean) {
    ARTICLE_PURCHASE("入库", true),

    ARTICLE_SOLD_ALIPAY("支付宝", false),
    ARTICLE_SOLD_WX("微信", false),
    ARTICLE_SOLD_CASH("现金", false),
    ARTICLE_RETURN("退货", false),

    /** 老数据兼容用 */
    ARTICLE_SOLD("出库", false),
    ;

    companion object {
        /** 出库页面能选的四项：三个收款方式 + 退货 */
        val soldTypes: List<OrderType> = listOf(
            ARTICLE_SOLD_ALIPAY,
            ARTICLE_SOLD_WX,
            ARTICLE_SOLD_CASH,
            ARTICLE_RETURN
        )

        /** 下拉框里拿到的是界面文字，这里按文字找回枚举 */
        fun matchDisplayName(displayName: String): OrderType? {
            return values().firstOrNull { it.displayName == displayName }
        }
    }
}
