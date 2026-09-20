package com.example.myapplication.utils;

import com.example.myapplication.model.Color;

public class EnumUtils {
    public static Color matchColor(String color) {
        switch(color) {
            case "黑色": return Color.BLACK;
            case "白色": return Color.WHITE;
            case "灰色": return Color.GRAY;
            default: return Color.OTHER;
        }
    }
}
