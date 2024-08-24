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
}