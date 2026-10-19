package com.example.myapplication.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.myapplication.model.Color as ShoeColor

object AppColors {
    val Transparent = Color.Transparent

    val White = Color.White

    val Gray50 = Color(0xFFF5F7F5)
    val Gray100 = Color(0xFFE9EEEA)
    val Gray200 = Color(0xFFC8CFCA)
    val Gray300 = Color(0xFFBEC3BF)
    val Gray400 = Color(0xFFA7AAA8)
    val Gray450 = Color(0xFFA4ACA6)
    val Gray500 = Color(0xFF87918B)
    val Gray700 = Color(0xFF4A4A48)
    val Gray800 = Color(0xFF36443B)
    val Gray900 = Color(0xFF19251F)

    val Green50 = Color(0xFFE8F2EC)
    val Green100 = Color(0xFFD8F2DC)
    val Green200 = Color(0xFFD0E1D8)
    val Green400 = Color(0xFF8DE0AD)
    val Green500 = Color(0xFF8BAF96)
    val Green700 = Color(0xFF2E7D32)
    val Green800 = Color(0xFF246B50)

    val Red100 = Color(0xFFF9D9D9)
    val Red700 = Color(0xFFC62828)

    val Brown100 = Color(0xFFA98B78)
    val Brown200 = Color(0xFF9D8878)

    val Blue400 = Color(0xFF8CAFC4)

    val Yellow300 = Color(0xFFD9C27A)

    val BlackTransparent = Color(0x33000000)

    fun shoeColor(color: ShoeColor): Color = when (color) {
        ShoeColor.BLACK -> Gray700
        ShoeColor.WHITE -> White
        ShoeColor.GRAY -> Gray400
        ShoeColor.YELLOW -> Yellow300
        ShoeColor.GREEN -> Green500
        ShoeColor.BLUE -> Blue400
        ShoeColor.RED -> Red100
        ShoeColor.BROWN -> Brown100
        ShoeColor.COFFEE -> Brown200
        ShoeColor.OTHER -> Gray300
    }
}
