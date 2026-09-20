package com.example.myapplication.model

enum class Color {
    WHITE,
    BLACK,
    GRAY,
    YELLOW,
    GREEN,
    BLUE,
    OTHER;

    companion object {
        @JvmStatic
        fun matchColor(color: String): Color {
            return when (color) {
                "黑色" -> BLACK
                "白色" -> WHITE
                "灰色" -> GRAY
                "黄色" -> YELLOW
                "绿色" -> GREEN
                "蓝色" -> BLUE
                else -> OTHER
            }
        }
    }
}