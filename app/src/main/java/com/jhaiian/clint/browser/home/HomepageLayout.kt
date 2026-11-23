package com.jhaiian.clint.browser.home

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jhaiian.clint.R
import com.jhaiian.clint.profiles.WebProfiles
import com.jhaiian.clint.ui.theme.ClintColors

const val PREF_HOMEPAGE_HERO_STYLE = "homepage_hero_style"
const val PREF_HOMEPAGE_TITLE_TEXT = "homepage_title_text"
const val PREF_HOMEPAGE_SHOW_GREETING = "homepage_show_greeting"
const val PREF_HOMEPAGE_SEARCH_SHAPE = "homepage_search_shape"
const val PREF_HOMEPAGE_SHOW_VOICE = "homepage_show_voice"
const val PREF_HOMEPAGE_COLUMNS = "homepage_columns"
const val PREF_HOMEPAGE_RECENT_COUNT = "homepage_recent_count"
const val PREF_HOMEPAGE_CARD_OPACITY = "homepage_card_opacity"
const val PREF_HOMEPAGE_CARD_CORNER = "homepage_card_corner"
const val PREF_HOMEPAGE_RECENT_FIRST = "homepage_recent_first"
const val PREF_HOMEPAGE_QUICK_ACTIONS = "homepage_quick_actions"
const val PREF_HOMEPAGE_SECTION_ORDER = "homepage_section_order"
const val PREF_HOMEPAGE_WIDGETS = "homepage_widgets"
const val PREF_HOMEPAGE_TILE_SHAPE = "homepage_tile_shape"
const val PREF_HOMEPAGE_TILE_SCALE = "homepage_tile_scale"
const val PREF_HOMEPAGE_TILE_LABELS = "homepage_tile_labels"
const val PREF_HOMEPAGE_FOLDER_STYLE = "homepage_folder_style"
const val PREF_HOMEPAGE_BG_COLOR = "homepage_bg_color"
const val PREF_HOMEPAGE_GRADIENT_START = "homepage_gradient_start"
const val PREF_HOMEPAGE_GRADIENT_END = "homepage_gradient_end"
const val PREF_HOMEPAGE_GRADIENT_ANGLE = "homepage_gradient_angle"
const val PREF_HOMEPAGE_IMAGE_DIM = "homepage_image_dim"
const val PREF_HOMEPAGE_IMAGE_BLUR = "homepage_image_blur"
const val PREF_HOMEPAGE_IMAGE_PARALLAX = "homepage_image_parallax"
const val PREF_HOMEPAGE_IMAGE_SEPARATE_DARK = "homepage_image_separate_dark"
const val PREF_HOMEPAGE_SEPARATE_TILES = "homepage_separate_tiles"

const val HOMEPAGE_HERO_FULL = "full"
const val HOMEPAGE_HERO_LOGO = "logo"
const val HOMEPAGE_HERO_TEXT = "text"
const val HOMEPAGE_HERO_NONE = "none"

const val HOMEPAGE_SHAPE_PILL = "pill"
const val HOMEPAGE_SHAPE_ROUNDED = "rounded"
const val HOMEPAGE_SHAPE_SQUARE = "square"

const val HOMEPAGE_TILE_SQUIRCLE = "squircle"
const val HOMEPAGE_TILE_CIRCLE = "circle"
const val HOMEPAGE_TILE_SQUARE = "square"

const val HOMEPAGE_FOLDER_GRID = "grid"
const val HOMEPAGE_FOLDER_PLAIN = "plain"

const val HOMEPAGE_ACTION_BOOKMARKS = "bookmarks"
const val HOMEPAGE_ACTION_HISTORY = "history"
const val HOMEPAGE_ACTION_DOWNLOADS = "downloads"
const val HOMEPAGE_ACTION_MODE = "mode"
const val HOMEPAGE_ACTION_SETTINGS = "settings"
const val HOMEPAGE_ACTION_EXTENSIONS = "extensions"
const val HOMEPAGE_ACTION_PROFILES = "profiles"

const val HOMEPAGE_BLOCK_SEARCH = "search"
const val HOMEPAGE_BLOCK_ACTIONS = "actions"
const val HOMEPAGE_BLOCK_FAVORITES = "favorites"
const val HOMEPAGE_BLOCK_RECENT = "recent"
const val HOMEPAGE_BLOCK_WIDGETS = "widgets"

