package com.jhaiian.clint.browser.sheets
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.ui.graphics.asAndroidBitmap

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.preference.PreferenceManager
import com.caverock.androidsvg.SVG
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.ui.theme.LocalClintColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

data class ImageLongPressRequest(
    val imageUrl: String,
    val pageTitle: String,
    val isStandalone: Boolean,
    val referer: String = "",
    val isPreviewContext: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ImageLongPressSheet(request: ImageLongPressRequest, activity: MainActivity, onDismiss: () -> Unit) {
    val colors = LocalClintColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val hideStatusBar = remember { PreferenceManager.getDefaultSharedPreferences(activity).getBoolean("hide_status_bar", false) }
    val hideSystemNavigation = remember { PreferenceManager.getDefaultSharedPreferences(activity).getBoolean("hide_system_navigation", false) }

    fun dismissAnd(action: () -> Unit) {
        onDismiss()
        action()
    }

    val isDataUri = request.imageUrl.startsWith("data:")
    val urlFilename = if (!isDataUri) {
        request.imageUrl.substringAfterLast("/").substringBefore("?")
            .takeIf { it.length > 4 && it.contains(".") }
            ?: runCatching {
                val uri = android.net.Uri.parse(request.imageUrl)
                listOf("u", "url", "src", "img", "imgurl").firstNotNullOfOrNull { key ->
                    uri.getQueryParameter(key)?.let { param ->
                        URLDecoder.decode(param, "UTF-8")
                            .substringAfterLast("/").substringBefore("?")
                            .takeIf { it.contains(".") }
                    }
                }
            }.getOrNull()
    } else null
    val displayTitle = request.pageTitle.ifEmpty {
        if (isDataUri) stringResource(R.string.image_embedded_title) else urlFilename ?: request.imageUrl
    }

    val showTabActions = !request.isStandalone || request.isPreviewContext
    val listState = rememberLazyListState()
    val flingBoundaryConnection = remember {
        object : NestedScrollConnection {
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = available
        }
    }
    val configuration = LocalConfiguration.current

    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val maxSheetHeight = if (isPortrait) {
        (configuration.screenHeightDp.dp * 0.8f).coerceAtLeast(320.dp)
    } else {
        (configuration.screenHeightDp.dp - 96.dp).coerceAtLeast(320.dp)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.popupBackground,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.divider) }
    ) {
        com.jhaiian.clint.ui.ClintDialogStatusBarEffect(hideStatusBar, hideSystemNavigation)
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = maxSheetHeight).nestedScroll(flingBoundaryConnection), state = listState) {
        item {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.fillMaxWidth().widthIn(max = LongPressContentMaxWidth).padding(bottom = 8.dp)) {
            ImageThumbnail(request.imageUrl, request.referer)

            Text(
                displayTitle,
                color = colors.secondaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
            )

            LongPressSheetDivider()

            if (showTabActions) {
                LongPressActionRow(androidx.compose.material.icons.Icons.AutoMirrored.Filled.OpenInNew, stringResource(R.string.image_open_in_new_tab)) {
                    dismissAnd { activity.onImageOpenInNewTab(request.imageUrl) }
                }
                LongPressActionRow(androidx.compose.material.icons.Icons.Filled.VisibilityOff, stringResource(R.string.image_open_incognito)) {
                    dismissAnd { activity.onImageOpenIncognito(request.imageUrl) }
                }
            }
            LongPressActionRow(androidx.compose.material.icons.Icons.Filled.Download, stringResource(R.string.image_download)) {
                dismissAnd { activity.onImageDownload(request.imageUrl, request.pageTitle) }
            }
            if (request.isPreviewContext) {
                LongPressActionRow(androidx.compose.material.icons.Icons.Filled.Tab, stringResource(R.string.image_open_in_current_tab)) {
                    dismissAnd { activity.onImageOpenInCurrentTab(request.imageUrl) }
                }
            }
            if (showTabActions && !request.isPreviewContext) {
                LongPressActionRow(androidx.compose.material.icons.Icons.Filled.ZoomIn, stringResource(R.string.image_preview)) {
                    dismissAnd { activity.onImagePreview(request.imageUrl) }
                }
            }
            LongPressActionRow(androidx.compose.material.icons.Icons.Filled.ContentCopy, stringResource(R.string.image_copy)) {
                dismissAnd { activity.onImageCopy(request.imageUrl) }
            }
            LongPressActionRow(androidx.compose.material.icons.Icons.Filled.Share, stringResource(R.string.image_share)) {
                dismissAnd { activity.onImageShare(request.imageUrl) }
            }
        }
        }
        }
        }
    }
}

