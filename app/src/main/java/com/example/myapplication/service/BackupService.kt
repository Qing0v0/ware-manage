package com.example.myapplication.service

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.example.myapplication.R
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 数据备份 / 恢复。
 *
 * 备份内容：合并后的数据库 inventory_database、旧版订单库 app_database（兼容旧备份）+
 * 存量的图片目录，
 * 打成一个 zip，结构和手机上的目录对应：
 *     backup_info.txt                 标记文件（格式版本 + 导出时间），用来判断是不是本应用的备份
 *     databases/app_database          旧版订单库（含 -wal / -shm）
 *     databases/inventory_database    订单和存量共用的数据库
 *     images/img_xxx.jpg              存量图片
 *
 * 导出走系统文件选择器，存到用户自己挑的位置（不需要存储权限）；导入是"整体覆盖"：
 * 先解压并校验备份，再备份当前文件；替换或数据库迁移失败时恢复原文件。
 */
class BackupService(private val context: Context) {

    /** 导出到外面给的输出流（一般是用户在文件选择器里选的备份文件） */
    fun exportTo(output: OutputStream): Boolean {
        return try {
            // 先把 WAL 里还没落盘的数据合并进主库文件，拷出来才完整
            checkpoint()

            ZipOutputStream(output).use { zip ->
                writeTextEntry(zip, BACKUP_INFO, buildBackupInfo())

                DATABASE_NAMES.forEach { name ->
                    val databaseFile = context.getDatabasePath(name)
                    addFileIfExists(zip, databaseFile, "$DATABASE_DIR/${databaseFile.name}")
                    addFileIfExists(
                        zip, File("${databaseFile.path}-wal"),
                        "$DATABASE_DIR/${databaseFile.name}-wal"
                    )
                    addFileIfExists(
                        zip, File("${databaseFile.path}-shm"),
                        "$DATABASE_DIR/${databaseFile.name}-shm"
                    )
                }

                imageDir().listFiles()?.forEach { image ->
                    addFileIfExists(zip, image, "$IMAGE_DIR/${image.name}")
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /** 用备份文件覆盖当前数据 */
    fun restoreFrom(input: InputStream): Boolean {
        val tempDir = File(context.cacheDir, TEMP_DIR)
        return try {
            if (tempDir.exists() && !tempDir.deleteRecursively()) {
                throw IOException(context.getString(R.string.backup_error_temp_cleanup))
            }
            if (!tempDir.mkdirs()) {
                throw IOException(context.getString(R.string.backup_error_temp_create))
            }

            // 1. 先解到临时目录，并检查备份格式和数据库完整性
            var backupInfo: String? = null
            val extractedEntries = HashSet<String>()
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (!entry.isDirectory && name == BACKUP_INFO) {
                        if (backupInfo != null) {
                            throw IOException(context.getString(R.string.backup_error_duplicate_info))
                        }
                        backupInfo = readBackupInfo(zip)
                    } else if (!entry.isDirectory && isWantedEntry(name)) {
                        if (!extractedEntries.add(name)) {
                            throw IOException(context.getString(R.string.backup_error_duplicate_file, name))
                        }
                        val target = File(tempDir, name)
                        val parent = target.parentFile
                            ?: throw IOException(
                                context.getString(R.string.backup_error_invalid_file_path, name)
                            )
                        if (!parent.exists() && !parent.mkdirs()) {
                            throw IOException(context.getString(R.string.backup_error_temp_create))
                        }
                        target.outputStream().use { outputStream -> zip.copyTo(outputStream) }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            validateBackup(tempDir, backupInfo)

            val databaseDir = context.getDatabasePath(DATABASE_NAMES[0]).parentFile
                ?: throw IOException(context.getString(R.string.backup_error_database_dir_invalid))
            val previousDir = File(tempDir, PREVIOUS_DIR)
            InventoryDatabase.closeDatabase()
            snapshotCurrentData(databaseDir, previousDir)

            try {
                replaceCurrentData(tempDir, databaseDir)
                validateInstalledDatabase()
                fixImagePaths()
            } catch (restoreError: Exception) {
                try {
                    InventoryDatabase.closeDatabase()
                    restoreSnapshot(previousDir, databaseDir)
                } catch (rollbackError: Exception) {
                    Log.e(
                        LOG_TAG,
                        context.getString(R.string.backup_error_restore_rollback),
                        rollbackError
                    )
                    restoreError.addSuppressed(rollbackError)
                }
                throw restoreError
            }

            true
        } catch (e: Exception) {
            Log.e(LOG_TAG, context.getString(R.string.backup_error_restore), e)
            false
        } finally {
            if (tempDir.exists() && !tempDir.deleteRecursively()) {
                Log.w(LOG_TAG, context.getString(R.string.backup_error_temp_cleanup))
            }
        }
    }

    private fun readBackupInfo(zip: ZipInputStream): String {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(256)
        while (true) {
            val count = zip.read(buffer)
            if (count < 0) break
            if (output.size() + count > MAX_BACKUP_INFO_BYTES) {
                throw IOException(context.getString(R.string.backup_error_marker_too_large))
            }
            output.write(buffer, 0, count)
        }
        return output.toString(Charsets.UTF_8.name())
    }

    private fun validateBackup(tempDir: File, backupInfo: String?) {
        val infoLines = backupInfo?.lineSequence()?.toList().orEmpty()
        if (infoLines.firstOrNull() != BACKUP_SIGNATURE) {
            throw IOException(context.getString(R.string.backup_error_marker_invalid))
        }
        val formatVersion = infoLines
            .firstOrNull { it.startsWith("formatVersion=") }
            ?.substringAfter("=")
            ?.toIntOrNull()
        if (formatVersion != FORMAT_VERSION) {
            throw IOException(
                context.getString(
                    R.string.backup_error_unsupported_format_version,
                    formatVersion?.toString()
                        ?: context.getString(R.string.backup_error_unknown_version)
                )
            )
        }

        val databaseDir = File(tempDir, DATABASE_DIR)
        val inventoryFile = File(databaseDir, "inventory_database")
        val legacyOrdersFile = File(databaseDir, "app_database")
        if (!inventoryFile.isFile && !legacyOrdersFile.isFile) {
            throw IOException(context.getString(R.string.backup_error_missing_database_files))
        }
        if (inventoryFile.isFile) {
            validateInventoryDatabase(inventoryFile)
        }
        if (legacyOrdersFile.isFile) {
            validateLegacyOrdersDatabase(legacyOrdersFile)
        }
    }

    private fun validateInventoryDatabase(file: File) {
        SQLiteDatabase.openDatabase(
            file.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY
        ).use { database ->
            requireIntegrityCheck(database)
            val version = database.version
            if (version !in MIN_SUPPORTED_DATABASE_VERSION..INVENTORY_DATABASE_VERSION) {
                throw IOException(
                    context.getString(R.string.backup_error_unsupported_database_version, version)
                )
            }
            if (!hasTable(database, "inventory")) {
                throw IOException(context.getString(R.string.backup_error_missing_inventory_table))
            }
            if (version >= 2 && !hasTable(database, "order")) {
                throw IOException(context.getString(R.string.backup_error_missing_order_table))
            }
        }
    }

    private fun validateLegacyOrdersDatabase(file: File) {
        SQLiteDatabase.openDatabase(
            file.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY
        ).use { database ->
            requireIntegrityCheck(database)
            if (!hasTable(database, "order")) {
                throw IOException(
                    context.getString(R.string.backup_error_missing_legacy_order_table)
                )
            }
        }
    }

    private fun requireIntegrityCheck(database: SQLiteDatabase) {
        database.rawQuery("PRAGMA integrity_check", null).use { cursor ->
            if (!cursor.moveToFirst() || cursor.getString(0) != "ok" || cursor.moveToNext()) {
                throw IOException(context.getString(R.string.backup_error_integrity_check))
            }
        }
    }

    private fun hasTable(database: SQLiteDatabase, tableName: String): Boolean {
        return database.rawQuery(
            "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?",
            arrayOf(tableName)
        ).use { it.moveToFirst() }
    }

    private fun snapshotCurrentData(databaseDir: File, snapshotDir: File) {
        val snapshotDatabaseDir = File(snapshotDir, DATABASE_DIR)
        if (!snapshotDatabaseDir.mkdirs()) {
            throw IOException(context.getString(R.string.backup_error_snapshot_dir_create))
        }
        databaseFileNames().forEach { name ->
            val current = File(databaseDir, name)
            if (current.isFile) {
                current.copyTo(File(snapshotDatabaseDir, name), overwrite = true)
            }
        }

        val currentImages = imageDir()
        if (currentImages.exists() &&
            !currentImages.copyRecursively(File(snapshotDir, IMAGE_DIR), overwrite = true)
        ) {
            throw IOException(context.getString(R.string.backup_error_snapshot_images))
        }
    }

    private fun replaceCurrentData(tempDir: File, databaseDir: File) {
        if (!databaseDir.exists() && !databaseDir.mkdirs()) {
            throw IOException(context.getString(R.string.backup_error_database_dir_create))
        }
        clearDatabaseFiles(databaseDir)

        val stagedDatabaseDir = File(tempDir, DATABASE_DIR)
        stagedDatabaseDir.listFiles()?.forEach { stagedFile ->
            stagedFile.copyTo(File(databaseDir, stagedFile.name), overwrite = true)
        }

        replaceImageDirectory(File(tempDir, IMAGE_DIR))
    }

    private fun restoreSnapshot(snapshotDir: File, databaseDir: File) {
        if (!databaseDir.exists() && !databaseDir.mkdirs()) {
            throw IOException(context.getString(R.string.backup_error_database_dir_restore))
        }
        clearDatabaseFiles(databaseDir)

        File(snapshotDir, DATABASE_DIR).listFiles()?.forEach { previousFile ->
            previousFile.copyTo(File(databaseDir, previousFile.name), overwrite = true)
        }

        replaceImageDirectory(File(snapshotDir, IMAGE_DIR))
    }

    private fun clearDatabaseFiles(databaseDir: File) {
        databaseFileNames().forEach { name ->
            val file = File(databaseDir, name)
            if (file.exists() && !file.delete()) {
                throw IOException(context.getString(R.string.backup_error_database_file_replace, name))
            }
        }
    }

    private fun replaceImageDirectory(source: File) {
        val target = imageDir()
        if (target.exists() && !target.deleteRecursively()) {
            throw IOException(context.getString(R.string.backup_error_image_dir_replace))
        }
        if (!target.mkdirs()) {
            throw IOException(context.getString(R.string.backup_error_image_dir_create))
        }
        if (source.isDirectory &&
            !source.copyRecursively(target, overwrite = true)
        ) {
            throw IOException(context.getString(R.string.backup_error_images_copy))
        }
    }

    private fun validateInstalledDatabase() {
        val database = InventoryDatabase.getDatabase(context)
        database.openHelper.writableDatabase
        database.shoeInventoryDAO().queryAll()
        database.orderBatchDAO().query()
    }

    /** PRAGMA wal_checkpoint(FULL)：把 WAL 里没落盘的数据合并进主库文件 */
    private fun checkpoint() {
        InventoryDatabase.getDatabase(context).query("PRAGMA wal_checkpoint(FULL)", null)?.close()
    }

    /** 备份里的图片路径可能是别的设备 / 包名的绝对路径，按文件名在当前机器上找回来 */
    private fun fixImagePaths() {
        val inventoryDAO = InventoryDatabase.getDatabase(context).shoeInventoryDAO()
        inventoryDAO.queryAll().forEach { inventory ->
            val oldPath = inventory.imagePath
            if (oldPath.isNullOrEmpty() || File(oldPath).exists()) {
                return@forEach
            }
            val restored = File(imageDir(), File(oldPath).name)
            if (restored.exists()) {
                inventory.imagePath = restored.absolutePath
                inventoryDAO.update(inventory)
            }
        }
    }

    private fun isWantedEntry(name: String): Boolean {
        if (name.startsWith("$DATABASE_DIR/")) {
            return name.substringAfter("$DATABASE_DIR/") in databaseFileNames()
        }
        if (!name.startsWith("$IMAGE_DIR/")) {
            return false
        }
        val imageName = name.substringAfter("$IMAGE_DIR/")
        return imageName.isNotBlank() &&
            imageName != "." &&
            imageName != ".." &&
            '/' !in imageName &&
            '\\' !in imageName
    }

    private fun databaseFileNames(): Set<String> = DATABASE_NAMES.flatMap { name ->
        listOf(name, "$name-wal", "$name-shm", "$name-journal")
    }.toSet()

    private fun addFileIfExists(zip: ZipOutputStream, file: File, entryName: String) {
        if (!file.exists() || !file.isFile) {
            return
        }
        zip.putNextEntry(ZipEntry(entryName))
        file.inputStream().use { inputStream -> inputStream.copyTo(zip) }
        zip.closeEntry()
    }

    private fun writeTextEntry(zip: ZipOutputStream, entryName: String, text: String) {
        zip.putNextEntry(ZipEntry(entryName))
        zip.write(text.toByteArray())
        zip.closeEntry()
    }

    private fun buildBackupInfo(): String {
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        return "shoe_warehouse_backup\nformatVersion=$FORMAT_VERSION\nexportTime=$time\n"
    }

    private fun imageDir(): File = File(context.filesDir, IMAGE_DIR_NAME)

    companion object {
        private const val LOG_TAG = "BackupService"
        private const val BACKUP_INFO = "backup_info.txt"
        private const val BACKUP_SIGNATURE = "shoe_warehouse_backup"
        private const val DATABASE_DIR = "databases"
        private const val IMAGE_DIR = "images"
        private const val TEMP_DIR = "restore_temp"
        private const val PREVIOUS_DIR = "previous"
        private const val FORMAT_VERSION = 1
        private const val MAX_BACKUP_INFO_BYTES = 4096
        private const val MIN_SUPPORTED_DATABASE_VERSION = 1

        /** 图片目录名，和 ImageUtils 里保持一致 */
        private const val IMAGE_DIR_NAME = "inventory_images"

        /** 要备份的两个数据库文件名 */
        private val DATABASE_NAMES = arrayOf("app_database", "inventory_database")

        /** 默认的备份文件名，比如 鞋子仓库备份_2026-09-22.zip */
        fun suggestedFileName(context: Context): String {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            return context.getString(R.string.backup_suggested_file_name, date)
        }
    }
}
