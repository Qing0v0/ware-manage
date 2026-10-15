package com.example.myapplication.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object AppTypography {
    val Caption = TextStyle(fontSize = 10.sp)
    val CaptionEmphasis = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold
    )
    val CaptionSemibold = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold
    )
    val Label = TextStyle(fontSize = 11.sp)
    val LabelEmphasis = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold
    )
    val LabelBold = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
    )
    val Body = TextStyle(fontSize = 12.sp)
    val BodyEmphasis = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold
    )
    val BodyLarge = TextStyle(fontSize = 13.sp)
    val Title3 = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold
    )
    val Title2 = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold
    )
    val Display = TextStyle(
        fontSize = 23.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.7).sp
    )
    val Title1 = TextStyle(
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.8).sp
    )
}
