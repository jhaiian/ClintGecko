package com.jhaiian.clint.settings.browser

import android.content.SharedPreferences
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Gradient
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.VerticalAlignCenter
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.home.HOMEPAGE_ALL_ACTIONS
import com.jhaiian.clint.browser.home.HOMEPAGE_ALL_WIDGETS
import com.jhaiian.clint.browser.home.HOMEPAGE_CLINT
import com.jhaiian.clint.browser.home.HOMEPAGE_COLUMN_CHOICES
import com.jhaiian.clint.browser.home.HOMEPAGE_CUSTOM
import com.jhaiian.clint.browser.home.HOMEPAGE_DESIGN_CUSTOM_GRADIENT
import com.jhaiian.clint.browser.home.HOMEPAGE_DESIGN_GRADIENT
import com.jhaiian.clint.browser.home.HOMEPAGE_DESIGN_IMAGE
import com.jhaiian.clint.browser.home.HOMEPAGE_DESIGN_SOLID
import com.jhaiian.clint.browser.home.HOMEPAGE_FOLDER_GRID
import com.jhaiian.clint.browser.home.HOMEPAGE_FOLDER_PLAIN
import com.jhaiian.clint.browser.home.HOMEPAGE_HERO_FULL
import com.jhaiian.clint.browser.home.HOMEPAGE_HERO_LOGO
import com.jhaiian.clint.browser.home.HOMEPAGE_HERO_NONE
import com.jhaiian.clint.browser.home.HOMEPAGE_HERO_TEXT
import com.jhaiian.clint.browser.home.HOMEPAGE_SEARCH_ENGINE
import com.jhaiian.clint.browser.home.HOMEPAGE_TILE_CIRCLE
import com.jhaiian.clint.browser.home.HOMEPAGE_TILE_SQUARE
import com.jhaiian.clint.browser.home.HOMEPAGE_TILE_SQUIRCLE
import com.jhaiian.clint.browser.home.PREF_HOMEPAGE
import com.jhaiian.clint.browser.home.PREF_HOMEPAGE_BG_COLOR
import com.jhaiian.clint.browser.home.PREF_HOMEPAGE_CARD_CORNER
import com.jhaiian.clint.browser.home.PREF_HOMEPAGE_CARD_OPACITY
import com.jhaiian.clint.browser.home.PREF_HOMEPAGE_CENTER_CONTENT
import com.jhaiian.clint.browser.home.PREF_HOMEPAGE_COLUMNS
import com.jhaiian.clint.browser.home.PREF_HOMEPAGE_CUSTOM_URL
import com.jhaiian.clint.browser.home.PREF_HOMEPAGE_DESIGN
import com.jhaiian.clint.browser.home.PREF_HOMEPAGE_FOLDER_STYLE
import com.jhaiian.clint.browser.home.PREF_HOMEPAGE_GRADIENT_ANGLE
import com.jhaiian.clint.browser.home.PREF_HOMEPAGE_GRADIENT_END
import com.jhaiian.clint.browser.home.PREF_HOMEPAGE_GRADIENT_START
import com.jhaiian.clint.browser.home.PREF_HOMEPAGE_HERO_STYLE
import com.jhaiian.clint.browser.home.PREF_HOMEPAGE_IMAGE_BLUR
import com.jhaiian.clint.browser.home.PREF_HOMEPAGE_IMAGE_DIM
import com.jhaiian.clint.browser.home.PREF_HOMEPAGE_IMAGE_FULL_BRIGHTNESS
import com.jhaiian.clint.browser.home.PREF_HOMEPAGE_IMAGE_PARALLAX
import com.jhaiian.clint.browser.home.PREF_HOMEPAGE_IMAGE_SEPARATE_DARK
import com.jhaiian.clint.browser.home.PREF_HOMEPAGE_IMAGE_VERSION
import com.jhaiian.clint.browser.home.PREF_HOMEPAGE_QUICK_ACTIONS
import com.jhaiian.clint.browser.home.PREF_HOMEPAGE_RECENT_COUNT
import com.jhaiian.clint.browser.home.PREF_HOMEPAGE_SEARCH_SHAPE
import com.jhaiian.clint.browser.home.PREF_HOMEPAGE_SECTION_ORDER
import com.jhaiian.clint.browser.home.PREF_HOMEPAGE_SEPARATE_TILES
import com.jhaiian.clint.browser.home.PREF_HOMEPAGE_SHOW_FAVORITES
import com.jhaiian.clint.browser.home.PREF_HOMEPAGE_SHOW_GREETING
import com.jhaiian.clint.browser.home.PREF_HOMEPAGE_SHOW_QUICK_ACTIONS
import com.jhaiian.clint.browser.home.PREF_HOMEPAGE_SHOW_RECENT
import com.jhaiian.clint.browser.home.PREF_HOMEPAGE_SHOW_VOICE
import com.jhaiian.clint.browser.home.PREF_HOMEPAGE_TILE_LABELS
import com.jhaiian.clint.browser.home.PREF_HOMEPAGE_TILE_SCALE
import com.jhaiian.clint.browser.home.PREF_HOMEPAGE_TILE_SHAPE
import com.jhaiian.clint.browser.home.PREF_HOMEPAGE_TITLE_TEXT
import com.jhaiian.clint.browser.home.PREF_HOMEPAGE_WIDGETS
import com.jhaiian.clint.browser.home.gradientEndColor
import com.jhaiian.clint.browser.home.HOMEPAGE_PREVIEW_RATIO
import com.jhaiian.clint.browser.home.HomepageLayout
import com.jhaiian.clint.browser.home.HomepagePreview
import com.jhaiian.clint.browser.home.gradientStartColor
import com.jhaiian.clint.browser.home.homepageActionIcon
import com.jhaiian.clint.browser.home.homepageActionLabel
import com.jhaiian.clint.browser.home.homepageBlockLabel
import com.jhaiian.clint.browser.home.homepageKey
import com.jhaiian.clint.browser.home.homepageWidgetLabel
import com.jhaiian.clint.browser.home.readHomepageLayout
import com.jhaiian.clint.browser.home.resetHomepageProfile
import com.jhaiian.clint.browser.home.resolveHomepageImage
import com.jhaiian.clint.browser.home.saveHomepageBackground
import com.jhaiian.clint.browser.home.solidColor
import com.jhaiian.clint.profiles.ProfileRepository
import com.jhaiian.clint.settings.SettingsActivity
import com.jhaiian.clint.settings.common.SettingsRow
import com.jhaiian.clint.settings.common.SettingsScreenScaffold
import com.jhaiian.clint.settings.common.SettingsSection
import com.jhaiian.clint.settings.common.SettingsSectionLabel
import com.jhaiian.clint.ui.ClintColorPickerDialog
import com.jhaiian.clint.ui.ClintSwitch
import com.jhaiian.clint.ui.theme.LocalClintColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
private fun HomepageSwitchRow(
    icon: ImageVector,
    title: String,
    summary: String,
    checked: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    SettingsRow(
        icon = icon,
        title = title,
        summary = summary,
        colors = LocalClintColors.current,
        enabled = enabled,
        onClick = { if (enabled) onToggle() },
        trailing = { ClintSwitch(checked = checked && enabled) }
    )
}

