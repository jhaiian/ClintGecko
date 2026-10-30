package com.jhaiian.clint.extensions

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.dialogs.JsDialog
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.ClintDialogActionFooter
import com.jhaiian.clint.ui.ClintSwitch
import com.jhaiian.clint.ui.theme.LocalClintColors
import org.mozilla.geckoview.GeckoView

@Composable
fun ExtensionHost() {
    val token = remember { Any() }
    DisposableEffect(token) {
        ClintExtensions.registerHost(token)
        onDispose { ClintExtensions.unregisterHost(token) }
    }
    if (ClintExtensions.activeHost !== token) return

    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    val hideStatusBar = prefs.getBoolean("hide_status_bar", false)
    val hideSystemNavigation = prefs.getBoolean("hide_system_navigation", false)

    ClintExtensions.prompt?.let { prompt ->
        ExtensionPromptDialog(prompt, hideStatusBar, hideSystemNavigation)
    }
    ClintExtensions.unsignedRequest?.let { request ->
        UnsignedInstallDialog(request, hideStatusBar, hideSystemNavigation)
    }
    ClintExtensions.popup?.let { popup ->
        ExtensionPopupDialog(popup)
    }
    ClintExtensions.popupDialog?.let { request ->
        JsDialog(request, hideStatusBar, hideSystemNavigation) { ClintExtensions.dismissPopupDialog(request) }
    }
    ExtensionFilePicker()
}

@Composable
private fun ExtensionFilePicker() {
    val single = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        ClintExtensions.finishFilePick(listOfNotNull(uri))
    }
    val multiple = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        ClintExtensions.finishFilePick(uris)
    }
    val request = ClintExtensions.popupFileRequest
    LaunchedEffect(request) {
        if (request == null || request.launched) return@LaunchedEffect
        request.launched = true
        val launched = runCatching {
            if (request.multiple) multiple.launch(request.mimeTypes) else single.launch(request.mimeTypes)
        }.isSuccess
        if (!launched) ClintExtensions.finishFilePick(emptyList())
    }
}

@Composable
private fun ExtensionPromptDialog(prompt: ExtensionPrompt, hideStatusBar: Boolean, hideSystemNavigation: Boolean) {
    val colors = LocalClintColors.current
    var allowPrivate by remember(prompt) { mutableStateOf(false) }
    var icon by remember(prompt) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(prompt) {
        runCatching {
            prompt.extension.metaData.icon.getBitmap(144).accept(
                { bitmap -> if (bitmap != null) icon = bitmap.asImageBitmap() },
                { }
            )
        }
    }
    val permissions = ExtensionPermissions.visible(prompt.permissions)
    val access = ExtensionPermissions.hostAccess(prompt.origins)
    val hasHosts = access.allSites || access.domains.isNotEmpty()
    val hasAny = permissions.isNotEmpty() || hasHosts || prompt.dataCollection.isNotEmpty()
    val risk = ExtensionPermissions.risk(prompt.permissions, prompt.origins)
    val deny = { ClintExtensions.answer(prompt, false) }

    val titleRes = when (prompt) {
        is ExtensionPrompt.Install -> R.string.ext_prompt_install_title
        is ExtensionPrompt.Update -> R.string.ext_prompt_update_title
        is ExtensionPrompt.Optional -> R.string.ext_prompt_optional_title
    }
    val messageRes = when (prompt) {
        is ExtensionPrompt.Install -> R.string.ext_prompt_install_message
        is ExtensionPrompt.Update -> R.string.ext_prompt_update_message
        is ExtensionPrompt.Optional -> R.string.ext_prompt_optional_message
    }
    val positiveRes = when (prompt) {
        is ExtensionPrompt.Install -> R.string.ext_prompt_add
        is ExtensionPrompt.Update -> R.string.ext_prompt_update_action
        is ExtensionPrompt.Optional -> R.string.ext_prompt_allow
    }

    ClintDialog(
        title = stringResource(titleRes, prompt.name),
        hideStatusBar = hideStatusBar,
        hideSystemNavigation = hideSystemNavigation,
        onDismiss = deny,
        cancelable = true,
        footer = {
            ClintDialogActionFooter(
                onCancel = deny,
                positiveLabel = stringResource(positiveRes),
                onPositive = {
                    if (prompt is ExtensionPrompt.Install) ClintExtensions.answerInstall(prompt, true, allowPrivate)
                    else ClintExtensions.answer(prompt, true)
                },
                cancelLabel = if (prompt is ExtensionPrompt.Optional) stringResource(R.string.ext_prompt_deny) else stringResource(R.string.action_cancel)
            )
        }
    ) {
        Column(Modifier.padding(horizontal = 8.dp)) {
            Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                ExtensionIconTile(icon, prompt.extension.id, prompt.name, 56.dp)
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(riskLabel(risk), color = riskColor(risk), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    RiskMeter(risk, Modifier.padding(top = 8.dp))
                }
            }
            Text(
                stringResource(messageRes),
                color = colors.secondaryText,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
            )
            if (hasAny) {
                if (prompt is ExtensionPrompt.Install) {
                    Text(
                        stringResource(R.string.ext_prompt_can),
                        color = colors.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                }
                permissions.forEach { permission ->
                    val spec = ExtensionPermissions.spec(permission)
                    PermissionLine(spec.icon, permissionText(permission), spec.risk)
                }
                HostAccessLine(prompt.origins)
                DataCollectionLines(prompt.dataCollection)
            } else {
                Text(
                    stringResource(R.string.ext_prompt_no_perms),
                    color = colors.onSurface,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                )
            }
            if (prompt is ExtensionPrompt.Install) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { allowPrivate = !allowPrivate }
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.ext_prompt_private),
                        color = colors.onSurface,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f).padding(end = 12.dp)
                    )
                    ClintSwitch(checked = allowPrivate)
                }
            }
        }
    }
}

