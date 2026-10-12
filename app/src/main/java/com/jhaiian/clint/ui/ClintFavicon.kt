package com.jhaiian.clint.ui

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberClintFavicon(pageUrl: String, storedFaviconUrl: String = ""): Bitmap? {
    val context = LocalContext.current
    var bitmap by remember(pageUrl, storedFaviconUrl) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(pageUrl, storedFaviconUrl) {
        val faviconUrl = storedFaviconUrl.ifBlank { FaviconCache.faviconUrlFor(pageUrl) }
        if (faviconUrl.isNotEmpty()) {
            FaviconCache.load(context, faviconUrl) { bmp -> bitmap = bmp }
        }
    }
    return bitmap
}