@Composable
private fun HomepageValueRow(
    icon: ImageVector,
    title: String,
    summary: String,
    enabled: Boolean,
    swatch: Color? = null,
    onClick: () -> Unit
) {
    val colors = LocalClintColors.current
    SettingsRow(
        icon = icon,
        title = title,
        summary = summary,
        colors = colors,
        enabled = enabled,
        onClick = { if (enabled) onClick() },
        trailing = {
            if (swatch != null) {
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(swatch)
                        .border(1.dp, colors.divider, CircleShape)
                )
            }
        }
    )
}

@Composable
private fun HomepagePreviewCard(layout: HomepageLayout, profileId: String?, enabled: Boolean, onExpand: () -> Unit) {
    val colors = LocalClintColors.current
    Surface(
        color = colors.cardBackground,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(12.dp)
        ) {
            HomepagePreview(layout, profileId, width = 72.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.homepage_preview_title),
                    color = colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold
                )
                Text(
                    stringResource(if (enabled) R.string.homepage_preview_hint else R.string.homepage_preview_unavailable),
                    color = colors.secondaryText, fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            IconButton(onClick = onExpand) {
                Icon(
                    imageVector = Icons.Filled.OpenInFull,
                    contentDescription = stringResource(R.string.homepage_preview_expand),
                    tint = colors.primary
                )
            }
        }
    }
}

