package com.example.myapplication.model

import java.math.BigDecimal
import java.util.Date

data class Order(
    val orderId: Int,
    val articleId: Int,
    val sellingDate: Date,
    val sellingType: SellingType,
    val sellingPrice: BigDecimal,
)
