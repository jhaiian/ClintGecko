package com.jhaiian.clint.setup

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.OndemandVideo
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jhaiian.clint.R
import com.jhaiian.clint.settings.lookandfeel.LanguageSelectorDialog
import com.jhaiian.clint.settings.lookandfeel.languageSummaryText
import com.jhaiian.clint.ui.ClintCheckbox
import com.jhaiian.clint.ui.ThemeSwatchUtils
import com.jhaiian.clint.ui.rememberClintFavicon
import com.jhaiian.clint.ui.theme.LocalClintColors
import com.jhaiian.clint.ui.theme.rememberIsLightTheme
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

data class IntensitySwatchColors(
    val noTintBg: Color, val noTintSurface: Color,
    val softBg: Color, val softSurface: Color,
    val strongBg: Color, val strongSurface: Color,
    val pureBg: Color, val pureSurface: Color,
    val accent: Color
)

@Composable
fun rememberIntensitySwatchColors(theme: String, accent: String): IntensitySwatchColors {
    val context = LocalContext.current
    val isLight = rememberIsLightTheme(theme)
    return remember(theme, accent, isLight) {
        val accentColorInt = ThemeSwatchUtils.resolveSwatchColors(context, theme, accent).accent
        val (noTintBg, noTintSurface) = ThemeSwatchUtils.resolveNoTintSwatchBgSurface(context, theme, accent)
        val (softBg, softSurface) = ThemeSwatchUtils.resolveSoftTintSwatchBgSurface(context, theme, accent)
        val strongSwatch = ThemeSwatchUtils.resolveSwatchColors(context, theme, accent)
        val pure = if (isLight) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
        IntensitySwatchColors(
            noTintBg = Color(noTintBg), noTintSurface = Color(noTintSurface),
            softBg = Color(softBg), softSurface = Color(softSurface),
            strongBg = Color(strongSwatch.bg),
            strongSurface = Color(strongSwatch.surface),
            pureBg = Color(pure), pureSurface = Color(pure),
            accent = Color(accentColorInt)
        )
    }
}

internal data class SetupAccent(val key: String, val titleRes: Int, val descRes: Int)

