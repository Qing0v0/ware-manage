package com.example.myapplication.service

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.myapplication.model.OrderBatch
import com.example.myapplication.model.ShoeInventory

@Database(entities = [OrderBatch::class], version = 1)
abstract class OrderBatchDatabase: RoomDatabase() {
    abstract fun orderBatchDAO(): OrderBatchDAO

    companion object {
        @Volatile
        private var INSTANCE: OrderBatchDatabase? = null

        fun getDatabase(context: Context): OrderBatchDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OrderBatchDatabase::class.java,
                    "app_database"
                )
                    // 开发期：改了表结构就把版本号 +1，Room 会直接把旧库删掉重建（测试数据不要紧）
                    // TODO 上线前要换成真正的 Migration，不然用户升级会丢数据
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

@Database(entities = [ShoeInventory::class], version = 1)
abstract class InventoryDatabase: RoomDatabase() {
    abstract fun shoeInventoryDAO(): ShoeInventoryDAO

    companion object {
        @Volatile
        private var INSTANCE: InventoryDatabase? = null

        fun getDatabase(context: Context): InventoryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    InventoryDatabase::class.java,
                    "inventory_database"
                )
                    // 同上：开发期允许直接重建库
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}