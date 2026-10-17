package com.example.myapplication.ui.screens.ware

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.model.OrderType
import com.example.myapplication.model.ShoeInventory
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.ui.theme.AppTypography
import com.example.myapplication.utils.SizeUtils

@Composable
internal fun InventoryProductList(
    inventories: List<ShoeInventory>,
    imageFor: (String) -> ImageBitmap?,
    onStockAction: (OrderType, ShoeInventory) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val groups = remember(inventories, searchQuery) {
        val keyword = searchQuery.trim()
        inventories
            .filter { inventory ->
                (keyword.isEmpty() ||
                    inventory.articleId.contains(keyword, ignoreCase = true) ||
                    inventory.dealer.contains(keyword, ignoreCase = true)) &&
                    SizeUtils.sizesOf(inventory).any { count -> count > 0 }
            }
            .groupBy { it.articleId }
            .entries
            .toList()
    }
    val productCount = remember(inventories) {
        inventories
            .filter { inventory -> SizeUtils.sizesOf(inventory).any { count -> count > 0 } }
            .map { it.articleId }
            .distinct()
            .size
    }

    Column(modifier = modifier.fillMaxWidth()) {
        InventorySearchField(
            query = searchQuery,
            productCount = productCount,
            onQueryChange = { searchQuery = it }
        )
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            if (groups.isEmpty()) {
                item(key = "empty_inventory") {
                    Text(
                        text = stringResource(R.string.inventory_empty),
                        color = AppColors.Ink19251F,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(32.dp)
                    )
                }
            } else {
                items(
                    items = groups,
                    key = { it.key }
                ) { (articleId, rows) ->
                    InventoryProductCard(
                        articleId = articleId,
                        rows = rows,
                        imageFor = imageFor,
                        onStockAction = onStockAction
                    )
                }
            }
        }
    }
}

@Composable
private fun InventorySearchField(
    query: String,
    productCount: Int,
    onQueryChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.inventory_products_title),
            color = AppColors.Ink19251F,
            style = AppTypography.Title3
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(R.string.inventory_product_group_count, productCount),
            color = AppColors.Gray87918B,
            style = AppTypography.Caption
        )
    }

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = TextStyle(color = AppColors.Ink19251F, fontSize = 14.sp),
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(AppColors.White)
                    .border(1.dp, AppColors.GrayE9EEEA, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchMark()
                Spacer(modifier = Modifier.width(9.dp))
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.inventory_search_hint),
                            color = AppColors.Green69756D,
                            fontSize = 14.sp
                        )
                    }
                    innerTextField()
                }
            }
        }
    )
}

@Composable
private fun SearchMark() {
    Canvas(modifier = Modifier.size(17.dp)) {
        val strokeWidth = 1.6.dp.toPx()
        val radius = size.minDimension * 0.31f
        val center = Offset(size.width * 0.42f, size.height * 0.42f)
        drawCircle(
            color = AppColors.Gray99A29B,
            radius = radius,
            center = center,
            style = Stroke(strokeWidth)
        )
        drawLine(
            color = AppColors.Gray99A29B,
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
            .background(AppColors.White)
            .border(1.dp, AppColors.GrayE9EEEA, shape)
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
                    color = AppColors.Ink19251F,
                    style = AppTypography.BodyLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${first.dealer} · ${
                        stringResource(R.string.inventory_product_color_count, rows.size)
                    }",
                    color = AppColors.Gray87918B,
                    style = AppTypography.Micro,
                    modifier = Modifier.padding(top = 4.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.inventory_product_stock_total, totalQuantity),
                color = AppColors.Green246B50,
                style = AppTypography.CaptionEmphasis,
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(AppColors.GreenE8F2EC)
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
            .background(AppColors.GrayF6F8F6),
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
                    .border(0.5.dp, AppColors.GrayE9EEEA, CircleShape)
            )
            Text(
                text = inventory.color.displayName,
                color = AppColors.Ink19251F,
                style = AppTypography.LabelEmphasis,
                modifier = Modifier.padding(start = 7.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.inventory_size_stock),
                color = AppColors.Gray87918B,
                style = AppTypography.Micro
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AppColors.GrayF6F8F6)
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
                        color = AppColors.Gray919B94,
                        style = AppTypography.Micro
                    )
                    Text(
                        text = if (inStock) count.toString() else "—",
                        color = if (inStock) AppColors.Green34443A else AppColors.GrayC8CFCA,
                        style = if (inStock) {
                            AppTypography.Micro.copy(fontWeight = FontWeight.Bold)
                        } else {
                            AppTypography.Micro
                        },
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
                color = AppColors.Green7B877F,
                style = AppTypography.Micro
            )
            Text(
                text = stringResource(R.string.inventory_product_stock_total, subtotal),
                color = AppColors.Ink19251F,
                style = AppTypography.LabelBold,
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
            .background(AppColors.White)
            .border(1.dp, AppColors.GrayE9EEEA, shape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = AppColors.Green246B50, fontSize = 14.sp, lineHeight = 16.sp)
    }
}

@Composable
private fun InventoryDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .height(1.dp)
            .background(AppColors.GrayF0F2F0)
    )
}
