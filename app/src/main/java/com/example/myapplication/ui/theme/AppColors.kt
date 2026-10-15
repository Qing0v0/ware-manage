package com.example.myapplication.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.myapplication.model.Color as ShoeColor

object AppColors {
    val TransparentBlack33 = Color(0x33000000)
    val Ink19251F = Color(0xFF19251F)
    val Green1D5942 = Color(0xFF1D5942)
    val Green246B50 = Color(0xFF246B50)
    val Black252525 = Color(0xFF252525)
    val Ink26352B = Color(0xFF26352B)
    val Green2E7D32 = Color(0xFF2E7D32)
    val Green34443A = Color(0xFF34443A)
    val Green36443B = Color(0xFF36443B)
    val Blue4285D4 = Color(0xFF4285D4)
    val Charcoal4A4A48 = Color(0xFF4A4A48)
    val Green4CAF50 = Color(0xFF4CAF50)
    val Green66736A = Color(0xFF66736A)
    val Green69756D = Color(0xFF69756D)
    val Brown6F4E37 = Color(0xFF6F4E37)
    val Green77827B = Color(0xFF77827B)
    val Green7B877F = Color(0xFF7B877F)
    val Gray87918B = Color(0xFF87918B)
    val Gray8A958D = Color(0xFF8A958D)
    val Sage8BAF96 = Color(0xFF8BAF96)
    val Blue8CAFC4 = Color(0xFF8CAFC4)
    val Brown8D5A3B = Color(0xFF8D5A3B)
    val Gray8F9992 = Color(0xFF8F9992)
    val Gray909A93 = Color(0xFF909A93)
    val Gray919B94 = Color(0xFF919B94)
    val Gray92999B = Color(0xFF92999B)
    val Gray939D96 = Color(0xFF939D96)
    val Gray99A29B = Color(0xFF99A29B)
    val Brown9D8878 = Color(0xFF9D8878)
    val GrayA4ACA6 = Color(0xFFA4ACA6)
    val GrayA7AAA8 = Color(0xFFA7AAA8)
    val BrownA98B78 = Color(0xFFA98B78)
    val GrayB7BDB8 = Color(0xFFB7BDB8)
    val GrayBEC3BF = Color(0xFFBEC3BF)
    val RedC62828 = Color(0xFFC62828)
    val GrayC8CFCA = Color(0xFFC8CFCA)
    val RoseC98C88 = Color(0xFFC98C88)
    val GreenD8F2DC = Color(0xFFD8F2DC)
    val SandD9C27A = Color(0xFFD9C27A)
    val RedE55353 = Color(0xFFE55353)
    val GreenE8F2EC = Color(0xFFE8F2EC)
    val GrayE9EEEA = Color(0xFFE9EEEA)
    val GrayEDF0ED = Color(0xFFEDF0ED)
    val GrayF0F2F0 = Color(0xFFF0F2F0)
    val GrayF5F7F5 = Color(0xFFF5F7F5)
    val GrayF6F8F6 = Color(0xFFF6F8F6)
    val RedF9D9D9 = Color(0xFFF9D9D9)
    val YellowFFD54F = Color(0xFFFFD54F)
    val White = Color.White

    fun shoeColor(color: ShoeColor): Color = when (color) {
        ShoeColor.BLACK -> Charcoal4A4A48
        ShoeColor.WHITE -> White
        ShoeColor.GRAY -> GrayA7AAA8
        ShoeColor.YELLOW -> SandD9C27A
        ShoeColor.GREEN -> Sage8BAF96
        ShoeColor.BLUE -> Blue8CAFC4
        ShoeColor.RED -> RoseC98C88
        ShoeColor.BROWN -> BrownA98B78
        ShoeColor.COFFEE -> Brown9D8878
        ShoeColor.OTHER -> GrayBEC3BF
    }
}
