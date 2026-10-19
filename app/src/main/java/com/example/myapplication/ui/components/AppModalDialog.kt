package com.example.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.ui.theme.AppDimensions
import com.example.myapplication.ui.theme.AppTypography

@Composable
fun AppModalDialog(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    footer: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.9f)
                .widthIn(max = AppDimensions.Size.Dp560),
            shape = AppDimensions.Corner.large,
            color = AppColors.Gray50,
            shadowElevation = 14.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.White)
                        .padding(start = AppDimensions.Padding.Dp20, top = AppDimensions.Padding.Dp20, end = AppDimensions.Padding.Dp16, bottom = AppDimensions.Padding.Dp16),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            color = AppColors.Gray900,
                            style = AppTypography.Size20.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (subtitle.isNotBlank()) {
                            Text(
                                text = subtitle,
                                color = AppColors.Gray500,
                                style = AppTypography.Size12,
                                modifier = Modifier.padding(top = AppDimensions.Padding.Dp4),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = AppDimensions.Padding.Dp16, vertical = AppDimensions.Padding.Dp12),
                    content = content
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.White)
                        .padding(horizontal = AppDimensions.Padding.Dp16, vertical = AppDimensions.Padding.Dp16),
                    content = { footer() }
                )
            }
        }
    }
}
