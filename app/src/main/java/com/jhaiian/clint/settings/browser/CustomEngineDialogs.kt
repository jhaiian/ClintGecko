package com.jhaiian.clint.settings.browser

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.CustomSearchEngine
import com.jhaiian.clint.browser.CustomSearchEngineQueryPlaceholder
import com.jhaiian.clint.browser.CustomSearchSuggestionsApiQueryPlaceholder
import com.jhaiian.clint.browser.isValidCustomSearchEngineUrl
import com.jhaiian.clint.browser.isValidCustomSearchSuggestionsApiUrl
import com.jhaiian.clint.browser.newCustomSearchEngineId
import com.jhaiian.clint.settings.common.SettingsPickerOptionBottomSpacing
import com.jhaiian.clint.settings.common.SettingsPickerOptionContentPadding
import com.jhaiian.clint.setup.SelectableCard
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.ClintOutlinedTextField
import com.jhaiian.clint.ui.ClintRadioButton
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
internal fun CustomEngineOptions(
    engines: List<CustomSearchEngine>,
    selected: String,
    forSuggestions: Boolean,
    onSelect: (String) -> Unit,
    onEdit: (CustomSearchEngine) -> Unit,
    onDelete: (CustomSearchEngine) -> Unit,
    onAdd: () -> Unit
) {
    val colors = LocalClintColors.current
    engines.forEach { engine ->
        val sel = selected == engine.key
        SelectableCard(
            selected = sel, onClick = { onSelect(engine.key) },
            cardBackground = colors.surfaceVariant, primary = colors.primary,
            contentPadding = SettingsPickerOptionContentPadding, bottomSpacing = SettingsPickerOptionBottomSpacing
        ) {
            ClintRadioButton(selected = sel)
            Column(Modifier.weight(1f).padding(start = 12.dp, end = 4.dp)) {
                Text(engine.name, color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    engine.description.ifBlank { if (forSuggestions) engine.suggestUrl else engine.searchUrl },
                    color = colors.secondaryText, fontSize = 13.sp, maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            IconButton(onClick = { onEdit(engine) }) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_edit), tint = colors.iconTint)
            }
            IconButton(onClick = { onDelete(engine) }) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = stringResource(R.string.action_delete), tint = colors.colorError)
            }
        }
    }
    SelectableCard(
        selected = false, onClick = onAdd,
        cardBackground = colors.surfaceVariant, primary = colors.primary,
        contentPadding = SettingsPickerOptionContentPadding, bottomSpacing = SettingsPickerOptionBottomSpacing
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = colors.primary, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
            Text(
                stringResource(if (forSuggestions) R.string.custom_engine_add_suggestions else R.string.custom_engine_add),
                color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium
            )
            Text(
                stringResource(if (forSuggestions) R.string.suggestions_api_custom_desc else R.string.engine_custom_desc),
                color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
internal fun CustomEngineEditorDialog(
    initial: CustomSearchEngine?,
    forSuggestions: Boolean,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onSave: (CustomSearchEngine) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    val placeholder = if (forSuggestions) CustomSearchSuggestionsApiQueryPlaceholder else CustomSearchEngineQueryPlaceholder
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var description by remember { mutableStateOf(initial?.description.orEmpty()) }
    var url by remember { mutableStateOf((if (forSuggestions) initial?.suggestUrl else initial?.searchUrl).orEmpty()) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var urlError by remember { mutableStateOf<String?>(null) }
    val requiredMessage = stringResource(
        if (forSuggestions) R.string.custom_search_suggestions_api_name_required else R.string.custom_search_engine_name_required
    )
    val urlErrorMessage = stringResource(
        if (forSuggestions) R.string.custom_search_suggestions_api_url_error else R.string.custom_search_engine_url_error,
        placeholder
    )

    ClintDialog(
        title = stringResource(
            if (forSuggestions) R.string.custom_search_suggestions_api_dialog_title else R.string.custom_search_engine_dialog_title
        ),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                TextButton(
                    onClick = {
                        val trimmedName = name.trim()
                        val trimmedUrl = url.trim()
                        val validUrl = if (forSuggestions) isValidCustomSearchSuggestionsApiUrl(trimmedUrl) else isValidCustomSearchEngineUrl(trimmedUrl)
                        nameError = if (trimmedName.isBlank()) requiredMessage else null
                        urlError = if (!validUrl) urlErrorMessage else null
                        if (nameError == null && urlError == null) {
                            onSave(
                                CustomSearchEngine(
                                    id = initial?.id ?: newCustomSearchEngineId(),
                                    name = trimmedName,
                                    searchUrl = if (forSuggestions) initial?.searchUrl.orEmpty() else trimmedUrl,
                                    suggestUrl = if (forSuggestions) trimmedUrl else initial?.suggestUrl.orEmpty(),
                                    description = description.trim()
                                )
                            )
                        }
                    }
                ) {
                    Text(stringResource(R.string.action_save), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            ClintOutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = null },
                label = {
                    Text(stringResource(if (forSuggestions) R.string.custom_search_suggestions_api_name_hint else R.string.custom_search_engine_name_hint))
                },
                singleLine = true,
                isError = nameError != null,
                supportingText = nameError?.let { error ->
                    { Text(error, color = colors.colorError, fontSize = 12.sp) }
                },
                modifier = Modifier.fillMaxWidth()
            )
            ClintOutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.custom_engine_optional_label, stringResource(R.string.custom_engine_description_hint))) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            )
            CustomEngineUrlField(
                value = url,
                onValueChange = { url = it; urlError = null },
                label = stringResource(if (forSuggestions) R.string.custom_search_suggestions_api_url_hint else R.string.custom_search_engine_url_hint),
                error = urlError,
                example = if (forSuggestions) {
                    "https://duckduckgo.com/ac/?q=$CustomSearchSuggestionsApiQueryPlaceholder&type=list"
                } else {
                    "https://www.google.com/search?q=$CustomSearchEngineQueryPlaceholder"
                }
            )
            Text(
                stringResource(
                    if (forSuggestions) R.string.custom_search_suggestions_api_url_helper else R.string.custom_search_engine_url_helper,
                    placeholder
                ),
                color = colors.secondaryText,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp)
            )
        }
    }
}

@Composable
private fun CustomEngineUrlField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    example: String
) {
    val colors = LocalClintColors.current
    ClintOutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = error != null,
        placeholder = { Text(example, color = colors.secondaryText, maxLines = 1) },
        supportingText = error?.let { message ->
            { Text(message, color = colors.colorError, fontSize = 12.sp) }
        },
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
    )
}