internal val SetupAccentOptions = listOf(
    SetupAccent("material_you", R.string.accent_material_you, R.string.accent_material_you_desc),
    SetupAccent("purple", R.string.accent_purple, R.string.accent_purple_desc),
    SetupAccent("deep_purple", R.string.accent_deep_purple, R.string.accent_deep_purple_desc),
    SetupAccent("royal_purple", R.string.accent_royal_purple, R.string.accent_royal_purple_desc),
    SetupAccent("amethyst", R.string.accent_amethyst, R.string.accent_amethyst_desc),
    SetupAccent("lavender", R.string.accent_lavender, R.string.accent_lavender_desc),
    SetupAccent("plum", R.string.accent_plum, R.string.accent_plum_desc),
    SetupAccent("violet", R.string.accent_violet, R.string.accent_violet_desc),
    SetupAccent("default", R.string.accent_default, R.string.accent_default_desc),
    SetupAccent("charcoal", R.string.accent_charcoal, R.string.accent_charcoal_desc),
    SetupAccent("slate", R.string.accent_slate, R.string.accent_slate_desc),
    SetupAccent("graphite", R.string.accent_graphite, R.string.accent_graphite_desc),
    SetupAccent("obsidian", R.string.accent_obsidian, R.string.accent_obsidian_desc),
    SetupAccent("onyx", R.string.accent_onyx, R.string.accent_onyx_desc),
    SetupAccent("titanium", R.string.accent_titanium, R.string.accent_titanium_desc),
    SetupAccent("indigo", R.string.accent_indigo, R.string.accent_indigo_desc),
    SetupAccent("blue", R.string.accent_blue, R.string.accent_blue_desc),
    SetupAccent("midnight", R.string.accent_midnight, R.string.accent_midnight_desc),
    SetupAccent("cyan", R.string.accent_cyan, R.string.accent_cyan_desc),
    SetupAccent("sky", R.string.accent_sky, R.string.accent_sky_desc),
    SetupAccent("teal", R.string.accent_teal, R.string.accent_teal_desc),
    SetupAccent("azure", R.string.accent_azure, R.string.accent_azure_desc),
    SetupAccent("yellow", R.string.accent_yellow, R.string.accent_yellow_desc),
    SetupAccent("lemon", R.string.accent_lemon, R.string.accent_lemon_desc),
    SetupAccent("gold", R.string.accent_gold, R.string.accent_gold_desc),
    SetupAccent("amber", R.string.accent_amber, R.string.accent_amber_desc),
    SetupAccent("sand", R.string.accent_sand, R.string.accent_sand_desc),
    SetupAccent("sepia", R.string.accent_sepia, R.string.accent_sepia_desc),
    SetupAccent("mustard", R.string.accent_mustard, R.string.accent_mustard_desc),
    SetupAccent("red", R.string.accent_red, R.string.accent_red_desc),
    SetupAccent("crimson", R.string.accent_crimson, R.string.accent_crimson_desc),
    SetupAccent("ruby", R.string.accent_ruby, R.string.accent_ruby_desc),
    SetupAccent("pink", R.string.accent_pink, R.string.accent_pink_desc),
    SetupAccent("coral", R.string.accent_coral, R.string.accent_coral_desc),
    SetupAccent("burgundy", R.string.accent_burgundy, R.string.accent_burgundy_desc),
    SetupAccent("scarlet", R.string.accent_scarlet, R.string.accent_scarlet_desc),
    SetupAccent("orange", R.string.accent_orange, R.string.accent_orange_desc),
    SetupAccent("deep_orange", R.string.accent_deep_orange, R.string.accent_deep_orange_desc),
    SetupAccent("tangerine", R.string.accent_tangerine, R.string.accent_tangerine_desc),
    SetupAccent("apricot", R.string.accent_apricot, R.string.accent_apricot_desc),
    SetupAccent("copper", R.string.accent_copper, R.string.accent_copper_desc),
    SetupAccent("peach", R.string.accent_peach, R.string.accent_peach_desc),
    SetupAccent("terracotta", R.string.accent_terracotta, R.string.accent_terracotta_desc),
    SetupAccent("green", R.string.accent_green, R.string.accent_green_desc),
    SetupAccent("emerald", R.string.accent_emerald, R.string.accent_emerald_desc),
    SetupAccent("mint", R.string.accent_mint, R.string.accent_mint_desc),
    SetupAccent("forest", R.string.accent_forest, R.string.accent_forest_desc),
    SetupAccent("sage", R.string.accent_sage, R.string.accent_sage_desc),
    SetupAccent("lime", R.string.accent_lime, R.string.accent_lime_desc),
    SetupAccent("olive", R.string.accent_olive, R.string.accent_olive_desc)
)

internal val SetupCuratedAccents = listOf(
    "material_you", "purple", "indigo", "blue", "cyan", "teal", "emerald", "lime",
    "gold", "orange", "coral", "red", "pink", "lavender", "charcoal"
)

private data class SetupEngine(val key: String, val titleRes: Int, val descRes: Int, val homeUrl: String, val isDefault: Boolean)

private val SetupEngines = listOf(
    SetupEngine("duckduckgo", R.string.engine_duckduckgo, R.string.engine_duckduckgo_desc, "https://duckduckgo.com", true),
    SetupEngine("brave", R.string.engine_brave, R.string.engine_brave_desc, "https://search.brave.com", false),
    SetupEngine("ecosia", R.string.engine_ecosia, R.string.engine_ecosia_desc, "https://www.ecosia.org", false),
    SetupEngine("google", R.string.engine_google, R.string.engine_google_desc, "https://www.google.com", false)
)

internal fun engineHomeUrl(state: SetupUiState): String =
    if (state.engine == "custom") state.customEngineUrl
    else SetupEngines.firstOrNull { it.key == state.engine }?.homeUrl ?: SetupEngines.first().homeUrl

