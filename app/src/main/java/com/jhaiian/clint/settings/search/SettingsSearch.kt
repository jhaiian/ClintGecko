package com.jhaiian.clint.settings.search

import android.content.Context
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.automirrored.filled.AddToHomeScreen
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowDropDownCircle
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.DataSaverOff
import androidx.compose.material.icons.filled.DataSaverOn
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.HideImage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.PausePresentation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.VerticalAlignCenter
import androidx.compose.material.icons.filled.VideoSettings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.ViewCompact
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Web
import androidx.compose.material.icons.filled.WebAsset
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.BuildConfig
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.webview.ClintAutofill
import com.jhaiian.clint.settings.DEST_ABOUT
import com.jhaiian.clint.settings.DEST_BACKUP_RESTORE
import com.jhaiian.clint.settings.DEST_BROWSER
import com.jhaiian.clint.settings.DEST_DATA_SAVER
import com.jhaiian.clint.settings.DEST_DEBUG
import com.jhaiian.clint.settings.DEST_DOWNLOADS
import com.jhaiian.clint.settings.DEST_LOOK_AND_FEEL
import com.jhaiian.clint.settings.DEST_MISC
import com.jhaiian.clint.settings.DEST_PRIVACY
import com.jhaiian.clint.settings.DEST_SITE_SETTINGS
import com.jhaiian.clint.settings.DEST_SUPPORT_CLINT
import com.jhaiian.clint.settings.DEST_UPDATES
import com.jhaiian.clint.ui.listscreen.ClintSearchField
import com.jhaiian.clint.ui.rememberMaxContentWidth
import com.jhaiian.clint.ui.theme.LocalClintColors
import java.text.Normalizer

class SettingsSearchEntry internal constructor(
    val destination: String,
    @StringRes val title: Int,
    @StringRes val summary: Int?,
    @StringRes val section: Int?,
    @StringRes val category: Int,
    val icon: ImageVector,
    val extras: List<Int>,
    val highlight: Boolean
)

class SettingsSearchResult internal constructor(
    val destination: String,
    val highlightTitle: String?
)

private class ResolvedEntry(
    val entry: SettingsSearchEntry,
    val title: String,
    val summary: String?,
    val category: String,
    val normalizedTitle: String,
    val normalizedRest: String
)

private class PageScope(
    private val out: MutableList<SettingsSearchEntry>,
    private val destination: String,
    private val category: Int,
    private val icon: ImageVector
) {
    fun item(
        @StringRes title: Int,
        @StringRes summary: Int? = null,
        @StringRes section: Int? = null,
        extras: List<Int> = emptyList(),
        icon: ImageVector = this.icon
    ) {
        out.add(SettingsSearchEntry(destination, title, summary, section, category, icon, extras, true))
    }
}

private fun MutableList<SettingsSearchEntry>.page(
    destination: String,
    @StringRes category: Int,
    icon: ImageVector,
    block: PageScope.() -> Unit
) {
    PageScope(this, destination, category, icon).block()
}

private fun MutableList<SettingsSearchEntry>.category(
    destination: String,
    @StringRes title: Int,
    @StringRes summary: Int?,
    icon: ImageVector
) {
    add(SettingsSearchEntry(destination, title, summary, null, title, icon, emptyList(), false))
}

private val AccentNames = listOf(
        R.string.accent_default,
        R.string.accent_material_you,
        R.string.accent_purple,
        R.string.accent_deep_purple,
        R.string.accent_royal_purple,
        R.string.accent_amethyst,
        R.string.accent_lavender,
        R.string.accent_teal,
        R.string.accent_azure,
        R.string.accent_pink,
        R.string.accent_indigo,
        R.string.accent_cyan,
        R.string.accent_amber,
        R.string.accent_mint,
        R.string.accent_crimson,
        R.string.accent_slate,
        R.string.accent_graphite,
        R.string.accent_obsidian,
        R.string.accent_onyx,
        R.string.accent_titanium,
        R.string.accent_coral,
        R.string.accent_burgundy,
        R.string.accent_midnight,
        R.string.accent_sepia,
        R.string.accent_mustard,
        R.string.accent_forest,
        R.string.accent_sage,
        R.string.accent_plum,
        R.string.accent_violet,
        R.string.accent_sand,
        R.string.accent_ruby,
        R.string.accent_sky,
        R.string.accent_charcoal,
        R.string.accent_peach,
        R.string.accent_terracotta,
        R.string.accent_emerald,
        R.string.accent_blue,
        R.string.accent_yellow,
        R.string.accent_lemon,
        R.string.accent_gold,
        R.string.accent_red,
        R.string.accent_green,
        R.string.accent_scarlet,
        R.string.accent_lime,
        R.string.accent_olive,
        R.string.accent_orange,
        R.string.accent_deep_orange,
        R.string.accent_tangerine,
        R.string.accent_apricot,
        R.string.accent_copper
)

