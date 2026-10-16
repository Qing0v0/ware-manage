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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.myapplication.R
import com.example.myapplication.ui.theme.AppColors
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
    val weekdays = listOf("星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六")

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(300.dp)
                .shadow(18.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(AppColors.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.Green246B50)
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Text(
                    text = selectedDate.year.toString(),
                    color = AppColors.White.copy(alpha = 0.82f),
                    style = AppTypography.BodyEmphasis
                )
                Text(
                    text = "${selectedDate.monthValue}月${selectedDate.dayOfMonth}日，${weekdays[selectedDate.dayOfWeek.value % 7]}",
                    color = AppColors.White,
                    style = AppTypography.Display,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${visibleMonth.year}年${visibleMonth.monthValue}月",
                        color = AppColors.Ink26352B,
                        style = AppTypography.BodyLarge.copy(fontWeight = FontWeight.Bold),
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
                                color = AppColors.Gray87918B,
                                style = AppTypography.Caption.copy(fontWeight = FontWeight.Medium),
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

                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
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
                    .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 12.dp),
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
            .size(32.dp)
            .clip(CircleShape)
            .semantics { this.contentDescription = contentDescription }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = AppColors.Green69756D,
            style = AppTypography.Title2,
            modifier = Modifier.padding(bottom = 3.dp)
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
    val background = if (isSelected) AppColors.Green246B50 else Color.Transparent
    val foreground = when {
        isSelected -> AppColors.White
        !inVisibleMonth -> AppColors.GrayC8CFCA
        isToday -> AppColors.Green246B50
        else -> AppColors.Green34443A
    }

    Box(
        modifier = modifier
            .padding(1.dp)
            .size(32.dp)
            .then(
                if (isToday && !isSelected) {
                    Modifier.border(1.dp, AppColors.Sage8BAF96, CircleShape)
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
                AppTypography.LabelBold
            } else {
                AppTypography.Label
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
            .clip(RoundedCornerShape(9.dp))
            .background(if (primary) AppColors.Green246B50 else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (primary) AppColors.White else AppColors.Green246B50,
            style = AppTypography.BodyEmphasis
        )
    }
}
