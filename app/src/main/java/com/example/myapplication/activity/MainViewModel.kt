package com.example.myapplication.activity

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.model.ShoeInventory
import com.example.myapplication.service.InventoryDatabase
import com.example.myapplication.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.coroutineContext

class MainViewModel : ViewModel() {
    var billsResetTick by mutableIntStateOf(0)
        private set

    var inventories by mutableStateOf<List<ShoeInventory>>(emptyList())
        private set

    var imageCacheVersion by mutableIntStateOf(0)
        private set

    private val pictureCache = LruCache<String, Bitmap>(64)
    private var inventoryLoadJob: Job? = null
    private val inventoryLoadRequest = AtomicLong()
    private val imageCacheGeneration = AtomicLong()

    fun resetBills() {
        billsResetTick++
    }

    fun refreshInventory(context: Context, clearImageCache: Boolean = false) {
        val appContext = context.applicationContext
        val request = inventoryLoadRequest.incrementAndGet()
        inventoryLoadJob?.cancel()
        if (clearImageCache) {
            imageCacheGeneration.incrementAndGet()
            pictureCache.evictAll()
            imageCacheVersion++
        }

        inventoryLoadJob = viewModelScope.launch {
            val rows = withContext(Dispatchers.IO) {
                InventoryDatabase.withDatabaseAccess(appContext) { database ->
                    database.shoeInventoryDAO().queryAll()
                }
            }
            if (request != inventoryLoadRequest.get()) return@launch
            inventories = rows
        }
    }

    suspend fun loadImage(path: String): ImageBitmap? {
        val cacheGeneration = imageCacheGeneration.get()
        val cachedBitmap = pictureCache.get(path)
        if (cachedBitmap != null && cacheGeneration == imageCacheGeneration.get()) {
            return cachedBitmap.asImageBitmap()
        }

        val bitmap = withContext(Dispatchers.IO) {
            InventoryDatabase.withDatabaseAccess {
                pictureCache.get(path) ?: run {
                    val decoded = ImageUtils.decodeFromPath(path, ImageUtils.MAX_SHOW_SIZE)
                    coroutineContext.ensureActive()
                    if (
                        decoded != null &&
                        cacheGeneration == imageCacheGeneration.get()
                    ) {
                        pictureCache.put(path, decoded)
                    }
                    decoded
                }
            }
        }
        return if (cacheGeneration == imageCacheGeneration.get()) {
            bitmap?.asImageBitmap()
        } else {
            null
        }
    }

    fun imageFor(path: String): ImageBitmap? =
        pictureCache.get(path)?.asImageBitmap()
}
