package com.example.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.ui.theme.AppTypography
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

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
                .widthIn(max = 560.dp),
            shape = RoundedCornerShape(24.dp),
            color = AppColors.GrayF6F8F6,
            shadowElevation = 14.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.White)
                        .padding(start = 20.dp, top = 17.dp, end = 16.dp, bottom = 15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            color = AppColors.Ink19251F,
                            style = AppTypography.Title2.copy(fontSize = 20.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (subtitle.isNotBlank()) {
                            Text(
                                text = subtitle,
                                color = AppColors.Gray87918B,
                                style = AppTypography.Body,
                                modifier = Modifier.padding(top = 4.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    content = content
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.White)
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    content = { footer() }
                )
            }
        }
    }
}
