package com.example.myapplication.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentContainerView
import com.example.myapplication.R
import com.example.myapplication.ui.theme.AppColors
import com.example.myapplication.ui.theme.AppDimensions

@Composable
fun MainScreen(
    selectedItemId: Int,
    onItemSelected: (Int) -> Unit,
    onSettingsClick: () -> Unit,
    onFragmentContainerReady: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Gray50)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        AppBrandHeader(
            onSettingsClick = onSettingsClick,
            modifier = Modifier.padding(
                start = AppDimensions.Padding.Dp16,
                top = AppDimensions.Padding.Dp12,
                end = AppDimensions.Padding.Dp16,
                bottom = AppDimensions.Padding.Dp12
            )
        )

        AndroidView(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            factory = {
                FragmentContainerView(context).apply {
                    id = R.id.content
                    post(onFragmentContainerReady)
                }
            }
        )

        MainBottomNavigation(
            selectedItemId = selectedItemId,
            onItemSelected = onItemSelected
        )
    }
}
