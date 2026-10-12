package com.jhaiian.clint.ui

import android.graphics.Bitmap

object ThemeRevealHolder {
    var bitmap: Bitmap? = null
    var cx: Int = 0
    var cy: Int = 0

    fun consume(): Triple<Bitmap, Int, Int>? {
        val bmp = bitmap ?: return null
        bitmap = null
        return Triple(bmp, cx, cy)
    }
}
