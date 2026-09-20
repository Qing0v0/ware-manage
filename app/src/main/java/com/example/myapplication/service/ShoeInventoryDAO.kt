package com.example.myapplication.service

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.myapplication.model.Color
import com.example.myapplication.model.ShoeInventory

@Dao
interface ShoeInventoryDAO {

    /** 查某一个货号 + 颜色的存量，没有就返回 null */
    @Query("SELECT * FROM inventory WHERE articleId = :articleId AND color = :color LIMIT 1")
    fun query(articleId: String, color: Color): ShoeInventory?

    /** 查所有存量（后面仓库页面显示存量用） */
    @Query("SELECT * FROM inventory ORDER BY articleId ASC, color ASC")
    fun queryAll(): List<ShoeInventory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(inventory: ShoeInventory): Long

    @Update
    fun update(inventory: ShoeInventory)
}