const val HOMEPAGE_WIDGET_CLOCK = "clock"
const val HOMEPAGE_WIDGET_CONTINUE = "continue"
const val HOMEPAGE_WIDGET_DOWNLOADS = "downloads"

val HOMEPAGE_ALL_ACTIONS = listOf(
    HOMEPAGE_ACTION_BOOKMARKS,
    HOMEPAGE_ACTION_HISTORY,
    HOMEPAGE_ACTION_DOWNLOADS,
    HOMEPAGE_ACTION_MODE,
    HOMEPAGE_ACTION_SETTINGS,
    HOMEPAGE_ACTION_EXTENSIONS,
    HOMEPAGE_ACTION_PROFILES
)

val HOMEPAGE_DEFAULT_ACTIONS = listOf(
    HOMEPAGE_ACTION_BOOKMARKS,
    HOMEPAGE_ACTION_HISTORY,
    HOMEPAGE_ACTION_DOWNLOADS,
    HOMEPAGE_ACTION_MODE
)

val HOMEPAGE_ALL_BLOCKS = listOf(
    HOMEPAGE_BLOCK_SEARCH,
    HOMEPAGE_BLOCK_ACTIONS,
    HOMEPAGE_BLOCK_FAVORITES,
    HOMEPAGE_BLOCK_RECENT,
    HOMEPAGE_BLOCK_WIDGETS
)

val HOMEPAGE_ALL_WIDGETS = listOf(
    HOMEPAGE_WIDGET_CLOCK,
    HOMEPAGE_WIDGET_CONTINUE,
    HOMEPAGE_WIDGET_DOWNLOADS
)

val HOMEPAGE_COLUMN_CHOICES = listOf(0, 3, 4, 5, 6)

internal data class HomeTileStyle(
    val shape: String = HOMEPAGE_TILE_SQUIRCLE,
    val scale: Float = 1f,
    val showLabels: Boolean = true,
    val folderPlain: Boolean = false
)

internal val LocalHomeTileStyle = compositionLocalOf { HomeTileStyle() }
internal val LocalHomeCardCorner = compositionLocalOf { 20.dp }

internal fun homeTileCorner(shape: String, size: Dp): Dp = when (shape) {
    HOMEPAGE_TILE_CIRCLE -> size * 0.5f
    HOMEPAGE_TILE_SQUARE -> size * 0.1f
    else -> size * 0.32f
}

data class HomepageLayout(
    val design: String = HOMEPAGE_DESIGN_PLAIN,
    val imageVersion: Long = 0L,
    val showFavorites: Boolean = true,
    val showQuickActions: Boolean = true,
    val showRecent: Boolean = true,
    val centerContent: Boolean = true,
    val fullBrightness: Boolean = false,
    val heroStyle: String = HOMEPAGE_HERO_FULL,
    val title: String = "",
    val showGreeting: Boolean = true,
    val searchShape: String = HOMEPAGE_SHAPE_PILL,
    val showVoice: Boolean = true,
    val columns: Int = 0,
    val recentCount: Int = 5,
    val cardOpacity: Int = 100,
    val cardCorner: Int = 20,
    val sectionOrder: List<String> = HOMEPAGE_ALL_BLOCKS,
    val quickActions: List<String> = HOMEPAGE_DEFAULT_ACTIONS,
    val widgets: List<String> = emptyList(),
    val tileShape: String = HOMEPAGE_TILE_SQUIRCLE,
    val tileScale: Int = 100,
    val showTileLabels: Boolean = true,
    val folderStyle: String = HOMEPAGE_FOLDER_GRID,
    val bgColor: Int = 0,
    val gradientStart: Int = 0,
    val gradientEnd: Int = 0,
    val gradientAngle: Int = 0,
    val imageDim: Int = 100,
    val imageBlur: Int = 0,
    val imageParallax: Boolean = false,
    val imageSeparateDark: Boolean = false,
    val separateTiles: Boolean = false
)

internal fun HomepageLayout.solidColor(colors: ClintColors): Color =
    if (bgColor != 0) Color(bgColor) else colors.background

internal fun HomepageLayout.gradientStartColor(colors: ClintColors): Color =
    if (gradientStart != 0) Color(gradientStart) else colors.primary.copy(alpha = 0.35f)

internal fun HomepageLayout.gradientEndColor(colors: ClintColors): Color =
    if (gradientEnd != 0) Color(gradientEnd) else colors.background

