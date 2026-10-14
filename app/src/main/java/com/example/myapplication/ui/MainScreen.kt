package com.example.myapplication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentContainerView
import com.example.myapplication.R
import com.example.myapplication.ui.components.AppBrandHeader
import com.example.myapplication.ui.navigation.MainBottomNavigation
import com.example.myapplication.ui.theme.AppColors

@Composable
fun MainScreen(
    selectedItemId: Int,
    onItemSelected: (Int) -> Unit,
    onFragmentContainerReady: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.PageBackground)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        AppBrandHeader(
            modifier = Modifier.padding(start = 18.dp, top = 10.dp, end = 18.dp, bottom = 14.dp)
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
