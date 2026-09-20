package com.example.myapplication.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.math.BigDecimal

data class ShoeInventory(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    var articleId: String,
    var articleName: String,
    var dealer: String,

    var color: Color,
    var size34: Int,
    var size35: Int,
    var size36: Int,
    var size37: Int,
    var size38: Int,
    var size39: Int,
    var size40: Int,
    var size41: Int,
    var size42: Int,
    var size43: Int,
    var size44: Int,
)
