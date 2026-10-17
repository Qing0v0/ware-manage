package com.example.myapplication.ui.screens.bill

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import com.example.myapplication.ui.components.DropdownSelector
import com.example.myapplication.ui.screens.main.PageTitleHeader
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.ui.theme.AppTypography
import java.time.LocalDate

@Composable
internal fun BillQueryCard(
    startDate: LocalDate,
    endDate: LocalDate,
    articleId: String,
    dealer: String,
    selectedTypeIndex: Int,
    orderTypeFilters: List<Pair<String, List<String>>>,
    onArticleIdChange: (String) -> Unit,
    onDealerChange: (String) -> Unit,
    onTypeSelected: (Int) -> Unit,
    onStartDateClick: () -> Unit,
    onEndDateClick: () -> Unit,
    onQuery: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.GrayF5F7F5)
            .padding(horizontal = 16.dp)
    ) {
        PageTitleHeader(
            eyebrow = stringResource(R.string.bills_transaction_history),
            title = stringResource(R.string.bills_title),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AppColors.GrayE9EEEA, RoundedCornerShape(17.dp))
                .background(AppColors.White, RoundedCornerShape(17.dp))
                .padding(12.dp)
        ) {
            Text(
                text = stringResource(R.string.bills_query_orders),
                color = AppColors.Green77827B,
                style = AppTypography.CaptionSemibold,
                modifier = Modifier.padding(start = 2.dp, bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BillFilterInput(
                    value = articleId,
                    onValueChange = onArticleIdChange,
                    hint = stringResource(R.string.purchase_order_article_id),
                    modifier = Modifier.weight(1f).height(36.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                BillFilterInput(
                    value = dealer,
                    onValueChange = onDealerChange,
                    hint = stringResource(R.string.purchase_order_dealer),
                    modifier = Modifier.weight(1f).height(36.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                DropdownSelector(
                    selectedLabel = orderTypeFilters[selectedTypeIndex].first,
                    options = orderTypeFilters.map { it.first },
                    onSelect = onTypeSelected,
                    modifier = Modifier.weight(1f).height(36.dp)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BillDateBox(
                    date = startDate,
                    onClick = onStartDateClick,
                    modifier = Modifier.weight(1f).height(36.dp)
                )
                Text(
                    text = stringResource(R.string.bills_date_separator),
                    color = AppColors.GrayA4ACA6,
                    style = AppTypography.Body,
                    modifier = Modifier.padding(horizontal = 5.dp)
                )
                BillDateBox(
                    date = endDate,
                    onClick = onEndDateClick,
                    modifier = Modifier.weight(1f).height(36.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onQuery,
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(11.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Green246B50,
                        contentColor = AppColors.White
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                ) {
                    Text(stringResource(R.string.bills_query))
                }
            }
        }
    }
}

@Composable
private fun BillFilterInput(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = AppTypography.Body.copy(color = AppColors.Green36443B),
        modifier = modifier,
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, AppColors.GrayE9EEEA, RoundedCornerShape(10.dp))
                    .background(AppColors.White, RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = hint,
                        color = AppColors.Gray8F9992,
                        style = AppTypography.Body,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
private fun BillDateBox(
    date: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .border(1.dp, AppColors.GrayE9EEEA, RoundedCornerShape(11.dp))
            .background(AppColors.White, RoundedCornerShape(11.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.format(billDateFormatter),
            color = AppColors.Green36443B,
            style = AppTypography.Body,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}