internal fun buildSettingsSearchEntries(context: Context): List<SettingsSearchEntry> = buildList {
    category(DEST_LOOK_AND_FEEL, R.string.look_and_feel, R.string.look_and_feel_summary, Icons.Filled.Palette)
    category(DEST_BROWSER, R.string.browser_settings, R.string.browser_settings_summary, Icons.Filled.Explore)
    category(DEST_PRIVACY, R.string.privacy_settings, R.string.privacy_settings_summary, Icons.Filled.Lock)
    category(DEST_SITE_SETTINGS, R.string.site_settings, R.string.site_settings_summary, Icons.Filled.WebAsset)
    category(DEST_DATA_SAVER, R.string.data_saver_title, R.string.data_saver_settings_summary, Icons.Filled.DataSaverOn)
    category(DEST_DOWNLOADS, R.string.download_settings_title, R.string.download_settings_summary, Icons.Filled.Download)
    category(DEST_BACKUP_RESTORE, R.string.backup_restore_title, R.string.backup_restore_summary, Icons.Filled.SettingsBackupRestore)
    if (BuildConfig.IS_FOSS) {
        category(DEST_UPDATES, R.string.view_changelog_title, R.string.view_changelog_summary, Icons.Filled.Update)
    } else {
        category(DEST_UPDATES, R.string.pref_updates_title, R.string.pref_updates_summary, Icons.Filled.Update)
    }
    category(DEST_MISC, R.string.pref_misc_title, R.string.pref_misc_summary, Icons.Filled.Tune)
    category(DEST_DEBUG, R.string.debug_title, R.string.debug_summary, Icons.Filled.BugReport)
    category(DEST_SUPPORT_CLINT, R.string.support_clint_title, R.string.support_clint_summary, Icons.Filled.VolunteerActivism)
    category(DEST_ABOUT, R.string.about, null, Icons.Filled.Info)

    page(DEST_LOOK_AND_FEEL, R.string.look_and_feel, Icons.Filled.Palette) {
        item(R.string.pref_language_title, section = R.string.pref_category_language, extras = listOf(R.string.language_system), icon = Icons.Filled.Translate)
        item(
            R.string.pref_app_theme_title, R.string.pref_app_theme_summary, R.string.pref_category_appearance,
            listOf(R.string.theme_system, R.string.theme_light, R.string.theme_dark)
        )
        item(R.string.pref_accent_color_title, section = R.string.pref_category_appearance, extras = AccentNames, icon = Icons.Filled.Brush)
        item(
            R.string.pref_surface_intensity_title, section = R.string.pref_category_appearance,
            extras = listOf(
                R.string.surface_intensity_pure, R.string.surface_intensity_soft, R.string.surface_intensity_strong,
                R.string.surface_intensity_no_tint, R.string.surface_intensity_amoled_no_tint
            )
        )
        item(
            R.string.pref_nested_scroll_title, section = R.string.pref_category_layout,
            extras = listOf(R.string.nested_scroll_off, R.string.nested_scroll_search_bar, R.string.nested_scroll_both)
        )
        item(
            R.string.pref_address_bar_position_title, section = R.string.pref_category_layout,
            extras = listOf(R.string.address_bar_position_top, R.string.address_bar_position_bottom, R.string.address_bar_position_split)
        )
        item(
            R.string.pref_menu_style_title, section = R.string.pref_category_layout,
            extras = listOf(R.string.menu_style_bottom_sheet, R.string.menu_style_popup)
        )
        item(R.string.menu_customization_title, R.string.menu_customization_settings_summary, R.string.pref_category_layout, icon = Icons.Filled.Reorder)
        item(R.string.hide_status_bar, R.string.hide_status_bar_summary, R.string.pref_category_layout, icon = Icons.Filled.Fullscreen)
        item(R.string.hide_system_navigation, R.string.hide_system_navigation_summary, R.string.pref_category_layout, icon = Icons.Filled.Fullscreen)
        item(
            R.string.exit_confirmation_title, section = R.string.pref_category_navigation,
            extras = listOf(R.string.exit_confirmation_off, R.string.exit_confirmation_toast, R.string.exit_confirmation_dialog)
        )
        item(
            R.string.pref_popup_alert_style_title, section = R.string.pref_category_navigation,
            extras = listOf(R.string.popup_alert_style_dialog, R.string.popup_alert_style_snackbar)
        )
    }

    page(DEST_BROWSER, R.string.browser_settings, Icons.Filled.Explore) {
        item(
            R.string.homepage_title, section = R.string.pref_category_homepage,
            extras = listOf(R.string.homepage_clint, R.string.homepage_search_engine)
        )
        item(
            R.string.homepage_design_title, section = R.string.pref_category_homepage,
            extras = listOf(R.string.homepage_design_plain, R.string.homepage_design_gradient, R.string.homepage_design_image)
        )
        item(R.string.homepage_show_favorites_title, R.string.homepage_show_favorites_summary, R.string.pref_category_homepage, icon = Icons.Filled.Bookmark)
        item(R.string.homepage_show_recent_title, R.string.homepage_show_recent_summary, R.string.pref_category_homepage, icon = Icons.Filled.History)
        item(R.string.homepage_center_content_title, R.string.homepage_center_content_summary, R.string.pref_category_homepage, icon = Icons.Filled.VerticalAlignCenter)
        item(
            R.string.search_engine, section = R.string.pref_category_search,
            extras = listOf(R.string.engine_google, R.string.engine_duckduckgo, R.string.engine_brave, R.string.engine_ecosia, R.string.engine_custom)
        )
        item(R.string.search_suggestions_api, section = R.string.pref_category_search, icon = Icons.AutoMirrored.Filled.ManageSearch)
        item(
            R.string.delete_inactive_tabs_title, section = R.string.pref_category_tabs,
            extras = listOf(
                R.string.delete_inactive_tabs_summary_never, R.string.delete_inactive_tabs_summary_1_day,
                R.string.delete_inactive_tabs_summary_1_week, R.string.delete_inactive_tabs_summary_1_month
            )
        )
        item(R.string.website_blocker_title, R.string.website_blocker_settings_summary, R.string.pref_category_protection, icon = Icons.Filled.Shield)
        item(R.string.incognito_search_history_title, R.string.incognito_search_history_summary, R.string.pref_category_incognito, icon = Icons.Filled.VisibilityOff)
        item(R.string.javascript_enabled, R.string.javascript_enabled_summary, R.string.browser_settings, icon = Icons.Filled.DesktopWindows)
        item(R.string.background_playback_title, R.string.background_playback_summary, R.string.browser_settings, icon = androidx.compose.material.icons.Icons.Filled.Headphones)
        item(R.string.profiles_title, R.string.profiles_settings_summary, R.string.browser_settings, icon = androidx.compose.material.icons.Icons.Filled.AccountCircle)
        item(R.string.ext_title, R.string.ext_settings_summary, R.string.browser_settings, icon = androidx.compose.material.icons.Icons.Filled.Extension)
        item(R.string.shortcut_manager_title, R.string.shortcut_manager_summary, R.string.pref_category_shortcuts, icon = Icons.AutoMirrored.Filled.AddToHomeScreen)
    }

    page(DEST_PRIVACY, R.string.privacy_settings, Icons.Filled.Lock) {
        item(R.string.block_third_party_cookies, R.string.block_third_party_cookies_summary, R.string.privacy_section_privacy, icon = Icons.Filled.Cookie)
        item(R.string.custom_user_agent, R.string.custom_user_agent_summary, R.string.privacy_section_privacy, icon = Icons.Filled.Language)
        item(R.string.https_only, R.string.https_only_summary, R.string.privacy_section_privacy, icon = Icons.Filled.Lock)
        item(R.string.do_not_track_title, R.string.do_not_track_summary, R.string.privacy_section_privacy, icon = Icons.Filled.VisibilityOff)
        if (ClintAutofill.isSupported(context)) {
            item(R.string.autofill_title, R.string.autofill_summary, R.string.privacy_section_autofill, icon = Icons.Filled.Password)
            item(R.string.autofill_service_title, section = R.string.privacy_section_autofill, icon = Icons.Filled.ManageAccounts)
        }
        item(R.string.history_title, R.string.history_summary, R.string.privacy_section_history, icon = Icons.Filled.History)
    }

    page(DEST_SITE_SETTINGS, R.string.site_settings, Icons.Filled.WebAsset) {
        item(R.string.site_settings_camera, section = R.string.site_section_permissions, icon = Icons.Filled.Videocam)
        item(R.string.site_settings_mic, section = R.string.site_section_permissions, icon = Icons.Filled.Mic)
        item(R.string.site_settings_location, section = R.string.site_section_permissions, icon = Icons.Filled.LocationOn)
        item(R.string.site_settings_notifications, section = R.string.site_section_permissions, icon = Icons.Filled.Notifications)
        item(R.string.site_settings_clipboard, section = R.string.site_section_permissions, icon = Icons.Filled.ContentPaste)
        item(R.string.site_settings_open_in_app, section = R.string.site_section_open_in_app, icon = Icons.AutoMirrored.Filled.OpenInNew)
        item(R.string.site_settings_desktop_mode, section = R.string.site_section_desktop_mode, icon = Icons.Filled.DesktopWindows)
        item(R.string.data_saver_title, R.string.site_settings_data_saver_summary, R.string.site_section_data_saver, icon = Icons.Filled.DataSaverOff)
    }

    page(DEST_DATA_SAVER, R.string.data_saver_title, Icons.Filled.DataSaverOn) {
        item(R.string.data_saver_title, R.string.data_saver_summary, R.string.data_saver_section, icon = Icons.Filled.DataSaverOn)
        item(R.string.data_saver_metered_only_title, R.string.data_saver_metered_only_summary, R.string.data_saver_section, icon = Icons.Filled.SignalCellularAlt)
        item(R.string.data_saver_disable_images_title, R.string.data_saver_disable_images_summary, R.string.data_saver_media_section, icon = Icons.Filled.HideImage)
        item(R.string.data_saver_block_video_title, R.string.data_saver_block_video_summary, R.string.data_saver_media_section, icon = Icons.Filled.VideocamOff)
        item(R.string.data_saver_disable_autoplay_title, R.string.data_saver_disable_autoplay_summary, R.string.data_saver_media_section, icon = Icons.Filled.PausePresentation)
        item(R.string.data_saver_block_preload_title, R.string.data_saver_block_preload_summary, R.string.data_saver_media_section, icon = Icons.Filled.Block)
        item(R.string.data_saver_block_fonts_title, R.string.data_saver_block_fonts_summary, R.string.data_saver_loading_section, icon = Icons.Filled.TextFields)
        item(R.string.data_saver_block_frames_title, R.string.data_saver_block_frames_summary, R.string.data_saver_loading_section, icon = Icons.Filled.Web)
        item(R.string.data_saver_block_scripts_title, R.string.data_saver_block_scripts_summary, R.string.data_saver_loading_section, icon = Icons.Filled.Code)
        item(R.string.data_saver_block_css_title, R.string.data_saver_block_css_summary, R.string.data_saver_loading_section, icon = Icons.Filled.Style)
        item(R.string.data_saver_cache_first_title, R.string.data_saver_cache_first_summary, R.string.data_saver_loading_section, icon = Icons.Filled.History)
        item(R.string.data_saver_send_header_title, R.string.data_saver_send_header_summary, R.string.data_saver_loading_section, icon = Icons.Filled.Speed)
        item(R.string.data_saver_disable_suggestions_title, R.string.data_saver_disable_suggestions_summary, R.string.data_saver_browser_section, icon = Icons.Filled.SearchOff)
        item(R.string.data_saver_disable_favicons_title, R.string.data_saver_disable_favicons_summary, R.string.data_saver_browser_section, icon = Icons.Filled.Language)
    }

    page(DEST_DOWNLOADS, R.string.download_settings_title, Icons.Filled.Download) {
        item(R.string.download_manager_title, section = R.string.download_section_manager, icon = Icons.Filled.Apps)
        item(R.string.download_categorize_title, R.string.download_categorize_summary, R.string.download_section_location, icon = Icons.Filled.Category)
        item(R.string.measurement_system_title, section = R.string.download_section_measurement, icon = Icons.Filled.FormatSize)
        item(R.string.download_unmetered_only_title, R.string.download_unmetered_only_summary, R.string.download_section_network, icon = Icons.Filled.Wifi)
        item(R.string.download_schedule_enabled_title, section = R.string.download_section_scheduling, icon = Icons.Filled.DateRange)
        item(R.string.download_schedule_start_title, section = R.string.download_section_scheduling, icon = Icons.Filled.HourglassEmpty)
        item(R.string.download_schedule_end_title, section = R.string.download_section_scheduling, icon = Icons.Filled.HourglassEmpty)
        item(R.string.download_concurrent_title, R.string.download_concurrent_desc, R.string.download_section_concurrent, icon = Icons.Filled.HourglassBottom)
        item(R.string.download_split_parts_title, R.string.download_split_parts_desc, R.string.download_section_split_parts, icon = Icons.Filled.Splitscreen)
        item(R.string.download_multithreading_title, R.string.download_multithreading_desc, R.string.download_section_multithreading, icon = Icons.Filled.KeyboardDoubleArrowDown)
        item(R.string.download_concurrent_segments_title, R.string.download_concurrent_segments_desc, R.string.download_section_stream_segments, icon = Icons.Filled.VideoSettings)
        item(R.string.download_speed_limit_title, section = R.string.download_section_speed_limit, icon = Icons.Filled.Speed)
        item(R.string.download_retry_enabled_title, R.string.download_retry_enabled_summary, R.string.download_section_retry, icon = Icons.Filled.RestartAlt)
        item(R.string.download_retry_unrecoverable_title, R.string.download_retry_unrecoverable_summary, R.string.download_section_retry, icon = Icons.Filled.Storage)
        item(R.string.download_retry_count_title, section = R.string.download_section_retry, icon = Icons.Filled.Numbers)
        item(R.string.download_retry_interval_title, section = R.string.download_section_retry, icon = Icons.Filled.HourglassEmpty)
        item(R.string.download_quick_download_title, R.string.download_quick_download_summary, R.string.download_section_general, icon = Icons.Filled.FlashOn)
        item(R.string.download_quick_download_images_title, R.string.download_quick_download_images_summary, R.string.download_section_general, icon = Icons.Filled.Image)
        item(R.string.download_keep_screen_on_title, R.string.download_keep_screen_on_summary, R.string.download_section_general, icon = Icons.Filled.Visibility)
        item(R.string.download_dialog_ui_title, section = R.string.download_section_dialog_ui, icon = Icons.Filled.ViewCompact)
        item(R.string.download_push_notifications_title, R.string.download_push_notifications_summary, R.string.download_section_notifications, icon = Icons.Filled.Notifications)
        item(R.string.download_ignore_battery_opt_title, R.string.download_ignore_battery_opt_summary, R.string.download_section_permissions, icon = Icons.Filled.BatterySaver)
        if (!BuildConfig.IS_FOSS && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            item(R.string.download_grant_all_files_access_title, R.string.download_grant_all_files_access_summary, R.string.download_section_permissions, icon = Icons.Filled.Folder)
        }
    }

    page(DEST_BACKUP_RESTORE, R.string.backup_restore_title, Icons.Filled.SettingsBackupRestore) {
        item(R.string.backup_row_title, R.string.backup_row_summary, icon = Icons.Filled.Backup)
        item(R.string.restore_row_title, R.string.restore_row_summary, icon = Icons.Filled.Restore)
    }

    if (!BuildConfig.IS_FOSS) {
        page(DEST_UPDATES, R.string.pref_updates_title, Icons.Filled.Update) {
            item(R.string.check_update_on_launch_title, R.string.check_update_on_launch_summary, R.string.update_section_updates, icon = Icons.Filled.Refresh)
            item(R.string.skip_update_on_metered_title, R.string.skip_update_on_metered_summary, R.string.update_section_updates, icon = Icons.Filled.Wifi)
            item(R.string.check_for_updates_title, R.string.check_for_updates_summary, R.string.update_section_updates, icon = Icons.Filled.Update)
            item(R.string.view_changelog_title, R.string.view_changelog_summary, R.string.update_section_updates, icon = Icons.Filled.History)
            item(R.string.beta_channel_title, R.string.beta_channel_summary, R.string.update_section_channel, icon = Icons.Filled.Warning)
        }
    }

    page(DEST_MISC, R.string.pref_misc_title, Icons.Filled.Tune) {
        item(R.string.default_browser_title, section = R.string.misc_section_app, icon = Icons.Filled.Language)
        item(R.string.pref_rerun_setup_title, R.string.pref_rerun_setup_summary, R.string.misc_section_setup, icon = Icons.Filled.Refresh)
    }

    page(DEST_ABOUT, R.string.about, Icons.Filled.Info) {
        item(R.string.about_section_version)
        item(R.string.about_section_license)
        item(R.string.about_section_repository)
        item(R.string.document_viewer_privacy_policy_title, section = R.string.about_section_legal)
        item(R.string.document_viewer_terms_title, section = R.string.about_section_legal)
        item(R.string.about_section_community, extras = listOf(R.string.about_community_discord_label, R.string.about_community_reddit_label))
        item(R.string.about_section_contact, extras = listOf(R.string.about_contact_email_label))
    }
}

