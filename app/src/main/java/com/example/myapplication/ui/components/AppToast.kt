package com.example.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.ui.theme.AppDimensions
import com.example.myapplication.ui.theme.AppTypography
import kotlinx.coroutines.delay

@Composable
fun AppToast(
    message: String?,
    messageId: Int,
    onDismissRequest: () -> Unit
) {
    LaunchedEffect(message, messageId) {
        if (message != null) {
            delay(2_500)
            onDismissRequest()
        }
    }

    if (message == null) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = AppDimensions.Size.Dp32),
        contentAlignment = Alignment.BottomCenter
    ) {
        Text(
            text = message,
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(AppColors.Gray700)
                .padding(
                    horizontal = AppDimensions.Padding.Dp12,
                    vertical = AppDimensions.Padding.Dp8
                ),
            color = AppColors.White,
            style = AppTypography.Size14.copy(fontWeight = FontWeight.Medium)
        )
    }
}
