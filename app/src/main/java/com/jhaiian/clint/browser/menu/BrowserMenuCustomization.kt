package com.jhaiian.clint.browser.menu

import android.content.SharedPreferences
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ChromeReaderMode
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.automirrored.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.DataSaverOff
import androidx.compose.material.icons.filled.DataSaverOn
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.graphics.vector.ImageVector
import com.jhaiian.clint.R

enum class CustomizableMenuItem(val id: String, val defaultSection: MenuSection) {
    NEW_TAB("new_tab", MenuSection.GRID),
    NEW_INCOGNITO_TAB("new_incognito_tab", MenuSection.GRID),
    NEW_PROFILE_TAB("new_profile_tab", MenuSection.GRID),
    SHARE("share", MenuSection.HIDDEN),
    OPEN_IN_APP("open_in_app", MenuSection.GRID),
    CREATE_SHORTCUT("create_shortcut", MenuSection.GRID),
    CREATE_FAVORITE("create_favorite", MenuSection.LIST),
    DOWNLOADS("downloads", MenuSection.GRID),
    MEDIA_CAPTURE("media_capture", MenuSection.TOGGLES),
    EXTENSIONS("extensions", MenuSection.LIST),
    WEBSITE_BLOCKER("website_blocker", MenuSection.TOGGLES),
    BOOKMARKS("bookmarks", MenuSection.GRID),
    HISTORY("history", MenuSection.GRID),
    READER_MODE("reader_mode", MenuSection.LIST),
    FIND_IN_PAGE("find_in_page", MenuSection.LIST),
    TRANSLATE("translate", MenuSection.TOGGLES),
    DESKTOP_MODE("desktop_mode", MenuSection.TOGGLES),
    DATA_SAVER("data_saver", MenuSection.TOGGLES),
    DISABLE_DATA_SAVER_FOR_SITE("disable_data_saver_for_site", MenuSection.LIST),
    MUTE_TAB("mute_tab", MenuSection.LIST),
    COOKIES_CONTROL("cookies_control", MenuSection.LIST);

    companion object {
        val DEFAULT_ORDER: List<CustomizableMenuItem> = entries.toList()
        fun fromId(id: String): CustomizableMenuItem? = entries.find { it.id == id }
    }
}

enum class MenuSection(val id: String) {
    GRID("grid"),
    TOGGLES("toggles"),
    LIST("list"),
    HIDDEN("hidden");

    companion object {
        fun fromId(id: String): MenuSection? = entries.find { it.id == id }
    }
}

fun MenuSection.titleRes(): Int = when (this) {
    MenuSection.GRID -> R.string.menu_section_grid
    MenuSection.TOGGLES -> R.string.menu_section_toggles
    MenuSection.LIST -> R.string.menu_section_list
    MenuSection.HIDDEN -> R.string.menu_section_hidden
}

fun MenuSection.descriptionRes(): Int = when (this) {
    MenuSection.GRID -> R.string.menu_section_grid_desc
    MenuSection.TOGGLES -> R.string.menu_section_toggles_desc
    MenuSection.LIST -> R.string.menu_section_list_desc
    MenuSection.HIDDEN -> R.string.menu_section_hidden_desc
}

enum class MenuNavSlot(val id: String) {
    BACK("back"),
    FORWARD("forward"),
    HOME("home"),
    RELOAD("reload"),
    BOOKMARK("bookmark"),
    SHARE("share"),
    NEW_TAB("new_tab");

    companion object {
        val DEFAULT: List<MenuNavSlot> = listOf(BACK, FORWARD, RELOAD, BOOKMARK, SHARE)
        fun fromId(id: String): MenuNavSlot? = entries.find { it.id == id }
    }
}

fun MenuNavSlot.icon(): ImageVector = when (this) {
    MenuNavSlot.BACK -> Icons.AutoMirrored.Filled.ArrowBack
    MenuNavSlot.FORWARD -> Icons.AutoMirrored.Filled.ArrowForward
    MenuNavSlot.HOME -> Icons.Filled.Home
    MenuNavSlot.RELOAD -> Icons.Filled.Refresh
    MenuNavSlot.BOOKMARK -> Icons.Filled.BookmarkBorder
    MenuNavSlot.SHARE -> Icons.Filled.Share
    MenuNavSlot.NEW_TAB -> Icons.Filled.Add
}

fun MenuNavSlot.titleRes(): Int = when (this) {
    MenuNavSlot.BACK -> R.string.back
    MenuNavSlot.FORWARD -> R.string.forward
    MenuNavSlot.HOME -> R.string.home
    MenuNavSlot.RELOAD -> R.string.refresh
    MenuNavSlot.BOOKMARK -> R.string.content_desc_bookmark
    MenuNavSlot.SHARE -> R.string.share_url
    MenuNavSlot.NEW_TAB -> R.string.new_tab
}

