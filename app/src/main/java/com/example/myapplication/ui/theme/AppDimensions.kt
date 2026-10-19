package com.example.myapplication.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object AppDimensions {
    object Padding {
        val Dp4 = 4.dp
        val Dp8 = 8.dp
        val Dp12 = 12.dp
        val Dp16 = 16.dp
        val Dp20 = 20.dp
    }

    object Size {
        val Dp32 = 32.dp
        val Dp36 = 36.dp
        val Dp40 = 40.dp
        val Dp56 = 56.dp
        val Dp300 = 300.dp // 日历
        val Dp560 = 520.dp // 弹窗上限
    }

    object Corner {
        val small = RoundedCornerShape(10.dp)
        val large = RoundedCornerShape(18.dp)
        val largeTop = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
    }
}
