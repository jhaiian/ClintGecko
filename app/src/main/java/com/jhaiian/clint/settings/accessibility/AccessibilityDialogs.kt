package com.jhaiian.clint.settings.accessibility

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.settings.common.SettingsPickerOptionBottomSpacing
import com.jhaiian.clint.settings.common.SettingsPickerOptionContentPadding
import com.jhaiian.clint.setup.SelectableCard
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.ClintRadioButton
import com.jhaiian.clint.ui.theme.LocalClintColors

@StringRes
fun colorFilterLabelRes(value: String): Int = when (value) {
    AccessibilityPrefs.COLOR_FILTER_GRAYSCALE -> R.string.accessibility_color_filter_grayscale
    AccessibilityPrefs.COLOR_FILTER_SEPIA -> R.string.accessibility_color_filter_sepia
    AccessibilityPrefs.COLOR_FILTER_BLACK_WHITE -> R.string.accessibility_color_filter_black_white
    AccessibilityPrefs.COLOR_FILTER_MONOCHROME -> R.string.accessibility_color_filter_monochrome
    else -> R.string.accessibility_color_filter_off
}

@StringRes
private fun colorFilterDescriptionRes(value: String): Int = when (value) {
    AccessibilityPrefs.COLOR_FILTER_GRAYSCALE -> R.string.accessibility_color_filter_grayscale_desc
    AccessibilityPrefs.COLOR_FILTER_SEPIA -> R.string.accessibility_color_filter_sepia_desc
    AccessibilityPrefs.COLOR_FILTER_BLACK_WHITE -> R.string.accessibility_color_filter_black_white_desc
    AccessibilityPrefs.COLOR_FILTER_MONOCHROME -> R.string.accessibility_color_filter_monochrome_desc
    else -> R.string.accessibility_color_filter_off_desc
}

@Composable
fun WebsiteColorFilterDialog(
    current: String,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    ClintDialog(
        title = stringResource(R.string.accessibility_color_filter_title),
        hideStatusBar = hideStatusBar,
        hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss
    ) {
        AccessibilityPrefs.COLOR_FILTERS.forEach { value ->
            val selected = value == current
            SelectableCard(
                selected = selected,
                onClick = { onSelect(value) },
                cardBackground = colors.surfaceVariant,
                primary = colors.primary,
                contentPadding = SettingsPickerOptionContentPadding,
                bottomSpacing = SettingsPickerOptionBottomSpacing
            ) {
                ClintRadioButton(selected = selected)
                Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
                    Text(
                        stringResource(colorFilterLabelRes(value)),
                        color = colors.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        stringResource(colorFilterDescriptionRes(value)),
                        color = colors.secondaryText,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
