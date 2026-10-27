@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import com.example.myapplication.ui.components.DropdownSelector
import com.example.myapplication.ui.components.hideKeyboard
import com.example.myapplication.ui.components.hideKeyboardOnTap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.example.myapplication.ui.screens.main.PageTitleHeader
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.ui.theme.AppDimensions
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
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.Gray50)
            .padding(horizontal = AppDimensions.Padding.Dp16)
            .hideKeyboardOnTap()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AppColors.Gray100, AppDimensions.Corner.large)
                .background(AppColors.White, AppDimensions.Corner.large)
                .padding(AppDimensions.Padding.Dp12)
        ) {
            Text(
                text = stringResource(R.string.bills_query_orders),
                color = AppColors.Gray500,
                style = AppTypography.Size10.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(start = AppDimensions.Padding.Dp4, bottom = AppDimensions.Padding.Dp8)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BillFilterInput(
                    value = articleId,
                    onValueChange = onArticleIdChange,
                    hint = stringResource(R.string.purchase_order_article_id),
                    modifier = Modifier.weight(1f).height(AppDimensions.Size.Dp36)
                )
                Spacer(modifier = Modifier.width(6.dp))
                BillFilterInput(
                    value = dealer,
                    onValueChange = onDealerChange,
                    hint = stringResource(R.string.purchase_order_dealer),
                    modifier = Modifier.weight(1f).height(AppDimensions.Size.Dp36)
                )
                Spacer(modifier = Modifier.width(6.dp))
                DropdownSelector(
                    selectedLabel = orderTypeFilters[selectedTypeIndex].first,
                    options = orderTypeFilters.map { it.first },
                    onSelect = onTypeSelected,
                    modifier = Modifier.weight(1f).height(AppDimensions.Size.Dp36)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = AppDimensions.Padding.Dp8),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BillDateBox(
                    date = startDate,
                    onClick = onStartDateClick,
                    modifier = Modifier.weight(1f).height(AppDimensions.Size.Dp36)
                )
                Text(
                    text = stringResource(R.string.bills_date_separator),
                    color = AppColors.Gray450,
                    style = AppTypography.Size12,
                    modifier = Modifier.padding(horizontal = AppDimensions.Padding.Dp8)
                )
                BillDateBox(
                    date = endDate,
                    onClick = onEndDateClick,
                    modifier = Modifier.weight(1f).height(AppDimensions.Size.Dp36)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        hideKeyboard(focusManager, keyboardController)
                        onQuery()
                    },
                    modifier = Modifier.height(AppDimensions.Size.Dp36),
                    shape = AppDimensions.Corner.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Green800,
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
        textStyle = AppTypography.Size12.copy(color = AppColors.Gray800),
        modifier = modifier,
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, AppColors.Gray100, AppDimensions.Corner.small)
                    .background(AppColors.White, AppDimensions.Corner.small)
                    .padding(horizontal = AppDimensions.Padding.Dp8),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = hint,
                        color = AppColors.Gray500,
                        style = AppTypography.Size12,
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
            .height(AppDimensions.Size.Dp36)
            .border(1.dp, AppColors.Gray100, AppDimensions.Corner.small)
            .background(AppColors.White, AppDimensions.Corner.small)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.format(billDateFormatter),
            color = AppColors.Gray800,
            style = AppTypography.Size12,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}
