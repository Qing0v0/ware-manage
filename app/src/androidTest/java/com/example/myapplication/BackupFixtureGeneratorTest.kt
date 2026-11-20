package com.example.myapplication

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 运行测试文件，在 /storage/emulated/0/Download/WareManageTestDebug 路劲下生成测试备份文件
 * 测试文件包括 version 1 / 2 版本数据库备份文件、非法备份文件
 * Android Studio查看文件目录 Views/Tool Windows/Device Explorer
*/
@RunWith(AndroidJUnit4::class)
class BackupFixtureGeneratorTest {

    @Test
    fun generateInvalidV1AndV2BackupFiles() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val outputContext = instrumentation.targetContext
        val outputDirectory = outputContext.getExternalFilesDir("backup-migration-fixtures")
            ?: error("External app files directory is unavailable")
        val sourceDirectory = File(outputContext.cacheDir, "backup-migration-fixtures")
        checkDirectory(outputDirectory)
        checkDirectory(sourceDirectory)
        clearDirectory(outputDirectory)
        clearDirectory(sourceDirectory)
        clearDownloadsFolder(outputContext)

        val invalidBackup = File(outputDirectory, "invalid-backup.zip")
        val v1Backup = File(outputDirectory, "version-1-backup.zip")
        val v2Backup = File(outputDirectory, "version-2-backup.zip")

        writeBackup(invalidBackup) { zip ->
            zip.writeEntry("backup_info.txt", backupInfo())
            zip.writeEntry(
                "databases/inventory_database",
                "not a SQLite database".toByteArray(Charsets.UTF_8)
            )
        }

        val v1Inventory = File(sourceDirectory, "v1-inventory_database")
        val v1LegacyOrders = File(sourceDirectory, "v1-app_database")
        createInventoryDatabase(v1Inventory, version = 1, includeOrders = false)
        createLegacyOrderDatabase(v1LegacyOrders)
        writeBackup(v1Backup) { zip ->
            zip.writeEntry("backup_info.txt", backupInfo())
            zip.writeFileEntry("databases/inventory_database", v1Inventory)
            zip.writeFileEntry("databases/app_database", v1LegacyOrders)
        }

        val v2Inventory = File(sourceDirectory, "v2-inventory_database")
        createInventoryDatabase(v2Inventory, version = 2, includeOrders = true)
        writeBackup(v2Backup) { zip ->
            zip.writeEntry("backup_info.txt", backupInfo())
            zip.writeFileEntry("databases/inventory_database", v2Inventory)
        }

