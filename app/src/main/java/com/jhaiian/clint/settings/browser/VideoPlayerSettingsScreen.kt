package com.jhaiian.clint.settings.browser

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.SmartButton
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.video.VideoPlayerSettings
import com.jhaiian.clint.browser.video.VideoSeekStyle
import com.jhaiian.clint.settings.SettingsActivity
import com.jhaiian.clint.settings.common.SettingsRow
import com.jhaiian.clint.settings.common.SettingsScreenScaffold
import com.jhaiian.clint.settings.common.SettingsSection
import com.jhaiian.clint.ui.ClintSwitch
import com.jhaiian.clint.ui.theme.LocalClintColors

private const val DIALOG_SKIP = "skip"
private const val DIALOG_SEEK = "seek"
private const val DIALOG_HIDE = "hide"
private const val DIALOG_BUTTONS = "buttons"

@Composable
fun VideoPlayerSettingsPane(activity: SettingsActivity) {
    val colors = LocalClintColors.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(activity) }
    val hideStatusBar = prefs.getBoolean("hide_status_bar", false)
    val hideSystemNavigation = prefs.getBoolean("hide_system_navigation", false)
    var enabled by remember { mutableStateOf(VideoPlayerSettings.isEnabled(activity)) }
    var siteFullscreen by remember { mutableStateOf(VideoPlayerSettings.isSiteFullscreen(activity)) }
    var floatingButton by remember { mutableStateOf(VideoPlayerSettings.isFloatingButton(activity)) }
    var autoPip by remember { mutableStateOf(VideoPlayerSettings.isAutoPip(activity)) }
    var rememberLevels by remember { mutableStateOf(VideoPlayerSettings.isRememberLevels(activity)) }
    var gestures by remember { mutableStateOf(VideoPlayerSettings.isGestures(activity)) }
    var doubleTap by remember { mutableStateOf(VideoPlayerSettings.isDoubleTap(activity)) }
    var holdBoost by remember { mutableStateOf(VideoPlayerSettings.isHoldBoost(activity)) }
    var accentIcons by remember { mutableStateOf(VideoPlayerSettings.isAccentIcons(activity)) }
    var skipSeconds by remember { mutableStateOf(VideoPlayerSettings.skipSeconds(activity)) }
    var hideDelay by remember { mutableStateOf(VideoPlayerSettings.hideDelaySeconds(activity)) }
    var seekStyle by remember { mutableStateOf(VideoPlayerSettings.seekStyle(activity).key) }
    var hiddenButtons by remember { mutableStateOf(VideoPlayerSettings.hiddenButtons(activity)) }
    var dialog by remember { mutableStateOf<String?>(null) }

    fun flip(key: String, current: Boolean, apply: (Boolean) -> Unit) {
        val next = !current
        prefs.edit().putBoolean(key, next).apply()
        apply(next)
    }

    SettingsScreenScaffold(
        overlay = {
            when (dialog) {
                DIALOG_SKIP -> VideoSkipDialog(
                    current = skipSeconds,
                    hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                    onConfirm = {
                        prefs.edit().putInt(VideoPlayerSettings.PREF_SKIP_SECONDS, it).apply()
                        skipSeconds = it
                        dialog = null
                    },
                    onDismiss = { dialog = null }
                )
                DIALOG_SEEK -> VideoChoiceDialog(
                    title = stringResource(R.string.video_player_seek_style_title),
                    options = VideoSeekStyle.entries.map { it.key to stringResource(it.label) },
                    defaultKey = VideoSeekStyle.SLIDER.key,
                    current = seekStyle,
                    hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                    onConfirm = {
                        prefs.edit().putString(VideoPlayerSettings.PREF_SEEK_STYLE, it).apply()
                        seekStyle = it
                        dialog = null
                    },
                    onDismiss = { dialog = null }
                )
                DIALOG_HIDE -> VideoChoiceDialog(
                    title = stringResource(R.string.video_player_hide_delay_title),
                    options = VideoPlayerSettings.HIDE_DELAY_OPTIONS.map { it.toString() to stringResource(R.string.video_player_skip_value, it) },
                    defaultKey = VideoPlayerSettings.DEFAULT_HIDE_DELAY.toString(),
                    current = hideDelay.toString(),
                    hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                    onConfirm = {
                        val seconds = it.toIntOrNull() ?: VideoPlayerSettings.DEFAULT_HIDE_DELAY
                        prefs.edit().putInt(VideoPlayerSettings.PREF_HIDE_DELAY, seconds).apply()
                        hideDelay = seconds
                        dialog = null
                    },
                    onDismiss = { dialog = null }
                )
                DIALOG_BUTTONS -> VideoButtonsDialog(
                    current = hiddenButtons,
                    hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                    onConfirm = {
                        prefs.edit().putStringSet(VideoPlayerSettings.PREF_HIDDEN_BUTTONS, it).apply()
                        hiddenButtons = it
                        dialog = null
                    },
                    onDismiss = { dialog = null }
                )
                else -> Unit
            }
        }
    ) {
        SettingsSection(colors.cardBackground) {
            VideoToggleRow(Icons.Filled.SmartDisplay, R.string.video_player_title, R.string.video_player_summary, enabled) {
                flip(VideoPlayerSettings.PREF_ENABLED, enabled) { enabled = it }
            }
            if (enabled) {
                VideoToggleRow(Icons.Filled.Fullscreen, R.string.video_player_site_fullscreen_title, R.string.video_player_site_fullscreen_summary, siteFullscreen) {
                    flip(VideoPlayerSettings.PREF_SITE_FULLSCREEN, siteFullscreen) { siteFullscreen = it }
                }
                VideoToggleRow(Icons.Filled.SmartButton, R.string.video_player_floating_button_title, R.string.video_player_floating_button_summary, floatingButton) {
                    flip(VideoPlayerSettings.PREF_FLOATING_BUTTON, floatingButton) { floatingButton = it }
                }
                VideoToggleRow(Icons.Filled.PictureInPictureAlt, R.string.video_player_auto_pip_title, R.string.video_player_auto_pip_summary, autoPip) {
                    flip(VideoPlayerSettings.PREF_AUTO_PIP, autoPip) { autoPip = it }
                }
            }
        }
        if (enabled) {
            SettingsSection(colors.cardBackground) {
                VideoToggleRow(Icons.Filled.TouchApp, R.string.video_player_gestures_title, R.string.video_player_gestures_summary, gestures) {
                    flip(VideoPlayerSettings.PREF_GESTURES, gestures) { gestures = it }
                }
                VideoToggleRow(Icons.Filled.BrightnessMedium, R.string.video_player_remember_levels_title, R.string.video_player_remember_levels_summary, rememberLevels) {
                    flip(VideoPlayerSettings.PREF_REMEMBER_LEVELS, rememberLevels) { rememberLevels = it }
                }
                VideoToggleRow(Icons.Filled.FastForward, R.string.video_player_double_tap_title, R.string.video_player_double_tap_summary, doubleTap) {
                    flip(VideoPlayerSettings.PREF_DOUBLE_TAP, doubleTap) { doubleTap = it }
                }
                VideoToggleRow(Icons.Filled.Speed, R.string.video_player_hold_boost_title, R.string.video_player_hold_boost_summary, holdBoost) {
                    flip(VideoPlayerSettings.PREF_HOLD_BOOST, holdBoost) { holdBoost = it }
                }
            }
            SettingsSection(colors.cardBackground) {
                SettingsRow(
                    icon = Icons.Filled.LinearScale,
                    title = stringResource(R.string.video_player_seek_style_title),
                    summary = stringResource(VideoSeekStyle.entries.firstOrNull { it.key == seekStyle }?.label ?: R.string.video_player_seek_slider),
                    colors = colors,
                    onClick = { dialog = DIALOG_SEEK }
                )
                SettingsRow(
                    icon = Icons.Filled.Tune,
                    title = stringResource(R.string.video_player_buttons_title),
                    summary = stringResource(R.string.video_player_buttons_summary),
                    colors = colors,
                    onClick = { dialog = DIALOG_BUTTONS }
                )
                SettingsRow(
                    icon = Icons.Filled.Forward10,
                    title = stringResource(R.string.video_player_skip_title),
                    summary = stringResource(R.string.video_player_skip_value, skipSeconds),
                    colors = colors,
                    onClick = { dialog = DIALOG_SKIP }
                )
                SettingsRow(
                    icon = Icons.Filled.Timer,
                    title = stringResource(R.string.video_player_hide_delay_title),
                    summary = stringResource(R.string.video_player_skip_value, hideDelay),
                    colors = colors,
                    onClick = { dialog = DIALOG_HIDE }
                )
                VideoToggleRow(Icons.Filled.Palette, R.string.video_player_accent_icons_title, R.string.video_player_accent_icons_summary, accentIcons) {
                    flip(VideoPlayerSettings.PREF_ACCENT_ICONS, accentIcons) { accentIcons = it }
                }
            }
        }
    }
}

@Composable
private fun VideoToggleRow(icon: ImageVector, title: Int, summary: Int, checked: Boolean, onClick: () -> Unit) {
    SettingsRow(
        icon = icon,
        title = stringResource(title),
        summary = stringResource(summary),
        colors = LocalClintColors.current,
        onClick = onClick,
        trailing = { ClintSwitch(checked = checked) }
    )
}
