package com.jhaiian.clint.settings.shortcutmanager

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.jhaiian.clint.profiles.ShortcutProfileField
import com.jhaiian.clint.profiles.shortcutProfileFieldVisible
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.settings.common.SettingsRow
import com.jhaiian.clint.settings.common.SettingsSection
import com.jhaiian.clint.shortcuts.ShortcutIconStore
import com.jhaiian.clint.ui.AdaptiveWidthContainer
import com.jhaiian.clint.ui.ClintOutlinedTextField
import com.jhaiian.clint.ui.ClintSwitch
import com.jhaiian.clint.ui.theme.LocalClintColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ShortcutSettingsScreen(
    state: ShortcutSettingsUiState,
    maxContentWidth: Dp?,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onExit: () -> Unit,
    onPickIcon: () -> Unit,
    onSave: () -> Unit
) {
    val colors = LocalClintColors.current
    val context = LocalContext.current
    val storedIcon by produceState<Bitmap?>(initialValue = null, state.shortcut.iconPath) {
        value = withContext(Dispatchers.IO) { ShortcutIconStore.load(state.shortcut.iconPath) }
    }
    val displayedIcon = state.customIcon ?: storedIcon

    Surface(color = colors.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Surface(color = colors.surface, shadowElevation = 4.dp, modifier = Modifier.statusBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().height(56.dp).padding(start = 4.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onExit) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = colors.onSurface)
                    }
                    Text(
                        text = stringResource(R.string.shortcut_manager_settings_title),
                        color = colors.onSurface,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(start = 4.dp)
                    )
                    TextButton(onClick = onSave, enabled = state.canSave) {
                        Text(
                            text = stringResource(R.string.action_save),
                            color = if (state.canSave) colors.primary else colors.secondaryText,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            HorizontalDivider(color = colors.divider, thickness = 1.dp)

            Box(Modifier.weight(1f).fillMaxWidth()) {
                AdaptiveWidthContainer(maxContentWidth) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .imePadding()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        SettingsSection(colors.cardBackground, bottomSpacing = 12.dp) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(colors.surfaceVariant)
                                        .clickable(onClick = onPickIcon),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (displayedIcon != null) {
                                        Image(
                                            bitmap = displayedIcon.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.size(64.dp).clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(Icons.Filled.Public, contentDescription = null, tint = colors.iconTint)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(colors.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.Edit,
                                            contentDescription = stringResource(R.string.create_shortcut_change_icon),
                                            tint = colors.onPrimary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                                ClintOutlinedTextField(
                                    value = state.name,
                                    onValueChange = { state.name = it },
                                    modifier = Modifier.weight(1f).padding(start = 16.dp),
                                    label = { Text(stringResource(R.string.create_shortcut_name_hint)) },
                                    singleLine = true,
                                    isError = state.name.isBlank()
                                )
                            }
                            ClintOutlinedTextField(
                                value = state.url,
                                onValueChange = { state.url = it },
                                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                                label = { Text(stringResource(R.string.shortcut_manager_url_hint)) },
                                singleLine = true,
                                isError = !state.isUrlValid,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                                supportingText = if (!state.isUrlValid) {
                                    { Text(stringResource(R.string.shortcut_manager_url_invalid), color = colors.colorError) }
                                } else null
                            )
                        }
                        SettingsSection(colors.cardBackground, bottomSpacing = 12.dp) {
                            SettingsRow(
                                icon = Icons.AutoMirrored.Filled.OpenInNew,
                                title = stringResource(R.string.frameless_shortcut_title),
                                summary = stringResource(R.string.frameless_shortcut_summary),
                                colors = colors,
                                onClick = { state.frameless = !state.frameless },
                                trailing = { ClintSwitch(checked = state.frameless) }
                            )
                        }
                        if (shortcutProfileFieldVisible(context, state.profileId)) {
                            SettingsSection(colors.cardBackground, bottomSpacing = 12.dp) {
                                ShortcutProfileField(state.profileId, hideStatusBar, hideSystemNavigation) { state.profileId = it }
                            }
                        }
                        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                    }
                }
            }
        }
    }
}
