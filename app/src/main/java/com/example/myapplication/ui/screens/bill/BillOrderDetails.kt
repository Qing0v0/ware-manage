@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.example.myapplication.ui.screens.bill

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.myapplication.ui.components.HideKeyboardOnScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myapplication.model.OrderBatch
import com.example.myapplication.R
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.ui.theme.AppDimensions
import com.example.myapplication.ui.theme.AppTypography
import com.example.myapplication.utils.SizeUtils

@Composable
internal fun BillOrderDetails(
    bills: List<OrderBatch>,
    hintId: Int,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    HideKeyboardOnScroll(listState.isScrollInProgress)
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimensions.Padding.Dp16)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppDimensions.Padding.Dp8),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.bills_order_details),
                    color = AppColors.Gray900,
                    style = AppTypography.Size16.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.bills_order_count, bills.size),
                    color = AppColors.Gray500,
                    style = AppTypography.Size10
                )
            }
            if (hintId != HIDE_BILL_HINT) {
                Text(
                    text = stringResource(hintId),
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
            }
        }
        items(items = bills, key = { it.orderId }) { bill ->
            BillOrderCard(bill)
        }
    }
}

@Composable
private fun BillOrderCard(bill: OrderBatch) {
    val isPurchase = bill.orderType.increaseStock
    val badgeTextColor =
        if (isPurchase) AppColors.Green700 else AppColors.Red700
    val badgeBackground =
        if (isPurchase) AppColors.Green100 else AppColors.Red100
    val instant = bill.date.toInstant()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = AppDimensions.Padding.Dp8)
            .border(1.dp, AppColors.Gray100, AppDimensions.Corner.large)
            .background(AppColors.White, AppDimensions.Corner.large)
            .padding(horizontal = AppDimensions.Padding.Dp12, vertical = AppDimensions.Padding.Dp12)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(AppDimensions.Size.Dp36)
                    .background(
                        if (isPurchase) AppColors.Green100 else AppColors.Red100,
                        AppDimensions.Corner.small
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(
                        if (isPurchase) R.drawable.ic_bill_in else R.drawable.ic_bill_out
                    ),
                    contentDescription = bill.orderType.displayName,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = bill.articleId,
                        color = AppColors.Gray900,
                        style = AppTypography.Size16.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = bill.orderType.displayName,
                        color = badgeTextColor,
                        style = AppTypography.Size10.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier
                            .padding(start = AppDimensions.Padding.Dp8)
                            .background(badgeBackground, AppDimensions.Corner.small)
                            .padding(horizontal = AppDimensions.Padding.Dp8, vertical = AppDimensions.Padding.Dp4)
                    )
                }
                Row(
                    modifier = Modifier.padding(top = AppDimensions.Padding.Dp8),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(AppColors.shoeColor(bill.color), CircleShape)
                            .border(0.5.dp, AppColors.BlackTransparent, CircleShape)
                    )
                    Text(
                        text = "${bill.color.displayName} · ${bill.dealer}",
                        color = AppColors.Gray500,
                        style = AppTypography.Size10,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = AppDimensions.Padding.Dp8)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "¥ ${billAmountText(billOrderTotal(bill))}",
                    color = AppColors.Gray900,
                    style = AppTypography.Size16.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
                Text(
                    text = "¥${billAmountText(bill.price)} / 双",
                    color = AppColors.Gray500,
                    style = AppTypography.Size10,
                    modifier = Modifier.padding(top = AppDimensions.Padding.Dp4)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppDimensions.Padding.Dp12, bottom = AppDimensions.Padding.Dp12)
                .background(AppColors.Gray50, AppDimensions.Corner.small)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = AppDimensions.Padding.Dp8, vertical = AppDimensions.Padding.Dp8),
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.Padding.Dp8)
        ) {
            val sizes = SizeUtils.sizesOf(bill)
            for (index in 0 until SizeUtils.SIZE_AMOUNT) {
                Column(
                    modifier = Modifier.width(26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = SizeUtils.sizeName(index).toString(),
                        color = AppColors.Gray500,
                        style = AppTypography.Size10
                    )
                    Text(
                        text = if (sizes[index] == 0) "—" else sizes[index].toString(),
                        color = if (sizes[index] == 0) AppColors.Gray200 else AppColors.Gray800,
                        style = if (sizes[index] == 0) {
                            AppTypography.Size10
                        } else {
                            AppTypography.Size10.copy(fontWeight = FontWeight.Bold)
                        },
                        modifier = Modifier.padding(top = AppDimensions.Padding.Dp4)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(AppColors.Gray100)
        )
        Text(
            text = "${billDateLineFormatter.format(instant)}  ${billTimeLineFormatter.format(instant)}",
            color = AppColors.Gray500,
            style = AppTypography.Size10,
            textAlign = TextAlign.End,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppDimensions.Padding.Dp8)
        )
    }
}
