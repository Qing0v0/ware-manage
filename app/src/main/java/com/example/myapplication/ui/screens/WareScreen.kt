package com.example.myapplication.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.activity.OrderEditActivity
import com.example.myapplication.model.OrderType
import com.example.myapplication.model.ShoeInventory
import com.example.myapplication.ui.components.HairlineColor
import com.example.myapplication.ui.screens.main.PageTitleHeader
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.utils.SizeUtils
import java.text.NumberFormat
import kotlinx.coroutines.launch

/**
 * 仓库页面（Compose 版，对应原来的 res/layout/fragment_ware.xml）：按货号分组的存量表。
 *
 * 一个货号一张表：
 * - 第一排：货号（左，占满剩余宽度）| 经销商 | 数量
 * - 第二排开始每个颜色一行：图片（占位）| 颜色 | 34 35 …… 44 码（可以横向滑动）| 小计 | ＋ －
 *
 * 左栏（图片 + 颜色）和右栏（小计 + ＋ －）固定不动，只有中间的尺码是横向可滑动的，
 * 这样一屏里总能看清是哪个货号、哪个颜色。
 * 行尾的 ＋ / － 会打开 [OrderEditActivity]，货号、颜色、经销商都是锁定的，只能填数量。
 *
 * 全局顶部栏的设置入口打开侧边栏（数据备份 / 恢复、版本更新），动作通过
 * [onBackupClick] / [onUpdateClick] 交给 WareFragment 里的 WareDrawerPanel 处理。
 */

/** 设置侧边栏宽度 */
private val DrawerWidth = 280.dp

