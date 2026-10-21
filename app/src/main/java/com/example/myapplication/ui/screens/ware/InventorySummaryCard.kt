package com.example.myapplication.ui.screens.ware

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.ui.theme.AppDimensions
import com.example.myapplication.ui.theme.AppTypography

@Composable
internal fun InventorySummaryCard(
    totalQuantity: String,
    styleCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = AppDimensions.Padding.Dp16, end = AppDimensions.Padding.Dp16, bottom = AppDimensions.Padding.Dp8)
            .clip(AppDimensions.Corner.large)
            .background(AppColors.Green800)
            .padding(horizontal = AppDimensions.Padding.Dp16, vertical = AppDimensions.Padding.Dp12)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.Padding.Dp8)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(AppColors.Green400, CircleShape)
            )
            Text(
                text = stringResource(R.string.inventory_total_label),
                color = AppColors.Green200,
                style = AppTypography.Size12.copy(fontWeight = FontWeight.SemiBold)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = AppDimensions.Padding.Dp8),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = totalQuantity,
                color = AppColors.White,
                style = AppTypography.Size30.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = stringResource(R.string.inventory_quantity_unit),
                color = AppColors.White,
                style = AppTypography.Size14,
                modifier = Modifier.padding(start = AppDimensions.Padding.Dp4, top = AppDimensions.Padding.Dp8)
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.inventory_total_count_prefix),
                color = AppColors.Green200,
                style = AppTypography.Size12
            )
            Text(
                text = styleCount.toString(),
                color = AppColors.White,
                style = AppTypography.Size16.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = AppDimensions.Padding.Dp4)
            )
            Text(
                text = stringResource(R.string.inventory_total_count_suffix),
                color = AppColors.Green200,
                style = AppTypography.Size12
            )
        }
    }
}