private val CombiningMarks = Regex("\\p{Mn}+")

private fun normalizeForSearch(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD).replace(CombiningMarks, "").lowercase()

private fun resolveEntries(context: Context): List<ResolvedEntry> =
    buildSettingsSearchEntries(context).map { entry ->
        val title = context.getString(entry.title)
        val summary = entry.summary?.let { context.getString(it) }
        val category = context.getString(entry.category)
        val rest = buildString {
            append(category)
            entry.section?.let { append(' ').append(context.getString(it)) }
            summary?.let { append(' ').append(it) }
            entry.extras.forEach { append(' ').append(context.getString(it)) }
        }
        ResolvedEntry(entry, title, summary, category, normalizeForSearch(title), normalizeForSearch(rest))
    }

private fun searchEntries(entries: List<ResolvedEntry>, query: String): List<ResolvedEntry> {
    val tokens = normalizeForSearch(query).split(' ', '\t').filter { it.isNotEmpty() }
    if (tokens.isEmpty()) return emptyList()
    return entries
        .mapNotNull { resolved ->
            var total = 0
            for (token in tokens) {
                val inTitle = resolved.normalizedTitle.indexOf(token)
                val score = when {
                    inTitle == 0 || (inTitle > 0 && resolved.normalizedTitle[inTitle - 1] == ' ') -> 4
                    inTitle > 0 -> 3
                    resolved.normalizedRest.contains(token) -> 1
                    else -> return@mapNotNull null
                }
                total += score
            }
            resolved to total
        }
        .sortedByDescending { it.second }
        .map { it.first }
}

