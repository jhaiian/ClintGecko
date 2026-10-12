package com.jhaiian.clint.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.CustomSearchSuggestionsApiQueryPlaceholder
import com.jhaiian.clint.browser.isValidCustomSearchSuggestionsApiUrl
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
fun CustomSearchSuggestionsApiDialog(
    initialName: String,
    initialUrl: String,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onConfirm: (name: String, url: String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    var name by remember { mutableStateOf(initialName) }
    var url by remember { mutableStateOf(initialUrl) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var urlError by remember { mutableStateOf<String?>(null) }
    val requiredMessage = stringResource(R.string.custom_search_suggestions_api_name_required)
    val invalidUrlMessage = stringResource(R.string.custom_search_suggestions_api_url_error, CustomSearchSuggestionsApiQueryPlaceholder)

    ClintDialog(
        title = stringResource(R.string.custom_search_suggestions_api_dialog_title),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                TextButton(
                    onClick = {
                        val trimmedUrl = url.trim()
                        val trimmedName = name.trim()
                        nameError = if (trimmedName.isBlank()) requiredMessage else null
                        urlError = if (!isValidCustomSearchSuggestionsApiUrl(trimmedUrl)) invalidUrlMessage else null
                        if (nameError == null && urlError == null) {
                            onConfirm(trimmedName, trimmedUrl)
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
                label = { Text(stringResource(R.string.custom_search_suggestions_api_name_hint)) },
                singleLine = true,
                isError = nameError != null,
                supportingText = nameError?.let { error ->
                    { Text(error, color = colors.colorError, fontSize = 12.sp) }
                },
                modifier = Modifier.fillMaxWidth()
            )
            ClintOutlinedTextField(
                value = url,
                onValueChange = { url = it; urlError = null },
                label = { Text(stringResource(R.string.custom_search_suggestions_api_url_hint)) },
                singleLine = true,
                isError = urlError != null,
                supportingText = {
                    Column {
                        Text(
                            urlError ?: stringResource(R.string.custom_search_suggestions_api_url_helper, CustomSearchSuggestionsApiQueryPlaceholder),
                            color = if (urlError != null) colors.colorError else colors.secondaryText,
                            fontSize = 12.sp
                        )
                        Text(
                            stringResource(R.string.custom_search_suggestions_api_url_example, CustomSearchSuggestionsApiQueryPlaceholder),
                            color = colors.secondaryText,
                            fontSize = 12.sp
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            )
        }
    }
}
