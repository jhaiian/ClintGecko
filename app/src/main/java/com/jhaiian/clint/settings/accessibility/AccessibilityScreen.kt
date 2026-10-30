package com.jhaiian.clint.settings.accessibility
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ZoomIn

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.ConfigurationCompat
import com.jhaiian.clint.R
import com.jhaiian.clint.settings.common.RowDivider
import com.jhaiian.clint.settings.common.SettingsIconTile
import com.jhaiian.clint.settings.common.SettingsRow
import com.jhaiian.clint.settings.common.SettingsScreenScaffold
import com.jhaiian.clint.settings.common.SettingsSection
import com.jhaiian.clint.settings.common.SettingsSectionLabel
import com.jhaiian.clint.settings.common.settingsSearchHighlight
import com.jhaiian.clint.ui.ClintSlider
import com.jhaiian.clint.ui.ClintSwitch
import com.jhaiian.clint.ui.theme.LocalClintColors
import java.text.NumberFormat
import java.util.Locale

private const val SAMPLE_TEXT_SIZE = 16f
private const val SAMPLE_LINE_HEIGHT = 24f

@Composable
fun AccessibilityScreen(
    state: AccessibilityUiState,
    onAutoFontSizeClick: () -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onFontSizeChangeFinished: () -> Unit,
    onForceZoomClick: () -> Unit
) {
    val colors = LocalClintColors.current

    SettingsScreenScaffold {
        SettingsSectionLabel(stringResource(R.string.accessibility_section_text))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.TextFields,
                title = stringResource(R.string.accessibility_auto_font_size_title),
                summary = stringResource(R.string.accessibility_auto_font_size_summary),
                colors = colors,
                onClick = onAutoFontSizeClick,
                trailing = {
                    ClintSwitch(checked = state.autoFontSize)
                }
            )
            RowDivider(colors.divider)
            FontSizeBlock(
                percent = state.fontSizePercent,
                enabled = !state.autoFontSize,
                onValueChange = onFontSizeChange,
                onValueChangeFinished = onFontSizeChangeFinished
            )
        }
        SettingsSectionLabel(stringResource(R.string.accessibility_section_zoom))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.ZoomIn,
                title = stringResource(R.string.accessibility_force_zoom_title),
                summary = stringResource(R.string.accessibility_force_zoom_summary),
                colors = colors,
                onClick = onForceZoomClick,
                trailing = {
                    ClintSwitch(checked = state.forceZoom)
                }
            )
        }
    }
}

@Composable
private fun FontSizeBlock(
    percent: Int,
    enabled: Boolean,
    onValueChange: (Int) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    val colors = LocalClintColors.current
    val title = stringResource(R.string.accessibility_font_size_title)
    val configuration = LocalConfiguration.current
    val locale = remember(configuration) { ConfigurationCompat.getLocales(configuration)[0] ?: Locale.getDefault() }
    val percentLabel = remember(percent, locale) { NumberFormat.getPercentInstance(locale).format(percent / 100.0) }
    val fontScale = LocalDensity.current.fontScale
    val sampleScale = if (enabled) percent / 100f / fontScale else 1f
    val stepCount = (AccessibilityPrefs.MAX_FONT_SIZE_PERCENT - AccessibilityPrefs.MIN_FONT_SIZE_PERCENT) /
        AccessibilityPrefs.FONT_SIZE_STEP_PERCENT - 1

    Column(
        Modifier
            .fillMaxWidth()
            .settingsSearchHighlight(title, colors.primary)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 44.dp).alpha(if (enabled) 1f else 0.45f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsIconTile(androidx.compose.material.icons.Icons.Filled.FormatSize, colors)
            Column(Modifier.weight(1f).padding(start = 16.dp, end = 12.dp)) {
                Text(title, color = colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium, lineHeight = 21.sp)
                Text(
                    stringResource(R.string.accessibility_font_size_summary),
                    color = colors.secondaryText,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ClintSlider(
                value = percent.toFloat(),
                onValueChange = { value -> onValueChange(AccessibilityPrefs.sanitizePercent(Math.round(value))) },
                onValueChangeFinished = onValueChangeFinished,
                enabled = enabled,
                valueRange = AccessibilityPrefs.MIN_FONT_SIZE_PERCENT.toFloat()..AccessibilityPrefs.MAX_FONT_SIZE_PERCENT.toFloat(),
                steps = stepCount,
                modifier = Modifier.weight(1f).semantics { contentDescription = title }
            )
            Text(
                percentLabel,
                color = colors.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .widthIn(min = 64.dp)
                    .padding(start = 12.dp)
                    .alpha(if (enabled) 1f else 0.45f)
            )
        }
        Text(
            stringResource(R.string.accessibility_font_size_sample),
            color = colors.onSurface,
            fontSize = (SAMPLE_TEXT_SIZE * sampleScale).sp,
            lineHeight = (SAMPLE_LINE_HEIGHT * sampleScale).sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(lerp(colors.cardBackground, colors.primary, 0.16f))
                .alpha(if (enabled) 1f else 0.6f)
                .padding(horizontal = 20.dp, vertical = 18.dp)
        )
    }
}
