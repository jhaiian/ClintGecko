package com.jhaiian.clint.setup

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jhaiian.clint.R
import com.jhaiian.clint.extensions.ClintExtensions
import com.jhaiian.clint.ui.theme.LocalClintColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class SetupAddon(
    val id: String,
    val slug: String,
    val nameRes: Int,
    val descRes: Int,
    val icon: ImageVector,
    val recommended: Boolean
) {
    val downloadUrl: String get() = "https://addons.mozilla.org/firefox/downloads/latest/$slug/latest.xpi"
}

internal val SetupAddons = listOf(
    SetupAddon("uBlock0@raymondhill.net", "ublock-origin", R.string.setup_addon_ublock_name, R.string.setup_addon_ublock, Icons.Outlined.Block, true),
    SetupAddon("addon@darkreader.org", "darkreader", R.string.setup_addon_darkreader_name, R.string.setup_addon_darkreader, Icons.Outlined.DarkMode, true),
    SetupAddon("{446900e4-71c2-419f-a6a7-df9c091e268b}", "bitwarden-password-manager", R.string.setup_addon_bitwarden_name, R.string.setup_addon_bitwarden, Icons.Outlined.Password, false),
    SetupAddon("sponsorBlocker@ajay.app", "sponsorblock", R.string.setup_addon_sponsorblock_name, R.string.setup_addon_sponsorblock, Icons.Outlined.FastForward, false),
    SetupAddon("idcac-pub@guus.ninja", "istilldontcareaboutcookies", R.string.setup_addon_cookies_name, R.string.setup_addon_cookies, Icons.Outlined.Cookie, false),
    SetupAddon("{aecec67f-0d10-4fa7-b7c7-609a2db280cf}", "violentmonkey", R.string.setup_addon_violentmonkey_name, R.string.setup_addon_violentmonkey, Icons.Outlined.Code, false)
)

internal object AddonIconCache {
    val icons = mutableStateMapOf<String, ImageBitmap>()
    private val loading = HashSet<String>()

    suspend fun load(context: Context, slug: String) {
        if (icons.containsKey(slug) || !loading.add(slug)) return
        val appContext = context.applicationContext
        val bitmap = withContext(Dispatchers.IO) { readOrFetch(appContext, slug) }
        loading.remove(slug)
        if (bitmap != null) icons[slug] = bitmap.asImageBitmap()
    }

    private fun readOrFetch(context: Context, slug: String): Bitmap? = runCatching {
        val dir = File(context.cacheDir, "setup_addon_icons").apply { mkdirs() }
        val file = File(dir, "$slug.png")
        if (file.exists()) {
            BitmapFactory.decodeFile(file.absolutePath)?.let { return@runCatching it }
        }
        val detail = JSONObject(httpText("https://addons.mozilla.org/api/v5/addons/addon/$slug/"))
        val sizes = detail.optJSONObject("icons")
        val iconUrl = listOf("128", "64").firstNotNullOfOrNull { size -> sizes?.optString(size)?.takeIf { it.startsWith("https://") } }
            ?: detail.optString("icon_url").takeIf { it.startsWith("https://") }
            ?: return@runCatching null
        val bitmap = httpBitmap(iconUrl) ?: return@runCatching null
        runCatching { file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        bitmap
    }.getOrNull()

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            instanceFollowRedirects = true
        }

