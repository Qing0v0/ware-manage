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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import com.example.myapplication.model.OrderBatch
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.ui.theme.AppTypography
import com.example.myapplication.utils.SizeUtils

@Composable
internal fun BillOrderDetails(
    bills: List<OrderBatch>,
    hintId: Int,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.bills_order_details),
                    color = AppColors.Ink26352B,
                    style = AppTypography.Title2
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.bills_order_count, bills.size),
                    color = AppColors.Gray87918B,
                    style = AppTypography.Body
                )
            }
            if (hintId != HIDE_BILL_HINT) {
                Text(
                    text = stringResource(hintId),
                    color = AppColors.Green69756D,
                    style = AppTypography.BodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 16.dp)
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
        if (isPurchase) AppColors.Green2E7D32 else AppColors.RedC62828
    val badgeBackground =
        if (isPurchase) AppColors.GreenD8F2DC else AppColors.RedF9D9D9
    val instant = bill.date.toInstant()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 9.dp)
            .border(1.dp, AppColors.GrayE9EEEA, RoundedCornerShape(16.dp))
            .background(AppColors.White, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 11.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        if (isPurchase) AppColors.GreenD8F2DC else AppColors.RedF9D9D9,
                        RoundedCornerShape(11.dp)
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
                        color = AppColors.Ink26352B,
                        style = AppTypography.Title3,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = bill.orderType.displayName,
                        color = badgeTextColor,
                        style = AppTypography.LabelEmphasis,
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .background(badgeBackground, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
                Row(
                    modifier = Modifier.padding(top = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(AppColors.shoeColor(bill.color), CircleShape)
                            .border(0.5.dp, AppColors.TransparentBlack33, CircleShape)
                    )
                    Text(
                        text = "${bill.color.displayName} · ${bill.dealer}",
                        color = AppColors.Green7B877F,
                        style = AppTypography.Label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 5.dp)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "¥ ${billAmountText(billOrderTotal(bill))}",
                    color = AppColors.Ink26352B,
                    style = AppTypography.Title3,
                    maxLines = 1
                )
                Text(
                    text = "¥${billAmountText(bill.price)} / 双",
                    color = AppColors.Gray8A958D,
                    style = AppTypography.Caption,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 9.dp)
                .background(AppColors.GrayF6F8F6, RoundedCornerShape(10.dp))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val sizes = SizeUtils.sizesOf(bill)
            for (index in 0 until SizeUtils.SIZE_AMOUNT) {
                Column(
                    modifier = Modifier.width(26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = SizeUtils.sizeName(index).toString(),
                        color = AppColors.Gray919B94,
                        style = AppTypography.Caption
                    )
                    Text(
                        text = if (sizes[index] == 0) "—" else sizes[index].toString(),
                        color = if (sizes[index] == 0) AppColors.GrayC8CFCA else AppColors.Green34443A,
                        style = if (sizes[index] == 0) {
                            AppTypography.Label
                        } else {
                            AppTypography.LabelBold
                        },
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(AppColors.GrayF0F2F0)
        )
        Text(
            text = "${billDateLineFormatter.format(instant)}  ${billTimeLineFormatter.format(instant)}",
            color = AppColors.Gray87918B,
            style = AppTypography.Caption,
            textAlign = TextAlign.End,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
    }
}
