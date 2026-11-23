package com.jhaiian.clint.browser.home

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.io.FileOutputStream

const val HOMEPAGE_DESIGN_IMAGE = "image"
const val HOMEPAGE_DESIGN_SOLID = "solid"
const val HOMEPAGE_DESIGN_CUSTOM_GRADIENT = "custom_gradient"
const val PREF_HOMEPAGE_IMAGE_VERSION = "homepage_image_version"

internal val LocalHomeCardAlpha = compositionLocalOf { 1f }

private const val HOMEPAGE_IMAGE_FILE = "homepage_background.jpg"
private const val HOMEPAGE_IMAGE_TEMP_FILE = "homepage_background.tmp"
private const val HOMEPAGE_IMAGE_MAX_SIDE = 2048
private const val HOMEPAGE_IMAGE_QUALITY = 88

private const val HOMEPAGE_IMAGE_DIR_NAME = "homepage"

fun homepageImageDir(context: Context): File = File(context.filesDir, HOMEPAGE_IMAGE_DIR_NAME)

fun homepageImageFile(context: Context, profileId: String? = null, dark: Boolean = false): File {
    val suffix = if (isDefaultHomepageProfile(profileId)) "" else "@$profileId"
    val name = if (dark) "homepage_background_dark" else "homepage_background"
    return File(homepageImageDir(context), "$name$suffix.jpg")
}

internal fun resolveHomepageImage(context: Context, profileId: String?, dark: Boolean): File? {
    val scopes = if (isDefaultHomepageProfile(profileId)) listOf<String?>(null) else listOf(profileId, null)
    for (scope in scopes) {
        if (dark) homepageImageFile(context, scope, true).takeIf { it.exists() }?.let { return it }
        homepageImageFile(context, scope, false).takeIf { it.exists() }?.let { return it }
    }
    return null
}

private fun migrateLegacyHomepageImage(context: Context) {
    val legacy = File(context.filesDir, HOMEPAGE_IMAGE_FILE)
    val target = homepageImageFile(context)
    if (legacy.exists() && !target.exists()) {
        homepageImageDir(context).mkdirs()
        if (!legacy.renameTo(target)) {
            runCatching { legacy.copyTo(target, overwrite = true) }
            legacy.delete()
        }
    }
}

fun readHomepageDesign(context: Context, prefs: SharedPreferences, profileId: String? = null): String {
    migrateLegacyHomepageImage(context)
    val raw = prefs.getString(homepageResolvedKey(prefs, PREF_HOMEPAGE_DESIGN, profileId), HOMEPAGE_DESIGN_PLAIN) ?: HOMEPAGE_DESIGN_PLAIN
    return when (raw) {
        HOMEPAGE_DESIGN_GRADIENT, HOMEPAGE_DESIGN_SOLID, HOMEPAGE_DESIGN_CUSTOM_GRADIENT -> raw
        HOMEPAGE_DESIGN_IMAGE -> if (resolveHomepageImage(context, profileId, false) != null) HOMEPAGE_DESIGN_IMAGE else HOMEPAGE_DESIGN_PLAIN
        else -> HOMEPAGE_DESIGN_PLAIN
    }
}

fun saveHomepageBackground(context: Context, uri: Uri, profileId: String? = null, dark: Boolean = false): Boolean = runCatching {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching false

    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= HOMEPAGE_IMAGE_MAX_SIDE) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        ?: return@runCatching false

    val orientation = runCatching {
        resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }
    }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

    val matrix = Matrix()
    val longSide = maxOf(decoded.width, decoded.height)
    if (longSide > HOMEPAGE_IMAGE_MAX_SIDE) {
        val scale = HOMEPAGE_IMAGE_MAX_SIDE.toFloat() / longSide
        matrix.postScale(scale, scale)
    }
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.postRotate(90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.postRotate(270f)
            matrix.postScale(-1f, 1f)
        }
    }
    val result = if (matrix.isIdentity) decoded
    else Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
    if (result !== decoded) decoded.recycle()

    val temp = File(context.filesDir, HOMEPAGE_IMAGE_TEMP_FILE)
    val written = FileOutputStream(temp).use { result.compress(Bitmap.CompressFormat.JPEG, HOMEPAGE_IMAGE_QUALITY, it) }
    result.recycle()
    if (!written) {
        temp.delete()
        return@runCatching false
    }
    homepageImageDir(context).mkdirs()
    val target = homepageImageFile(context, profileId, dark)
    if (!temp.renameTo(target)) {
        temp.copyTo(target, overwrite = true)
        temp.delete()
    }
    true
}.getOrDefault(false)

private fun softenBitmap(source: Bitmap, blur: Int): Bitmap {
    val targetWidth = (source.width / (1f + blur / 100f * 23f)).toInt().coerceAtLeast(16)
    var current = source
    while (current.width > targetWidth) {
        val nextWidth = maxOf(targetWidth, current.width / 2)
        val nextHeight = (current.height.toLong() * nextWidth / current.width).toInt().coerceAtLeast(1)
        val next = Bitmap.createScaledBitmap(current, nextWidth, nextHeight, true)
        if (current !== source) current.recycle()
        current = next
    }
    return current
}

fun loadHomepageBackground(context: Context, profileId: String? = null, dark: Boolean = false, blur: Int = 0): ImageBitmap? = runCatching {
    val file = resolveHomepageImage(context, profileId, dark) ?: return@runCatching null
    val decoded = BitmapFactory.decodeFile(file.absolutePath) ?: return@runCatching null
    val result = if (blur > 0) softenBitmap(decoded, blur) else decoded
    if (result !== decoded) decoded.recycle()
    result.asImageBitmap()
}.getOrNull()
