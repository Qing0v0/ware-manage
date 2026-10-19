package com.example.myapplication.ui.screens.ware

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.ui.theme.AppDimensions
import com.example.myapplication.ui.theme.AppTypography

@Composable
internal fun WareQuickActions(
    onPurchase: () -> Unit,
    onSelling: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = AppDimensions.Padding.Dp16, end = AppDimensions.Padding.Dp16),
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.Padding.Dp8)
    ) {
        WareQuickAction(
            title = stringResource(R.string.purchase_button),
            subtitle = stringResource(R.string.inventory_purchase_caption),
            symbol = "+",
            onClick = onPurchase,
            modifier = Modifier.weight(1f)
        )
        WareQuickAction(
            title = stringResource(R.string.selling_button),
            subtitle = stringResource(R.string.inventory_selling_caption),
            symbol = "-",
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = AppDimensions.Corner.large
    Row(
        modifier = modifier
            .height(AppDimensions.Size.Dp56)
            .clip(shape)
            .background(AppColors.White)
            .border(1.dp, AppColors.Gray100, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = AppDimensions.Padding.Dp12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.Padding.Dp12)
    ) {
        Box(
            modifier = Modifier
                .size(AppDimensions.Size.Dp32)
                .clip(AppDimensions.Corner.small)
                .background(AppColors.Green50),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = symbol,
                color = AppColors.Green800,
                style = AppTypography.Size16.copy(fontWeight = FontWeight.Medium)
            )
        }
        Column {
            Text(
                text = title,
                color = AppColors.Gray900,
                style = AppTypography.Size12.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = subtitle,
                color = AppColors.Gray500,
                style = AppTypography.Size10,
                modifier = Modifier.padding(top = AppDimensions.Padding.Dp4)
            )
        }
    }
}