@Composable
private fun UnsignedInstallDialog(request: UnsignedInstallRequest, hideStatusBar: Boolean, hideSystemNavigation: Boolean) {
    val colors = LocalClintColors.current
    val deny = { ClintExtensions.answerUnsigned(request, false) }
    ClintDialog(
        title = stringResource(R.string.ext_unsigned_title),
        hideStatusBar = hideStatusBar,
        hideSystemNavigation = hideSystemNavigation,
        onDismiss = deny,
        cancelable = true,
        footer = {
            ClintDialogActionFooter(
                onCancel = deny,
                positiveLabel = stringResource(R.string.ext_unsigned_install_anyway),
                onPositive = { ClintExtensions.answerUnsigned(request, true) }
            )
        }
    ) {
        Column(Modifier.padding(horizontal = 8.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.colorErrorContainer)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    tint = colors.colorError,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    request.fileName,
                    color = colors.colorOnErrorContainer,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 12.dp)
                )
            }
            Text(
                stringResource(R.string.ext_unsigned_message),
                color = colors.onSurface,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
            )
            Text(
                stringResource(R.string.ext_unsigned_note),
                color = colors.secondaryText,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun ExtensionPopupDialog(popup: ExtensionPopup) {
    val colors = LocalClintColors.current
    val close = { ClintExtensions.closePopup() }
    val noRipple = remember { MutableInteractionSource() }
    Dialog(
        onDismissRequest = close,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .then(
                    if (popup.fullScreen) Modifier
                    else Modifier.clickable(interactionSource = noRipple, indication = null, onClick = close)
                ),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(if (popup.fullScreen) 0.dp else 24.dp),
                color = colors.popupBackground,
                modifier = if (popup.fullScreen) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .padding(16.dp)
                        .widthIn(max = 420.dp)
                        .fillMaxWidth()
                        .fillMaxHeight(0.78f)
                        .heightIn(max = 620.dp)
                        .clickable(interactionSource = noRipple, indication = null) { }
                }
            ) {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        Modifier.fillMaxWidth().height(52.dp).padding(start = 18.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            popup.title,
                            color = colors.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = close) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.ext_action_close), tint = colors.iconTint)
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
                    val background = colors.popupBackground.toArgb()
                    AndroidView(
                        factory = { ctx ->
                            GeckoView(ctx).apply {
                                setBackgroundColor(background)
                                setSession(popup.session)
                            }
                        },
                        onRelease = { view -> runCatching { view.releaseSession() } },
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )
                }
            }
        }
    }
}
