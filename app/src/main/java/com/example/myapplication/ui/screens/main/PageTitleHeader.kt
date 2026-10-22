package com.example.myapplication.ui.screens.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.ui.theme.AppDimensions
import com.example.myapplication.ui.theme.AppTypography
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PageDateFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)

@Composable
fun PageTitleHeader(
    eyebrow: String,
    title: String,
) {
    val today = remember { LocalDate.now().format(PageDateFormatter) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(
            start = AppDimensions.Padding.Dp16,
            top = AppDimensions.Padding.Dp4,
            end = AppDimensions.Padding.Dp16,
            bottom = AppDimensions.Padding.Dp12
        ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column {
            Text(
                text = eyebrow,
                color = AppColors.Green800,
                style = AppTypography.Size10.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = title,
                color = AppColors.Gray900,
                style = AppTypography.Size24.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(top = AppDimensions.Padding.Dp4)
            )
        }
        Row(
            modifier = Modifier.padding(bottom = AppDimensions.Padding.Dp4),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CalendarMark()
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = today,
                color = AppColors.Gray500,
                style = AppTypography.Size10
            )
        }
    }
}

@Composable
private fun CalendarMark() {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(width = 13.dp, height = 13.dp)
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawRoundRect(
                    color = AppColors.Gray500,
                    topLeft = Offset(stroke / 2, stroke * 2),
                    size = androidx.compose.ui.geometry.Size(
                        size.width - stroke,
                        size.height - stroke * 2.5f
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(stroke)
                )
                drawLine(
                    color = AppColors.Gray500,
                    start = Offset(stroke, size.height * 0.42f),
                    end = Offset(size.width - stroke, size.height * 0.42f),
                    strokeWidth = stroke
                )
                drawLine(
                    color = AppColors.Gray500,
                    start = Offset(size.width * 0.32f, 0f),
                    end = Offset(size.width * 0.32f, size.height * 0.28f),
                    strokeWidth = stroke
                )
                drawLine(
                    color = AppColors.Gray500,
                    start = Offset(size.width * 0.68f, 0f),
                    end = Offset(size.width * 0.68f, size.height * 0.28f),
                    strokeWidth = stroke
                )
            }
    )
}
