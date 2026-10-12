package com.jhaiian.clint.ui
import androidx.compose.material.icons.filled.Check

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
fun ClintSwitch(checked: Boolean, modifier: Modifier = Modifier) {
    val colors = LocalClintColors.current
    Switch(
        checked = checked,
        onCheckedChange = null,
        modifier = modifier,
        thumbContent = if (checked) {
            {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize)
                )
            }
        } else null,
        colors = SwitchDefaults.colors(
            checkedThumbColor = colors.onSurface,
            checkedTrackColor = colors.primary,
            checkedBorderColor = Color.Transparent,
            checkedIconColor = colors.primary,
            uncheckedThumbColor = colors.secondaryText,
            uncheckedTrackColor = colors.surfaceVariant,
            uncheckedBorderColor = colors.divider,
            uncheckedIconColor = colors.surfaceVariant,
            disabledCheckedThumbColor = colors.onSurface.copy(alpha = 0.38f),
            disabledCheckedTrackColor = colors.primary.copy(alpha = 0.38f),
            disabledCheckedBorderColor = Color.Transparent,
            disabledCheckedIconColor = colors.primary.copy(alpha = 0.38f),
            disabledUncheckedThumbColor = colors.secondaryText.copy(alpha = 0.38f),
            disabledUncheckedTrackColor = colors.surfaceVariant.copy(alpha = 0.38f),
            disabledUncheckedBorderColor = colors.divider.copy(alpha = 0.38f),
            disabledUncheckedIconColor = colors.surfaceVariant.copy(alpha = 0.38f)
        )
    )
}
