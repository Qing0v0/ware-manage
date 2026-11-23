package com.example.myapplication.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.compose.setContent
import com.example.myapplication.R
import com.example.myapplication.ui.screens.main.MainScreen

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()
    private lateinit var actionsController: AppActionsController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        actionsController = AppActionsController(this) {
            mainViewModel.refreshInventory(applicationContext, clearImageCache = true)
            mainViewModel.resetBills()
        }

        setContent {
            MainScreen(
                billsResetTick = mainViewModel.billsResetTick,
                inventories = mainViewModel.inventories,
                imageCacheVersion = mainViewModel.imageCacheVersion,
                imageFor = mainViewModel::imageFor,
                loadImage = mainViewModel::loadImage,
                dialog = actionsController.dialogState.value,
                onTabSelected = { tabId ->
                    when (tabId) {
                        R.id.warehouse -> mainViewModel.refreshInventory(applicationContext)
                        R.id.bills -> mainViewModel.resetBills()
                    }
                },
                onInventoryChanged = {
                    mainViewModel.refreshInventory(applicationContext)
                },
                onBackupClick = actionsController::showBackupDialog,
                onUpdateClick = actionsController::showUpdateDialog,
                onDialogDismiss = actionsController::dismissDialog
            )
        }
    }

    override fun onResume() {
        super.onResume()
        mainViewModel.refreshInventory(applicationContext)
    }
}
