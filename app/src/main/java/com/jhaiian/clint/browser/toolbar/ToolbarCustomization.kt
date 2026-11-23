package com.jhaiian.clint.browser.toolbar

import android.content.SharedPreferences
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.home.HOMEPAGE_SHAPE_PILL
import com.jhaiian.clint.browser.home.HOMEPAGE_SHAPE_ROUNDED
import com.jhaiian.clint.browser.home.HOMEPAGE_SHAPE_SQUARE
import com.jhaiian.clint.browser.home.PREF_HOMEPAGE_SEARCH_SHAPE

const val PREF_SEARCH_BAR_SHAPE = "search_bar_shape"
const val PREF_PROGRESS_STYLE = "progress_bar_style"
const val PROGRESS_CLASSIC = "classic"
const val PROGRESS_SQUIGGLE = "squiggle"

private val SEARCH_SHAPES = setOf(HOMEPAGE_SHAPE_PILL, HOMEPAGE_SHAPE_ROUNDED, HOMEPAGE_SHAPE_SQUARE)

fun resolveSearchBarShape(prefs: SharedPreferences): String =
    listOf(PREF_SEARCH_BAR_SHAPE, PREF_HOMEPAGE_SEARCH_SHAPE)
        .firstNotNullOfOrNull { key -> prefs.getString(key, null)?.takeIf { it in SEARCH_SHAPES } }
        ?: HOMEPAGE_SHAPE_PILL

fun searchBarShape(shape: String): Shape = when (shape) {
    HOMEPAGE_SHAPE_ROUNDED -> RoundedCornerShape(16.dp)
    HOMEPAGE_SHAPE_SQUARE -> RoundedCornerShape(6.dp)
    else -> RoundedCornerShape(50)
}

enum class ToolbarButton(val id: String, val canHide: Boolean) {
    EXTENSIONS("extensions", true),
    MEDIA_CAPTURE("media_capture", true),
    PROFILE("profile", true),
    TAB_COUNT("tab_count", true),
    MENU("menu", false);

    companion object {
        fun fromId(id: String): ToolbarButton? = entries.find { it.id == id }
    }
}

fun ToolbarButton.icon(): ImageVector = when (this) {
    ToolbarButton.EXTENSIONS -> Icons.Filled.Extension
    ToolbarButton.MEDIA_CAPTURE -> Icons.Filled.VideoLibrary
    ToolbarButton.PROFILE -> Icons.Filled.AccountCircle
    ToolbarButton.TAB_COUNT -> Icons.Filled.Tab
    ToolbarButton.MENU -> Icons.Filled.MoreVert
}

fun ToolbarButton.titleRes(): Int = when (this) {
    ToolbarButton.EXTENSIONS -> R.string.ext_title
    ToolbarButton.MEDIA_CAPTURE -> R.string.menu_media_capture
    ToolbarButton.PROFILE -> R.string.profiles_toolbar_button
    ToolbarButton.TAB_COUNT -> R.string.toolbar_customization_tab_counter
    ToolbarButton.MENU -> R.string.menu
}

enum class BottomBarAction(val id: String) {
    BACK("back"),
    FORWARD("forward"),
    HOME("home"),
    RELOAD("reload"),
    BOOKMARK("bookmark"),
    SHARE("share"),
    NEW_TAB("new_tab"),
    TABS("tabs"),
    DOWNLOADS("downloads"),
    HISTORY("history"),
    FIND_IN_PAGE("find_in_page"),
    MENU("menu");

    val canLongPress: Boolean get() = this != MENU

    companion object {
        val DEFAULT: List<BottomBarAction> = listOf(BACK, FORWARD, HOME, RELOAD, BOOKMARK)
        fun fromId(id: String): BottomBarAction? = entries.find { it.id == id }
    }
}

fun BottomBarAction.icon(): ImageVector = when (this) {
    BottomBarAction.BACK -> Icons.AutoMirrored.Filled.ArrowBack
    BottomBarAction.FORWARD -> Icons.AutoMirrored.Filled.ArrowForward
    BottomBarAction.HOME -> Icons.Filled.Home
    BottomBarAction.RELOAD -> Icons.Filled.Refresh
    BottomBarAction.BOOKMARK -> Icons.Filled.BookmarkBorder
    BottomBarAction.SHARE -> Icons.Filled.Share
    BottomBarAction.NEW_TAB -> Icons.Filled.Add
    BottomBarAction.TABS -> Icons.Filled.Tab
    BottomBarAction.DOWNLOADS -> Icons.Filled.Download
    BottomBarAction.HISTORY -> Icons.Filled.History
    BottomBarAction.FIND_IN_PAGE -> Icons.Filled.FindInPage
    BottomBarAction.MENU -> Icons.Filled.MoreVert
}

fun BottomBarAction.titleRes(): Int = when (this) {
    BottomBarAction.BACK -> R.string.back
    BottomBarAction.FORWARD -> R.string.forward
    BottomBarAction.HOME -> R.string.home
    BottomBarAction.RELOAD -> R.string.refresh
    BottomBarAction.BOOKMARK -> R.string.content_desc_bookmark
    BottomBarAction.SHARE -> R.string.share_url
    BottomBarAction.NEW_TAB -> R.string.new_tab
    BottomBarAction.TABS -> R.string.tabs
    BottomBarAction.DOWNLOADS -> R.string.menu_downloads
    BottomBarAction.HISTORY -> R.string.menu_history
    BottomBarAction.FIND_IN_PAGE -> R.string.menu_find_in_page
    BottomBarAction.MENU -> R.string.menu
}

