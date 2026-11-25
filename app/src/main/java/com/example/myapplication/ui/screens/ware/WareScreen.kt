@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.example.myapplication.ui.screens.ware

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.myapplication.ui.components.HideKeyboardOnScroll
import com.example.myapplication.ui.components.hideKeyboardOnAnyPress
import com.example.myapplication.ui.components.hideKeyboardOnTap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.myapplication.model.OrderType
import com.example.myapplication.model.Color
import com.example.myapplication.model.ShoeInventory
import com.example.myapplication.R
import com.example.myapplication.ui.components.AppToast
import com.example.myapplication.ui.screens.main.PageTitleHeader
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.utils.SizeUtils
import java.text.NumberFormat

/**
 * 仓库页面（见文件头的说明）。
 *
 * @param inventories 查出来的全部存量（搜索过滤前）
 * @param imageFor 按图片路径取解码好的缩略图，没图就不会调用
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WareScreen(
    inventories: List<ShoeInventory>,
    imageCacheVersion: Int,
    imageFor: (String) -> ImageBitmap?,
    loadImage: suspend (String) -> ImageBitmap?,
    onInventoryChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedOrderType by remember { mutableStateOf<OrderType?>(null) }
    var selectedInventory by remember { mutableStateOf<ShoeInventory?>(null) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var toastMessageId by remember { mutableStateOf(0) }

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

    val listState = rememberLazyListState()
    HideKeyboardOnScroll(listState.isScrollInProgress)

    val productGroups = remember(inventories, searchQuery) {
        inventoryProductGroups(inventories, searchQuery)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().hideKeyboardOnAnyPress()) {
            PageTitleHeader(
                eyebrow = stringResource(R.string.inventory_overview_eyebrow),
                title = stringResource(R.string.bottom_warehouse)
            )

            LazyColumn(
                state = listState,
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
                            .hideKeyboardOnTap()
                    ) {
                        InventorySearchField(
                            query = searchQuery,
                            productCount = productGroups.size,
                            onQueryChange = { searchQuery = it }
                        )
                    }
                }

                inventoryProductItems(
                    groups = productGroups,
                    imageCacheVersion = imageCacheVersion,
                    imageFor = imageFor,
                    loadImage = loadImage,
                    onStockAction = { orderType, inventory ->
                        selectedInventory = inventory
                        selectedOrderType = orderType
                    }
                )
            }

            selectedOrderType?.let { orderType ->
                InventoryOrderDialog(
                    orderType = orderType,
                    fixedInventory = selectedInventory,
                    onDismiss = {
                        selectedOrderType = null
                        selectedInventory = null
                    },
                    onSaved = { message ->
                        selectedOrderType = null
                        selectedInventory = null
                        toastMessage = message
                        toastMessageId++
                        onInventoryChanged()
                    }
                )
            }
        }

        AppToast(
            message = toastMessage,
            messageId = toastMessageId,
            onDismissRequest = { toastMessage = null }
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun WareScreenPreview() {
    WareScreen(
        inventories = listOf(
            ShoeInventory(
                articleId = "SH-2026-001",
                dealer = "示例经销商",
                color = Color.BLACK,
                purchasePrice = java.math.BigDecimal("199.00"),
                size38 = 2,
                size39 = 4,
                size40 = 3
            )
        ),
        imageCacheVersion = 0,
        imageFor = { null },
        loadImage = { null },
        onInventoryChanged = {}
    )
}