fun isDefaultHomepageProfile(profileId: String?): Boolean =
    profileId.isNullOrEmpty() || profileId == WebProfiles.DEFAULT_ID

fun homepageKey(base: String, profileId: String?): String =
    if (isDefaultHomepageProfile(profileId)) base else "$base@$profileId"

fun homepageResolvedKey(prefs: SharedPreferences, base: String, profileId: String?): String {
    val scoped = homepageKey(base, profileId)
    return if (scoped != base && prefs.contains(scoped)) scoped else base
}

fun homepageIsOverridden(prefs: SharedPreferences, base: String, profileId: String?): Boolean =
    isDefaultHomepageProfile(profileId) || prefs.contains(homepageKey(base, profileId))

private class HomeScope(val prefs: SharedPreferences, val profileId: String?) {
    fun string(base: String, default: String): String =
        prefs.getString(homepageResolvedKey(prefs, base, profileId), default) ?: default

    fun bool(base: String, default: Boolean): Boolean =
        prefs.getBoolean(homepageResolvedKey(prefs, base, profileId), default)

    fun int(base: String, default: Int): Int =
        prefs.getInt(homepageResolvedKey(prefs, base, profileId), default)

    fun long(base: String, default: Long): Long =
        prefs.getLong(homepageResolvedKey(prefs, base, profileId), default)

    fun raw(base: String): String? =
        prefs.getString(homepageResolvedKey(prefs, base, profileId), null)
}

private fun parseKeys(raw: String?, all: List<String>, appendMissing: Boolean, default: List<String>): List<String> {
    if (raw == null) return default
    val picked = raw.split(',').filter { it in all }.distinct()
    return if (appendMissing) picked + all.filter { it !in picked } else picked
}

fun readHomepageLayout(context: Context, prefs: SharedPreferences, profileId: String? = null): HomepageLayout {
    val scope = HomeScope(prefs, profileId)
    val legacyRecentFirst = scope.bool(PREF_HOMEPAGE_RECENT_FIRST, false)
    val defaultOrder = if (legacyRecentFirst) {
        listOf(HOMEPAGE_BLOCK_SEARCH, HOMEPAGE_BLOCK_ACTIONS, HOMEPAGE_BLOCK_RECENT, HOMEPAGE_BLOCK_FAVORITES, HOMEPAGE_BLOCK_WIDGETS)
    } else HOMEPAGE_ALL_BLOCKS
    val legacyFullBrightness = scope.bool(PREF_HOMEPAGE_IMAGE_FULL_BRIGHTNESS, false)
    return HomepageLayout(
        design = readHomepageDesign(context, prefs, profileId),
        imageVersion = scope.long(PREF_HOMEPAGE_IMAGE_VERSION, 0L),
        showFavorites = scope.bool(PREF_HOMEPAGE_SHOW_FAVORITES, true),
        showQuickActions = scope.bool(PREF_HOMEPAGE_SHOW_QUICK_ACTIONS, true),
        showRecent = scope.bool(PREF_HOMEPAGE_SHOW_RECENT, true),
        centerContent = scope.bool(PREF_HOMEPAGE_CENTER_CONTENT, true),
        fullBrightness = legacyFullBrightness,
        heroStyle = scope.string(PREF_HOMEPAGE_HERO_STYLE, HOMEPAGE_HERO_FULL),
        title = scope.string(PREF_HOMEPAGE_TITLE_TEXT, ""),
        showGreeting = scope.bool(PREF_HOMEPAGE_SHOW_GREETING, true),
        searchShape = com.jhaiian.clint.browser.toolbar.resolveSearchBarShape(prefs),
        showVoice = scope.bool(PREF_HOMEPAGE_SHOW_VOICE, true),
        columns = scope.int(PREF_HOMEPAGE_COLUMNS, 0).coerceIn(0, 6),
        recentCount = scope.int(PREF_HOMEPAGE_RECENT_COUNT, 5).coerceIn(1, 10),
        cardOpacity = scope.int(PREF_HOMEPAGE_CARD_OPACITY, 100).coerceIn(30, 100),
        cardCorner = scope.int(PREF_HOMEPAGE_CARD_CORNER, 20).coerceIn(4, 32),
        sectionOrder = parseKeys(scope.raw(PREF_HOMEPAGE_SECTION_ORDER), HOMEPAGE_ALL_BLOCKS, true, defaultOrder),
        quickActions = parseKeys(scope.raw(PREF_HOMEPAGE_QUICK_ACTIONS), HOMEPAGE_ALL_ACTIONS, false, HOMEPAGE_DEFAULT_ACTIONS),
        widgets = parseKeys(scope.raw(PREF_HOMEPAGE_WIDGETS), HOMEPAGE_ALL_WIDGETS, false, emptyList()),
        tileShape = scope.string(PREF_HOMEPAGE_TILE_SHAPE, HOMEPAGE_TILE_SQUIRCLE),
        tileScale = scope.int(PREF_HOMEPAGE_TILE_SCALE, 100).coerceIn(70, 140),
        showTileLabels = scope.bool(PREF_HOMEPAGE_TILE_LABELS, true),
        folderStyle = scope.string(PREF_HOMEPAGE_FOLDER_STYLE, HOMEPAGE_FOLDER_GRID),
        bgColor = scope.int(PREF_HOMEPAGE_BG_COLOR, 0),
        gradientStart = scope.int(PREF_HOMEPAGE_GRADIENT_START, 0),
        gradientEnd = scope.int(PREF_HOMEPAGE_GRADIENT_END, 0),
        gradientAngle = scope.int(PREF_HOMEPAGE_GRADIENT_ANGLE, 0).coerceIn(0, 359),
        imageDim = scope.int(PREF_HOMEPAGE_IMAGE_DIM, 100).coerceIn(0, 100),
        imageBlur = scope.int(PREF_HOMEPAGE_IMAGE_BLUR, 0).coerceIn(0, 100),
        imageParallax = scope.bool(PREF_HOMEPAGE_IMAGE_PARALLAX, false),
        imageSeparateDark = scope.bool(PREF_HOMEPAGE_IMAGE_SEPARATE_DARK, false),
        separateTiles = !isDefaultHomepageProfile(profileId) && prefs.getBoolean(homepageKey(PREF_HOMEPAGE_SEPARATE_TILES, profileId), false)
    )
}

