package com.example.myapplication.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.ui.theme.AppDimensions
import com.example.myapplication.ui.theme.AppTypography

sealed interface MainScreenSection {
    data class Header(val onSettingsClick: () -> Unit) : MainScreenSection
    data class BottomNavigation(
        val selectedItemId: Int,
        val onItemSelected: (Int) -> Unit
    ) : MainScreenSection
}

private data class MainNavigationItem(
    val id: Int,
    val icon: Int,
    val title: Int
)

private val navigationItems = listOf(
    MainNavigationItem(R.id.warehouse, R.drawable.warehouse, R.string.bottom_warehouse),
    MainNavigationItem(R.id.bills, R.drawable.bills, R.string.bottom_bills)
)

@Composable
fun MainScreen(section: MainScreenSection) {
    when (section) {
        is MainScreenSection.Header -> {
            AppBrandHeader(
                onSettingsClick = section.onSettingsClick,
                modifier = Modifier.padding(
                    start = AppDimensions.Padding.Dp16,
                    top = AppDimensions.Padding.Dp12,
                    end = AppDimensions.Padding.Dp16,
                    bottom = AppDimensions.Padding.Dp12
                )
            )
        }

        is MainScreenSection.BottomNavigation -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .background(AppColors.White)
                    .drawBehind {
                        drawLine(
                            color = AppColors.Gray100,
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx()
                        )
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                navigationItems.forEach { item ->
                    val selected = item.id == section.selectedItemId
                    val contentColor =
                        if (selected) AppColors.Green800 else AppColors.Gray500

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .selectable(
                                selected = selected,
                                role = Role.Tab,
                                onClick = { section.onItemSelected(item.id) }
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(item.icon),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = contentColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(item.title),
                            color = contentColor,
                            style = AppTypography.Size10.copy(
                                fontWeight = if (selected) {
                                    FontWeight.SemiBold
                                } else {
                                    FontWeight.Medium
                                }
                            )
                        )
                    }
                }
            }
        }
    }
}