@Composable
fun SettingsSearchToolbar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    val colors = LocalClintColors.current
    Surface(color = colors.surface, shadowElevation = 4.dp, modifier = Modifier.statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = colors.onSurface)
            }
            ClintSearchField(
                query = query,
                onQueryChange = onQueryChange,
                hint = stringResource(R.string.settings_search_hint),
                onClose = onClose,
                autoFocus = query.isEmpty()
            )
        }
    }
}

@Composable
fun SettingsSearchResults(query: String, onResultClick: (SettingsSearchResult) -> Unit) {
    val colors = LocalClintColors.current
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val entries = remember(context, configuration) { resolveEntries(context) }
    val results = remember(entries, query) { searchEntries(entries, query) }
    val maxContentWidth = rememberMaxContentWidth(context)

    Surface(color = colors.background, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            when {
                query.isBlank() -> SearchMessage(stringResource(R.string.settings_search_prompt), showIcon = true)
                results.isEmpty() -> SearchMessage(stringResource(R.string.settings_search_no_results, query.trim()), showIcon = true)
                else -> LazyColumn(
                    modifier = Modifier
                        .then(if (maxContentWidth != null) Modifier.widthIn(max = maxContentWidth) else Modifier.fillMaxWidth())
                        .fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(results) { resolved ->
                        SearchResultCard(resolved) {
                            keyboardController?.hide()
                            onResultClick(
                                SettingsSearchResult(
                                    destination = resolved.entry.destination,
                                    highlightTitle = if (resolved.entry.highlight) resolved.title else null
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchMessage(text: String, showIcon: Boolean) {
    val colors = LocalClintColors.current
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (showIcon) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = colors.secondaryText, modifier = Modifier.size(48.dp))
            }
            Text(
                text,
                color = colors.secondaryText,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
private fun SearchResultCard(resolved: ResolvedEntry, onClick: () -> Unit) {
    val colors = LocalClintColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
    ) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(resolved.entry.icon, contentDescription = null, tint = colors.iconTint, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                Text(resolved.title, color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                resolved.summary?.let {
                    Text(
                        it,
                        color = colors.secondaryText,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                if (resolved.entry.highlight) {
                    Text(
                        resolved.category,
                        color = colors.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}
