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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.ui.theme.AppTypography

@Composable
internal fun InventorySummaryCard(
    totalQuantity: String,
    styleCount: Int
) {
    val shape = RoundedCornerShape(18.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(shape)
            .background(AppColors.Green276E52)
            .padding(horizontal = 16.dp, vertical = 13.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(AppColors.Green8DE0AD, CircleShape)
            )
            Text(
                text = stringResource(R.string.inventory_total_label),
                color = AppColors.MintD0E1D8,
                style = AppTypography.BodyEmphasis
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = totalQuantity,
                color = AppColors.White,
                fontSize = 30.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.8).sp
            )
            Text(
                text = stringResource(R.string.inventory_quantity_unit),
                color = AppColors.White,
                style = AppTypography.BodyLarge,
                modifier = Modifier.padding(start = 3.dp, top = 7.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.inventory_total_count_prefix),
                color = AppColors.MintD0E1D8,
                style = AppTypography.Body
            )
            Text(
                text = styleCount.toString(),
                color = AppColors.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Text(
                text = stringResource(R.string.inventory_total_count_suffix),
                color = AppColors.MintD0E1D8,
                style = AppTypography.Body
            )
        }
    }
}
