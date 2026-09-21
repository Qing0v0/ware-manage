package com.example.myapplication.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/**
 * 图片的存和读。
 *
 * 图片本身不放进数据库（免得把库撑大），而是压缩后存到应用私有目录
 * filesDir/inventory_images/，数据库里只留文件路径（ShoeInventory.imagePath）。
 */
object ImageUtils {

    private const val IMAGE_DIR = "inventory_images"

    /** 存盘时最长边压到 800px，一张大概几十 KB */
    private const val MAX_SAVE_SIZE = 800

    /** 界面里显示缩略图时最长边取 320px 就够清楚了 */
    const val MAX_SHOW_SIZE = 320

    private const val JPEG_QUALITY = 85

    /** 把相册里选中的图片压缩存到应用私有目录，返回文件路径；失败返回 null */
    fun saveToAppStorage(context: Context, uri: Uri): String? {
        val bitmap = decodeFromUri(context, uri, MAX_SAVE_SIZE) ?: return null
        return try {
            val dir = File(context.filesDir, IMAGE_DIR)
            if (!dir.exists() && !dir.mkdirs()) {
                bitmap.recycle()
                return null
            }
            val file = File(dir, "img_" + System.currentTimeMillis() + ".jpg")
            FileOutputStream(file).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
            }
            bitmap.recycle()
            file.absolutePath
        } catch (e: Exception) {
            bitmap.recycle()
            null
        }
    }

    /** 读相册里的图片，最长边缩到 maxSize 以内（分两次解码，先量尺寸再按比例缩，不会把原图读进内存） */
    fun decodeFromUri(context: Context, uri: Uri, maxSize: Int): Bitmap? {
        return try {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return null
            }
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxSize)
            }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        } catch (e: Exception) {
            null
        }
    }

    /** 读本地文件里的图片，最长边缩到 maxSize 以内 */
    fun decodeFromPath(path: String, maxSize: Int): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return null
            }
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxSize)
            }
            BitmapFactory.decodeFile(path, options)
        } catch (e: Exception) {
            null
        }
    }

    /** 算 2 的几次方倍的缩放比，保证缩完的长边不小于 maxSize */
    private fun sampleSize(width: Int, height: Int, maxSize: Int): Int {
        var sample = 1
        var w = width
        var h = height
        while (w / 2 >= maxSize && h / 2 >= maxSize) {
            sample *= 2
            w /= 2
            h /= 2
        }
        return sample
    }
}
