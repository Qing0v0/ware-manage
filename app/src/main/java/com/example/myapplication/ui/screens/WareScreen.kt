package com.example.myapplication.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.activity.OrderEditActivity
import com.example.myapplication.model.OrderType
import com.example.myapplication.model.ShoeInventory
import com.example.myapplication.ui.components.HairlineColor
import com.example.myapplication.ui.components.tableCellBackground
import com.example.myapplication.utils.SizeUtils
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
 * 左上角齿轮打开设置侧边栏（数据备份 / 恢复、版本更新），动作通过 [onBackupClick] /
 * [onUpdateClick] 交给 WareFragment 里的 WareDrawerPanel 处理（那里注册着系统文件选择器）。
 */

/** 表格列宽（和原布局里的 dp 值一一对应） */
private val PictureCellWidth = 44.dp
private val ColorCellWidth = 60.dp
private val SizeCellWidth = 44.dp
private val SubtotalCellWidth = 52.dp
private val StockButtonWidth = 32.dp

/** 行尾 ＋ － 两个按钮加起来的宽度 */
private val StockButtonsWidth = StockButtonWidth * 2

/** 每行高度 */
private val RowHeight = 44.dp

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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var searchQuery by remember { mutableStateOf("") }

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

    val totalQuantity = remember(visibleRows) {
        visibleRows.sumOf { inventory -> SizeUtils.sizesOf(inventory).sum() }
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
            // 顶栏：左边齿轮（设置），右边整单出入库
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { scope.launch { drawerState.open() } },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings),
                        contentDescription = stringResource(R.string.settings_title),
                        // 原来是 ImageButton 直接放图，自带颜色，不额外着色
                        tint = Color.Unspecified
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = {
                        // 整单入库 / 出库：货号自己填，用来开一个新的货号
                        context.startActivity(
                            OrderEditActivity.createIntent(context, OrderType.ARTICLE_PURCHASE)
                        )
                    }
                ) {
                    Text(stringResource(R.string.purchase_button))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        context.startActivity(
                            OrderEditActivity.createIntent(context, OrderType.ARTICLE_SOLD_CASH)
                        )
                    }
                ) {
                    Text(stringResource(R.string.selling_button))
                }
            }

            // 搜索：边打字边按货号 / 经销商过滤下面的表格
            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                singleLine = true,
                textStyle = TextStyle(color = Color.Black, fontSize = 14.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(start = 8.dp, end = 8.dp, bottom = 4.dp),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .tableCellBackground()
                            .padding(horizontal = 8.dp),
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
            )

            // 没有存量时的提示
            if (visibleRows.isEmpty()) {
                Text(
                    text = stringResource(R.string.inventory_empty),
                    color = Color.Black,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(32.dp)
                )
            }

            // 存量表：可上下滑；每组 = 一个货号
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp)
            ) {
                groups.forEach { (articleId, rows) ->
                    ArticleHeader(rows)
                    SizeTable(
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
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // 固定在最下面的合计：款式总数 + 库存总数
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorResource(R.color.table_head_bg))
                    .padding(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.total_articleId_num),
                    color = Color.Black,
                    fontSize = 16.sp
                )
                Text(
                    text = groups.size.toString(),
                    color = colorResource(R.color.light_blue_900),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(start = 12.dp)
                )
                Text(
                    text = stringResource(R.string.total_inventory_quantity),
                    color = Color.Black,
                    fontSize = 16.sp
                )
                Text(
                    text = totalQuantity.toString(),
                    color = colorResource(R.color.light_blue_900),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(start = 12.dp)
                )
            }
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

