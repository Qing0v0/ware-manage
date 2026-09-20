package com.example.myapplication.model

/**
 * 鞋子颜色。
 *
 * - [displayName] 是界面上显示的文字，取值和 res/values/arrays.xml 里的 color_array 一一对应；
 * - Room 存的是枚举名（WHITE / BLACK / ...），所以枚举常量名不能随便改，改名要配数据库迁移。
 */
enum class Color(val displayName: String) {
    WHITE("白色"),
    BLACK("黑色"),
    GRAY("灰色"),
    YELLOW("黄色"),
    GREEN("绿色"),
    BLUE("蓝色"),
    OTHER("其他");

    companion object {
        /** 界面上的文字 -> 枚举，比如 "黑色" -> Color.BLACK；对不上就当“其他” */
        @JvmStatic
        fun matchColor(colorName: String): Color {
            return entries.firstOrNull { it.displayName == colorName } ?: OTHER
        }

        /** 枚举 -> 界面上的文字，比如 Color.BLACK -> "黑色"（Java 里也可以直接用 color.getDisplayName()） */
        @JvmStatic
        fun matchColorName(color: Color): String {
            return color.displayName
        }
    }
}