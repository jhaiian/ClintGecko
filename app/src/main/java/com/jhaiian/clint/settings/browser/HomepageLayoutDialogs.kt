package com.jhaiian.clint.settings.browser

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.settings.common.SettingsPickerOptionBottomSpacing
import com.jhaiian.clint.settings.common.SettingsPickerOptionContentPadding
import com.jhaiian.clint.setup.SelectableCard
import com.jhaiian.clint.ui.ClintCheckbox
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.ClintOutlinedTextField
import com.jhaiian.clint.ui.ClintRadioButton
import com.jhaiian.clint.ui.ClintSlider
import com.jhaiian.clint.ui.theme.LocalClintColors

private const val TITLE_MAX_LENGTH = 24

@Composable
private fun HomepageDialogFooter(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val colors = LocalClintColors.current
    Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onDismiss) {
            Text(stringResource(android.R.string.cancel), color = colors.primary, fontWeight = FontWeight.Medium)
        }
        TextButton(onClick = onConfirm) {
            Text(stringResource(android.R.string.ok), color = colors.primary, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun HomepageChoiceDialog(
    title: String,
    options: List<Pair<String, String>>,
    current: String,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    var selected by remember(current) { mutableStateOf(current) }

    ClintDialog(
        title = title,
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = { HomepageDialogFooter(onDismiss) { onConfirm(selected) } }
    ) {
        options.forEach { (key, label) ->
            val sel = selected == key
            SelectableCard(
                selected = sel, onClick = { selected = key },
                cardBackground = colors.surfaceVariant, primary = colors.primary,
                contentPadding = SettingsPickerOptionContentPadding, bottomSpacing = SettingsPickerOptionBottomSpacing
            ) {
                ClintRadioButton(selected = sel)
                Text(
                    label, color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)
                )
            }
        }
    }
}

@Composable
fun HomepageOrderDialog(
    title: String,
    keys: List<String>,
    enabled: Set<String>?,
    label: @Composable (String) -> String,
    icon: (String) -> ImageVector?,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onConfirm: (List<String>, Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    var order by remember(keys) { mutableStateOf(keys) }
    var checked by remember(enabled) { mutableStateOf(enabled ?: keys.toSet()) }

    fun move(index: Int, delta: Int) {
        val target = index + delta
        if (target !in order.indices) return
        order = order.toMutableList().apply { add(target, removeAt(index)) }
    }

    ClintDialog(
        title = title,
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = { HomepageDialogFooter(onDismiss) { onConfirm(order, checked) } }
    ) {
        order.forEachIndexed { index, key ->
            val isChecked = key in checked
            SelectableCard(
                selected = enabled != null && isChecked,
                onClick = { if (enabled != null) checked = if (isChecked) checked - key else checked + key },
                cardBackground = colors.surfaceVariant, primary = colors.primary,
                contentPadding = SettingsPickerOptionContentPadding, bottomSpacing = SettingsPickerOptionBottomSpacing
            ) {
                if (enabled != null) {
                    ClintCheckbox(
                        checked = isChecked,
                        onCheckedChange = { checked = if (isChecked) checked - key else checked + key }
                    )
                }
                icon(key)?.let {
                    Icon(imageVector = it, contentDescription = null, tint = colors.primary, modifier = Modifier.size(22.dp))
                }
                Text(
                    label(key), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f).padding(start = 12.dp, end = 4.dp)
                )
                IconButton(onClick = { move(index, -1) }, enabled = index > 0, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null, tint = colors.iconTint)
                }
                IconButton(onClick = { move(index, 1) }, enabled = index < order.lastIndex, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = colors.iconTint)
                }
            }
        }
    }
}

@Composable
fun HomepageSliderDialog(
    title: String,
    value: Int,
    range: IntRange,
    step: Int,
    valueLabel: @Composable (Int) -> String,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    var current by remember(value) { mutableIntStateOf(value.coerceIn(range.first, range.last)) }

    ClintDialog(
        title = title,
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = { HomepageDialogFooter(onDismiss) { onConfirm(current) } }
    ) {
        Text(
            valueLabel(current), color = colors.onSurface, fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )
        ClintSlider(
            value = current.toFloat(),
            onValueChange = { raw ->
                current = (Math.round(raw / step) * step).coerceIn(range.first, range.last)
            },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = ((range.last - range.first) / step - 1).coerceAtLeast(0),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun HomepageConfirmDialog(
    title: String,
    message: String,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    ClintDialog(
        title = title,
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = { HomepageDialogFooter(onDismiss, onConfirm) }
    ) {
        Text(message, color = colors.secondaryText, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp))
    }
}

@Composable
fun HomepageTitleDialog(
    current: String,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    var text by remember(current) { mutableStateOf(current) }

    ClintDialog(
        title = stringResource(R.string.homepage_title_text_title),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = { HomepageDialogFooter(onDismiss) { onConfirm(text.trim()) } }
    ) {
        ClintOutlinedTextField(
            value = text,
            onValueChange = { text = it.take(TITLE_MAX_LENGTH) },
            label = { Text(stringResource(R.string.homepage_title_text_hint)) },
            singleLine = true,
            supportingText = {
                Text(stringResource(R.string.homepage_title_text_helper), color = colors.secondaryText, fontSize = 12.sp)
            },
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 8.dp)
        )
    }
}
