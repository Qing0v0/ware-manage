package com.example.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.myapplication.R
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.ui.theme.AppDimensions
import com.example.myapplication.ui.theme.AppTypography
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth

@Composable
fun CustomCalendarDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    var selectedDate by remember(initialDate) { mutableStateOf(initialDate) }
    var visibleMonth by remember(initialDate) { mutableStateOf(YearMonth.from(initialDate)) }
    val today = remember { LocalDate.now(ZoneId.of("Asia/Shanghai")) }
    val weekdays = listOf("日", "一", "二", "三", "四", "五", "六")

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(AppDimensions.Size.Dp300)
                .clip(AppDimensions.Corner.large)
                .background(AppColors.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.Green800)
                    .padding(horizontal = AppDimensions.Padding.Dp20, vertical = AppDimensions.Padding.Dp16)
            ) {
                Text(
                    text = selectedDate.year.toString(),
                    color = AppColors.White.copy(alpha = 0.82f),
                    style = AppTypography.Size12.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "${selectedDate.monthValue}月${selectedDate.dayOfMonth}日，${weekdays[selectedDate.dayOfWeek.value % 7]}",
                    color = AppColors.White,
                    style = AppTypography.Size24.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = AppDimensions.Padding.Dp4)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppDimensions.Padding.Dp16, vertical = AppDimensions.Padding.Dp8)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${visibleMonth.year}年${visibleMonth.monthValue}月",
                        color = AppColors.Gray900,
                        style = AppTypography.Size14.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f)
                    )
                    CalendarMonthButton(
                        label = "«",
                        contentDescription = "上一年",
                        onClick = { visibleMonth = visibleMonth.minusYears(1) }
                    )
                    CalendarMonthButton(
                        label = "‹",
                        contentDescription = "上个月",
                        onClick = { visibleMonth = visibleMonth.minusMonths(1) }
                    )
                    CalendarMonthButton(
                        label = "›",
                        contentDescription = "下个月",
                        onClick = { visibleMonth = visibleMonth.plusMonths(1) }
                    )
                    CalendarMonthButton(
                        label = "»",
                        contentDescription = "下一年",
                        onClick = { visibleMonth = visibleMonth.plusYears(1) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    weekdays.forEach { day ->
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text(
                                text = day,
                                color = AppColors.Gray500,
                                style = AppTypography.Size10.copy(fontWeight = FontWeight.Medium),
                                modifier = Modifier.height(26.dp)
                            )
                        }
                    }
                }

                val firstDay = visibleMonth.atDay(1)
                val leadingDays = firstDay.dayOfWeek.value % 7
                val cellCount = ((leadingDays + visibleMonth.lengthOfMonth() + 6) / 7) * 7
                val calendarStart = firstDay.minusDays(leadingDays.toLong())
                val weekCount = cellCount / 7

                Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.Padding.Dp4)) {
                    repeat(weekCount) { week ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            repeat(7) { dayOfWeek ->
                                val date = calendarStart.plusDays((week * 7 + dayOfWeek).toLong())
                                CalendarDay(
                                    date = date,
                                    inVisibleMonth = YearMonth.from(date) == visibleMonth,
                                    isSelected = date == selectedDate,
                                    isToday = date == today,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        selectedDate = date
                                        visibleMonth = YearMonth.from(date)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = AppDimensions.Padding.Dp16, end = AppDimensions.Padding.Dp16, top = AppDimensions.Padding.Dp4, bottom = AppDimensions.Padding.Dp12),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CalendarAction(
                    text = stringResource(R.string.cancel),
                    onClick = onDismiss
                )
                Spacer(modifier = Modifier.width(6.dp))
                CalendarAction(
                    text = stringResource(R.string.confirm),
                    primary = true,
                    onClick = { onDateSelected(selectedDate) }
                )
            }
        }
    }
}

@Composable
private fun CalendarMonthButton(
    label: String,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(AppDimensions.Size.Dp32)
            .clip(CircleShape)
            .semantics { this.contentDescription = contentDescription }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = AppColors.Gray500,
            style = AppTypography.Size16.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = AppDimensions.Padding.Dp4)
        )
    }
}

@Composable
private fun CalendarDay(
    date: LocalDate,
    inVisibleMonth: Boolean,
    isSelected: Boolean,
    isToday: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val background = if (isSelected) AppColors.Green800 else AppColors.Transparent
    val foreground = when {
        isSelected -> AppColors.White
        !inVisibleMonth -> AppColors.Gray200
        isToday -> AppColors.Green800
        else -> AppColors.Gray800
    }

    Box(
        modifier = modifier
            .padding(AppDimensions.Padding.Dp4)
            .size(AppDimensions.Size.Dp32)
            .then(
                if (isToday && !isSelected) {
                    Modifier.border(1.dp, AppColors.Green500, CircleShape)
                } else {
                    Modifier
                }
            )
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            color = foreground,
            style = if (isSelected || isToday) {
                AppTypography.Size10.copy(fontWeight = FontWeight.Bold)
            } else {
                AppTypography.Size10
            }
        )
    }
}

@Composable
private fun CalendarAction(
    text: String,
    primary: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(AppDimensions.Corner.small)
            .background(if (primary) AppColors.Green800 else AppColors.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = AppDimensions.Padding.Dp16, vertical = AppDimensions.Padding.Dp8),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (primary) AppColors.White else AppColors.Green800,
            style = AppTypography.Size12.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}
