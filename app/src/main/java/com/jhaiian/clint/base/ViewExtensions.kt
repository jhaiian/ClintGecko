package com.jhaiian.clint.base

import android.view.View
import android.view.Window
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

fun View.showSoftKeyboard(window: Window) {
    requestFocus()
    WindowCompat.getInsetsController(window, this).show(WindowInsetsCompat.Type.ime())
}