/** 表格里的一个格子：宽高统一、单行居中，几栏才对得齐（对应原来的 cell()） */
@Composable
private fun WareCell(
    text: String,
    modifier: Modifier = Modifier,
    head: Boolean = false,
    bold: Boolean = false,
    startAligned: Boolean = false
) {
    Box(
        modifier = modifier
            .height(RowHeight)
            .tableCellBackground(head)
            .then(if (startAligned) Modifier.padding(start = 6.dp) else Modifier),
        contentAlignment = if (startAligned) Alignment.CenterStart else Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.Black,
            fontSize = 14.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** 图片格子：有图就显示缩略图（后台已经解码好放在缓存里），没图就是一个空的带边框格子 */
@Composable
private fun PictureCell(
    inventory: ShoeInventory,
    imageFor: (String) -> ImageBitmap?
) {
    Box(
        modifier = Modifier
            .width(PictureCellWidth)
            .height(RowHeight)
            .tableCellBackground()
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = inventory.imagePath?.let(imageFor)
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = stringResource(R.string.row_picture_hint),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/** 行尾的 ＋ －：打开出入库界面，货号、颜色、经销商都是锁死的 */
@Composable
private fun StockButtons(
    inventory: ShoeInventory,
    onStockAction: (OrderType, ShoeInventory) -> Unit
) {
    Row(
        modifier = Modifier.width(StockButtonsWidth).height(RowHeight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StockButton(text = stringResource(R.string.stock_add_button)) {
            onStockAction(OrderType.ARTICLE_PURCHASE, inventory)
        }
        StockButton(text = stringResource(R.string.stock_minus_button)) {
            onStockAction(OrderType.ARTICLE_SOLD_CASH, inventory)
        }
    }
}

/** 行尾的小按钮：带边框的方块，居中一个 ＋ / － */
@Composable
private fun StockButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(StockButtonWidth)
            .height(RowHeight - 8.dp)
            .tableCellBackground()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Color.Black, fontSize = 16.sp)
    }
}

/** 货号这一块的表头：第一排列名（货号 / 经销商 / 数量），第二排数值 */
@Composable
private fun ArticleHeader(rows: List<ShoeInventory>) {
    val first = rows.first()
    val quantity = rows.sumOf { inventory -> SizeUtils.sizesOf(inventory).sum() }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            WareCell(
                stringResource(R.string.purchase_order_article_id),
                Modifier.weight(1f),
                head = true,
                startAligned = true
            )
            WareCell(
                stringResource(R.string.purchase_order_dealer),
                Modifier.weight(1f),
                head = true
            )
            WareCell(
                stringResource(R.string.inventory_amount),
                Modifier.weight(1f),
                head = true
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            WareCell(
                first.articleId,
                Modifier.weight(1f),
                head = true,
                bold = true,
                startAligned = true
            )
            WareCell(first.dealer, Modifier.weight(1f), head = true)
            WareCell(quantity.toString(), Modifier.weight(1f), head = true, bold = true)
        }
    }
}

/**
 * 一个货号下面的颜色表：左栏（图片 + 颜色）和右栏（小计 + ＋ －）固定不动，
 * 中间的 34 ~ 44 码放在横向滚动里；每行高度一样，所以三栏能对齐。
 */
@Composable
private fun SizeTable(
    rows: List<ShoeInventory>,
    imageFor: (String) -> ImageBitmap?,
    onStockAction: (OrderType, ShoeInventory) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        // 左栏：图片（先占位）+ 颜色，固定不动
        Column(modifier = Modifier.width(PictureCellWidth + ColorCellWidth)) {
            Row {
                WareCell(
                    stringResource(R.string.inventory_picture),
                    Modifier.width(PictureCellWidth),
                    head = true
                )
                WareCell(
                    stringResource(R.string.purchase_order_color),
                    Modifier.width(ColorCellWidth),
                    head = true
                )
            }
            rows.forEach { inventory ->
                Row {
                    PictureCell(inventory, imageFor)
                    WareCell(inventory.color.displayName, Modifier.width(ColorCellWidth))
                }
            }
        }

        // 中间：34 ~ 44 码，横向可滑动
        Column(
            modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState())
        ) {
            Row {
                for (index in 0 until SizeUtils.SIZE_AMOUNT) {
                    WareCell(
                        SizeUtils.sizeName(index).toString(),
                        Modifier.width(SizeCellWidth),
                        head = true
                    )
                }
            }
            rows.forEach { inventory ->
                val sizes = SizeUtils.sizesOf(inventory)
                Row {
                    for (index in 0 until SizeUtils.SIZE_AMOUNT) {
                        WareCell(countText(sizes[index]), Modifier.width(SizeCellWidth))
                    }
                }
            }
        }

        // 右栏：小计 + 行尾的 ＋ / －，固定不动
        Column(modifier = Modifier.width(SubtotalCellWidth + StockButtonsWidth)) {
            Row {
                WareCell(
                    stringResource(R.string.inventory_subtotal),
                    Modifier.width(SubtotalCellWidth),
                    head = true
                )
                WareCell("", Modifier.width(StockButtonsWidth), head = true)
            }
            rows.forEach { inventory ->
                Row {
                    WareCell(
                        countText(SizeUtils.sizesOf(inventory).sum()),
                        Modifier.width(SubtotalCellWidth)
                    )
                    StockButtons(inventory, onStockAction)
                }
            }
        }
    }
}

/** 数量为 0 一律显示空白，免得看成一格一格的数字以为有货 */
private fun countText(count: Int): String {
    if (count == 0) {
        return ""
    }
    return count.toString()
}

