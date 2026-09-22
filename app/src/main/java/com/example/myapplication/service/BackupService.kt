package com.example.myapplication.service

import android.content.Context
import java.io.File
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
 * 备份内容：两个数据库（订单库 app_database、存量库 inventory_database）+ 存量的图片目录，
 * 打成一个 zip，结构和手机上的目录对应：
 *     backup_info.txt                 标记文件（格式版本 + 导出时间），用来判断是不是本应用的备份
 *     databases/app_database          订单库（含 -wal / -shm）
 *     databases/inventory_database    存量库
 *     images/img_xxx.jpg              存量图片
 *
 * 导出走系统文件选择器，存到用户自己挑的位置（不需要存储权限）；导入是"整体覆盖"：
 * 先解到临时目录校验标记文件，再关掉数据库连接、清掉旧数据，最后搬过去。
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
        return try {
            val tempDir = File(context.cacheDir, TEMP_DIR).apply {
                deleteRecursively()
                mkdirs()
            }

            // 1. 先解到临时目录，同时确认这是本应用导出的备份（带标记文件）
            var hasBackupInfo = false
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (name == BACKUP_INFO) {
                        hasBackupInfo = true
                    } else if (!entry.isDirectory && isWantedEntry(name)) {
                        val target = File(tempDir, name)
                        target.parentFile?.mkdirs()
                        target.outputStream().use { outputStream -> zip.copyTo(outputStream) }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            if (!hasBackupInfo) {
                tempDir.deleteRecursively()
                return false
            }

            // 2. 关掉数据库连接，清掉旧数据（-wal / -shm 也要删，否则可能被旧的日志污染）
            OrderBatchDatabase.closeDatabase()
            InventoryDatabase.closeDatabase()

            val databaseDir = context.getDatabasePath(DATABASE_NAMES[0]).parentFile
            DATABASE_NAMES.forEach { name ->
                File(databaseDir, name).delete()
                File(databaseDir, "$name-wal").delete()
                File(databaseDir, "$name-shm").delete()
            }
            imageDir().deleteRecursively()
            imageDir().mkdirs()

            // 3. 把临时目录里的东西搬到正式位置
            if (databaseDir != null) {
                File(tempDir, DATABASE_DIR).listFiles()?.forEach { file ->
                    file.copyTo(File(databaseDir, file.name), overwrite = true)
                }
            }
            File(tempDir, IMAGE_DIR).listFiles()?.forEach { file ->
                file.copyTo(File(imageDir(), file.name), overwrite = true)
            }
            tempDir.deleteRecursively()

            // 4. 图片存的是绝对路径，换了设备 / 包名就失效，这里按文件名找回来
            fixImagePaths()
            true
        } catch (e: Exception) {
            false
        }
    }

    /** PRAGMA wal_checkpoint(FULL)：把 WAL 里没落盘的数据合并进主库文件 */
    private fun checkpoint() {
        OrderBatchDatabase.getDatabase(context).query("PRAGMA wal_checkpoint(FULL)", null)?.close()
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
        return name.startsWith("$DATABASE_DIR/") || name.startsWith("$IMAGE_DIR/")
    }

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
        private const val BACKUP_INFO = "backup_info.txt"
        private const val DATABASE_DIR = "databases"
        private const val IMAGE_DIR = "images"
        private const val TEMP_DIR = "restore_temp"
        private const val FORMAT_VERSION = 1

        /** 图片目录名，和 ImageUtils 里保持一致 */
        private const val IMAGE_DIR_NAME = "inventory_images"

        /** 要备份的两个数据库文件名 */
        private val DATABASE_NAMES = arrayOf("app_database", "inventory_database")

        /** 默认的备份文件名，比如 鞋子仓库备份_2026-09-22.zip */
        fun suggestedFileName(): String {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            return "鞋子仓库备份_$date.zip"
        }
    }
}
