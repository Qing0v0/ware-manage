package com.example.myapplication.ui.screens.ware

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import com.example.myapplication.model.OrderType
import com.example.myapplication.model.ShoeInventory
import com.example.myapplication.ui.components.AppAlertDialog
import com.example.myapplication.ui.components.AppBottomSheetMenu
import com.example.myapplication.ui.components.AppBottomSheetMenuItem
import com.example.myapplication.ui.components.AppDialogSpec
import com.example.myapplication.ui.screens.main.PageTitleHeader
import com.example.myapplication.utils.SizeUtils
import java.text.NumberFormat

/**
 * 仓库页面（Compose 版，对应原来的 res/layout/fragment_ware.xml）：按货号分组的存量表。
 *
 * 一个货号一张表：
 * - 第一排：货号（左，占满剩余宽度）| 经销商 | 数量
 * - 第二排开始每个颜色一行：图片（占位）| 颜色 | 34 35 …… 44 码（可以横向滑动）| 小计 | ＋ －
 *
 * 左栏（图片 + 颜色）和右栏（小计 + ＋ －）固定不动，只有中间的尺码是横向可滑动的，
 * 这样一屏里总能看清是哪个货号、哪个颜色。
 * 行尾的 ＋ / － 会在当前页打开出入库弹窗，货号、颜色、经销商都是锁定的，只能填数量。
 *
 * 全局顶部栏的更多入口打开底部菜单（数据备份 / 恢复、版本更新），动作通过
 * [onBackupClick] / [onUpdateClick] 交给 WareFragment 处理。
 */

/**
 * 仓库页面（见文件头的说明）。
 *
 * @param inventories 查出来的全部存量（搜索过滤前），由 WareFragment 负责查库
 * @param imageFor 按图片路径取解码好的缩略图（WareFragment 的缓存），没图就不会调用
 * @param onBackupClick 数据备份 / 恢复（WareDrawerPanel.showBackupDialog）
 * @param onUpdateClick 版本更新（WareDrawerPanel.showUpdateDialog）
 */
@Composable
fun WareScreen(
    inventories: List<ShoeInventory>,
    imageFor: (String) -> ImageBitmap?,
    onBackupClick: () -> Unit,
    onUpdateClick: () -> Unit,
    onInventoryChanged: () -> Unit,
    moreMenuRequest: Int,
    dialog: AppDialogSpec?,
    onDialogDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMoreMenu by remember { mutableStateOf(false) }
    var selectedOrderType by remember { mutableStateOf<OrderType?>(null) }
    var selectedInventory by remember { mutableStateOf<ShoeInventory?>(null) }

    LaunchedEffect(moreMenuRequest) {
        if (moreMenuRequest > 0) {
            showMoreMenu = true
        }
    }

    val inventoryGroups = remember(inventories) {
        inventories
            .filter { inventory -> SizeUtils.sizesOf(inventory).any { count -> count > 0 } }
            .groupBy { it.articleId }
    }

    val totalQuantity = remember(inventories) {
        inventories.sumOf { inventory -> SizeUtils.sizesOf(inventory).sum() }
    }
    val formattedTotalQuantity = remember(totalQuantity) {
        NumberFormat.getIntegerInstance().format(totalQuantity.toLong())
    }

    Column(modifier = modifier.fillMaxSize()) {
        PageTitleHeader(
            eyebrow = stringResource(R.string.inventory_overview_eyebrow),
            title = stringResource(R.string.bottom_warehouse),
            modifier = Modifier.fillMaxWidth().padding(
                start = 18.dp,
                top = 4.dp,
                end = 18.dp,
                bottom = 12.dp
            )
        )

        InventorySummaryCard(
            totalQuantity = formattedTotalQuantity,
            styleCount = inventoryGroups.size
        )

        WareQuickActions(
            onPurchase = {
                selectedInventory = null
                selectedOrderType = OrderType.ARTICLE_PURCHASE
            },
            onSelling = {
                selectedInventory = null
                selectedOrderType = OrderType.ARTICLE_SOLD_CASH
            }
        )

        InventoryProductList(
            inventories = inventories,
            imageFor = imageFor,
            modifier = Modifier.weight(1f),
            onStockAction = { orderType, inventory ->
                selectedInventory = inventory
                selectedOrderType = orderType
            }
        )
        selectedOrderType?.let { orderType ->
            InventoryOrderDialog(
                orderType = orderType,
                fixedInventory = selectedInventory,
                onDismiss = {
                    selectedOrderType = null
                    selectedInventory = null
                },
                onSaved = {
                    selectedOrderType = null
                    selectedInventory = null
                    onInventoryChanged()
                }
            )
        }
    }

    AppBottomSheetMenu(
        visible = showMoreMenu,
        title = stringResource(R.string.more_options),
        items = listOf(
            AppBottomSheetMenuItem(
                label = stringResource(R.string.settings_backup),
                onClick = onBackupClick
            ),
            AppBottomSheetMenuItem(
                label = stringResource(R.string.settings_update),
                onClick = onUpdateClick
            )
        ),
        onDismissRequest = { showMoreMenu = false }
    )

    AppAlertDialog(
        dialog = dialog,
        onDismissRequest = onDialogDismiss
    )
}
