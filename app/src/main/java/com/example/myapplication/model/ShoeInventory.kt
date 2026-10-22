package com.example.myapplication.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.myapplication.utils.DataConvertor
import java.math.BigDecimal

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
@TypeConverters(DataConvertor::class)
data class ShoeInventory(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    var articleId: String,
    var dealer: String,

    var color: Color,
    /** 图片存在本地文件的路径（filesDir/inventory_images/xxx.jpg），没图就是 null；只存在存量库，订单库不存 */
    var imagePath: String? = null,
    /** 进价（从入库账单中补全），单位：元，Room 内部以文本存储，避免 Double 精度问题 */
    @ColumnInfo(name = "purchase_price") var purchasePrice: BigDecimal? = null,
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
