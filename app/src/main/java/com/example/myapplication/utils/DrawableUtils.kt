package com.example.myapplication.utils

import android.content.res.Resources
import android.graphics.drawable.Drawable

object DrawableUtils {
    fun ownDrawable(source: Drawable?, resources: Resources): Drawable? {
        val state = source?.constantState ?: return source
        return state.newDrawable(resources) ?: source
    }
}
