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
     * 按时间范围查订单，新的排在前面（账单页用）。
     * date 列存的是时间戳（毫秒），所以直接和 Long 比；startTime / endTime 都包含在内。
     */
    @Query("SELECT * from `order` WHERE date BETWEEN :startTime AND :endTime ORDER BY date DESC")
    fun queryByDateRange(startTime: Long, endTime: Long): List<OrderBatch>
}