    private fun httpText(url: String): String {
        val connection = open(url)
        connection.setRequestProperty("Accept", "application/json")
        return try {
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun httpBitmap(url: String): Bitmap? {
        val connection = open(url)
        return try {
            connection.inputStream.use { BitmapFactory.decodeStream(it) }
        } finally {
            connection.disconnect()
        }
    }
}

@Composable
internal fun rememberAddonIcon(addon: SetupAddon): ImageBitmap? {
    val context = LocalContext.current
    LaunchedEffect(addon.slug) { AddonIconCache.load(context, addon.slug) }
    return ClintExtensions.icons[addon.id] ?: AddonIconCache.icons[addon.slug]
}

@Composable
private fun AddonIcon(addon: SetupAddon, installed: Boolean) {
    val colors = LocalClintColors.current
    val bitmap = rememberAddonIcon(addon)
    if (bitmap == null) {
        IconTile(addon.icon, selected = installed)
        return
    }
    val container by animateColorAsState(
        if (installed) colors.primary.copy(alpha = 0.28f) else colors.surfaceVariant,
        label = "addon_icon_bg"
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(container)
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(7.dp))
        )
    }
}

@Composable
internal fun isAddonInstalled(addon: SetupAddon): Boolean {
    val name = stringResource(addon.nameRes)
    return ClintExtensions.extensions.any { it.id == addon.id || it.name.equals(name, ignoreCase = true) }
}

@Composable
internal fun addonSummaryChips(): List<Pair<ImageVector, String>> =
    SetupAddons.filter { isAddonInstalled(it) }.map { it.icon to stringResource(it.nameRes) }

private enum class AddonStatus { AVAILABLE, INSTALLING, INSTALLED }

@Composable
internal fun SetupAddonsPage(activity: SetupActivity, state: SetupUiState, metrics: PageMetrics, scrollState: ScrollState) {
    val colors = LocalClintColors.current
    SetupPage(metrics, scrollState, hero = { AddonsHero(it) }) {
        PageHeader(stringResource(R.string.setup_addons_title), stringResource(R.string.setup_addons_body, stringResource(R.string.app_name)), metrics)
        Spacer(Modifier.height(20.dp))
        SetupAddons.forEachIndexed { i, addon ->
            AddonCard(
                addon = addon,
                requested = addon.id in state.addonRequests,
                onAdd = { activity.installAddon(addon) },
                modifier = Modifier.rise(2 + i)
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .rise(2 + SetupAddons.size)
        ) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = colors.secondaryText, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.setup_addons_note),
                style = MaterialTheme.typography.bodySmall,
                color = colors.secondaryText,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AddonCard(addon: SetupAddon, requested: Boolean, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalClintColors.current
    val haptics = rememberSetupHaptics()
    val installed = isAddonInstalled(addon)
    val status = when {
        installed -> AddonStatus.INSTALLED
        requested && ClintExtensions.installsInFlight > 0 -> AddonStatus.INSTALLING
        else -> AddonStatus.AVAILABLE
    }
    LaunchedEffect(installed) {
        if (installed && requested) haptics.confirm()
    }
    val container by animateColorAsState(
        if (installed) colors.primary.copy(alpha = 0.14f) else colors.cardBackground,
        label = "addon_bg"
    )
    val stroke by animateColorAsState(if (installed) colors.primary else colors.divider, label = "addon_stroke")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(if (installed) 2.dp else 1.dp, stroke, RoundedCornerShape(20.dp))
            .background(container)
            .padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 10.dp)
    ) {
        AddonIcon(addon, installed)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            if (addon.recommended) {
                DefaultChip(stringResource(R.string.setup_addon_recommended), colors.primary)
                Spacer(Modifier.height(6.dp))
            }
            Text(
                text = stringResource(addon.nameRes),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(addon.descRes),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.secondaryText
            )
        }
        Spacer(Modifier.width(8.dp))
        AnimatedContent(
            targetState = status,
            transitionSpec = {
                (fadeIn(tween(220)) + scaleIn(spring(dampingRatio = 0.55f, stiffness = 320f), initialScale = 0.7f)) togetherWith
                    (fadeOut(tween(140)) + scaleOut(tween(140), targetScale = 0.8f))
            },
            contentAlignment = Alignment.Center,
            label = "addon_status"
        ) { current ->
            when (current) {
                AddonStatus.INSTALLED -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp)
                ) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.setup_addon_added),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                }
                AddonStatus.INSTALLING -> Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp)) {
                    CircularProgressIndicator(color = colors.primary, strokeWidth = 3.dp, modifier = Modifier.size(24.dp))
                }
                AddonStatus.AVAILABLE -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(colors.primary)
                        .clickable(role = Role.Button) {
                            haptics.click()
                            onAdd()
                        }
                        .padding(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 8.dp)
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.setup_addon_add),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.onPrimary
                    )
                }
            }
        }
    }
}

