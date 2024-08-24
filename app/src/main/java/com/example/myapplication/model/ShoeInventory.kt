package com.example.myapplication.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.math.BigDecimal

data class ShoeInventory(
    var id: Int,
    var articleId: String,
    var articleName: String,

    var color: Color,
    var size35: Int,
    var size36: Int,
    var size37: Int,
    var size38: Int,
    var size39: Int,
    var size40: Int,
    var size41: Int,
    var size42: Int,
    var size43: Int,

    var sellingPrice: BigDecimal,
)
