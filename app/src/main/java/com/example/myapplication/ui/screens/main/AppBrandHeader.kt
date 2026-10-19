package com.example.myapplication.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.ui.theme.AppDimensions
import com.example.myapplication.ui.theme.AppTypography

@Composable
fun AppBrandHeader(
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.warehouse),
                contentDescription = null,
                modifier = Modifier
                    .size(AppDimensions.Size.Dp40)
                    .clip(AppDimensions.Corner.small)
                    .background(AppColors.Green800)
                    .padding(AppDimensions.Padding.Dp8),
                tint = AppColors.White
            )
            Spacer(modifier = Modifier.size(8.dp))
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    color = AppColors.Gray900,
                    style = AppTypography.Size14.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "WAREHOUSE · INVENTORY",
                    color = AppColors.Gray500,
                    style = AppTypography.Size10
                )
            }
        }
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .size(AppDimensions.Size.Dp40)
                .clip(AppDimensions.Corner.small)
                .background(AppColors.White)
                .border(1.dp, AppColors.Gray100, AppDimensions.Corner.small)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_more),
                contentDescription = stringResource(R.string.more_options),
                modifier = Modifier.size(18.dp),
                tint = AppColors.Gray500
            )
        }
    }
}