data class BrowserMenuLayout(
    val order: List<CustomizableMenuItem>,
    val sections: Map<CustomizableMenuItem, MenuSection>,
    val gridColumns: Int,
    val compact: Boolean,
    val showHeader: Boolean,
    val gridLabels: Boolean,
    val navSlots: List<MenuNavSlot>,
    val showNavBar: Boolean = true
) {
    fun itemsIn(section: MenuSection): List<CustomizableMenuItem> =
        order.filter { (sections[it] ?: it.defaultSection) == section }
}

fun CustomizableMenuItem.icon(): ImageVector = when (this) {
    CustomizableMenuItem.NEW_TAB -> Icons.Filled.Add
    CustomizableMenuItem.NEW_INCOGNITO_TAB -> Icons.Filled.VisibilityOff
    CustomizableMenuItem.NEW_PROFILE_TAB -> Icons.Filled.AccountCircle
    CustomizableMenuItem.SHARE -> Icons.Filled.Share
    CustomizableMenuItem.OPEN_IN_APP -> Icons.AutoMirrored.Filled.OpenInNew
    CustomizableMenuItem.CREATE_SHORTCUT -> Icons.AutoMirrored.Filled.AddToHomeScreen
    CustomizableMenuItem.CREATE_FAVORITE -> Icons.Filled.StarBorder
    CustomizableMenuItem.DOWNLOADS -> Icons.Filled.Download
    CustomizableMenuItem.MEDIA_CAPTURE -> Icons.Filled.VideoLibrary
    CustomizableMenuItem.EXTENSIONS -> Icons.Filled.Extension
    CustomizableMenuItem.WEBSITE_BLOCKER -> Icons.Filled.Shield
    CustomizableMenuItem.BOOKMARKS -> Icons.Filled.BookmarkBorder
    CustomizableMenuItem.HISTORY -> Icons.Filled.History
    CustomizableMenuItem.READER_MODE -> Icons.AutoMirrored.Filled.ChromeReaderMode
    CustomizableMenuItem.FIND_IN_PAGE -> Icons.Filled.FindInPage
    CustomizableMenuItem.TRANSLATE -> Icons.Filled.Translate
    CustomizableMenuItem.DESKTOP_MODE -> Icons.Filled.DesktopWindows
    CustomizableMenuItem.DATA_SAVER -> Icons.Filled.DataSaverOn
    CustomizableMenuItem.DISABLE_DATA_SAVER_FOR_SITE -> Icons.Filled.DataSaverOff
    CustomizableMenuItem.MUTE_TAB -> Icons.AutoMirrored.Filled.VolumeOff
    CustomizableMenuItem.COOKIES_CONTROL -> CookieIcon
}

fun CustomizableMenuItem.titleRes(): Int = when (this) {
    CustomizableMenuItem.NEW_TAB -> R.string.new_tab
    CustomizableMenuItem.NEW_INCOGNITO_TAB -> R.string.new_incognito_tab
    CustomizableMenuItem.NEW_PROFILE_TAB -> R.string.profiles_switch
    CustomizableMenuItem.SHARE -> R.string.share_url
    CustomizableMenuItem.OPEN_IN_APP -> R.string.menu_open_in_app
    CustomizableMenuItem.CREATE_SHORTCUT -> R.string.menu_create_shortcut
    CustomizableMenuItem.CREATE_FAVORITE -> R.string.menu_create_favorite
    CustomizableMenuItem.DOWNLOADS -> R.string.menu_downloads
    CustomizableMenuItem.MEDIA_CAPTURE -> R.string.menu_media_capture
    CustomizableMenuItem.EXTENSIONS -> R.string.ext_title
    CustomizableMenuItem.WEBSITE_BLOCKER -> R.string.menu_website_blocker
    CustomizableMenuItem.BOOKMARKS -> R.string.menu_bookmarks
    CustomizableMenuItem.HISTORY -> R.string.menu_history
    CustomizableMenuItem.READER_MODE -> R.string.reader_mode
    CustomizableMenuItem.FIND_IN_PAGE -> R.string.menu_find_in_page
    CustomizableMenuItem.TRANSLATE -> R.string.translation_menu_translate
    CustomizableMenuItem.DESKTOP_MODE -> R.string.desktop_mode
    CustomizableMenuItem.DATA_SAVER -> R.string.menu_data_saver
    CustomizableMenuItem.DISABLE_DATA_SAVER_FOR_SITE -> R.string.menu_disable_data_saver_for_site
    CustomizableMenuItem.MUTE_TAB -> R.string.menu_mute_tab
    CustomizableMenuItem.COOKIES_CONTROL -> R.string.menu_cookies_control
}

