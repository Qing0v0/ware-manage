package com.example.myapplication.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.ui.components.HairlineColor

private val DrawerWidth = 280.dp

@Composable
internal fun SettingsDrawer(
    onBackupClick: () -> Unit,
    onUpdateClick: () -> Unit
) {
    val hairlineHeight = with(LocalDensity.current) { 0.5f.toDp() }

    Column(
        modifier = Modifier
            .width(DrawerWidth)
            .fillMaxHeight()
            .background(colorResource(R.color.white))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorResource(R.color.table_head_bg))
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                color = Color.Black,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        DrawerRow(label = stringResource(R.string.settings_backup), onClick = onBackupClick)
        DrawerRow(label = stringResource(R.string.settings_update), onClick = onUpdateClick)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(hairlineHeight)
                .background(HairlineColor)
        )
    }
}

@Composable
private fun DrawerRow(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text = label, color = Color.Black, fontSize = 16.sp)
    }
}
