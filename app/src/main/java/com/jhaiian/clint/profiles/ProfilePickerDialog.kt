package com.jhaiian.clint.profiles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.ClintDialogActionFooter
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
fun ProfilePickerDialog(
    profiles: List<BrowserProfile>,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onPick: (String) -> Unit,
    onManage: () -> Unit,
    onDismiss: () -> Unit,
    title: String = stringResource(R.string.profiles_picker_title)
) {
    ClintDialog(
        title = title,
        hideStatusBar = hideStatusBar,
        hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            ClintDialogActionFooter(
                onCancel = onDismiss,
                positiveLabel = stringResource(R.string.profiles_picker_manage),
                onPositive = onManage
            )
        }
    ) {
        PickerRow(stringResource(R.string.profiles_default_name), ProfileRepository.defaultColor) { onPick(WebProfiles.DEFAULT_ID) }
        profiles.forEach { profile ->
            PickerRow(profile.name, profile.color) { onPick(profile.id) }
        }
    }
}

@Composable
private fun PickerRow(name: String, color: Int, onClick: () -> Unit) {
    val colors = LocalClintColors.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProfileAvatar(name, color, 36)
        Text(
            name,
            color = colors.onSurface,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 14.dp)
        )
    }
}
