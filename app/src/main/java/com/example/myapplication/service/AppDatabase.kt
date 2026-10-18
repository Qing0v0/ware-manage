package com.example.myapplication.service

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteStatement
import com.example.myapplication.model.OrderBatch
import com.example.myapplication.model.ShoeInventory

@Database(entities = [OrderBatch::class, ShoeInventory::class], version = 2)
abstract class InventoryDatabase: RoomDatabase() {
    abstract fun orderBatchDAO(): OrderBatchDAO
    abstract fun shoeInventoryDAO(): ShoeInventoryDAO

    companion object {
        @Volatile
        private var INSTANCE: InventoryDatabase? = null

        /** 迁移旧数据，从两个数据库改为一个数据库两张表 */
        private fun migrationFromV1(context: Context) = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `order` (" +
                        "`orderId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`articleId` TEXT NOT NULL, `color` TEXT NOT NULL, " +
                        "`size34` INTEGER NOT NULL, `size35` INTEGER NOT NULL, " +
                        "`size36` INTEGER NOT NULL, `size37` INTEGER NOT NULL, " +
                        "`size38` INTEGER NOT NULL, `size39` INTEGER NOT NULL, " +
                        "`size40` INTEGER NOT NULL, `size41` INTEGER NOT NULL, " +
                        "`size42` INTEGER NOT NULL, `size43` INTEGER NOT NULL, " +
                        "`size44` INTEGER NOT NULL, `orderType` TEXT NOT NULL, " +
                        "`dealer` TEXT NOT NULL, `price` TEXT NOT NULL, `date` INTEGER NOT NULL)"
                )

                importLegacyOrders(context, database)
                database.execSQL(
                    "UPDATE `order` SET dealer = (" +
                        "SELECT inventory.dealer FROM inventory " +
                        "WHERE inventory.articleId = `order`.articleId " +
                        "AND TRIM(inventory.dealer) != '' " +
                        "ORDER BY inventory.id ASC LIMIT 1" +
                        ") WHERE TRIM(dealer) = '' " +
                        "AND EXISTS (" +
                        "SELECT 1 FROM inventory " +
                        "WHERE inventory.articleId = `order`.articleId " +
                        "AND TRIM(inventory.dealer) != ''" +
                        ")"
                )
            }
        }

        private fun importLegacyOrders(context: Context, database: SupportSQLiteDatabase) {
            val legacyOrdersFile = context.getDatabasePath("app_database")
            if (!legacyOrdersFile.exists()) {
                return
            }

            SQLiteDatabase.openDatabase(
                legacyOrdersFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            ).use { legacyOrders ->
                val hasOrdersTable = legacyOrders.rawQuery(
                    "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = 'order'",
                    null
                ).use { it.moveToFirst() }
                if (hasOrdersTable) {
                    copyLegacyOrders(legacyOrders, database)
                }
            }
        }

        private fun copyLegacyOrders(
            legacyDatabase: SQLiteDatabase,
            database: SupportSQLiteDatabase
        ) {
            val columns = arrayOf(
                "orderId", "articleId", "color", "size34", "size35", "size36",
                "size37", "size38", "size39", "size40", "size41", "size42",
                "size43", "size44", "orderType", "dealer", "price", "date"
            )
            val placeholders = columns.joinToString(",") { "?" }
            val columnNames = columns.joinToString(",") { "`$it`" }
            val insert = database.compileStatement(
                "INSERT INTO `order` ($columnNames) VALUES ($placeholders)"
            )
            val query = "SELECT $columnNames FROM `order`"

            legacyDatabase.rawQuery(query, null).use { cursor ->
                while (cursor.moveToNext()) {
                    insert.clearBindings()
                    bindRow(cursor, insert)
                    insert.executeInsert()
                }
            }
        }

        private fun bindRow(cursor: Cursor, statement: SupportSQLiteStatement) {
            for (index in 0 until cursor.columnCount) {
                when (cursor.getType(index)) {
                    Cursor.FIELD_TYPE_NULL -> statement.bindNull(index + 1)
                    Cursor.FIELD_TYPE_INTEGER -> statement.bindLong(index + 1, cursor.getLong(index))
                    Cursor.FIELD_TYPE_FLOAT -> statement.bindDouble(index + 1, cursor.getDouble(index))
                    Cursor.FIELD_TYPE_STRING -> statement.bindString(index + 1, cursor.getString(index))
                    Cursor.FIELD_TYPE_BLOB -> statement.bindBlob(index + 1, cursor.getBlob(index))
                }
            }
        }

        /** 备份 / 恢复数据前先把连接关掉，之后再用会自动重开 */
        fun closeDatabase() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }

        fun getDatabase(context: Context): InventoryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    InventoryDatabase::class.java,
                    "inventory_database"
                )
                    .addMigrations(migrationFromV1(context.applicationContext))
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            importLegacyOrders(context.applicationContext, db)
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}