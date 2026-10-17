package com.example.myapplication.ui.screens.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.AppColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PageDateFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)

@Composable
fun PageTitleHeader(
    eyebrow: String,
    title: String,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now().format(PageDateFormatter) }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column {
            Text(
                text = eyebrow,
                color = AppColors.Green246B50,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.15.sp
            )
            Text(
                text = title,
                color = AppColors.Ink19251F,
                fontSize = 26.sp,
                lineHeight = 29.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.8).sp,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
        Row(
            modifier = Modifier.padding(bottom = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CalendarMark()
            Spacer(modifier = Modifier.size(5.dp))
            Text(
                text = today,
                color = AppColors.Green77827B,
                fontSize = 10.sp
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
                    color = AppColors.Green77827B,
                    topLeft = Offset(stroke / 2, stroke * 2),
                    size = androidx.compose.ui.geometry.Size(
                        size.width - stroke,
                        size.height - stroke * 2.5f
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(stroke)
                )
                drawLine(
                    color = AppColors.Green77827B,
                    start = Offset(stroke, size.height * 0.42f),
                    end = Offset(size.width - stroke, size.height * 0.42f),
                    strokeWidth = stroke
                )
                drawLine(
                    color = AppColors.Green77827B,
                    start = Offset(size.width * 0.32f, 0f),
                    end = Offset(size.width * 0.32f, size.height * 0.28f),
                    strokeWidth = stroke
                )
                drawLine(
                    color = AppColors.Green77827B,
                    start = Offset(size.width * 0.68f, 0f),
                    end = Offset(size.width * 0.68f, size.height * 0.28f),
                    strokeWidth = stroke
                )
            }
    )
}
