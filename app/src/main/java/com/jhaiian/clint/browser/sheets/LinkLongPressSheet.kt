package com.jhaiian.clint.browser.sheets
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ZoomIn

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.ui.FaviconCache
import com.jhaiian.clint.ui.theme.LocalClintColors

data class LinkLongPressRequest(val url: String, val linkText: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LinkLongPressSheet(request: LinkLongPressRequest, activity: MainActivity, onDismiss: () -> Unit) {
    val colors = LocalClintColors.current
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var favicon by remember(request.url) { mutableStateOf<Bitmap?>(null) }
    val hideStatusBar = remember { PreferenceManager.getDefaultSharedPreferences(activity).getBoolean("hide_status_bar", false) }
    val hideSystemNavigation = remember { PreferenceManager.getDefaultSharedPreferences(activity).getBoolean("hide_system_navigation", false) }

    LaunchedEffect(request.url) {
        val faviconUrl = FaviconCache.faviconUrlFor(request.url)
        if (faviconUrl.isNotEmpty()) FaviconCache.load(context, faviconUrl) { bmp -> favicon = bmp }
    }

    fun dismissAnd(action: () -> Unit) {
        onDismiss()
        action()
    }

    val hasLinkText = request.linkText.isNotEmpty() && request.linkText != request.url
    val listState = rememberLazyListState()
    val flingBoundaryConnection = remember {
        object : NestedScrollConnection {
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = available
        }
    }
    val configuration = LocalConfiguration.current

    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    val maxSheetHeight = if (isPortrait) {
        (configuration.screenHeightDp.dp * 0.5f).coerceAtLeast(320.dp)
    } else {
        (configuration.screenHeightDp.dp - 96.dp).coerceAtLeast(320.dp)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.popupBackground,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.divider) }
    ) {
        com.jhaiian.clint.ui.ClintDialogStatusBarEffect(hideStatusBar, hideSystemNavigation)
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = maxSheetHeight).nestedScroll(flingBoundaryConnection), state = listState) {
        item {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.fillMaxWidth().widthIn(max = LongPressContentMaxWidth).padding(bottom = 8.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinkFaviconChip(favicon, colors)
                Column(Modifier.padding(start = 14.dp)) {
                    if (hasLinkText) {
                        Text(request.linkText, color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                    Text(request.url, color = colors.secondaryText, fontSize = 12.5.sp, maxLines = 2)
                }
            }

            LongPressSheetDivider()

            LongPressActionRow(androidx.compose.material.icons.Icons.AutoMirrored.Filled.OpenInNew, stringResource(R.string.link_open_in_new_tab)) {
                dismissAnd { activity.onLinkOpenInNewTab(request.url) }
            }
            LongPressActionRow(androidx.compose.material.icons.Icons.Filled.Tab, stringResource(R.string.link_open_new_tab_background)) {
                dismissAnd { activity.onLinkOpenNewTabBackground(request.url) }
            }
            LongPressActionRow(androidx.compose.material.icons.Icons.Filled.VisibilityOff, stringResource(R.string.link_open_incognito)) {
                dismissAnd { activity.onLinkOpenIncognito(request.url) }
            }
            LongPressActionRow(androidx.compose.material.icons.Icons.Filled.ZoomIn, stringResource(R.string.link_preview_page)) {
                dismissAnd { activity.onLinkPreviewPage(request.url) }
            }
            LongPressActionRow(androidx.compose.material.icons.Icons.Filled.ContentCopy, stringResource(R.string.link_copy_address)) {
                dismissAnd { activity.onLinkCopyAddress(request.url) }
            }
            if (hasLinkText) {
                LongPressActionRow(androidx.compose.material.icons.Icons.Filled.ContentCopy, stringResource(R.string.link_copy_text)) {
                    dismissAnd { activity.onLinkCopyText(request.url, request.linkText) }
                }
            }
            LongPressActionRow(androidx.compose.material.icons.Icons.Filled.Share, stringResource(R.string.link_share)) {
                dismissAnd { activity.onLinkShare(request.url) }
            }
        }
        }
        }
        }
    }
}

@Composable
private fun LinkFaviconChip(favicon: Bitmap?, colors: com.jhaiian.clint.ui.theme.ClintColors) {
    val chipShape = RoundedCornerShape(11.dp)
    Box(
        Modifier.size(40.dp).clip(chipShape).background(colors.surfaceVariant).border(1.dp, colors.popupStroke, chipShape),
        contentAlignment = Alignment.Center
    ) {
        if (favicon != null) {
            Image(favicon.asImageBitmap(), contentDescription = null, modifier = Modifier.size(22.dp))
        } else {
            Icon(androidx.compose.material.icons.Icons.Filled.Link, contentDescription = null, tint = colors.iconTint, modifier = Modifier.size(20.dp))
        }
    }
}
