@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myapplication.model.OrderType
import com.example.myapplication.model.ShoeInventory
import com.example.myapplication.R
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.ui.theme.AppDimensions
import com.example.myapplication.ui.theme.AppTypography
import com.example.myapplication.ui.components.hideKeyboardOnTap
import com.example.myapplication.utils.SizeUtils

internal fun inventoryProductGroups(
    inventories: List<ShoeInventory>,
    searchQuery: String
): List<Map.Entry<String, List<ShoeInventory>>> {
    val keyword = searchQuery.trim()
    return inventories
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

@Composable
internal fun InventoryProductList(
    groups: List<Map.Entry<String, List<ShoeInventory>>>,
    imageFor: (String) -> ImageBitmap?,
    onStockAction: (OrderType, ShoeInventory) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = AppDimensions.Padding.Dp16).hideKeyboardOnTap()) {
        if (groups.isEmpty()) {
            Text(
                text = stringResource(R.string.inventory_empty),
                color = AppColors.Gray500,
                style = AppTypography.Size14,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = AppDimensions.Padding.Dp8,
                        bottom = AppDimensions.Padding.Dp16
                    )
            )
        } else {
            groups.forEach { (articleId, rows) ->
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

@Composable
internal fun InventorySearchField(
    query: String,
    productCount: Int,
    onQueryChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = AppDimensions.Padding.Dp16,
                vertical = AppDimensions.Padding.Dp8
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.inventory_products_title),
            color = AppColors.Gray900,
            style = AppTypography.Size16.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(R.string.inventory_product_group_count, productCount),
            color = AppColors.Gray500,
            style = AppTypography.Size10
        )
    }

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = AppTypography.Size14.copy(color = AppColors.Gray900),
        modifier = Modifier
            .fillMaxWidth()
            .height(AppDimensions.Size.Dp40)
            .padding(start = AppDimensions.Padding.Dp16, end = AppDimensions.Padding.Dp16, bottom = AppDimensions.Padding.Dp8),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(AppDimensions.Corner.large)
                    .background(AppColors.White)
                    .border(1.dp, AppColors.Gray100, AppDimensions.Corner.small)
                    .padding(horizontal = AppDimensions.Padding.Dp12),
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
                            color = AppColors.Gray500,
                            style = AppTypography.Size14
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
            color = AppColors.Gray500,
            radius = radius,
            center = center,
            style = Stroke(strokeWidth)
        )
        drawLine(
            color = AppColors.Gray500,
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
    val shape = AppDimensions.Corner.large
    val first = rows.first()
    val totalQuantity = rows.sumOf { inventory -> SizeUtils.sizesOf(inventory).sum() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = AppDimensions.Padding.Dp8)
            .clip(shape)
            .background(AppColors.White)
            .border(1.dp, AppColors.Gray100, shape)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = AppDimensions.Padding.Dp16, vertical = AppDimensions.Padding.Dp12),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProductThumbnail(inventory = first, imageFor = imageFor)
            Spacer(modifier = Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = articleId,
                    color = AppColors.Gray900,
                    style = AppTypography.Size12.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${first.dealer} · ${
                        stringResource(R.string.inventory_product_color_count, rows.size)
                    } · ${
                        stringResource(R.string.inventory_product_purchase_price,
                            first.purchasePrice.toString()
                        )
                    }",
                    color = AppColors.Gray500,
                    style = AppTypography.Size10,
                    modifier = Modifier.padding(top = AppDimensions.Padding.Dp4),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.inventory_product_stock_total, totalQuantity),
                color = AppColors.Green800,
                style = AppTypography.Size10.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .clip(AppDimensions.Corner.small)
                    .background(AppColors.Green50)
                    .padding(horizontal = AppDimensions.Padding.Dp8, vertical = AppDimensions.Padding.Dp8),
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
    val shape = AppDimensions.Corner.small
    Box(
        modifier = Modifier
            .size(AppDimensions.Size.Dp36)
            .clip(shape)
            .background(AppColors.Gray50),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = inventory.imagePath?.let(imageFor)
        val imageModifier = Modifier.fillMaxSize()
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = inventory.articleId,
                contentScale = ContentScale.Crop,
                modifier = imageModifier
            )
        } else {
            Image(
                painter = painterResource(R.drawable.ic_shoe_placeholder),
                contentDescription = inventory.articleId,
                contentScale = ContentScale.Fit,
                modifier = imageModifier
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = AppDimensions.Padding.Dp12, vertical = AppDimensions.Padding.Dp12)
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
                    .border(0.5.dp, AppColors.Gray100, CircleShape)
            )
            Text(
                text = inventory.color.displayName,
                color = AppColors.Gray900,
                style = AppTypography.Size10.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(start = AppDimensions.Padding.Dp8)
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.inventory_size_stock),
                color = AppColors.Gray500,
                style = AppTypography.Size10
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppDimensions.Padding.Dp8)
                .clip(AppDimensions.Corner.small)
                .background(AppColors.Gray50)
                .padding(horizontal = AppDimensions.Padding.Dp8, vertical = AppDimensions.Padding.Dp8)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.Padding.Dp8)
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
                        color = AppColors.Gray500,
                        style = AppTypography.Size10
                    )
                    Text(
                        text = if (inStock) count.toString() else "—",
                        color = if (inStock) AppColors.Gray800 else AppColors.Gray200,
                        style = if (inStock) {
                            AppTypography.Size10.copy(fontWeight = FontWeight.Bold)
                        } else {
                            AppTypography.Size10
                        },
                        modifier = Modifier.padding(top = AppDimensions.Padding.Dp4)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = AppDimensions.Padding.Dp12),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.inventory_color_subtotal),
                color = AppColors.Gray500,
                style = AppTypography.Size10
            )
            Text(
                text = stringResource(R.string.inventory_product_stock_total, subtotal),
                color = AppColors.Gray900,
                style = AppTypography.Size10.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(start = AppDimensions.Padding.Dp4)
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
    val shape = AppDimensions.Corner.small
    Box(
        modifier = Modifier
            .size(width = 27.dp, height = 25.dp)
            .clip(shape)
            .background(AppColors.White)
            .border(1.dp, AppColors.Gray100, shape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = AppColors.Green800, style = AppTypography.Size14)
    }
}

@Composable
private fun InventoryDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimensions.Padding.Dp12)
            .height(1.dp)
            .background(AppColors.Gray100)
    )
}