data class ToolbarLayout(
    val order: List<ToolbarButton>,
    val hidden: Set<ToolbarButton>,
    val bottomActions: List<BottomBarAction>,
    val longPress: Map<BottomBarAction, BottomBarAction> = emptyMap(),
    val compact: Boolean = false,
    val showLabels: Boolean = false,
    val showInBottomPosition: Boolean = false,
    val searchShape: String = HOMEPAGE_SHAPE_PILL,
    val progressStyle: String = PROGRESS_CLASSIC
) {
    val visibleButtons: List<ToolbarButton>
        get() = order.filter { !it.canHide || it !in hidden }

    companion object {
        val DEFAULT = ToolbarLayout(ToolbarButton.entries.toList(), setOf(ToolbarButton.PROFILE), BottomBarAction.DEFAULT)
    }
}

object ToolbarCustomizationStore {
    private const val PREF_ORDER = "toolbar_button_order"
    private const val PREF_HIDDEN = "toolbar_hidden_buttons"
    private const val PREF_BOTTOM = "bottom_bar_actions"
    private const val PREF_LONG_PRESS = "bottom_bar_long_press"
    private const val PREF_COMPACT = "toolbar_compact"
    private const val PREF_LABELS = "bottom_bar_labels"
    private const val PREF_BOTTOM_POSITION = "bottom_bar_in_bottom_position"

    const val MIN_BOTTOM_ACTIONS = 3
    const val MAX_BOTTOM_ACTIONS = 6

    fun isLayoutKey(key: String?): Boolean = key == PREF_ORDER || key == PREF_HIDDEN || key == PREF_BOTTOM ||
        key == PREF_LONG_PRESS || key == PREF_COMPACT || key == PREF_LABELS || key == PREF_BOTTOM_POSITION ||
        key == PREF_SEARCH_BAR_SHAPE || key == PREF_PROGRESS_STYLE || key == PREF_HOMEPAGE_SEARCH_SHAPE

    private fun ids(prefs: SharedPreferences, key: String): List<String> =
        prefs.getString(key, null)?.split(",")?.filter { it.isNotBlank() }.orEmpty()

    fun read(prefs: SharedPreferences): ToolbarLayout {
        val stored = ids(prefs, PREF_ORDER).mapNotNull { ToolbarButton.fromId(it) }.distinct()
        val order = stored.toMutableList().also { list ->
            ToolbarButton.entries.filter { it !in stored }.forEach { missing ->
                val anchor = list.indexOf(ToolbarButton.TAB_COUNT)
                if (missing == ToolbarButton.PROFILE && anchor >= 0) list.add(anchor, missing) else list.add(missing)
            }
        }
        val hidden = ids(prefs, PREF_HIDDEN).mapNotNull { ToolbarButton.fromId(it) }.filter { it.canHide }.toSet()
            .let { if (ToolbarButton.PROFILE !in stored) it + ToolbarButton.PROFILE else it }
        val bottom = ids(prefs, PREF_BOTTOM).mapNotNull { BottomBarAction.fromId(it) }.distinct()
        val longPress = ids(prefs, PREF_LONG_PRESS).mapNotNull { pair ->
            val parts = pair.split(":")
            if (parts.size != 2) return@mapNotNull null
            val source = BottomBarAction.fromId(parts[0]) ?: return@mapNotNull null
            val target = BottomBarAction.fromId(parts[1]) ?: return@mapNotNull null
            if (target == source || !target.canLongPress) null else source to target
        }.toMap()
        return ToolbarLayout(
            order,
            hidden,
            if (bottom.size in MIN_BOTTOM_ACTIONS..MAX_BOTTOM_ACTIONS) bottom else BottomBarAction.DEFAULT,
            longPress,
            prefs.getBoolean(PREF_COMPACT, false),
            prefs.getBoolean(PREF_LABELS, false),
            prefs.getBoolean(PREF_BOTTOM_POSITION, false),
            resolveSearchBarShape(prefs),
            prefs.getString(PREF_PROGRESS_STYLE, null)?.takeIf { it == PROGRESS_SQUIGGLE } ?: PROGRESS_CLASSIC
        )
    }

    fun write(prefs: SharedPreferences, layout: ToolbarLayout) {
        prefs.edit()
            .putString(PREF_ORDER, layout.order.joinToString(",") { it.id })
            .putString(PREF_HIDDEN, layout.hidden.joinToString(",") { it.id })
            .putString(PREF_BOTTOM, layout.bottomActions.joinToString(",") { it.id })
            .putString(PREF_LONG_PRESS, layout.longPress.entries.joinToString(",") { "${it.key.id}:${it.value.id}" })
            .putBoolean(PREF_COMPACT, layout.compact)
            .putBoolean(PREF_LABELS, layout.showLabels)
            .putBoolean(PREF_BOTTOM_POSITION, layout.showInBottomPosition)
            .putString(PREF_SEARCH_BAR_SHAPE, layout.searchShape)
            .putString(PREF_PROGRESS_STYLE, layout.progressStyle)
            .apply()
    }

    fun reset(prefs: SharedPreferences) {
        prefs.edit()
            .remove(PREF_ORDER).remove(PREF_HIDDEN).remove(PREF_BOTTOM).remove(PREF_LONG_PRESS)
            .remove(PREF_COMPACT).remove(PREF_LABELS).remove(PREF_BOTTOM_POSITION)
            .remove(PREF_SEARCH_BAR_SHAPE).remove(PREF_PROGRESS_STYLE).remove(PREF_HOMEPAGE_SEARCH_SHAPE)
            .apply()
    }
}