@Composable
private fun ImageThumbnail(imageUrl: String, referer: String) {
    val context = LocalContext.current
    val colors = LocalClintColors.current
    var drawable by remember(imageUrl) { mutableStateOf<Drawable?>(null) }

    LaunchedEffect(imageUrl) {
        if (imageUrl.isEmpty()) return@LaunchedEffect
        val userAgent = com.jhaiian.clint.browser.webview.ClintGecko.defaultUserAgent()
        val bytes = withContext(Dispatchers.IO) { fetchImageBytes(context, imageUrl, referer, userAgent) } ?: return@LaunchedEffect
        drawable = withContext(Dispatchers.IO) { decodeThumbnailDrawable(bytes) }
    }

    val placeholderDrawable = rememberVectorDrawable(androidx.compose.material.icons.Icons.Filled.Public, 48)
    val cardShape = RoundedCornerShape(LongPressCardCorner)

    AndroidView(
        factory = { ctx ->
            ImageView(ctx).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                setImageDrawable(placeholderDrawable)
                imageTintList = android.content.res.ColorStateList.valueOf(colors.iconTint.toArgbCompat())
                setPadding(dpToPx(ctx, 48), dpToPx(ctx, 48), dpToPx(ctx, 48), dpToPx(ctx, 48))
            }
        },
        update = { imageView ->
            val d = drawable
            if (d != null) {
                imageView.setPadding(0, 0, 0, 0)
                imageView.imageTintList = null
                imageView.setImageDrawable(d)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) startIfAnimated(d)
            } else {
                imageView.imageTintList = android.content.res.ColorStateList.valueOf(colors.iconTint.toArgbCompat())
            }
        },
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp)
            .height(180.dp)
            .clip(cardShape)
            .background(colors.surfaceVariant)
            .border(1.dp, colors.popupStroke, cardShape)
    )
}

@Composable
private fun rememberVectorDrawable(image: androidx.compose.ui.graphics.vector.ImageVector, sizeDp: Int): Drawable {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(image)
    val resources = LocalContext.current.resources
    return remember(image, sizeDp, density) {
        val sizePx = with(density) { sizeDp.dp.roundToPx() }
        val imageBitmap = androidx.compose.ui.graphics.ImageBitmap(sizePx, sizePx)
        val canvas = androidx.compose.ui.graphics.Canvas(imageBitmap)
        androidx.compose.ui.graphics.drawscope.CanvasDrawScope().draw(
            density = density,
            layoutDirection = androidx.compose.ui.unit.LayoutDirection.Ltr,
            canvas = canvas,
            size = androidx.compose.ui.geometry.Size(sizePx.toFloat(), sizePx.toFloat())
        ) {
            with(painter) {
                draw(androidx.compose.ui.geometry.Size(sizePx.toFloat(), sizePx.toFloat()))
            }
        }
        android.graphics.drawable.BitmapDrawable(resources, imageBitmap.asAndroidBitmap())
    }
}

@androidx.annotation.RequiresApi(Build.VERSION_CODES.P)
private fun startIfAnimated(d: Drawable) {
    (d as? AnimatedImageDrawable)?.start()
}

private fun dpToPx(context: android.content.Context, dp: Int): Int =
    (dp * context.resources.displayMetrics.density).toInt()

private fun androidx.compose.ui.graphics.Color.toArgbCompat(): Int =
    android.graphics.Color.argb(
        (alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt()
    )

private fun fetchImageBytes(context: android.content.Context, url: String, referer: String, userAgent: String): ByteArray? {
    if (url.startsWith("data:")) {
        val commaIdx = url.indexOf(",")
        if (commaIdx < 0) return null
        val header = url.substring(0, commaIdx)
        val content = url.substring(commaIdx + 1)
        return if (header.contains(";base64")) {
            runCatching { android.util.Base64.decode(content, android.util.Base64.DEFAULT) }.getOrNull()
        } else {
            runCatching { URLDecoder.decode(content, "UTF-8").toByteArray(Charsets.UTF_8) }
                .getOrElse { content.toByteArray(Charsets.UTF_8) }
        }
    }
    return runCatching {
        val client = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .apply { if (referer.isNotEmpty()) header("Referer", referer) }
            .build()
        client.newCall(request).execute().body.bytes()
    }.getOrNull()
}

private fun decodeThumbnailDrawable(bytes: ByteArray): Drawable? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        runCatching {
            val source = ImageDecoder.createSource(ByteBuffer.wrap(bytes))
            return ImageDecoder.decodeDrawable(source)
        }
    } else {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        if (bitmap != null) return android.graphics.drawable.BitmapDrawable(null, bitmap)
    }
    return decodeSvgDrawable(bytes)
}

private fun decodeSvgDrawable(bytes: ByteArray): Drawable? = runCatching {
    val svg = SVG.getFromString(bytes.toString(Charsets.UTF_8))
    val vb = svg.documentViewBox
    val rawW = vb?.width()?.toInt()?.takeIf { it > 0 } ?: svg.documentWidth.toInt().takeIf { it > 0 } ?: 512
    val rawH = vb?.height()?.toInt()?.takeIf { it > 0 } ?: svg.documentHeight.toInt().takeIf { it > 0 } ?: 512
    val scale = 512f / maxOf(rawW, rawH).toFloat()
    val bw = (rawW * scale).toInt().coerceAtLeast(1)
    val bh = (rawH * scale).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
    svg.renderToCanvas(Canvas(bitmap))
    android.graphics.drawable.BitmapDrawable(null, bitmap)
}.getOrNull()