@Composable
internal fun engineDisplayName(state: SetupUiState): String {
    if (state.engine == "custom") return state.customEngineName.ifBlank { stringResource(R.string.engine_custom) }
    val res = SetupEngines.firstOrNull { it.key == state.engine }?.titleRes ?: R.string.engine_duckduckgo
    return stringResource(res)
}

@Composable
internal fun SetupWelcomePage(activity: SetupActivity, state: SetupUiState, metrics: PageMetrics, scrollState: ScrollState) {
    val colors = LocalClintColors.current
    var showLanguageDialog by remember { mutableStateOf(false) }
    if (showLanguageDialog) {
        LanguageSelectorDialog(
            current = state.language,
            hideStatusBar = state.hideStatusBar,
            hideSystemNavigation = state.hideSystemNavigation,
            onSelect = {
                showLanguageDialog = false
                activity.selectLanguage(it)
            },
            onDismiss = { showLanguageDialog = false }
        )
    }
    SetupPage(metrics, scrollState, hero = { ClintMarkHero(it) }) {
        Text(
            text = stringResource(R.string.setup_welcome_eyebrow),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.primary,
            textAlign = metrics.align,
            modifier = Modifier
                .fillMaxWidth()
                .rise(0)
        )
        RevealTitle(stringResource(R.string.app_name), centered = !metrics.wide)
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.setup_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.secondaryText,
            textAlign = metrics.align,
            modifier = Modifier
                .fillMaxWidth()
                .rise(4)
        )
        Spacer(Modifier.height(22.dp))
        FeatureRow(Icons.Outlined.Download, stringResource(R.string.setup_feature_downloads_title), stringResource(R.string.setup_feature_downloads), Modifier.rise(5))
        FeatureRow(Icons.Outlined.OndemandVideo, stringResource(R.string.setup_feature_media_title), stringResource(R.string.setup_feature_media), Modifier.rise(6))
        FeatureRow(Icons.Outlined.Shield, stringResource(R.string.setup_feature_privacy_title), stringResource(R.string.setup_feature_privacy), Modifier.rise(7))
        FeatureRow(Icons.Outlined.Extension, stringResource(R.string.setup_feature_extensions_title), stringResource(R.string.setup_feature_extensions), Modifier.rise(8))
        Spacer(Modifier.height(18.dp))
        LanguageCard(state.language, Modifier.rise(9)) { showLanguageDialog = true }
        Spacer(Modifier.height(10.dp))
        ConsentCard(activity, state, Modifier.rise(10))
    }
}

@Composable
private fun LanguageCard(language: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = LocalClintColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.cardBackground)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(12.dp)
    ) {
        IconTile(Icons.Outlined.Language)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.pref_language_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )
            Text(
                text = languageSummaryText(language),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.primary
            )
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = colors.secondaryText)
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun ConsentCard(activity: SetupActivity, state: SetupUiState, modifier: Modifier = Modifier) {
    val colors = LocalClintColors.current
    val haptics = rememberSetupHaptics()
    val shake = remember { Animatable(1f) }
    val requester = remember { BringIntoViewRequester() }
    LaunchedEffect(state.consentNudge) {
        if (state.consentNudge > 0) {
            requester.bringIntoView()
            shake.snapTo(0f)
            shake.animateTo(1f, tween(560, easing = LinearEasing))
        }
    }
    val stroke by animateColorAsState(
        when {
            state.consentChecked -> colors.primary
            shake.value < 1f -> colors.colorError
            else -> colors.divider
        },
        label = "consent_stroke"
    )
    val container by animateColorAsState(
        if (state.consentChecked) colors.primary.copy(alpha = 0.12f) else colors.cardBackground,
        label = "consent_bg"
    )
    val toggle: (Boolean) -> Unit = { checked ->
        haptics.toggle(checked)
        state.consentChecked = checked
    }
    Column(
        modifier = modifier
            .bringIntoViewRequester(requester)
            .graphicsLayer {
                val v = shake.value
                translationX = sin(v * PI.toFloat() * 6f) * (1f - v) * 14.dp.toPx()
            }
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(if (state.consentChecked) 2.dp else 1.dp, stroke, RoundedCornerShape(20.dp))
            .background(container)
            .padding(vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = state.consentChecked, role = Role.Checkbox, onValueChange = toggle)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            ClintCheckbox(checked = state.consentChecked, onCheckedChange = toggle)
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.setup_terms_agree),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            DocumentLink(Icons.Outlined.Policy, stringResource(R.string.document_viewer_privacy_policy_title)) { activity.openPrivacyPolicy() }
            DocumentLink(Icons.Outlined.Gavel, stringResource(R.string.document_viewer_terms_title)) { activity.openTerms() }
        }
    }
}

