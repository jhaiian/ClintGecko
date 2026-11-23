package com.jhaiian.clint.tabs

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.browser.delegates.reopenClosedTab
import com.jhaiian.clint.ui.ClintDialogStatusBarEffect
import com.jhaiian.clint.ui.ClintSheetWindowInsets
import com.jhaiian.clint.ui.rememberClintFavicon
import com.jhaiian.clint.ui.rememberMaxContentWidth
import com.jhaiian.clint.ui.rememberSheetFlingConnection
import com.jhaiian.clint.ui.theme.LocalClintColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecentlyClosedSheet(activity: MainActivity, onDismiss: () -> Unit, onReopened: () -> Unit) {
    val colors = LocalClintColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val flingBoundaryConnection = rememberSheetFlingConnection(sheetState)
    val hideStatusBar = remember { PreferenceManager.getDefaultSharedPreferences(activity).getBoolean("hide_status_bar", false) }
    val hideSystemNavigation = remember { PreferenceManager.getDefaultSharedPreferences(activity).getBoolean("hide_system_navigation", false) }
    var entries by remember { mutableStateOf<List<ClosedTab>?>(null) }
    val maxContentWidth = rememberMaxContentWidth(activity)
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val maxSheetHeight = if (isPortrait) {
        (configuration.screenHeightDp.dp * 0.8f).coerceAtLeast(320.dp)
    } else {
        (configuration.screenHeightDp.dp - 96.dp).coerceAtLeast(320.dp)
    }

    LaunchedEffect(Unit) {
        entries = withContext(Dispatchers.IO) { TabSessionManager.loadClosed(activity) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { ClintSheetWindowInsets },
        containerColor = colors.popupBackground,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.divider) }
    ) {
        ClintDialogStatusBarEffect(hideStatusBar, hideSystemNavigation)
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .then(if (maxContentWidth != null) Modifier.widthIn(max = maxContentWidth) else Modifier)
                    .padding(bottom = 8.dp)
            ) {
                val list = entries
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.tab_menu_recently_closed),
                        color = colors.onSurface,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    if (!list.isNullOrEmpty()) {
                        TextButton(onClick = {
                            entries = emptyList()
                            TabSessionManager.clearClosedAsync(activity)
                        }) {
                            Text(stringResource(R.string.recently_closed_clear_all), color = colors.primary)
                        }
                    }
                }
                if (list != null) {
                    if (list.isEmpty()) {
                        Text(
                            stringResource(R.string.recently_closed_empty),
                            color = colors.secondaryText,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 32.dp)
                        )
                    } else {
                        LazyColumn(Modifier.fillMaxWidth().heightIn(max = maxSheetHeight).nestedScroll(flingBoundaryConnection)) {
                            items(list, key = { it.id }) { entry ->
                                RecentlyClosedRow(
                                    entry = entry,
                                    onClick = {
                                        onDismiss()
                                        activity.reopenClosedTab(entry)
                                        onReopened()
                                    },
                                    onRemove = {
                                        entries = entries?.filterNot { it.id == entry.id }
                                        TabSessionManager.removeClosedAsync(activity, entry.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentlyClosedRow(entry: ClosedTab, onClick: () -> Unit, onRemove: () -> Unit) {
    val colors = LocalClintColors.current
    val favicon = rememberClintFavicon(entry.url)
    val host = remember(entry.url) {
        runCatching { android.net.Uri.parse(entry.url).host }.getOrNull()
            ?.removePrefix("www.")
            ?.takeIf { it.isNotBlank() }
            ?: entry.url
    }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (favicon != null) {
            Image(favicon.asImageBitmap(), contentDescription = null, modifier = Modifier.size(20.dp).clip(CircleShape))
        } else {
            Icon(Icons.Filled.Public, contentDescription = null, tint = colors.iconTint, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f).padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)) {
            Text(
                entry.title.ifBlank { host },
                color = colors.onSurface,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                entry.groupColor?.let { groupColor ->
                    Box(Modifier.padding(end = 6.dp).size(8.dp).clip(CircleShape).background(Color(groupColor)))
                }
                Text(
                    host,
                    color = colors.secondaryText,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(onClick = onRemove) {
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(R.string.recently_closed_remove_desc),
                tint = colors.iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
