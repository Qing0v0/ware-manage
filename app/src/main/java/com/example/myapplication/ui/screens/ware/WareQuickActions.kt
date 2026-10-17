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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.ui.theme.AppTypography

@Composable
internal fun WareQuickActions(
    onPurchase: () -> Unit,
    onSelling: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
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
    val shape = RoundedCornerShape(15.dp)
    Row(
        modifier = modifier
            .height(58.dp)
            .clip(shape)
            .background(AppColors.White)
            .border(1.dp, AppColors.GrayE9EEEA, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(AppColors.GreenE8F2EC),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = symbol,
                color = AppColors.Green246B50,
                fontSize = 18.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Column {
            Text(
                text = title,
                color = AppColors.Ink19251F,
                style = AppTypography.BodyEmphasis
            )
            Text(
                text = subtitle,
                color = AppColors.Gray87918B,
                style = AppTypography.Micro,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
    }
}