object BrowserMenuCustomizationStore {
    private const val PREF_ORDER = "browser_menu_item_order"
    private const val PREF_HIDDEN = "browser_menu_hidden_items"
    private const val PREF_SECTIONS = "browser_menu_item_sections"
    private const val PREF_GRID_COLUMNS = "browser_menu_grid_columns"
    private const val PREF_COMPACT = "browser_menu_compact"
    private const val PREF_SHOW_HEADER = "browser_menu_show_header"
    private const val PREF_GRID_LABELS = "browser_menu_grid_labels"
    private const val PREF_NAV_SLOTS = "browser_menu_nav_slots"
    private const val PREF_SHOW_NAV_BAR = "browser_menu_show_nav_bar"

    val GRID_COLUMN_OPTIONS = listOf(3, 4, 5)
    const val NAV_SLOT_COUNT = 5

    fun readOrder(prefs: SharedPreferences): List<CustomizableMenuItem> {
        val storedIds = prefs.getString(PREF_ORDER, null)?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        val storedItems = storedIds.mapNotNull { CustomizableMenuItem.fromId(it) }.distinct()
        val missing = CustomizableMenuItem.DEFAULT_ORDER.filter { it !in storedItems }
        return storedItems + missing
    }

    fun readSections(prefs: SharedPreferences): Map<CustomizableMenuItem, MenuSection> {
        val stored = prefs.getString(PREF_SECTIONS, null)?.split(",").orEmpty().mapNotNull { pair ->
            val parts = pair.split(":")
            if (parts.size != 2) return@mapNotNull null
            val item = CustomizableMenuItem.fromId(parts[0]) ?: return@mapNotNull null
            val section = MenuSection.fromId(parts[1]) ?: return@mapNotNull null
            item to section
        }.toMap()
        val legacyHidden = prefs.getStringSet(PREF_HIDDEN, null).orEmpty()
        return CustomizableMenuItem.entries.associateWith { item ->
            stored[item] ?: if (item.id in legacyHidden) MenuSection.HIDDEN else item.defaultSection
        }
    }

    fun readNavSlots(prefs: SharedPreferences): List<MenuNavSlot> {
        val stored = prefs.getString(PREF_NAV_SLOTS, null)?.split(",")?.mapNotNull { MenuNavSlot.fromId(it) }?.distinct()
        return if (stored != null && stored.size == NAV_SLOT_COUNT) stored else MenuNavSlot.DEFAULT
    }

    fun readLayout(prefs: SharedPreferences): BrowserMenuLayout = BrowserMenuLayout(
        order = readOrder(prefs),
        sections = readSections(prefs),
        gridColumns = prefs.getInt(PREF_GRID_COLUMNS, 4).takeIf { it in GRID_COLUMN_OPTIONS } ?: 4,
        compact = prefs.getBoolean(PREF_COMPACT, false),
        showHeader = prefs.getBoolean(PREF_SHOW_HEADER, true),
        gridLabels = prefs.getBoolean(PREF_GRID_LABELS, true),
        navSlots = readNavSlots(prefs),
        showNavBar = prefs.getBoolean(PREF_SHOW_NAV_BAR, true)
    )

    fun writeArrangement(prefs: SharedPreferences, order: List<CustomizableMenuItem>, sections: Map<CustomizableMenuItem, MenuSection>) {
        prefs.edit()
            .putString(PREF_ORDER, order.joinToString(",") { it.id })
            .putString(PREF_SECTIONS, order.joinToString(",") { "${it.id}:${(sections[it] ?: it.defaultSection).id}" })
            .remove(PREF_HIDDEN)
            .apply()
    }

    fun writeGridColumns(prefs: SharedPreferences, columns: Int) {
        prefs.edit().putInt(PREF_GRID_COLUMNS, columns).apply()
    }

    fun writeCompact(prefs: SharedPreferences, compact: Boolean) {
        prefs.edit().putBoolean(PREF_COMPACT, compact).apply()
    }

    fun writeShowHeader(prefs: SharedPreferences, show: Boolean) {
        prefs.edit().putBoolean(PREF_SHOW_HEADER, show).apply()
    }

    fun writeGridLabels(prefs: SharedPreferences, show: Boolean) {
        prefs.edit().putBoolean(PREF_GRID_LABELS, show).apply()
    }

    fun writeShowNavBar(prefs: SharedPreferences, show: Boolean) {
        prefs.edit().putBoolean(PREF_SHOW_NAV_BAR, show).apply()
    }

    fun writeNavSlots(prefs: SharedPreferences, slots: List<MenuNavSlot>) {
        prefs.edit().putString(PREF_NAV_SLOTS, slots.joinToString(",") { it.id }).apply()
    }

    fun resetToDefault(prefs: SharedPreferences) {
        prefs.edit()
            .remove(PREF_ORDER)
            .remove(PREF_HIDDEN)
            .remove(PREF_SECTIONS)
            .remove(PREF_GRID_COLUMNS)
            .remove(PREF_COMPACT)
            .remove(PREF_SHOW_HEADER)
            .remove(PREF_GRID_LABELS)
            .remove(PREF_NAV_SLOTS)
            .remove(PREF_SHOW_NAV_BAR)
            .apply()
    }
}
