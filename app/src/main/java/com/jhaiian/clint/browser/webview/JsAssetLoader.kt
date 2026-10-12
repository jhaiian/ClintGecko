package com.jhaiian.clint.browser.webview
import com.jhaiian.clint.browser.MainActivity

internal fun MainActivity.loadJsAsset(filename: String): String {
    return assets.open("JavaScript/$filename").bufferedReader().use { it.readText() }
}
