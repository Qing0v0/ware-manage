package com.example.myapplication.utils;

import com.example.myapplication.model.Color;

public class EnumUtils {
    public static Color matchColor(String color) {
        if ("黑色".equals(color)) {
            return Color.BLACK;
        } else if ("白色".equals(color)) {
            return Color.WHITE;
        } else if ("灰色".equals(color)) {
            return Color.GRAY;
        }
        return Color.OTHER;
    }
}