@Composable
private fun DocumentLink(icon: ImageVector, label: String, onClick: () -> Unit) {
    val colors = LocalClintColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .background(colors.primary.copy(alpha = 0.12f))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = colors.primary
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SetupLookPage(activity: SetupActivity, state: SetupUiState, metrics: PageMetrics, scrollState: ScrollState) {
    val context = LocalContext.current
    val colors = LocalClintColors.current
    val haptics = rememberSetupHaptics()
    val swatchTheme = if (colors.isLight) "light" else "dark"
    val swatches = remember(swatchTheme) {
        SetupAccentOptions.associate { option ->
            option.key to runCatching { Color(ThemeSwatchUtils.resolveSwatchColors(context, swatchTheme, option.key).accent) }.getOrDefault(Color.Gray)
        }
    }
    val visibleAccents = if (state.showAllAccents) SetupAccentOptions else SetupAccentOptions.filter { it.key in SetupCuratedAccents }
    SetupPage(metrics, scrollState, hero = { BrowserPreviewHero(it) }) {
        PageHeader(stringResource(R.string.setup_theme_title), stringResource(R.string.setup_theme_subtitle), metrics)
        Spacer(Modifier.height(24.dp))
        SectionTitle(stringResource(R.string.setup_section_theme), metrics, Modifier.rise(2))
        Spacer(Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .rise(3)
        ) {
            listOf(
                Triple("system", R.string.theme_system, Icons.Outlined.BrightnessAuto),
                Triple("light", R.string.theme_light, Icons.Outlined.LightMode),
                Triple("dark", R.string.theme_dark, Icons.Outlined.DarkMode)
            ).forEach { (key, labelRes, icon) ->
                val selected = state.theme == key
                ChoiceCard(
                    label = stringResource(labelRes),
                    selected = selected,
                    onClick = {
                        if (!selected) {
                            haptics.tick()
                            activity.selectTheme(key)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    val lift by animateFloatAsState(if (selected) 1f else 0f, spring(dampingRatio = 0.5f, stiffness = 300f), label = "theme_icon_lift")
                    Box(Modifier.graphicsLayer { rotationZ = -24f * (1f - lift) }) {
                        IconTile(icon, selected = selected, size = 44.dp)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        SectionTitle(stringResource(R.string.setup_accent_section_label), metrics, Modifier.rise(4))
        Spacer(Modifier.height(12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp, if (metrics.wide) Alignment.Start else Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(spring(dampingRatio = 0.8f, stiffness = 220f))
                .rise(5)
        ) {
            visibleAccents.forEach { option ->
                key(option.key) {
                    AccentChip(
                        color = swatches[option.key] ?: Color.Gray,
                        selected = state.accent == option.key,
                        wallpaper = option.key == "material_you",
                        label = stringResource(option.titleRes),
                        onClick = {
                            if (state.accent != option.key) {
                                haptics.tick()
                                activity.selectAccent(option.key)
                            }
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        ShowAllAccentsButton(
            expanded = state.showAllAccents,
            metrics = metrics,
            modifier = Modifier.rise(5)
        ) {
            haptics.tick()
            state.showAllAccents = !state.showAllAccents
        }
        Crossfade(targetState = state.accent, label = "accent_caption", modifier = Modifier.rise(6)) { key ->
            SetupAccentOptions.firstOrNull { it.key == key }?.let { option ->
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(option.titleRes),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface,
                        textAlign = metrics.align,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                    Text(
                        text = stringResource(option.descRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.secondaryText,
                        textAlign = metrics.align,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        val pure = state.intensity == "pure_mode"
        SetupToggleCard(
            icon = Icons.Outlined.Contrast,
            title = stringResource(R.string.setup_look_pure_title),
            body = stringResource(if (colors.isLight) R.string.surface_intensity_pure_light_desc else R.string.surface_intensity_pure_dark_desc),
            checked = pure,
            onCheckedChange = { enabled ->
                haptics.toggle(enabled)
                activity.selectIntensity(if (enabled) "pure_mode" else "soft_tint")
            },
            modifier = Modifier.rise(7)
        )
    }
}

@Composable
private fun ShowAllAccentsButton(expanded: Boolean, metrics: PageMetrics, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = LocalClintColors.current
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, spring(dampingRatio = 0.6f, stiffness = 300f), label = "accent_expand")
    Box(
        contentAlignment = if (metrics.wide) Alignment.CenterStart else Alignment.Center,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(if (expanded) R.string.setup_accent_show_less else R.string.setup_accent_show_all),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.primary
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = rotation }
            )
        }
    }
}

@Composable
private fun AccentChip(
    color: Color,
    selected: Boolean,
    wallpaper: Boolean,
    label: String,
    onClick: () -> Unit
) {
    val ring by animateFloatAsState(if (selected) 1f else 0f, spring(dampingRatio = 0.5f, stiffness = 320f), label = "swatch_ring")
    val appear = rememberEntranceProgress()
    LaunchedEffect(Unit) { if (appear.value < 1f) appear.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 260f)) }
    val onColor = if (color.luminance() > 0.5f) Color.Black else Color.White
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .graphicsLayer {
                val v = appear.value
                scaleX = 0.6f + 0.4f * v
                scaleY = 0.6f + 0.4f * v
                alpha = v.coerceIn(0f, 1f)
            }
            .clip(CircleShape)
            .semantics { contentDescription = label }
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .drawBehind {
                val outer = size.minDimension / 2f
                drawCircle(
                    color = color.copy(alpha = ring),
                    radius = outer - 1.5.dp.toPx(),
                    style = Stroke(2.5.dp.toPx())
                )
                drawCircle(
                    color = color,
                    radius = outer * (0.86f - 0.14f * ring)
                )
            }
    ) {
        if (wallpaper) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = onColor.copy(alpha = 0.85f * (1f - ring)),
                modifier = Modifier.size(18.dp)
            )
        }
        Icon(
            imageVector = Icons.Outlined.Check,
            contentDescription = null,
            tint = onColor,
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer {
                    scaleX = ring
                    scaleY = ring
                    alpha = ring.coerceIn(0f, 1f)
                    rotationZ = -90f * (1f - ring)
                }
        )
    }
}

@Composable
internal fun SetupSearchPage(state: SetupUiState, metrics: PageMetrics, scrollState: ScrollState) {
    val haptics = rememberSetupHaptics()
    var customEditorOpen by remember { mutableStateOf(false) }
    val hasCustomEngine = state.customEngineName.isNotBlank() && state.customEngineUrl.isNotBlank()
    if (customEditorOpen) {
        com.jhaiian.clint.ui.CustomSearchEngineDialog(
            initialName = state.customEngineName,
            initialUrl = state.customEngineUrl,
            hideStatusBar = state.hideStatusBar,
            hideSystemNavigation = state.hideSystemNavigation,
            onConfirm = { name, url ->
                state.customEngineName = name
                state.customEngineUrl = url
                state.engine = "custom"
                customEditorOpen = false
                haptics.confirm()
            },
            onDismiss = { customEditorOpen = false }
        )
    }
    val select: (String) -> Unit = { key ->
        if (state.engine != key) {
            haptics.tick()
            state.engine = key
        }
    }
    SetupPage(metrics, scrollState, hero = { SearchHero(engineDisplayName(state), engineHomeUrl(state), it) }) {
        PageHeader(stringResource(R.string.setup_search_title), stringResource(R.string.setup_search_body), metrics)
        Spacer(Modifier.height(20.dp))
        SetupEngines.forEachIndexed { i, engine ->
            val title = stringResource(engine.titleRes)
            EngineCard(
                initial = title.take(1),
                homeUrl = engine.homeUrl,
                title = title,
                body = stringResource(engine.descRes),
                selected = state.engine == engine.key,
                isDefault = engine.isDefault,
                onClick = { select(engine.key) },
                modifier = Modifier.rise(2 + i)
            )
        }
        val customTitle = if (hasCustomEngine) state.customEngineName else stringResource(R.string.engine_custom)
        EngineCard(
            initial = customTitle.take(1),
            homeUrl = if (hasCustomEngine) state.customEngineUrl else "",
            title = customTitle,
            body = if (hasCustomEngine) state.customEngineUrl else stringResource(R.string.engine_custom_desc),
            selected = state.engine == "custom",
            isDefault = false,
            onClick = { if (hasCustomEngine) select("custom") else customEditorOpen = true },
            onEdit = { customEditorOpen = true },
            modifier = Modifier.rise(2 + SetupEngines.size)
        )
    }
}

@Composable
private fun EngineCard(
    initial: String,
    homeUrl: String,
    title: String,
    body: String,
    selected: Boolean,
    isDefault: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null
) {
    val colors = LocalClintColors.current
    val container by animateColorAsState(
        if (selected) colors.primary.copy(alpha = 0.14f) else colors.cardBackground,
        label = "engine_bg"
    )
    val stroke by animateColorAsState(if (selected) colors.primary else colors.divider, label = "engine_stroke")
    val favicon = if (homeUrl.isNotBlank()) rememberClintFavicon(homeUrl) else null
    val avatar by animateColorAsState(
        when {
            favicon != null -> if (selected) colors.primary.copy(alpha = 0.28f) else colors.surfaceVariant
            selected -> colors.primary
            else -> colors.primary.copy(alpha = 0.16f)
        },
        label = "engine_avatar"
    )
    val avatarText by animateColorAsState(if (selected) colors.onPrimary else colors.primary, label = "engine_avatar_text")
    val mark by animateFloatAsState(if (selected) 1f else 0f, spring(dampingRatio = 0.5f, stiffness = 320f), label = "engine_mark")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(if (selected) 2.dp else 1.dp, stroke, RoundedCornerShape(20.dp))
            .background(container)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = if (onEdit != null) 4.dp else 14.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(42.dp)
                .graphicsLayer {
                    scaleX = 1f + 0.08f * mark
                    scaleY = 1f + 0.08f * mark
                }
                .clip(CircleShape)
                .background(avatar)
        ) {
            if (favicon != null) {
                Image(
                    bitmap = favicon.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
            } else {
                Text(
                    text = initial.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = avatarText
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isDefault) {
                    Spacer(Modifier.width(8.dp))
                    DefaultChip(stringResource(R.string.default_label), colors.primary)
                }
            }
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.secondaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .border(2.dp, if (selected) colors.primary else colors.divider, CircleShape)
                .drawBehind { drawCircle(colors.primary, size.minDimension / 2f * mark) }
        ) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer {
                        scaleX = mark
                        scaleY = mark
                        alpha = mark.coerceIn(0f, 1f)
                    }
            )
        }
        if (onEdit != null) {
            IconButton(onClick = onEdit) {
                Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.action_edit), tint = colors.iconTint)
            }
        }
    }
}

@Composable
private fun StatusBanner(text: String) {
    val colors = LocalClintColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.primary.copy(alpha = 0.16f))
            .padding(16.dp)
    ) {
        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = colors.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = colors.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun SetupNotificationsPage(state: SetupUiState, metrics: PageMetrics, scrollState: ScrollState, onAllow: () -> Unit) {
    SetupPage(metrics, scrollState, hero = { NotificationHero(it) }) {
        PageHeader(stringResource(R.string.setup_notifications_title), stringResource(R.string.setup_notifications_body), metrics)
        Spacer(Modifier.height(24.dp))
        AnimatedContent(
            targetState = state.notificationsGranted,
            transitionSpec = {
                (fadeIn(tween(260)) + scaleIn(spring(dampingRatio = 0.5f, stiffness = 260f), initialScale = 0.85f)) togetherWith
                    (fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 0.9f))
            },
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .rise(2),
            label = "notification_state"
        ) { on ->
            if (on) {
                StatusBanner(stringResource(R.string.setup_notifications_on))
            } else {
                SetupActionButton(
                    label = stringResource(R.string.setup_notifications_allow),
                    icon = Icons.Outlined.NotificationsActive,
                    minHeight = 52.dp,
                    onClick = onAllow,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
internal fun SetupDefaultBrowserPage(state: SetupUiState, metrics: PageMetrics, scrollState: ScrollState, onSetDefault: () -> Unit) {
    val appName = stringResource(R.string.app_name)
    SetupPage(metrics, scrollState, hero = { DefaultBrowserHero(it) }) {
        PageHeader(stringResource(R.string.setup_default_browser_title), stringResource(R.string.setup_default_browser_description), metrics)
        Spacer(Modifier.height(24.dp))
        AnimatedContent(
            targetState = state.isDefaultBrowser,
            transitionSpec = {
                (fadeIn(tween(260)) + scaleIn(spring(dampingRatio = 0.5f, stiffness = 260f), initialScale = 0.85f)) togetherWith
                    (fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 0.9f))
            },
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .rise(2),
            label = "default_browser_state"
        ) { isDefault ->
            if (isDefault) {
                StatusBanner(stringResource(R.string.setup_default_browser_on, appName))
            } else {
                SetupActionButton(
                    label = stringResource(R.string.setup_default_browser_set_button),
                    icon = Icons.Outlined.OpenInBrowser,
                    minHeight = 52.dp,
                    onClick = onSetDefault,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SetupDonePage(state: SetupUiState, metrics: PageMetrics, scrollState: ScrollState, extraChips: List<Pair<ImageVector, String>> = emptyList()) {
    val appName = stringResource(R.string.app_name)
    val accent = SetupAccentOptions.firstOrNull { it.key == state.accent }
    val position = when (state.addressBarPosition) {
        "bottom" -> R.string.address_bar_position_bottom
        "split" -> R.string.address_bar_position_split
        else -> R.string.address_bar_position_top
    }
    val themeRes = when (state.theme) {
        "light" -> R.string.theme_light
        "dark" -> R.string.theme_dark
        else -> R.string.theme_system
    }
    val themeIcon = when (state.theme) {
        "light" -> Icons.Outlined.LightMode
        "dark" -> Icons.Outlined.DarkMode
        else -> Icons.Outlined.BrightnessAuto
    }
    val chips = buildList {
        add(Icons.Outlined.Search to engineDisplayName(state))
        add(themeIcon to stringResource(themeRes))
        if (accent != null) add(Icons.Outlined.Palette to stringResource(accent.titleRes))
        add(Icons.Outlined.ViewAgenda to stringResource(position))
        if (state.httpsOnly) add(Icons.Outlined.Security to stringResource(R.string.https_only))
        addAll(extraChips)
    }
    SetupPage(metrics, scrollState, hero = { CelebrationHero(it) }) {
        PageHeader(stringResource(R.string.setup_done_title), stringResource(R.string.setup_done_body, appName), metrics)
        Spacer(Modifier.height(22.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, if (metrics.wide) Alignment.Start else Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            chips.forEachIndexed { i, (icon, label) ->
                SummaryChip(icon, label, Modifier.rise(2 + i))
            }
        }
    }
}

@Composable
private fun SummaryChip(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    val colors = LocalClintColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(CircleShape)
            .background(colors.cardBackground)
            .border(1.dp, colors.divider, CircleShape)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
