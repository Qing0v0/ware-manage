package com.example.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.ui.theme.AppColors

@Composable
fun AppBrandHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.warehouse),
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(AppColors.Green246B50)
                .padding(9.dp),
            tint = AppColors.White
        )
        Spacer(modifier = Modifier.size(10.dp))
        Column {
            Text(
                text = stringResource(R.string.app_name),
                color = AppColors.Ink19251F,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.25.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "WAREHOUSE · INVENTORY",
                color = AppColors.Gray87918B,
                fontSize = 10.sp,
                letterSpacing = 0.2.sp
            )
        }
    }
}
