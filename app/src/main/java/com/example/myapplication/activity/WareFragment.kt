package com.example.myapplication.activity

import android.graphics.Bitmap
import android.os.Bundle
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.example.myapplication.model.ShoeInventory
import com.example.myapplication.service.InventoryDatabase
import com.example.myapplication.ui.screens.ware.WareScreen
import com.example.myapplication.utils.ImageUtils
import kotlin.concurrent.thread

/**
 * 仓库页面的宿主 Fragment：界面全部在 [WareScreen]（Compose）里，
 * 这里只负责查存量、后台解码缩略图，以及备份 / 更新操作（[WareDrawerPanel]）的系统回调注册
 * （两个 registerForActivityResult 必须在 Fragment 初始化阶段注册，所以面板留在这里）。
 *
 * （MainActivity 目前靠 show/hide 切页，见 [MainActivity]；等页面全部 Compose 化后再收掉这些壳。）
 */
class WareFragment : Fragment() {

    /** 数据库里查出来的全部存量（搜索过滤前的原始数据），按关键字过滤交给 WareScreen */
    private val inventories = mutableStateOf<List<ShoeInventory>>(emptyList())
    private val moreMenuRequest = mutableIntStateOf(0)

    /** 存量缩略图缓存：路径 -> Bitmap，避免每次回到页面都重新解码 */
    private val pictureCache = LruCache<String, Bitmap>(64)

    /**
     * 备份恢复、版本更新的系统回调。
     * 必须在这里就 new 出来：里面的 registerForActivityResult 要求在 Fragment
     * 初始化阶段注册，放到 onCreateView 里就晚了。
     */
    private val drawerPanel = WareDrawerPanel(this) { loadInventory() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            id = R.id.warehouse_compose_view
            setContent {
                WareScreen(
                    inventories = inventories.value,
                    imageFor = { path -> pictureCache.get(path)?.asImageBitmap() },
                    onBackupClick = drawerPanel::showBackupDialog,
                    onUpdateClick = drawerPanel::showUpdateDialog,
                    onInventoryChanged = ::loadInventory,
                    moreMenuRequest = moreMenuRequest.intValue,
                    dialog = drawerPanel.dialogState.value,
                    onDialogDismiss = drawerPanel::dismissDialog
                )
            }
        }
    }

    fun openMoreMenu() {
        moreMenuRequest.intValue++
    }

    /** 每次回到这个页面（包括从出入库界面返回）都重新查一遍存量 */
    override fun onResume() {
        super.onResume()
        loadInventory()
    }

    /** 从账单页切回来时也重新查一遍，免得数据还是切走前那一次查出来的 */
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            loadInventory()
        }
    }

    private fun loadInventory() {
        val context = requireContext().applicationContext
        thread {
            val inventoryDAO = InventoryDatabase.getDatabase(context).shoeInventoryDAO()
            val rows = inventoryDAO.queryAll()

            // 缩略图先在后台解码好，界面上直接取缓存
            rows.forEach { inventory ->
                val path = inventory.imagePath
                if (!path.isNullOrEmpty() && pictureCache.get(path) == null) {
                    ImageUtils.decodeFromPath(path, ImageUtils.MAX_SHOW_SIZE)
                        ?.let { pictureCache.put(path, it) }
                }
            }

            activity?.runOnUiThread {
                if (!isAdded) {
                    return@runOnUiThread
                }
                inventories.value = rows
            }
        }
    }
}
