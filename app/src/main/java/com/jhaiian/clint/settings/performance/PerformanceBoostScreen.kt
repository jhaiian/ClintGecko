package com.jhaiian.clint.settings.performance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.webview.PerformancePrefs
import com.jhaiian.clint.settings.SettingsActivity
import com.jhaiian.clint.settings.common.RowDivider
import com.jhaiian.clint.settings.common.SettingsRow
import com.jhaiian.clint.settings.common.SettingsScreenScaffold
import com.jhaiian.clint.settings.common.SettingsSection
import com.jhaiian.clint.settings.common.SettingsSectionLabel
import com.jhaiian.clint.ui.ClintSwitch
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
fun PerformanceBoostPane(activity: SettingsActivity) {
    val uiState = remember {
        PerformanceUiState(
            initialReduceBlur = PerformancePrefs.isEnabled(activity, PerformancePrefs.REDUCE_BLUR),
            initialDisableWebGl = PerformancePrefs.isEnabled(activity, PerformancePrefs.DISABLE_WEBGL),
            initialLimitFrameRate = PerformancePrefs.isEnabled(activity, PerformancePrefs.LIMIT_FRAME_RATE),
            initialReduceMotion = PerformancePrefs.isEnabled(activity, PerformancePrefs.REDUCE_MOTION),
            initialStopAnimatedImages = PerformancePrefs.isEnabled(activity, PerformancePrefs.STOP_ANIMATED_IMAGES)
        )
    }
    fun toggle(key: String, current: Boolean, apply: (Boolean) -> Unit) {
        val newValue = !current
        PerformancePrefs.setEnabled(activity, key, newValue)
        apply(newValue)
    }

    PerformanceBoostScreen(
        state = uiState,
        onReduceBlurClick = { toggle(PerformancePrefs.REDUCE_BLUR, uiState.reduceBlur) { uiState.reduceBlur = it } },
        onDisableWebGlClick = { toggle(PerformancePrefs.DISABLE_WEBGL, uiState.disableWebGl) { uiState.disableWebGl = it } },
        onLimitFrameRateClick = { toggle(PerformancePrefs.LIMIT_FRAME_RATE, uiState.limitFrameRate) { uiState.limitFrameRate = it } },
        onReduceMotionClick = { toggle(PerformancePrefs.REDUCE_MOTION, uiState.reduceMotion) { uiState.reduceMotion = it } },
        onStopAnimatedImagesClick = { toggle(PerformancePrefs.STOP_ANIMATED_IMAGES, uiState.stopAnimatedImages) { uiState.stopAnimatedImages = it } }
    )
}

@Composable
fun PerformanceBoostScreen(
    state: PerformanceUiState,
    onReduceBlurClick: () -> Unit,
    onDisableWebGlClick: () -> Unit,
    onLimitFrameRateClick: () -> Unit,
    onReduceMotionClick: () -> Unit,
    onStopAnimatedImagesClick: () -> Unit
) {
    val colors = LocalClintColors.current

    SettingsScreenScaffold {
        PerformanceIntroCard()
        SettingsSectionLabel(stringResource(R.string.perf_boost_section_graphics))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = Icons.Filled.BlurOn,
                title = stringResource(R.string.perf_boost_reduce_blur_title),
                summary = stringResource(R.string.perf_boost_reduce_blur_summary),
                colors = colors,
                onClick = onReduceBlurClick,
                trailing = { ClintSwitch(checked = state.reduceBlur) }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = Icons.Filled.ViewInAr,
                title = stringResource(R.string.perf_boost_disable_webgl_title),
                summary = stringResource(R.string.perf_boost_disable_webgl_summary),
                colors = colors,
                onClick = onDisableWebGlClick,
                trailing = { ClintSwitch(checked = state.disableWebGl) }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = Icons.Filled.Speed,
                title = stringResource(R.string.perf_boost_limit_frame_rate_title),
                summary = stringResource(R.string.perf_boost_limit_frame_rate_summary),
                colors = colors,
                onClick = onLimitFrameRateClick,
                trailing = { ClintSwitch(checked = state.limitFrameRate) }
            )
        }
        SettingsSectionLabel(stringResource(R.string.perf_boost_section_motion))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = Icons.Filled.Animation,
                title = stringResource(R.string.perf_boost_reduce_motion_title),
                summary = stringResource(R.string.perf_boost_reduce_motion_summary),
                colors = colors,
                onClick = onReduceMotionClick,
                trailing = { ClintSwitch(checked = state.reduceMotion) }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = Icons.Filled.Gif,
                title = stringResource(R.string.perf_boost_stop_animated_images_title),
                summary = stringResource(R.string.perf_boost_stop_animated_images_summary),
                colors = colors,
                onClick = onStopAnimatedImagesClick,
                trailing = { ClintSwitch(checked = state.stopAnimatedImages) }
            )
        }
    }
}

@Composable
private fun PerformanceIntroCard() {
    val colors = LocalClintColors.current
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(lerp(colors.cardBackground, colors.primary, 0.12f))
            .padding(16.dp)
    ) {
        Icon(
            Icons.Filled.Info,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(22.dp)
        )
        Text(
            stringResource(R.string.perf_boost_intro),
            color = colors.onSurface,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            modifier = Modifier.padding(start = 14.dp)
        )
    }
}
