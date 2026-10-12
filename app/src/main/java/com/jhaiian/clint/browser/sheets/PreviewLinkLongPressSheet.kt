package com.jhaiian.clint.browser.sheets
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.VisibilityOff

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.ui.theme.LocalClintColors

data class PreviewLinkLongPressRequest(val url: String, val linkText: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PreviewLinkLongPressSheet(request: PreviewLinkLongPressRequest, activity: MainActivity, onDismiss: () -> Unit) {
    val colors = LocalClintColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val hideStatusBar = remember { PreferenceManager.getDefaultSharedPreferences(activity).getBoolean("hide_status_bar", false) }
    val hideSystemNavigation = remember { PreferenceManager.getDefaultSharedPreferences(activity).getBoolean("hide_system_navigation", false) }

    fun dismissAnd(action: () -> Unit) {
        onDismiss()
        action()
    }

    val hasLinkText = request.linkText.isNotEmpty() && request.linkText != request.url

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = colors.popupBackground) {
        com.jhaiian.clint.ui.ClintDialogStatusBarEffect(hideStatusBar, hideSystemNavigation)
        Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(androidx.compose.material.icons.Icons.Filled.Link, contentDescription = null, tint = colors.iconTint, modifier = Modifier.size(20.dp))
                Column(Modifier.padding(start = 12.dp)) {
                    if (hasLinkText) {
                        Text(request.linkText, color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                    Text(request.url, color = colors.secondaryText, fontSize = 13.sp, maxLines = 2)
                }
            }

            ActionSheetDivider()
            ActionSheetRow(androidx.compose.material.icons.Icons.AutoMirrored.Filled.OpenInNew, stringResource(R.string.link_open_in_new_tab)) {
                dismissAnd { activity.onPreviewLinkOpenInNewTab(request.url) }
            }
            ActionSheetDivider()
            ActionSheetRow(androidx.compose.material.icons.Icons.Filled.VisibilityOff, stringResource(R.string.link_open_incognito)) {
                dismissAnd { activity.onPreviewLinkOpenIncognito(request.url) }
            }
            ActionSheetDivider()
            ActionSheetRow(androidx.compose.material.icons.Icons.Filled.Tab, stringResource(R.string.preview_link_open_in_current_tab)) {
                dismissAnd { activity.onPreviewLinkOpenInCurrentTab(request.url) }
            }
            ActionSheetDivider()
            ActionSheetRow(androidx.compose.material.icons.Icons.Filled.ContentCopy, stringResource(R.string.link_copy_address)) {
                dismissAnd { activity.onPreviewLinkCopyAddress(request.url) }
            }
            if (hasLinkText) {
                ActionSheetDivider()
                ActionSheetRow(androidx.compose.material.icons.Icons.Filled.ContentCopy, stringResource(R.string.link_copy_text)) {
                    dismissAnd { activity.onPreviewLinkCopyText(request.url, request.linkText) }
                }
            }
            ActionSheetDivider()
            ActionSheetRow(androidx.compose.material.icons.Icons.Filled.Share, stringResource(R.string.link_share)) {
                dismissAnd { activity.onPreviewLinkShare(request.url) }
            }
        }
    }
}
