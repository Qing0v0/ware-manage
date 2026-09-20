package com.example.myapplication.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.myapplication.utils.DataConvertor
import java.math.BigDecimal
import java.util.Date

/*
param:
    - articleId: 货号
    - articleName: 货名
    - color: 鞋子颜色
    - size xx: 尺寸为xx码的鞋子数量
    - orderType: 出库还是入库
    - dealer: 经销商
    - price: 出库时记录总利润，入库时为单价
 */
@Entity(tableName = "order")
@TypeConverters(DataConvertor::class)
data class OrderBatch(
    @PrimaryKey(autoGenerate = true) var orderId: Int = 0,
    var articleId: String,
    var articleName: String,

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

    var orderType: OrderType,
    var dealer: String,
    var price: BigDecimal,
    var date: Date,
)