internal fun resetHomepageProfile(context: Context, prefs: SharedPreferences, profileId: String) {
    val suffix = "@$profileId"
    val editor = prefs.edit()
    prefs.all.keys.filter { it.startsWith("homepage_") && it.endsWith(suffix) }.forEach { editor.remove(it) }
    editor.apply()
    homepageImageDir(context).listFiles()?.forEach { if (it.name.contains(suffix)) it.delete() }
    HomeTileStore.forget(profileId)
}

fun homepageActionIcon(key: String): ImageVector = when (key) {
    HOMEPAGE_ACTION_BOOKMARKS -> Icons.Filled.Bookmark
    HOMEPAGE_ACTION_HISTORY -> Icons.Filled.History
    HOMEPAGE_ACTION_DOWNLOADS -> Icons.Filled.Download
    HOMEPAGE_ACTION_SETTINGS -> Icons.Filled.Settings
    HOMEPAGE_ACTION_EXTENSIONS -> Icons.Filled.Extension
    HOMEPAGE_ACTION_PROFILES -> Icons.Filled.AccountCircle
    else -> Icons.Filled.VisibilityOff
}

fun homepageActionLabel(key: String): Int = when (key) {
    HOMEPAGE_ACTION_BOOKMARKS -> R.string.menu_bookmarks
    HOMEPAGE_ACTION_HISTORY -> R.string.menu_history
    HOMEPAGE_ACTION_DOWNLOADS -> R.string.menu_downloads
    HOMEPAGE_ACTION_SETTINGS -> R.string.settings
    HOMEPAGE_ACTION_EXTENSIONS -> R.string.ext_title
    HOMEPAGE_ACTION_PROFILES -> R.string.profiles_title
    else -> R.string.homepage_action_mode
}

fun homepageBlockLabel(key: String): Int = when (key) {
    HOMEPAGE_BLOCK_SEARCH -> R.string.homepage_block_search
    HOMEPAGE_BLOCK_ACTIONS -> R.string.homepage_block_actions
    HOMEPAGE_BLOCK_FAVORITES -> R.string.home_favorites
    HOMEPAGE_BLOCK_RECENT -> R.string.home_recent
    else -> R.string.homepage_block_widgets
}

fun homepageWidgetLabel(key: String): Int = when (key) {
    HOMEPAGE_WIDGET_CLOCK -> R.string.homepage_widget_clock
    HOMEPAGE_WIDGET_CONTINUE -> R.string.homepage_widget_continue
    else -> R.string.homepage_widget_downloads
}
