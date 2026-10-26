package com.example.myapplication.ui.screens.ware

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import com.example.myapplication.model.OrderType
import com.example.myapplication.model.ShoeInventory
import com.example.myapplication.R
import com.example.myapplication.ui.components.AppAlertDialog
import com.example.myapplication.ui.components.AppBottomSheetMenu
import com.example.myapplication.ui.components.AppBottomSheetMenuItem
import com.example.myapplication.ui.components.AppDialogSpec
import com.example.myapplication.ui.screens.main.PageTitleHeader
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.utils.SizeUtils
import java.text.NumberFormat

/**
 * 仓库页面（见文件头的说明）。
 *
 * @param inventories 查出来的全部存量（搜索过滤前），由 WareFragment 负责查库
 * @param imageFor 按图片路径取解码好的缩略图（WareFragment 的缓存），没图就不会调用
 * @param onBackupClick 数据备份 / 恢复（WareDrawerPanel.showBackupDialog）
 * @param onUpdateClick 版本更新（WareDrawerPanel.showUpdateDialog）
 */
@OptIn(ExperimentalFoundationApi::class)
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

    var searchQuery by rememberSaveable { mutableStateOf("") }

    val productGroups = remember(inventories, searchQuery) {
        inventoryProductGroups(inventories, searchQuery)
    }

    Column(modifier = modifier.fillMaxSize()) {
        PageTitleHeader(
            eyebrow = stringResource(R.string.inventory_overview_eyebrow),
            title = stringResource(R.string.bottom_warehouse)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            item(key = "inventory_summary") {
                InventorySummaryCard(
                    totalQuantity = formattedTotalQuantity,
                    styleCount = inventoryGroups.size
                )
            }

            item(key = "ware_quick_actions") {
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
            }

            stickyHeader(key = "inventory_search") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.Gray50)
                ) {
                    InventorySearchField(
                        query = searchQuery,
                        productCount = productGroups.size,
                        onQueryChange = { searchQuery = it }
                    )
                }
            }

            item(key = "inventory_products") {
                InventoryProductList(
                    groups = productGroups,
                    imageFor = imageFor,
                    onStockAction = { orderType, inventory ->
                        selectedInventory = inventory
                        selectedOrderType = orderType
                    }
                )
            }
        }

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
