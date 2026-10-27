@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.ui.theme.AppDimensions
import com.example.myapplication.ui.theme.AppTypography

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController

/** 下拉框 */
@Composable
fun DropdownSelector(
    selectedLabel: String,
    options: List<String>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectorShape = AppDimensions.Corner.small
    val menuShape = AppDimensions.Corner.small

    BoxWithConstraints(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(selectorShape)
                .background(AppColors.White)
                .border(1.dp, AppColors.Gray100, selectorShape)
                .clickable { expanded = true }
                .semantics { role = Role.Button }
                .padding(horizontal = AppDimensions.Padding.Dp12),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = selectedLabel,
                    color = AppColors.Gray800,
                    style = AppTypography.Size12,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                DropdownChevron(
                    modifier = Modifier
                        .padding(start = AppDimensions.Padding.Dp4)
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
                    .border(1.dp, AppColors.Gray100, menuShape)
            ) {
                options.forEachIndexed { index, label ->
                    val selected = label == selectedLabel
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = label,
                                color = if (selected) AppColors.Green800 else AppColors.Gray900,
                                style = if (selected) AppTypography.Size12.copy(fontWeight = FontWeight.SemiBold) else AppTypography.Size12
                            )
                        },
                        modifier = Modifier
                            .padding(horizontal = AppDimensions.Padding.Dp8)
                            .clip(AppDimensions.Corner.small)
                            .background(
                                if (selected) AppColors.Green50 else AppColors.White
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
            color = AppColors.Gray500,
            start = androidx.compose.ui.geometry.Offset(0f, 0f),
            end = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height),
            strokeWidth = lineWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = AppColors.Gray500,
            start = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height),
            end = androidx.compose.ui.geometry.Offset(size.width, 0f),
            strokeWidth = lineWidth,
            cap = StrokeCap.Round
        )
    }
}

/** 按钮 */
@Composable
fun AppButton(
    modifier: Modifier = Modifier,
    text: String,
    primary: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
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

/** 任意位置按下时收起键盘 */
@Composable
fun Modifier.hideKeyboardOnAnyPress(): Modifier {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    return this.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Final)
                if (event.type == PointerEventType.Press && event.changes.none { it.isConsumed }) {
                    hideKeyboard(focusManager, keyboardController)
                }
            }
        }
    }
}

/** 弹出键盘 */
fun showKeyboard(
    focusRequester: FocusRequester,
    keyboardController: SoftwareKeyboardController? = null
) {
    focusRequester.requestFocus()
    keyboardController?.show()
}

/** 收起键盘 */
fun hideKeyboard(
    focusManager: FocusManager? = null,
    keyboardController: SoftwareKeyboardController? = null
) {
    focusManager?.clearFocus(force = true)
    keyboardController?.hide()
}

/** 点击时收起键盘 */
@Composable
fun Modifier.hideKeyboardOnTap(): Modifier {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    return this.clickable(
        interactionSource = interactionSource,
        indication = null
    ) {
        hideKeyboard(focusManager, keyboardController)
    }
}

/** 滚动时收起键盘 */
@Composable
fun HideKeyboardOnScroll(
    isScrolling: Boolean,
    keyboardController: SoftwareKeyboardController? = LocalSoftwareKeyboardController.current,
    focusManager: FocusManager = LocalFocusManager.current
) {
    LaunchedEffect(isScrolling) {
        if (isScrolling) {
            hideKeyboard(focusManager, keyboardController)
        }
    }
}
