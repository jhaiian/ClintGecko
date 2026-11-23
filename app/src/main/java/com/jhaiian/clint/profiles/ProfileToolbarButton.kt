package com.jhaiian.clint.profiles

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.MainActivity

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ProfileToolbarButton(activity: MainActivity) {
    val revision = activity.uiState.profileRevision
    val haptic = LocalHapticFeedback.current
    val profiles = remember(revision) { ProfileRepository.all(activity) }
    if (profiles.isEmpty()) return
    val activeId = remember(revision) { activity.tabManager.activeProfileId }
    val defaultName = stringResource(R.string.profiles_default_name)
    val active = profiles.firstOrNull { it.id == activeId }
    val name = active?.name ?: defaultName
    val color = active?.color ?: ProfileRepository.defaultColor
    val ids = listOf(WebProfiles.DEFAULT_ID) + profiles.map { it.id }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(start = 10.dp)
            .size(34.dp)
            .clip(CircleShape)
            .combinedClickable(
                onClick = { activity.uiState.profilePickerOpen = true },
                onLongClickLabel = stringResource(R.string.profiles_switch_next),
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val next = ids[(ids.indexOf(activeId).coerceAtLeast(0) + 1) % ids.size]
                    activity.onSwitchProfile(next)
                }
            )
    ) {
        ProfileAvatar(name, color, 28)
    }
}
