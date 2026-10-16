package com.example.myapplication.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.ui.theme.AppTypography

@Composable
fun DropdownSelector(
    selectedLabel: String,
    options: List<String>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectorShape = RoundedCornerShape(10.dp)
    val menuShape = RoundedCornerShape(12.dp)

    BoxWithConstraints(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(selectorShape)
                .background(AppColors.White)
                .border(1.dp, AppColors.GrayE9EEEA, selectorShape)
                .clickable { expanded = true }
                .semantics { role = Role.Button }
                .padding(horizontal = 9.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = selectedLabel,
                    color = AppColors.Green36443B,
                    style = AppTypography.Body,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                DropdownChevron(
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(width = 10.dp, height = 6.dp)
                )
            }
        }

        MaterialTheme(
            colorScheme = MaterialTheme.colorScheme.copy(
                surface = AppColors.White,
                surfaceTint = AppColors.White
            ),
            shapes = MaterialTheme.shapes.copy(extraSmall = menuShape)
        ) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .width(maxWidth)
                    .heightIn(max = 400.dp)
                    .clip(menuShape)
                    .background(AppColors.White)
                    .border(1.dp, AppColors.GrayE9EEEA, menuShape)
            ) {
                options.forEachIndexed { index, label ->
                    val selected = label == selectedLabel
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = label,
                                color = if (selected) AppColors.Green246B50 else AppColors.Ink26352B,
                                style = if (selected) AppTypography.BodyEmphasis else AppTypography.Body
                            )
                        },
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (selected) AppColors.GreenE8F2EC else AppColors.White
                            ),
                        onClick = {
                            onSelect(index)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DropdownChevron(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val lineWidth = 1.5.dp.toPx()
        drawLine(
            color = AppColors.Gray87918B,
            start = androidx.compose.ui.geometry.Offset(0f, 0f),
            end = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height),
            strokeWidth = lineWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = AppColors.Gray87918B,
            start = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height),
            end = androidx.compose.ui.geometry.Offset(size.width, 0f),
            strokeWidth = lineWidth,
            cap = StrokeCap.Round
        )
    }
}
