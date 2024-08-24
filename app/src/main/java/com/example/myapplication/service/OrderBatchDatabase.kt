package com.example.myapplication.service

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.myapplication.model.OrderBatch

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
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}