@Composable
private fun HomepagePreviewOverlay(layout: HomepageLayout, profileId: String?, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center
    ) {
        val byWidth = maxWidth - 48.dp
        val byHeight = (maxHeight - 64.dp) / HOMEPAGE_PREVIEW_RATIO
        HomepagePreview(layout, profileId, width = if (byWidth < byHeight) byWidth else byHeight)
    }
}

@Composable
fun HomepageSettingsPane(activity: SettingsActivity) {
    val colors = LocalClintColors.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(activity) }
    val hideStatusBar = remember { prefs.getBoolean("hide_status_bar", false) }
    val hideSystemNavigation = remember { prefs.getBoolean("hide_system_navigation", false) }
    var homepage by remember { mutableStateOf(prefs.getString(PREF_HOMEPAGE, HOMEPAGE_CLINT) ?: HOMEPAGE_CLINT) }
    var customUrl by remember { mutableStateOf(prefs.getString(PREF_HOMEPAGE_CUSTOM_URL, "") ?: "") }
    var homepageDialogOpen by remember { mutableStateOf(false) }
    var designDialogOpen by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<String?>(null) }
    var pickDark by remember { mutableStateOf(false) }
    var previewExpanded by remember { mutableStateOf(false) }
    var version by remember { mutableIntStateOf(0) }
    var scopeId by remember { mutableStateOf<String?>(null) }
    val profiles = remember { runCatching { ProfileRepository.all(activity) }.getOrElse { emptyList() } }
    val layout = remember(scopeId, version) { readHomepageLayout(activity, prefs, scopeId) }
    val optionsEnabled = homepage != HOMEPAGE_SEARCH_ENGINE && homepage != HOMEPAGE_CUSTOM
    val scope = rememberCoroutineScope()
    val defaultProfileName = stringResource(R.string.profiles_default_name)
    val scopeName = profiles.firstOrNull { it.id == scopeId }?.name ?: defaultProfileName

    fun key(base: String): String = homepageKey(base, scopeId)

    fun save(block: SharedPreferences.Editor.() -> Unit) {
        prefs.edit().apply(block).apply()
        version++
        dialog = null
    }

    fun flip(base: String, current: Boolean) = save { putBoolean(key(base), !current) }

    val heroOptions = listOf(
        HOMEPAGE_HERO_FULL to stringResource(R.string.homepage_hero_full),
        HOMEPAGE_HERO_LOGO to stringResource(R.string.homepage_hero_logo),
        HOMEPAGE_HERO_TEXT to stringResource(R.string.homepage_hero_text),
        HOMEPAGE_HERO_NONE to stringResource(R.string.homepage_hero_none)
    )
    val tileShapeOptions = listOf(
        HOMEPAGE_TILE_SQUIRCLE to stringResource(R.string.homepage_tile_squircle),
        HOMEPAGE_TILE_CIRCLE to stringResource(R.string.homepage_tile_circle),
        HOMEPAGE_TILE_SQUARE to stringResource(R.string.homepage_tile_square)
    )
    val folderOptions = listOf(
        HOMEPAGE_FOLDER_GRID to stringResource(R.string.homepage_folder_grid),
        HOMEPAGE_FOLDER_PLAIN to stringResource(R.string.homepage_folder_plain)
    )
    val columnOptions = HOMEPAGE_COLUMN_CHOICES.map {
        it.toString() to if (it == 0) stringResource(R.string.homepage_columns_auto) else it.toString()
    }
    val scopeOptions = listOf("" to defaultProfileName) + profiles.map { it.id to it.name }
    val defaultTitle = stringResource(R.string.app_name)
    val noneLabel = stringResource(R.string.homepage_quick_actions_none)
    val orderSummary = layout.sectionOrder.map { stringResource(homepageBlockLabel(it)) }.joinToString(", ")
    val actionsSummary = layout.quickActions.map { stringResource(homepageActionLabel(it)) }.joinToString(", ").ifEmpty { noneLabel }
    val widgetsSummary = layout.widgets.map { stringResource(homepageWidgetLabel(it)) }.joinToString(", ").ifEmpty { noneLabel }

    fun optionLabel(options: List<Pair<String, String>>, key: String): String =
        options.firstOrNull { it.first == key }?.second.orEmpty()

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val targetScope = scopeId
            val dark = pickDark
            scope.launch {
                val saved = withContext(Dispatchers.IO) { saveHomepageBackground(activity, uri, targetScope, dark) }
                if (saved) {
                    save {
                        putString(homepageKey(PREF_HOMEPAGE_DESIGN, targetScope), HOMEPAGE_DESIGN_IMAGE)
                        putLong(homepageKey(PREF_HOMEPAGE_IMAGE_VERSION, targetScope), System.currentTimeMillis())
                    }
                } else {
                    Toast.makeText(activity, R.string.homepage_image_error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun launchImagePicker(dark: Boolean) {
        pickDark = dark
        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    SettingsScreenScaffold(
        header = {
            HomepagePreviewCard(layout, scopeId, enabled = optionsEnabled) { previewExpanded = true }
        },
        overlay = {
            if (previewExpanded) {
                HomepagePreviewOverlay(layout, scopeId) { previewExpanded = false }
            }
            if (homepageDialogOpen) {
                HomepageDialog(
                    current = homepage,
                    currentCustomUrl = customUrl,
                    hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                    onConfirm = { selected, url ->
                        prefs.edit().putString(PREF_HOMEPAGE, selected).putString(PREF_HOMEPAGE_CUSTOM_URL, url).apply()
                        homepage = selected
                        customUrl = url
                        homepageDialogOpen = false
                    },
                    onDismiss = { homepageDialogOpen = false }
                )
            }
            if (designDialogOpen) {
                HomepageDesignDialog(
                    current = layout.design,
                    hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                    onConfirm = { selected ->
                        designDialogOpen = false
                        if (selected == HOMEPAGE_DESIGN_IMAGE && resolveHomepageImage(activity, scopeId, false) == null) {
                            launchImagePicker(false)
                        } else {
                            save { putString(key(PREF_HOMEPAGE_DESIGN), selected) }
                        }
                    },
                    onDismiss = { designDialogOpen = false }
                )
            }
            val dismiss = { dialog = null }
            when (dialog) {
                "scope" -> HomepageChoiceDialog(
                    title = stringResource(R.string.homepage_scope_title), options = scopeOptions, current = scopeId.orEmpty(),
                    hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                    onConfirm = { picked -> scopeId = picked.ifEmpty { null }; dialog = null },
                    onDismiss = dismiss
                )
                "hero" -> HomepageChoiceDialog(
                    title = stringResource(R.string.homepage_hero_style_title), options = heroOptions, current = layout.heroStyle,
                    hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                    onConfirm = { picked -> save { putString(key(PREF_HOMEPAGE_HERO_STYLE), picked) } },
                    onDismiss = dismiss
                )
                "title" -> HomepageTitleDialog(
                    current = layout.title,
                    hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                    onConfirm = { text -> save { putString(key(PREF_HOMEPAGE_TITLE_TEXT), text) } },
                    onDismiss = dismiss
                )
                "order" -> HomepageOrderDialog(
                    title = stringResource(R.string.homepage_section_order_title),
                    keys = layout.sectionOrder, enabled = null,
                    label = { stringResource(homepageBlockLabel(it)) }, icon = { null },
                    hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                    onConfirm = { ordered, _ -> save { putString(key(PREF_HOMEPAGE_SECTION_ORDER), ordered.joinToString(",")) } },
                    onDismiss = dismiss
                )
                "actions" -> HomepageOrderDialog(
                    title = stringResource(R.string.homepage_quick_actions_choose),
                    keys = layout.quickActions + HOMEPAGE_ALL_ACTIONS.filter { it !in layout.quickActions },
                    enabled = layout.quickActions.toSet(),
                    label = { stringResource(homepageActionLabel(it)) }, icon = { homepageActionIcon(it) },
                    hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                    onConfirm = { ordered, enabled ->
                        save { putString(key(PREF_HOMEPAGE_QUICK_ACTIONS), ordered.filter { it in enabled }.joinToString(",")) }
                    },
                    onDismiss = dismiss
                )
                "widgets" -> HomepageOrderDialog(
                    title = stringResource(R.string.homepage_block_widgets),
                    keys = layout.widgets + HOMEPAGE_ALL_WIDGETS.filter { it !in layout.widgets },
                    enabled = layout.widgets.toSet(),
                    label = { stringResource(homepageWidgetLabel(it)) }, icon = { null },
                    hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                    onConfirm = { ordered, enabled ->
                        save { putString(key(PREF_HOMEPAGE_WIDGETS), ordered.filter { it in enabled }.joinToString(",")) }
                    },
                    onDismiss = dismiss
                )
                "columns" -> HomepageChoiceDialog(
                    title = stringResource(R.string.homepage_columns_title), options = columnOptions, current = layout.columns.toString(),
                    hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                    onConfirm = { picked -> save { putInt(key(PREF_HOMEPAGE_COLUMNS), picked.toInt()) } },
                    onDismiss = dismiss
                )
                "tileShape" -> HomepageChoiceDialog(
                    title = stringResource(R.string.homepage_tile_shape_title), options = tileShapeOptions, current = layout.tileShape,
                    hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                    onConfirm = { picked -> save { putString(key(PREF_HOMEPAGE_TILE_SHAPE), picked) } },
                    onDismiss = dismiss
                )
                "folder" -> HomepageChoiceDialog(
                    title = stringResource(R.string.homepage_folder_style_title), options = folderOptions, current = layout.folderStyle,
                    hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                    onConfirm = { picked -> save { putString(key(PREF_HOMEPAGE_FOLDER_STYLE), picked) } },
                    onDismiss = dismiss
                )
                "recent" -> HomepageSliderDialog(
                    title = stringResource(R.string.homepage_recent_count_title), value = layout.recentCount, range = 1..10, step = 1,
                    valueLabel = { it.toString() },
                    hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                    onConfirm = { picked -> save { putInt(key(PREF_HOMEPAGE_RECENT_COUNT), picked) } },
                    onDismiss = dismiss
                )
                "opacity" -> HomepageSliderDialog(
                    title = stringResource(R.string.homepage_card_opacity_title), value = layout.cardOpacity, range = 30..100, step = 5,
                    valueLabel = { stringResource(R.string.homepage_percent_value, it) },
                    hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                    onConfirm = { picked -> save { putInt(key(PREF_HOMEPAGE_CARD_OPACITY), picked) } },
                    onDismiss = dismiss
                )
                "corner" -> HomepageSliderDialog(
                    title = stringResource(R.string.homepage_card_corner_title), value = layout.cardCorner, range = 4..32, step = 2,
                    valueLabel = { stringResource(R.string.homepage_dp_value, it) },
                    hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                    onConfirm = { picked -> save { putInt(key(PREF_HOMEPAGE_CARD_CORNER), picked) } },
                    onDismiss = dismiss
                )
                "tileScale" -> HomepageSliderDialog(
                    title = stringResource(R.string.homepage_tile_size_title), value = layout.tileScale, range = 70..140, step = 5,
                    valueLabel = { stringResource(R.string.homepage_percent_value, it) },
                    hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                    onConfirm = { picked -> save { putInt(key(PREF_HOMEPAGE_TILE_SCALE), picked) } },
                    onDismiss = dismiss
                )
                "dim" -> HomepageSliderDialog(
                    title = stringResource(R.string.homepage_image_dim_title), value = layout.imageDim, range = 0..100, step = 5,
                    valueLabel = { stringResource(R.string.homepage_percent_value, it) },
                    hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                    onConfirm = { picked -> save { putInt(key(PREF_HOMEPAGE_IMAGE_DIM), picked) } },
                    onDismiss = dismiss
                )
                "blur" -> HomepageSliderDialog(
                    title = stringResource(R.string.homepage_image_blur_title), value = layout.imageBlur, range = 0..100, step = 5,
                    valueLabel = { stringResource(R.string.homepage_percent_value, it) },
                    hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                    onConfirm = { picked -> save { putInt(key(PREF_HOMEPAGE_IMAGE_BLUR), picked) } },
                    onDismiss = dismiss
                )
                "gradAngle" -> HomepageSliderDialog(
                    title = stringResource(R.string.homepage_gradient_angle_title), value = layout.gradientAngle, range = 0..345, step = 15,
                    valueLabel = { stringResource(R.string.homepage_degree_value, it) },
                    hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                    onConfirm = { picked -> save { putInt(key(PREF_HOMEPAGE_GRADIENT_ANGLE), picked) } },
                    onDismiss = dismiss
                )
                "bgColor" -> ClintColorPickerDialog(
                    initial = layout.solidColor(colors),
                    hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                    onDismiss = dismiss,
                    onPicked = { picked -> save { putInt(key(PREF_HOMEPAGE_BG_COLOR), picked.toArgb()) } }
                )
                "gradStart" -> ClintColorPickerDialog(
                    initial = layout.gradientStartColor(colors),
                    hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                    onDismiss = dismiss,
                    onPicked = { picked -> save { putInt(key(PREF_HOMEPAGE_GRADIENT_START), picked.toArgb()) } }
                )
                "gradEnd" -> ClintColorPickerDialog(
                    initial = layout.gradientEndColor(colors),
                    hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                    onDismiss = dismiss,
                    onPicked = { picked -> save { putInt(key(PREF_HOMEPAGE_GRADIENT_END), picked.toArgb()) } }
                )
                "reset" -> HomepageConfirmDialog(
                    title = stringResource(R.string.homepage_profile_reset_title),
                    message = stringResource(R.string.homepage_profile_reset_message),
                    hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                    onConfirm = {
                        scopeId?.let { resetHomepageProfile(activity, prefs, it) }
                        version++
                        dialog = null
                    },
                    onDismiss = dismiss
                )
            }
        }
    ) {
        if (profiles.isNotEmpty()) {
            SettingsSectionLabel(stringResource(R.string.homepage_section_profile))
            SettingsSection(colors.cardBackground) {
                HomepageValueRow(Icons.Filled.AccountCircle, stringResource(R.string.homepage_scope_title), scopeName, true) { dialog = "scope" }
                if (scopeId != null) {
                    HomepageSwitchRow(
                        Icons.Filled.Bookmarks, stringResource(R.string.homepage_separate_tiles_title),
                        stringResource(R.string.homepage_separate_tiles_summary), layout.separateTiles, true
                    ) { flip(PREF_HOMEPAGE_SEPARATE_TILES, layout.separateTiles) }
                    HomepageValueRow(
                        Icons.Filled.RestartAlt, stringResource(R.string.homepage_profile_reset_title),
                        stringResource(R.string.homepage_profile_reset_summary), true
                    ) { dialog = "reset" }
                }
            }
        }

        SettingsSectionLabel(stringResource(R.string.pref_category_homepage))
        SettingsSection(colors.cardBackground) {
            if (scopeId == null) {
                SettingsRow(
                    icon = Icons.Filled.Home,
                    title = stringResource(R.string.homepage_title),
                    summary = homepageSummaryText(homepage, customUrl),
                    colors = colors,
                    onClick = { homepageDialogOpen = true }
                )
            }
            HomepageValueRow(
                Icons.Filled.Gradient, stringResource(R.string.homepage_design_title),
                homepageDesignSummaryText(layout.design), optionsEnabled
            ) { designDialogOpen = true }
            if (layout.design == HOMEPAGE_DESIGN_SOLID) {
                HomepageValueRow(
                    Icons.Filled.Palette, stringResource(R.string.homepage_bg_color_title), "", optionsEnabled,
                    swatch = layout.solidColor(colors)
                ) { dialog = "bgColor" }
            }
            if (layout.design == HOMEPAGE_DESIGN_CUSTOM_GRADIENT) {
                HomepageValueRow(
                    Icons.Filled.ColorLens, stringResource(R.string.homepage_gradient_start_title), "", optionsEnabled,
                    swatch = layout.gradientStartColor(colors)
                ) { dialog = "gradStart" }
                HomepageValueRow(
                    Icons.Filled.ColorLens, stringResource(R.string.homepage_gradient_end_title), "", optionsEnabled,
                    swatch = layout.gradientEndColor(colors)
                ) { dialog = "gradEnd" }
                HomepageValueRow(
                    Icons.AutoMirrored.Filled.RotateRight, stringResource(R.string.homepage_gradient_angle_title),
                    stringResource(R.string.homepage_degree_value, layout.gradientAngle), optionsEnabled
                ) { dialog = "gradAngle" }
            }
            if (layout.design == HOMEPAGE_DESIGN_IMAGE) {
                HomepageValueRow(
                    Icons.Filled.Image, stringResource(R.string.homepage_image_title),
                    stringResource(R.string.homepage_image_summary), optionsEnabled
                ) { launchImagePicker(false) }
                HomepageSwitchRow(
                    Icons.Filled.DarkMode, stringResource(R.string.homepage_image_dark_title),
                    stringResource(R.string.homepage_image_dark_summary), layout.imageSeparateDark, optionsEnabled
                ) { flip(PREF_HOMEPAGE_IMAGE_SEPARATE_DARK, layout.imageSeparateDark) }
                if (layout.imageSeparateDark) {
                    HomepageValueRow(
                        Icons.Filled.Image, stringResource(R.string.homepage_image_dark_pick_title),
                        stringResource(R.string.homepage_image_summary), optionsEnabled
                    ) { launchImagePicker(true) }
                }
                HomepageSwitchRow(
                    Icons.Filled.BrightnessHigh, stringResource(R.string.homepage_image_full_brightness_title),
                    stringResource(R.string.homepage_image_full_brightness_summary), layout.fullBrightness, optionsEnabled
                ) { flip(PREF_HOMEPAGE_IMAGE_FULL_BRIGHTNESS, layout.fullBrightness) }
                if (!layout.fullBrightness) {
                    HomepageValueRow(
                        Icons.Filled.Brightness6, stringResource(R.string.homepage_image_dim_title),
                        stringResource(R.string.homepage_percent_value, layout.imageDim), optionsEnabled
                    ) { dialog = "dim" }
                }
                HomepageValueRow(
                    Icons.Filled.BlurOn, stringResource(R.string.homepage_image_blur_title),
                    stringResource(R.string.homepage_percent_value, layout.imageBlur), optionsEnabled
                ) { dialog = "blur" }
                HomepageSwitchRow(
                    Icons.Filled.Layers, stringResource(R.string.homepage_image_parallax_title),
                    stringResource(R.string.homepage_image_parallax_summary), layout.imageParallax, optionsEnabled
                ) { flip(PREF_HOMEPAGE_IMAGE_PARALLAX, layout.imageParallax) }
            }
        }

        SettingsSectionLabel(stringResource(R.string.homepage_section_content))
        SettingsSection(colors.cardBackground) {
            HomepageSwitchRow(
                Icons.Filled.Apps, stringResource(R.string.homepage_show_quick_actions_title),
                stringResource(R.string.homepage_show_quick_actions_summary), layout.showQuickActions, optionsEnabled
            ) { flip(PREF_HOMEPAGE_SHOW_QUICK_ACTIONS, layout.showQuickActions) }
            HomepageSwitchRow(
                Icons.Filled.Bookmark, stringResource(R.string.homepage_show_favorites_title),
                stringResource(R.string.homepage_show_favorites_summary), layout.showFavorites, optionsEnabled
            ) { flip(PREF_HOMEPAGE_SHOW_FAVORITES, layout.showFavorites) }
            HomepageSwitchRow(
                Icons.Filled.History, stringResource(R.string.homepage_show_recent_title),
                stringResource(R.string.homepage_show_recent_summary), layout.showRecent, optionsEnabled
            ) { flip(PREF_HOMEPAGE_SHOW_RECENT, layout.showRecent) }
            HomepageSwitchRow(
                Icons.Filled.VerticalAlignCenter, stringResource(R.string.homepage_center_content_title),
                stringResource(R.string.homepage_center_content_summary), layout.centerContent, optionsEnabled
            ) { flip(PREF_HOMEPAGE_CENTER_CONTENT, layout.centerContent) }
            HomepageValueRow(
                Icons.Filled.ViewAgenda, stringResource(R.string.homepage_section_order_title), orderSummary, optionsEnabled
            ) { dialog = "order" }
            HomepageValueRow(
                Icons.Filled.Apps, stringResource(R.string.homepage_quick_actions_choose), actionsSummary, optionsEnabled
            ) { dialog = "actions" }
            HomepageValueRow(
                Icons.Filled.Widgets, stringResource(R.string.homepage_block_widgets), widgetsSummary, optionsEnabled
            ) { dialog = "widgets" }
            HomepageValueRow(
                Icons.Filled.FormatListNumbered, stringResource(R.string.homepage_recent_count_title),
                layout.recentCount.toString(), optionsEnabled
            ) { dialog = "recent" }
            HomepageValueRow(
                Icons.Filled.GridView, stringResource(R.string.homepage_columns_title),
                optionLabel(columnOptions, layout.columns.toString()), optionsEnabled
            ) { dialog = "columns" }
        }

        SettingsSectionLabel(stringResource(R.string.homepage_section_header))
        SettingsSection(colors.cardBackground) {
            HomepageValueRow(
                Icons.Filled.Title, stringResource(R.string.homepage_hero_style_title),
                optionLabel(heroOptions, layout.heroStyle), optionsEnabled
            ) { dialog = "hero" }
            HomepageValueRow(
                Icons.Filled.Edit, stringResource(R.string.homepage_title_text_title),
                layout.title.ifBlank { defaultTitle }, optionsEnabled
            ) { dialog = "title" }
            HomepageSwitchRow(
                Icons.Filled.WbSunny, stringResource(R.string.homepage_greeting_title),
                stringResource(R.string.homepage_greeting_summary), layout.showGreeting, optionsEnabled
            ) { flip(PREF_HOMEPAGE_SHOW_GREETING, layout.showGreeting) }
        }

        SettingsSectionLabel(stringResource(R.string.homepage_section_shortcuts))
        SettingsSection(colors.cardBackground) {
            HomepageSwitchRow(
                Icons.Filled.Mic, stringResource(R.string.homepage_voice_title),
                stringResource(R.string.homepage_voice_summary), layout.showVoice, optionsEnabled
            ) { flip(PREF_HOMEPAGE_SHOW_VOICE, layout.showVoice) }
            HomepageValueRow(
                Icons.Filled.Opacity, stringResource(R.string.homepage_card_opacity_title),
                stringResource(R.string.homepage_percent_value, layout.cardOpacity), optionsEnabled
            ) { dialog = "opacity" }
            HomepageValueRow(
                Icons.Filled.RoundedCorner, stringResource(R.string.homepage_card_corner_title),
                stringResource(R.string.homepage_dp_value, layout.cardCorner), optionsEnabled
            ) { dialog = "corner" }
        }

        SettingsSectionLabel(stringResource(R.string.homepage_section_tiles))
        SettingsSection(colors.cardBackground) {
            HomepageValueRow(
                Icons.Filled.Category, stringResource(R.string.homepage_tile_shape_title),
                optionLabel(tileShapeOptions, layout.tileShape), optionsEnabled
            ) { dialog = "tileShape" }
            HomepageValueRow(
                Icons.Filled.ZoomIn, stringResource(R.string.homepage_tile_size_title),
                stringResource(R.string.homepage_percent_value, layout.tileScale), optionsEnabled
            ) { dialog = "tileScale" }
            HomepageSwitchRow(
                Icons.Filled.TextFields, stringResource(R.string.homepage_tile_labels_title),
                stringResource(R.string.homepage_tile_labels_summary), layout.showTileLabels, optionsEnabled
            ) { flip(PREF_HOMEPAGE_TILE_LABELS, layout.showTileLabels) }
            HomepageValueRow(
                Icons.Filled.Folder, stringResource(R.string.homepage_folder_style_title),
                optionLabel(folderOptions, layout.folderStyle), optionsEnabled
            ) { dialog = "folder" }
        }
    }
}

@Composable
private fun homepageSummaryText(homepage: String, customUrl: String): String = when (homepage) {
    HOMEPAGE_SEARCH_ENGINE -> stringResource(R.string.homepage_search_engine)
    HOMEPAGE_CUSTOM -> customUrl.ifBlank { stringResource(R.string.homepage_custom) }
    else -> stringResource(R.string.homepage_clint)
}

@Composable
private fun homepageDesignSummaryText(design: String): String = when (design) {
    HOMEPAGE_DESIGN_GRADIENT -> stringResource(R.string.homepage_design_gradient)
    HOMEPAGE_DESIGN_SOLID -> stringResource(R.string.homepage_design_solid)
    HOMEPAGE_DESIGN_CUSTOM_GRADIENT -> stringResource(R.string.homepage_design_custom_gradient)
    HOMEPAGE_DESIGN_IMAGE -> stringResource(R.string.homepage_design_image)
    else -> stringResource(R.string.homepage_design_plain)
}
