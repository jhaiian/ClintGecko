package com.jhaiian.clint.browser.dialogs

import com.jhaiian.clint.browser.webview.ClintWebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.jhaiian.clint.ui.ClintColorPickerDialog
import java.lang.ref.WeakReference
import java.util.Locale

class ColorPickerRequest(
    val id: String,
    val value: String,
    val webView: WeakReference<ClintWebView>
)

private fun parseColorValue(value: String): Color {
    val match = Regex("^#([0-9a-fA-F]{6})$").matchEntire(value.trim())
    val rgb = match?.groupValues?.get(1)?.toLong(16) ?: 0L
    return Color(0xFF000000L or rgb)
}

private fun formatColorValue(color: Color): String =
    String.format(Locale.ROOT, "#%06x", color.toArgb() and 0xFFFFFF)

@Composable
internal fun ColorPickerDialog(request: ColorPickerRequest, hideStatusBar: Boolean, hideSystemNavigation: Boolean, onDismiss: () -> Unit) {
    val initial = remember(request) { parseColorValue(request.value) }

    fun applyValue(value: String) {
        val webView = request.webView.get() ?: return
        val safeId = request.id.replace("'", "")
        val quoted = org.json.JSONObject.quote(value)
        webView.evaluateJavascript("window.__clintApplyColor && window.__clintApplyColor('$safeId', $quoted)", null)
    }

    ClintColorPickerDialog(
        initial = initial,
        hideStatusBar = hideStatusBar,
        hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss
    ) { picked ->
        applyValue(formatColorValue(picked))
    }
}