private fun overshoot(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    val c1 = 1.70158f
    val c3 = c1 + 1f
    return 1f + c3 * (x - 1f) * (x - 1f) * (x - 1f) + c1 * (x - 1f) * (x - 1f)
}

private fun span(t: Float, start: Float, end: Float): Float = ((t - start) / (end - start)).coerceIn(0f, 1f)

@Composable
private fun AddonsHero(modifier: Modifier = Modifier) {
    val colors = LocalClintColors.current
    val clock = rememberLoopClock(6200, "addons_clock")
    val wobble = rememberLoopClock(3000, "addons_wobble")
    val pop = rememberEntranceProgress()
    LaunchedEffect(Unit) { if (pop.value < 1f) pop.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 170f)) }
    val slots = remember { SetupAddons.take(4) }
    val slotIcons = slots.map { rememberAddonIcon(it) }
    val primary = colors.primary
    FittedBox(300.dp, 250.dp, modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val v = pop.value
                    scaleX = 0.9f + 0.1f * v
                    scaleY = 0.9f + 0.1f * v
                    alpha = v.coerceIn(0f, 1f)
                }
                .clip(RoundedCornerShape(26.dp))
                .background(colors.cardBackground)
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceVariant)
                        .padding(horizontal = 10.dp)
                ) {
                    Icon(Icons.Outlined.Lock, null, tint = primary, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .fillMaxWidth(0.6f)
                            .height(7.dp)
                            .clip(CircleShape)
                            .background(colors.onSurface.copy(alpha = 0.12f))
                    )
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .graphicsLayer { rotationZ = kotlin.math.sin(wobble.value * 2f * kotlin.math.PI.toFloat()) * 8f }
                        .clip(CircleShape)
                        .background(primary)
                ) {
                    Icon(Icons.Outlined.Extension, null, tint = colors.onPrimary, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(22.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                slots.forEachIndexed { i, addon ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.surfaceVariant)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    val t = clock.value
                                    val land = span(t, 0.06f + i * 0.13f, 0.2f + i * 0.13f)
                                    val leave = span(t, 0.84f, 0.95f)
                                    translationY = (1f - overshoot(land)) * -70.dp.toPx() + leave * 20.dp.toPx()
                                    alpha = (land * 3f).coerceIn(0f, 1f) * (1f - leave)
                                    val s = 0.8f + 0.2f * overshoot(land)
                                    scaleX = s
                                    scaleY = s
                                }
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (slotIcons[i] != null) colors.cardBackground else primary)
                                .border(2.dp, primary, RoundedCornerShape(16.dp))
                        ) {
                            val bitmap = slotIcons[i]
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            } else {
                                Icon(addon.icon, null, tint = colors.onPrimary, modifier = Modifier.size(26.dp))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(colors.divider)
                    .drawBehind {
                        val t = clock.value
                        val filled = (0 until slots.size).count { span(t, 0.06f + it * 0.13f, 0.2f + it * 0.13f) >= 1f }
                        val fade = 1f - span(t, 0.84f, 0.95f)
                        val w = size.width * filled / slots.size
                        drawRoundRect(primary.copy(alpha = fade), size = Size(w, size.height), cornerRadius = CornerRadius(size.height / 2f))
                    }
            )
            Spacer(Modifier.height(16.dp))
            listOf(0.9f, 0.7f, 0.8f).forEach { fraction ->
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(colors.onSurface.copy(alpha = 0.12f))
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
