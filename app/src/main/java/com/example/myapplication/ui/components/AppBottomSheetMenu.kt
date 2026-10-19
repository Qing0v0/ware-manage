package com.example.myapplication.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Divider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.ui.theme.AppDimensions
import com.example.myapplication.ui.theme.AppTypography

data class AppBottomSheetMenuItem(
    val label: String,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheetMenu(
    visible: Boolean,
    title: String,
    items: List<AppBottomSheetMenuItem>,
    onDismissRequest: () -> Unit
) {
    if (!visible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = AppDimensions.Corner.largeTop,
        containerColor = AppColors.Gray50,
        contentColor = AppColors.Gray900,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = AppColors.Gray200)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = AppDimensions.Padding.Dp16)
        ) {
            Text(
                text = title,
                modifier = Modifier.padding(start = AppDimensions.Padding.Dp20, top = AppDimensions.Padding.Dp4, end = AppDimensions.Padding.Dp20, bottom = AppDimensions.Padding.Dp12),
                color = AppColors.Gray900,
                style = AppTypography.Size16.copy(fontWeight = FontWeight.Bold)
            )
            items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AppDimensions.Size.Dp56)
                        .clickable {
                            scope.launch {
                                sheetState.hide()
                                onDismissRequest()
                                item.onClick()
                            }
                        }
                        .padding(horizontal = AppDimensions.Padding.Dp20),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.label,
                        color = AppColors.Gray800,
                        style = AppTypography.Size12.copy(fontWeight = FontWeight.Medium)
                    )
                }
                if (index < items.lastIndex) {
                    Divider(
                        modifier = Modifier.padding(horizontal = AppDimensions.Padding.Dp20),
                        color = AppColors.Gray100
                    )
                }
            }
        }
    }
}