/** 输入框里没有字时的提示语颜色（EditText 默认 hint 色） */
private val HintColor = Color(0xFF757575)

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
    settingsRequest: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(settingsRequest) {
        if (settingsRequest > 0) {
            drawerState.open()
        }
    }

    /**
     * 过滤：先按搜索框的关键字（货号 / 经销商，忽略大小写），
     * 再把 11 个尺码全是 0 的颜色藏起来，不然会以为这个颜色还有货。
     */
    val visibleRows = remember(inventories, searchQuery) {
        val keyword = searchQuery.trim()
        inventories
            .filter { inventory ->
                keyword.isEmpty() ||
                    inventory.articleId.contains(keyword, ignoreCase = true) ||
                    inventory.dealer.contains(keyword, ignoreCase = true)
            }
            .filter { inventory -> SizeUtils.sizesOf(inventory).any { count -> count > 0 } }
    }

    // 一个货号一张表（queryAll 已经按货号、颜色排好序，分组的先后顺序和查询一致）
    val groups = remember(visibleRows) { visibleRows.groupBy { it.articleId } }
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SettingsDrawer(
                onBackupClick = {
                    // 和原来一样：先关侧边栏，再弹对话框
                    scope.launch { drawerState.close() }
                    onBackupClick()
                },
                onUpdateClick = {
                    scope.launch { drawerState.close() }
                    onUpdateClick()
                }
            )
        }
    ) {
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
                    context.startActivity(
                        OrderEditActivity.createIntent(context, OrderType.ARTICLE_PURCHASE)
                    )
                },
                onSelling = {
                    context.startActivity(
                        OrderEditActivity.createIntent(context, OrderType.ARTICLE_SOLD_CASH)
                    )
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.inventory_products_title),
                    color = Color(0xFF19251F),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.inventory_product_group_count, inventoryGroups.size),
                    color = Color(0xFF87918B),
                    fontSize = 10.sp
                )
            }

            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                singleLine = true,
                textStyle = TextStyle(color = Color.Black, fontSize = 14.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE8EDE8), RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SearchMark()
                        Spacer(modifier = Modifier.width(9.dp))
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.inventory_search_hint),
                                    color = HintColor,
                                    fontSize = 14.sp
                                )
                            }
                            innerTextField()
                        }
                    }
                }
            )

            val groupEntries = remember(groups) { groups.entries.toList() }
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                if (groupEntries.isEmpty()) {
                    item(key = "empty_inventory") {
                        Text(
                            text = stringResource(R.string.inventory_empty),
                            color = Color.Black,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(32.dp)
                        )
                    }
                } else {
                    items(
                        items = groupEntries,
                        key = { it.key }
                    ) { (articleId, rows) ->
                        InventoryProductCard(
                            articleId = articleId,
                            rows = rows,
                            imageFor = imageFor,
                            onStockAction = { orderType, inventory ->
                                context.startActivity(
                                    OrderEditActivity.createIntent(
                                        context,
                                        orderType,
                                        articleId,
                                        inventory.color.displayName,
                                        inventory.dealer,
                                        inventory.imagePath
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InventorySummaryCard(
    totalQuantity: String,
    styleCount: Int
) {
    val shape = RoundedCornerShape(18.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(shape)
            .background(Color(0xFF276E52))
            .padding(horizontal = 16.dp, vertical = 13.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(Color(0xFF8DE0AD), CircleShape)
            )
            Text(
                text = stringResource(R.string.inventory_total_label),
                color = Color(0xFFD0E1D8),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = totalQuantity,
                color = Color.White,
                fontSize = 30.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.8).sp
            )
            Text(
                text = stringResource(R.string.inventory_quantity_unit),
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 3.dp, top = 7.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.inventory_total_count_prefix),
                color = Color(0xFFD0E1D8),
                fontSize = 12.sp
            )
            Text(
                text = styleCount.toString(),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Text(
                text = stringResource(R.string.inventory_total_count_suffix),
                color = Color(0xFFD0E1D8),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun SearchMark() {
    Canvas(modifier = Modifier.size(17.dp)) {
        val strokeWidth = 1.6.dp.toPx()
        val radius = size.minDimension * 0.31f
        val center = Offset(size.width * 0.42f, size.height * 0.42f)
        drawCircle(
            color = Color(0xFF9AA39D),
            radius = radius,
            center = center,
            style = Stroke(strokeWidth)
        )
        drawLine(
            color = Color(0xFF9AA39D),
            start = Offset(center.x + radius * 0.68f, center.y + radius * 0.68f),
            end = Offset(size.width * 0.9f, size.height * 0.9f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun InventoryProductCard(
    articleId: String,
    rows: List<ShoeInventory>,
    imageFor: (String) -> ImageBitmap?,
    onStockAction: (OrderType, ShoeInventory) -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    val first = rows.first()
    val totalQuantity = rows.sumOf { inventory -> SizeUtils.sizesOf(inventory).sum() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(shape)
            .background(Color.White)
            .border(1.dp, Color(0xFFE9EEEA), shape)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProductThumbnail(inventory = first, imageFor = imageFor)
            Spacer(modifier = Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = articleId,
                    color = Color(0xFF19251F),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${first.dealer} · ${
                        stringResource(R.string.inventory_product_color_count, rows.size)
                    }",
                    color = Color(0xFF87918B),
                    fontSize = 9.sp,
                    modifier = Modifier.padding(top = 4.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.inventory_product_stock_total, totalQuantity),
                color = Color(0xFF246B50),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color(0xFFEDF5EF))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                maxLines = 1
            )
        }

        InventoryDivider()
        rows.forEachIndexed { index, inventory ->
            if (index > 0) InventoryDivider()
            InventoryColorRow(
                inventory = inventory,
                onStockAction = { orderType -> onStockAction(orderType, inventory) }
            )
        }
    }
}

@Composable
private fun ProductThumbnail(
    inventory: ShoeInventory,
    imageFor: (String) -> ImageBitmap?
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(shape)
            .background(Color(0xFFF1F4F0)),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = inventory.imagePath?.let(imageFor)
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = inventory.articleId,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun InventoryColorRow(
    inventory: ShoeInventory,
    onStockAction: (OrderType) -> Unit
) {
    val sizes = SizeUtils.sizesOf(inventory)
    val subtotal = sizes.sum()

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(AppColors.shoeColor(inventory.color))
                    .border(0.5.dp, Color(0xFFDDE2DD), CircleShape)
            )
            Text(
                text = inventory.color.displayName,
                color = Color(0xFF19251F),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 7.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.inventory_size_stock),
                color = Color(0xFF87918B),
                fontSize = 9.sp
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF6F8F6))
                .padding(horizontal = 8.dp, vertical = 7.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            sizes.forEachIndexed { index, count ->
                val inStock = count > 0
                Column(
                    modifier = Modifier.width(26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = SizeUtils.sizeName(index).toString(),
                        color = Color(0xFF919B94),
                        fontSize = 9.sp
                    )
                    Text(
                        text = if (inStock) count.toString() else "—",
                        color = if (inStock) Color(0xFF34443A) else Color(0xFFC8CFCA),
                        fontSize = 9.sp,
                        fontWeight = if (inStock) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.inventory_color_subtotal),
                color = Color(0xFF7F8A82),
                fontSize = 9.sp
            )
            Text(
                text = stringResource(R.string.inventory_product_stock_total, subtotal),
                color = Color(0xFF19251F),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            StockMiniAction(
                text = stringResource(R.string.stock_add_button),
                onClick = { onStockAction(OrderType.ARTICLE_PURCHASE) }
            )
            Spacer(modifier = Modifier.width(5.dp))
            StockMiniAction(
                text = stringResource(R.string.stock_minus_button),
                onClick = { onStockAction(OrderType.ARTICLE_SOLD_CASH) }
            )
        }
    }
}

@Composable
private fun StockMiniAction(text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .size(width = 27.dp, height = 25.dp)
            .clip(shape)
            .background(Color.White)
            .border(1.dp, Color(0xFFE6ECE7), shape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Color(0xFF246B50), fontSize = 14.sp, lineHeight = 16.sp)
    }
}

@Composable
private fun InventoryDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .height(1.dp)
            .background(Color(0xFFF0F2F0))
    )
}

@Composable
private fun WareQuickActions(
    onPurchase: () -> Unit,
    onSelling: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        WareQuickAction(
            title = stringResource(R.string.purchase_button),
            subtitle = stringResource(R.string.inventory_purchase_caption),
            symbol = "+",
            iconBackground = Color(0xFFE8F2EC),
            iconColor = Color(0xFF246B50),
            background = Color.White,
            onClick = onPurchase,
            modifier = Modifier.weight(1f)
        )
        WareQuickAction(
            title = stringResource(R.string.selling_button),
            subtitle = stringResource(R.string.inventory_selling_caption),
            symbol = "-",
            iconBackground = Color(0xFFE8F2EC),
            iconColor = Color(0xFF246B50),
            background = Color.White,
            onClick = onSelling,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun WareQuickAction(
    title: String,
    subtitle: String,
    symbol: String,
    iconBackground: Color,
    iconColor: Color,
    background: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(15.dp)
    Row(
        modifier = modifier
            .height(58.dp)
            .clip(shape)
            .background(background)
            .border(1.dp, Color(0xFFE7ECE7), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(iconBackground),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = symbol,
                color = iconColor,
                fontSize = 18.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Column {
            Text(
                text = title,
                color = Color(0xFF19251F),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = Color(0xFF87918B),
                fontSize = 9.sp,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
    }
}

/** 设置侧边栏：标题 + 数据备份 / 版本更新两行（以后加设置项就往下再加一行） */
@Composable
private fun SettingsDrawer(
    onBackupClick: () -> Unit,
    onUpdateClick: () -> Unit
) {
    // 分隔线：0.5 物理像素，和原布局里的写法一致
    val hairlineHeight = with(LocalDensity.current) { 0.5f.toDp() }

    Column(
        modifier = Modifier
            .width(DrawerWidth)
            .fillMaxHeight()
            .background(colorResource(R.color.white))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorResource(R.color.table_head_bg))
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                color = Color.Black,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        DrawerRow(label = stringResource(R.string.settings_backup), onClick = onBackupClick)
        DrawerRow(label = stringResource(R.string.settings_update), onClick = onUpdateClick)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(hairlineHeight)
                .background(HairlineColor)
        )
    }
}

/** 侧边栏里的一行设置项 */
@Composable
private fun DrawerRow(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text = label, color = Color.Black, fontSize = 16.sp)
    }
}
