package com.jhaiian.clint.browser.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.ClintCheckbox
import com.jhaiian.clint.ui.ClintTitlelessDialog
import com.jhaiian.clint.ui.ClintDialogCancelFooter
import com.jhaiian.clint.ui.ClintRadioButton
import com.jhaiian.clint.ui.theme.LocalClintColors

data class SelectPickerOption(
    val value: String,
    val label: String,
    val selected: Boolean,
    val disabled: Boolean,
    val group: String?
)

data class SelectPickerRequest(
    val id: String,
    val title: String,
    val options: List<SelectPickerOption>,
    val multiple: Boolean,
    val webView: java.lang.ref.WeakReference<com.jhaiian.clint.browser.webview.ClintWebView>,
    val onApply: ((List<String>) -> Unit)? = null,
    val onCancel: (() -> Unit)? = null
)

@Composable
internal fun SelectPickerDialog(request: SelectPickerRequest, hideStatusBar: Boolean, hideSystemNavigation: Boolean, onDismiss: () -> Unit) {
    val colors = LocalClintColors.current
    val selectedValues = remember(request.id) {
        mutableStateListOf<String>().apply { addAll(request.options.filter { it.selected }.map { it.value }) }
    }
    val scrollState = rememberScrollState()
    var selectedRowOffset by remember(request.id) { mutableStateOf<Int?>(null) }

    LaunchedEffect(selectedRowOffset) {
        selectedRowOffset?.let { offset -> scrollState.scrollTo(offset.coerceIn(0, scrollState.maxValue)) }
    }

    fun applyValues(values: List<String>) {
        request.onApply?.let {
            it(values)
            return
        }
        val webView = request.webView.get() ?: return
        val safeId = request.id.replace("'", "")
        val json = org.json.JSONArray(values).toString()
        val quotedJson = org.json.JSONObject.quote(json)
        webView.evaluateJavascript("window.__clintApplySelect && window.__clintApplySelect('$safeId', $quotedJson)", null)
    }

    ClintTitlelessDialog(
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        scrollState = scrollState,
        footer = {
            if (request.multiple) {
                Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.action_cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                    }
                    TextButton(onClick = { applyValues(selectedValues.toList()); onDismiss() }) {
                        Text(stringResource(android.R.string.ok), color = colors.primary, fontWeight = FontWeight.Medium)
                    }
                }
            } else {
                ClintDialogCancelFooter(onDismiss)
            }
        }
    ) {
        var lastGroup: String? = null
        request.options.forEach { option ->
            if (option.group != lastGroup) {
                lastGroup = option.group
                option.group?.let { SelectPickerGroupHeader(it) }
            }
            val isSelected = selectedValues.contains(option.value)
            val rowModifier = if (option.selected && selectedRowOffset == null) {
                Modifier.onGloballyPositioned { coordinates -> selectedRowOffset = coordinates.positionInParent().y.roundToInt() }
            } else Modifier
            SelectPickerOptionRow(option, isSelected, request.multiple, rowModifier) {
                if (option.disabled) return@SelectPickerOptionRow
                if (request.multiple) {
                    if (selectedValues.contains(option.value)) selectedValues.remove(option.value) else selectedValues.add(option.value)
                } else {
                    applyValues(listOf(option.value))
                    onDismiss()
                }
            }
        }
    }
}

@Composable
private fun SelectPickerGroupHeader(label: String) {
    val colors = LocalClintColors.current
    Text(
        label,
        color = colors.secondaryText,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 14.dp, bottom = 4.dp)
    )
}

@Composable
private fun SelectPickerOptionRow(option: SelectPickerOption, selected: Boolean, multiple: Boolean, modifier: Modifier = Modifier, onToggle: () -> Unit) {
    val colors = LocalClintColors.current
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(if (!option.disabled) Modifier.clickable(onClick = onToggle) else Modifier)
            .background(if (selected) colors.primary.copy(alpha = 0.10f) else Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .alpha(if (option.disabled) 0.4f else 1f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (multiple) {
            ClintCheckbox(checked = selected, onCheckedChange = { if (!option.disabled) onToggle() })
        } else {
            ClintRadioButton(selected = selected)
        }
        Text(
            option.label,
            color = colors.onSurface,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f).padding(start = 12.dp)
        )
    }
}