        listOf(invalidBackup, v1Backup, v2Backup).forEach { file ->
            assertTrue("Backup file was not generated: ${file.absolutePath}", file.isFile)
            assertTrue("Backup file is empty: ${file.absolutePath}", file.length() > 0)
            publishToDownloads(outputContext, file)
        }
        Log.i(
            TAG,
            "Generated backup fixtures in ${outputDirectory.absolutePath}" +
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    " and Download/$DOWNLOAD_SUBDIRECTORY"
                } else {
                    ""
                }
        )
    }

    private fun createInventoryDatabase(file: File, version: Int, includeOrders: Boolean) {
        removeDatabaseFiles(file)
        SQLiteDatabase.openOrCreateDatabase(file, null).use { database ->
            database.execSQL(
                "CREATE TABLE `inventory` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`articleId` TEXT NOT NULL, `dealer` TEXT NOT NULL, " +
                    "`color` TEXT NOT NULL, `imagePath` TEXT, " +
                    "`size34` INTEGER NOT NULL, `size35` INTEGER NOT NULL, " +
                    "`size36` INTEGER NOT NULL, `size37` INTEGER NOT NULL, " +
                    "`size38` INTEGER NOT NULL, `size39` INTEGER NOT NULL, " +
                    "`size40` INTEGER NOT NULL, `size41` INTEGER NOT NULL, " +
                    "`size42` INTEGER NOT NULL, `size43` INTEGER NOT NULL, " +
                    "`size44` INTEGER NOT NULL" +
                    (if (version >= 3) ", `purchase_price` TEXT" else "") +
                    ")"
            )
            database.execSQL(
                "CREATE UNIQUE INDEX `index_inventory_articleId_color` " +
                    "ON `inventory` (`articleId`, `color`)"
            )
            if (includeOrders) {
                createOrderTable(database)
            }
            createRoomMasterTable(
                database,
                if (version == 1) V1_IDENTITY_HASH else V2_IDENTITY_HASH
            )
            insertInventoryRows(database, includePurchasePrice = version >= 3)
            if (includeOrders) {
                insertOrderRows(database)
            }
            database.version = version
        }
    }

    private fun createLegacyOrderDatabase(file: File) {
        removeDatabaseFiles(file)
        SQLiteDatabase.openOrCreateDatabase(file, null).use { database ->
            createOrderTable(database)
            createRoomMasterTable(database, LEGACY_ORDER_IDENTITY_HASH)
            insertOrderRows(database)
            database.version = 1
        }
    }

    private fun createOrderTable(database: SQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE `order` (" +
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
    }

    private fun createRoomMasterTable(database: SQLiteDatabase, identityHash: String) {
        database.execSQL(
            "CREATE TABLE room_master_table " +
                "(id INTEGER PRIMARY KEY, identity_hash TEXT)"
        )
        database.execSQL(
            "INSERT INTO room_master_table (id, identity_hash) VALUES (42, ?)",
            arrayOf(identityHash)
        )
    }

    private fun insertInventoryRows(database: SQLiteDatabase, includePurchasePrice: Boolean) {
        repeat(ROW_COUNT) { index ->
            val values = ContentValues().apply {
                put("id", index + 1)
                put("articleId", "TEST-${index + 1}")
                put("dealer", "测试经销商")
                put("color", "BLACK")
                putNull("imagePath")
                for (size in 34..44) {
                    put("size$size", if (size == 34) index + 1 else 0)
                }
                if (includePurchasePrice) {
                    putNull("purchase_price")
                }
            }
            database.insertOrThrow("inventory", null, values)
        }
    }

    private fun insertOrderRows(database: SQLiteDatabase) {
        repeat(ROW_COUNT) { index ->
            val values = ContentValues().apply {
                put("orderId", index + 1)
                put("articleId", "TEST-${index + 1}")
                put("color", "BLACK")
                for (size in 34..44) {
                    put("size$size", if (size == 34) 1 else 0)
                }
                put("orderType", "ARTICLE_PURCHASE")
                put("dealer", "测试经销商")
                put("price", (40 + index).toString())
                put("date", 1_700_000_000_000L + index)
            }
            database.insertOrThrow("`order`", null, values)
        }
    }

    private fun writeBackup(file: File, buildEntries: (ZipOutputStream) -> Unit) {
        file.parentFile?.let(::checkDirectory)
        ZipOutputStream(file.outputStream().buffered()).use(buildEntries)
    }

    private fun publishToDownloads(context: android.content.Context, file: File) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return
        }

        val resolver = context.contentResolver
        val relativePath =
            "${Environment.DIRECTORY_DOWNLOADS}/$DOWNLOAD_SUBDIRECTORY/"
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, file.name)
            put(MediaStore.Downloads.MIME_TYPE, "application/zip")
            put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("Unable to create ${file.name} in Downloads")
        try {
            val output = resolver.openOutputStream(uri)
                ?: error("Unable to open ${file.name} in Downloads")
            output.use { stream ->
                file.inputStream().use { it.copyTo(stream) }
            }
            val publishedValues = ContentValues().apply {
                put(MediaStore.Downloads.IS_PENDING, 0)
            }
            check(resolver.update(uri, publishedValues, null, null) == 1) {
                "Unable to publish ${file.name} in Downloads"
            }
        } catch (error: Exception) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    private fun clearDownloadsFolder(context: android.content.Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return
        }

        val resolver = context.contentResolver
        val relativePath =
            "${Environment.DIRECTORY_DOWNLOADS}/$DOWNLOAD_SUBDIRECTORY/"
        val uris = ArrayList<android.net.Uri>()
        resolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Downloads._ID),
            "${MediaStore.Downloads.RELATIVE_PATH} = ?",
            arrayOf(relativePath),
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                uris.add(
                    android.content.ContentUris.withAppendedId(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        cursor.getLong(0)
                    )
                )
            }
        }
        uris.forEach { uri ->
            check(resolver.delete(uri, null, null) == 1) {
                "Unable to clear generated backup folder"
            }
        }
    }

    private fun ZipOutputStream.writeEntry(name: String, contents: ByteArray) {
        putNextEntry(ZipEntry(name))
        write(contents)
        closeEntry()
    }

    private fun ZipOutputStream.writeFileEntry(name: String, source: File) {
        putNextEntry(ZipEntry(name))
        source.inputStream().use { it.copyTo(this) }
        closeEntry()
    }

    private fun backupInfo(): ByteArray =
        "shoe_warehouse_backup\nformatVersion=1\nexportTime=test\n"
            .toByteArray(Charsets.UTF_8)

    private fun removeDatabaseFiles(file: File) {
        listOf(file, File("${file.path}-wal"), File("${file.path}-shm"), File("${file.path}-journal"))
            .forEach { if (it.exists() && !it.delete()) error("Unable to remove $it") }
    }

    private fun checkDirectory(directory: File) {
        if (!directory.isDirectory && !directory.mkdirs()) {
            error("Unable to create directory: $directory")
        }
    }

    private fun clearDirectory(directory: File) {
        directory.listFiles().orEmpty().forEach { child ->
            check(child.deleteRecursively()) {
                "Unable to clear generated fixture: $child"
            }
        }
    }

    companion object {
        private const val TAG = "BackupFixtureTest"
        private const val ROW_COUNT = 10
        private const val DOWNLOAD_SUBDIRECTORY = "WareManageTestDebug"
        private const val V1_IDENTITY_HASH = "68e74783028e458016f6bdfe1a2a1a4b"
        private const val V2_IDENTITY_HASH = "e213d9d2968752bf005c18b51b4222fd"
        private const val LEGACY_ORDER_IDENTITY_HASH = "bf1d76192ac43c1d8e6d7cc0256751b5"
    }
}
