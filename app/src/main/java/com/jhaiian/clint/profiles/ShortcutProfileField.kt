package com.jhaiian.clint.profiles

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.theme.LocalClintColors

fun shortcutProfileFieldVisible(context: Context, profileId: String): Boolean =
    WebProfiles.isSupported() && (profileId != WebProfiles.DEFAULT_ID || ProfileRepository.all(context).isNotEmpty())

@Composable
fun ShortcutProfileField(
    profileId: String,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onPick: (String) -> Unit
) {
    val context = LocalContext.current
    val colors = LocalClintColors.current
    var showPicker by remember { mutableStateOf(false) }
    val profiles = remember(showPicker) { ProfileRepository.all(context) }
    val current = profiles.firstOrNull { it.id == profileId }
    val name = current?.name ?: stringResource(R.string.profiles_default_name)
    val color = current?.color ?: ProfileRepository.defaultColor

    Row(
        Modifier.fillMaxWidth().clickable { showPicker = true }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(stringResource(R.string.shortcut_profile_title), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(
                stringResource(R.string.shortcut_profile_summary),
                color = colors.secondaryText,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        ProfileAvatar(name, color, 28)
        Text(
            name,
            color = colors.onSurface,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 96.dp).padding(start = 8.dp)
        )
    }

    if (showPicker) {
        ProfilePickerDialog(
            profiles = profiles,
            hideStatusBar = hideStatusBar,
            hideSystemNavigation = hideSystemNavigation,
            onPick = { id ->
                showPicker = false
                onPick(id)
            },
            onManage = {
                showPicker = false
                context.startActivity(Intent(context, ProfilesActivity::class.java))
            },
            onDismiss = { showPicker = false },
            title = stringResource(R.string.profiles_picker_shortcut_title)
        )
    }
}
