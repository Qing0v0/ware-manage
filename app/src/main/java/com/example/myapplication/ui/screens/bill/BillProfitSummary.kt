package com.example.myapplication.ui.screens.bill

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.model.OrderBatch
import com.example.myapplication.R
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.ui.theme.AppDimensions
import com.example.myapplication.ui.theme.AppTypography
import java.time.LocalDate

@Composable
internal fun BillProfitSummary(
    startDate: LocalDate,
    endDate: LocalDate,
    bills: List<OrderBatch>,
    modifier: Modifier = Modifier
) {
    val totals = remember(bills) { calculateBillTotals(bills) }
    val profit = totals.profit

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.White)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(AppColors.Gray100)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = AppDimensions.Padding.Dp16, top = AppDimensions.Padding.Dp12, end = AppDimensions.Padding.Dp16),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.ic_profit_trend),
                contentDescription = null,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = stringResource(R.string.bills_total_profit),
                color = AppColors.Gray500,
                style = AppTypography.Size12.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(start = AppDimensions.Padding.Dp8)
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${startDate.format(billSummaryDateFormatter)} — ${endDate.format(billSummaryDateFormatter)}",
                color = AppColors.Gray500,
                style = AppTypography.Size12
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = AppDimensions.Padding.Dp16, top = AppDimensions.Padding.Dp4, end = AppDimensions.Padding.Dp16, bottom = AppDimensions.Padding.Dp12),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "${if (profit.signum() < 0) "-" else "+"}¥ ${billAmountText(profit.abs())}",
                color = if (profit.signum() < 0) AppColors.Red700 else AppColors.Green700,
                style = AppTypography.Size20.copy(fontWeight = FontWeight.Bold),
                maxLines = 1
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = AppDimensions.Padding.Dp4)
            ) {
                Text(
                    text = "${stringResource(R.string.selling_button)} ",
                    color = AppColors.Gray500,
                    style = AppTypography.Size12
                )
                Text(
                    text = "¥${billAmountText(totals.income)}",
                    color = AppColors.Gray500,
                    style = AppTypography.Size12.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${stringResource(R.string.purchase_button)} ",
                    color = AppColors.Gray500,
                    style = AppTypography.Size12
                )
                Text(
                    text = "¥${billAmountText(totals.cost)}",
                    color = AppColors.Gray500,
                    style = AppTypography.Size12.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}
