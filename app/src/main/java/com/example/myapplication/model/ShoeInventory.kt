package com.example.myapplication.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 存量：一种「货号 + 颜色」的鞋，34~44 码各有多少双。
 *
 * 一个货号的一个颜色只有一条记录，入库累加、出库减少都改这一条；
 * 查询用的就是 [articleId] + [color] 这个唯一索引。
 */
@Entity(
    tableName = "inventory",
    indices = [Index(value = ["articleId", "color"], unique = true)]
)
data class ShoeInventory(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    var articleId: String,
    var articleName: String,
    var dealer: String,

    var color: Color,
    var size34: Int = 0,
    var size35: Int = 0,
    var size36: Int = 0,
    var size37: Int = 0,
    var size38: Int = 0,
    var size39: Int = 0,
    var size40: Int = 0,
    var size41: Int = 0,
    var size42: Int = 0,
    var size43: Int = 0,
    var size44: Int = 0,
)
