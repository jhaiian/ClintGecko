package com.jhaiian.clint.browser.dialogs

import android.graphics.Bitmap
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.browser.delegates.activeProfileId
import com.jhaiian.clint.browser.delegates.createHomeScreenShortcut
import com.jhaiian.clint.profiles.ShortcutProfileField
import com.jhaiian.clint.profiles.shortcutProfileFieldVisible
import androidx.compose.ui.platform.LocalContext
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.ClintOutlinedTextField
import com.jhaiian.clint.ui.ClintSwitch
import com.jhaiian.clint.ui.rememberClintFavicon
import com.jhaiian.clint.ui.theme.LocalClintColors

data class CreateShortcutRequest(
    val pageUrl: String,
    val initialName: String
)

@Composable
internal fun CreateShortcutDialog(
    request: CreateShortcutRequest,
    activity: MainActivity,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    var name by remember(request) { mutableStateOf(request.initialName) }
    var frameless by remember(request) { mutableStateOf(true) }
    var profileId by remember(request) { mutableStateOf(activity.activeProfileId()) }
    val context = LocalContext.current
    var customIcon by remember(request) { mutableStateOf<Bitmap?>(null) }
    val faviconBitmap = rememberClintFavicon(pageUrl = request.pageUrl)
    val displayedIcon = customIcon ?: faviconBitmap

    DisposableEffect(Unit) {
        onDispose { activity.pendingShortcutIconCallback = null }
    }

    ClintDialog(
        title = stringResource(R.string.create_shortcut_dialog_title),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                TextButton(
                    onClick = {
                        val finalName = name.trim().ifEmpty { request.initialName }
                        onDismiss()
                        activity.createHomeScreenShortcut(request.pageUrl, finalName, displayedIcon, frameless, profileId)
                    },
                    enabled = name.isNotBlank()
                ) {
                    Text(stringResource(R.string.action_add), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceVariant)
                    .clickable {
                        activity.pendingShortcutIconCallback = { picked -> if (picked != null) customIcon = picked }
                        activity.shortcutIconPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (displayedIcon != null) {
                    Image(
                        bitmap = displayedIcon.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(56.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Filled.Public, contentDescription = null, tint = colors.iconTint)
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(colors.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.create_shortcut_change_icon),
                        tint = colors.onPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            ClintOutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.weight(1f).padding(start = 16.dp),
                label = { Text(stringResource(R.string.create_shortcut_name_hint)) },
                singleLine = true
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { frameless = !frameless }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text(stringResource(R.string.frameless_shortcut_title), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(
                    stringResource(R.string.frameless_shortcut_summary),
                    color = colors.secondaryText,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            ClintSwitch(checked = frameless)
        }
        if (shortcutProfileFieldVisible(context, profileId)) {
            ShortcutProfileField(profileId, hideStatusBar, hideSystemNavigation) { profileId = it }
        }
    }
}
