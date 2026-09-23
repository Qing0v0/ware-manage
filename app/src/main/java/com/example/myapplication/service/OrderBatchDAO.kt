package com.example.myapplication.service

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.model.OrderBatch

@Dao
interface OrderBatchDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(orderBatch: OrderBatch): Long

    @Query("SELECT * from `order`")
    fun query(): List<OrderBatch>

    /**
     * 账单页查询：时间范围（含首尾）+ 货号 / 经销商模糊匹配 + 类型筛选，新的排在前面。
     * articleIdLike / dealerLike 传 "%关键词%"；orderTypes 传枚举名列表，不能是空列表
     * （SQLite 不接受空的 IN ()）。
     */
    @Query(
        "SELECT * from `order` WHERE date BETWEEN :startTime AND :endTime " +
            "AND articleId LIKE :articleIdLike AND dealer LIKE :dealerLike " +
            "AND orderType IN (:orderTypes) ORDER BY date DESC"
    )
    fun queryOrders(
        startTime: Long,
        endTime: Long,
        articleIdLike: String,
        dealerLike: String,
        orderTypes: List<String>
    ): List<OrderBatch>
}