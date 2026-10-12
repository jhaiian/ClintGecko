package com.jhaiian.clint.settings.datasaver
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.DataSaverOn
import androidx.compose.material.icons.filled.HideImage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Web
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.PausePresentation
import androidx.compose.material.icons.filled.Speed

import androidx.compose.foundation.layout.padding
import com.jhaiian.clint.ui.ClintSwitch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jhaiian.clint.R
import com.jhaiian.clint.settings.common.RowDivider
import com.jhaiian.clint.settings.common.SettingsRow
import com.jhaiian.clint.settings.common.SettingsScreenScaffold
import com.jhaiian.clint.settings.common.SettingsSection
import com.jhaiian.clint.setup.SectionLabel
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
fun DataSaverScreen(
    state: DataSaverUiState,
    onEnabledClick: () -> Unit,
    onMeteredOnlyClick: () -> Unit,
    onDisableImagesClick: () -> Unit,
    onDisableAutoplayClick: () -> Unit,
    onSendHeaderClick: () -> Unit,
    onBlockFontsClick: () -> Unit,
    onBlockFramesClick: () -> Unit,
    onBlockPreloadClick: () -> Unit,
    onCacheFirstClick: () -> Unit,
    onBlockVideoClick: () -> Unit,
    onDisableSuggestionsClick: () -> Unit,
    onDisableFaviconsClick: () -> Unit,
    onBlockScriptsClick: () -> Unit,
    onBlockCssClick: () -> Unit
) {
    val colors = LocalClintColors.current

    SettingsScreenScaffold {
        SectionLabel(stringResource(R.string.data_saver_section), colors.primary, Modifier.padding(start = 4.dp, bottom = 8.dp))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.DataSaverOn,
                title = stringResource(R.string.data_saver_title),
                summary = stringResource(R.string.data_saver_summary),
                colors = colors,
                onClick = onEnabledClick,
                trailing = {
                    ClintSwitch(checked = state.enabled)
                }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.SignalCellularAlt,
                title = stringResource(R.string.data_saver_metered_only_title),
                summary = stringResource(R.string.data_saver_metered_only_summary),
                colors = colors,
                onClick = onMeteredOnlyClick,
                enabled = state.enabled,
                trailing = {
                    ClintSwitch(checked = state.meteredOnly)
                }
            )
        }
        SectionLabel(stringResource(R.string.data_saver_media_section), colors.primary, Modifier.padding(start = 4.dp, bottom = 8.dp))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.HideImage,
                title = stringResource(R.string.data_saver_disable_images_title),
                summary = stringResource(R.string.data_saver_disable_images_summary),
                colors = colors,
                onClick = onDisableImagesClick,
                enabled = state.enabled,
                trailing = {
                    ClintSwitch(checked = state.disableImages)
                }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.VideocamOff,
                title = stringResource(R.string.data_saver_block_video_title),
                summary = stringResource(R.string.data_saver_block_video_summary),
                colors = colors,
                onClick = onBlockVideoClick,
                enabled = state.enabled,
                trailing = {
                    ClintSwitch(checked = state.blockVideo)
                }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.PausePresentation,
                title = stringResource(R.string.data_saver_disable_autoplay_title),
                summary = stringResource(R.string.data_saver_disable_autoplay_summary),
                colors = colors,
                onClick = onDisableAutoplayClick,
                enabled = state.enabled,
                trailing = {
                    ClintSwitch(checked = state.disableAutoplay)
                }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Block,
                title = stringResource(R.string.data_saver_block_preload_title),
                summary = stringResource(R.string.data_saver_block_preload_summary),
                colors = colors,
                onClick = onBlockPreloadClick,
                enabled = state.enabled,
                trailing = {
                    ClintSwitch(checked = state.blockPreload)
                }
            )
        }
        SectionLabel(stringResource(R.string.data_saver_loading_section), colors.primary, Modifier.padding(start = 4.dp, bottom = 8.dp))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.TextFields,
                title = stringResource(R.string.data_saver_block_fonts_title),
                summary = stringResource(R.string.data_saver_block_fonts_summary),
                colors = colors,
                onClick = onBlockFontsClick,
                enabled = state.enabled,
                trailing = {
                    ClintSwitch(checked = state.blockFonts)
                }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Web,
                title = stringResource(R.string.data_saver_block_frames_title),
                summary = stringResource(R.string.data_saver_block_frames_summary),
                colors = colors,
                onClick = onBlockFramesClick,
                enabled = state.enabled,
                trailing = {
                    ClintSwitch(checked = state.blockFrames)
                }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Code,
                title = stringResource(R.string.data_saver_block_scripts_title),
                summary = stringResource(R.string.data_saver_block_scripts_summary),
                colors = colors,
                onClick = onBlockScriptsClick,
                enabled = state.enabled,
                trailing = {
                    ClintSwitch(checked = state.blockScripts)
                }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Style,
                title = stringResource(R.string.data_saver_block_css_title),
                summary = stringResource(R.string.data_saver_block_css_summary),
                colors = colors,
                onClick = onBlockCssClick,
                enabled = state.enabled,
                trailing = {
                    ClintSwitch(checked = state.blockCss)
                }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.History,
                title = stringResource(R.string.data_saver_cache_first_title),
                summary = stringResource(R.string.data_saver_cache_first_summary),
                colors = colors,
                onClick = onCacheFirstClick,
                enabled = state.enabled,
                trailing = {
                    ClintSwitch(checked = state.cacheFirst)
                }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Speed,
                title = stringResource(R.string.data_saver_send_header_title),
                summary = stringResource(R.string.data_saver_send_header_summary),
                colors = colors,
                onClick = onSendHeaderClick,
                enabled = state.enabled,
                trailing = {
                    ClintSwitch(checked = state.sendHeader)
                }
            )
        }
        SectionLabel(stringResource(R.string.data_saver_browser_section), colors.primary, Modifier.padding(start = 4.dp, bottom = 8.dp))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.SearchOff,
                title = stringResource(R.string.data_saver_disable_suggestions_title),
                summary = stringResource(R.string.data_saver_disable_suggestions_summary),
                colors = colors,
                onClick = onDisableSuggestionsClick,
                enabled = state.enabled,
                trailing = {
                    ClintSwitch(checked = state.disableSuggestions)
                }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Language,
                title = stringResource(R.string.data_saver_disable_favicons_title),
                summary = stringResource(R.string.data_saver_disable_favicons_summary),
                colors = colors,
                onClick = onDisableFaviconsClick,
                enabled = state.enabled,
                trailing = {
                    ClintSwitch(checked = state.disableFavicons)
                }
            )
        }
    }
}
