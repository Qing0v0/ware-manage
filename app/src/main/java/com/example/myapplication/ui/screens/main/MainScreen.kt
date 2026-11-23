package com.example.myapplication.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import com.example.myapplication.model.ShoeInventory
import com.example.myapplication.ui.components.AppAlertDialog
import com.example.myapplication.ui.components.AppBottomSheetMenu
import com.example.myapplication.ui.components.AppBottomSheetMenuItem
import com.example.myapplication.ui.components.AppDialogSpec
import com.example.myapplication.ui.screens.bill.BillsScreen
import com.example.myapplication.ui.screens.ware.WareScreen
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.ui.theme.AppDimensions
import com.example.myapplication.ui.theme.AppTypography

private const val WAREHOUSE_STATE_KEY = "warehouse"
private const val BILLS_STATE_KEY = "bills"

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
fun MainScreen(
    billsResetTick: Int,
    inventories: List<ShoeInventory>,
    imageCacheVersion: Int,
    imageFor: (String) -> ImageBitmap?,
    loadImage: suspend (String) -> ImageBitmap?,
    dialog: AppDialogSpec?,
    onTabSelected: (Int) -> Unit,
    onInventoryChanged: () -> Unit,
    onBackupClick: () -> Unit,
    onUpdateClick: () -> Unit,
    onDialogDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedItemId by rememberSaveable { mutableIntStateOf(R.id.warehouse) }
    var showMoreMenu by remember { mutableStateOf(false) }
    val saveableStateHolder = rememberSaveableStateHolder()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Gray50)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppBrandHeader(
                onSettingsClick = { showMoreMenu = true },
                modifier = Modifier.padding(
                    start = AppDimensions.Padding.Dp16,
                    top = AppDimensions.Padding.Dp12,
                    end = AppDimensions.Padding.Dp16,
                    bottom = AppDimensions.Padding.Dp12
                )
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (selectedItemId == R.id.warehouse) {
                    saveableStateHolder.SaveableStateProvider(WAREHOUSE_STATE_KEY) {
                        WareScreen(
                            inventories = inventories,
                            imageCacheVersion = imageCacheVersion,
                            imageFor = imageFor,
                            loadImage = loadImage,
                            onInventoryChanged = onInventoryChanged
                        )
                    }
                } else {
                    saveableStateHolder.SaveableStateProvider(BILLS_STATE_KEY) {
                        BillsScreen(resetTick = billsResetTick)
                    }
                }
            }

            MainBottomNavigation(
                selectedItemId = selectedItemId,
                onItemSelected = { itemId ->
                    selectedItemId = itemId
                    onTabSelected(itemId)
                }
            )
        }

        AppBottomSheetMenu(
            visible = showMoreMenu,
            title = stringResource(R.string.more_options),
            items = listOf(
                AppBottomSheetMenuItem(
                    label = stringResource(R.string.settings_backup),
                    onClick = onBackupClick
                ),
                AppBottomSheetMenuItem(
                    label = stringResource(R.string.settings_update),
                    onClick = onUpdateClick
                )
            ),
            onDismissRequest = { showMoreMenu = false }
        )

        AppAlertDialog(
            dialog = dialog,
            onDismissRequest = onDialogDismiss
        )
    }
}

@Composable
private fun MainBottomNavigation(
    selectedItemId: Int,
    onItemSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .background(AppColors.White)
            .drawBehind {
                drawLine(
                    color = AppColors.Gray100,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        navigationItems.forEach { item ->
            val selected = item.id == selectedItemId
            val contentColor = if (selected) AppColors.Green800 else AppColors.Gray500

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .selectable(
                        selected = selected,
                        role = Role.Tab,
                        onClick = { onItemSelected(item.id) }
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
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                    )
                )
            }
        }
    }
}
