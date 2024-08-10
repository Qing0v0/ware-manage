package com.example.myapplication.model

import java.math.BigDecimal
import java.util.Date

data class Shoe(
    val articleId: Int,
    val articleNo: Int,
    val articleName: String,
    val size: Int,
    val color: Color,
    val purchasePrice: BigDecimal,
    val sellingPrice: BigDecimal,
    val purchaseDate: Date,
    val sellingDate: Date,
    val dealer: String,
    val isInWare: Boolean,
)
