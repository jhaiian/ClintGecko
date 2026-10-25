package com.jhaiian.clint.translation

import android.content.Context
import android.os.Build
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.webview.ClintWebView
import com.jhaiian.clint.ui.listscreen.ListMenuItem
import com.jhaiian.clint.ui.listscreen.PopupShape
import com.jhaiian.clint.ui.theme.LocalClintColors
import com.jhaiian.clint.util.formatFileSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class TranslateBarRequest(
    val webView: ClintWebView,
    val tabId: String,
    val allowSiteSetting: Boolean,
    val offered: Boolean
)

private const val TRANSLATE_BAR_OFFER_TIMEOUT_MS = 10000L
private const val TRANSLATE_BAR_DONE_TIMEOUT_MS = 6000L
private val TranslateBarMaxWidth = 560.dp

private fun Context.accessibleTimeout(timeoutMs: Long): Long {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return timeoutMs
    val manager = getSystemService(AccessibilityManager::class.java) ?: return timeoutMs
    return manager.getRecommendedTimeoutMillis(
        timeoutMs.toInt(),
        AccessibilityManager.FLAG_CONTENT_TEXT or AccessibilityManager.FLAG_CONTENT_CONTROLS
    ).toLong()
}

@Composable
internal fun TranslateBar(
    request: TranslateBarRequest?,
    visible: Boolean,
    activeTabId: String?,
    onTranslate: (ClintWebView, String, String) -> Unit,
    onRestore: (ClintWebView) -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var lastRequest by remember { mutableStateOf(request) }
    LaunchedEffect(request) {
        if (request != null) lastRequest = request
    }
    LaunchedEffect(request, activeTabId) {
        if (request != null && request.tabId != activeTabId) onDismiss()
    }

    AnimatedVisibility(
        visible = visible && request != null,
        enter = slideInVertically(tween(220)) { -it } + fadeIn(tween(220)),
        exit = slideOutVertically(tween(180)) { -it } + fadeOut(tween(180)),
        modifier = modifier
    ) {
        val current = request ?: lastRequest
        if (current != null) {
            TranslateBarContent(
                request = current,
                active = request != null,
                onTranslate = onTranslate,
                onRestore = onRestore,
                onOpenSettings = onOpenSettings,
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
private fun TranslateBarContent(
    request: TranslateBarRequest,
    active: Boolean,
    onTranslate: (ClintWebView, String, String) -> Unit,
    onRestore: (ClintWebView) -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val locale = remember(configuration) { ClintTranslations.appLocale(context) }
    val pageState by request.webView.translator.state.collectAsState()
    val scope = rememberCoroutineScope()

    var loading by remember(request) { mutableStateOf(true) }
    var engineSupported by remember(request) { mutableStateOf(true) }
    var support by remember(request) { mutableStateOf<TranslationLanguageSupport?>(null) }
    var fromCode by remember(request) { mutableStateOf<String?>(null) }
    var toCode by remember(request) { mutableStateOf<String?>(null) }
    var userPicked by remember(request) { mutableStateOf(false) }
    var downloadSize by remember(request) { mutableStateOf<Long?>(null) }
    var languageSetting by remember(request) { mutableStateOf<TranslationLanguageSetting?>(null) }
    var neverSite by remember(request) { mutableStateOf<Boolean?>(null) }
    var hadPageState by remember(request) { mutableStateOf(false) }
    var fromMenuOpen by remember(request) { mutableStateOf(false) }
    var toMenuOpen by remember(request) { mutableStateOf(false) }
    var overflowOpen by remember(request) { mutableStateOf(false) }
    var dragging by remember(request) { mutableStateOf(false) }
    var dragOffset by remember(request) { mutableFloatStateOf(0f) }
    val dismissThresholdPx = with(density) { 48.dp.toPx() }

    LaunchedEffect(request) {
        engineSupported = ClintTranslations.isEngineSupported()
        if (engineSupported) {
            support = ClintTranslations.supportedLanguages(locale)
            if (request.allowSiteSetting) neverSite = request.webView.translator.isNeverTranslateSite()
        }
        loading = false
    }

    LaunchedEffect(support, pageState.docLanguage, pageState.requestedFrom, pageState.requestedTo) {
        val loaded = support ?: return@LaunchedEffect
        if (userPicked) return@LaunchedEffect
        val from = ClintTranslations.findCompatible(pageState.requestedFrom ?: pageState.docLanguage, loaded.fromLanguages)?.code
        fromCode = from
        toCode = listOf(pageState.requestedTo, ClintTranslations.defaultTarget(context, pageState), locale.toLanguageTag())
            .firstNotNullOfOrNull { candidate ->
                ClintTranslations.findCompatible(candidate, loaded.toLanguages)?.takeIf { !ClintTranslations.sameLanguage(it.code, from) }
            }?.code
    }

    LaunchedEffect(fromCode, toCode) {
        downloadSize = null
        val from = fromCode
        val to = toCode
        if (from != null && to != null && !ClintTranslations.sameLanguage(from, to)) {
            downloadSize = ClintTranslations.downloadSize(from, to)
        }
    }

    LaunchedEffect(fromCode) {
        languageSetting = null
        val from = fromCode ?: return@LaunchedEffect
        languageSetting = ClintTranslations.languageSetting(from)
    }

    LaunchedEffect(pageState) {
        if (pageState.docLanguage != null || pageState.isRequested) {
            hadPageState = true
        } else if (hadPageState && active) {
            onDismiss()
        }
    }

    val interacting = fromMenuOpen || toMenuOpen || overflowOpen || dragging
    LaunchedEffect(request, active, interacting, pageState.isTranslated, pageState.isTranslating, pageState.isRequested) {
        if (!active || interacting || pageState.isTranslating) return@LaunchedEffect
        val timeout = when {
            pageState.isTranslated -> TRANSLATE_BAR_DONE_TIMEOUT_MS
            request.offered && !pageState.isRequested -> TRANSLATE_BAR_OFFER_TIMEOUT_MS
            else -> return@LaunchedEffect
        }
        delay(context.accessibleTimeout(timeout))
        onDismiss()
    }

    val loadedSupport = support
    val from = fromCode
    val to = toCode
    val canTranslate = !loading && engineSupported && loadedSupport != null && from != null && to != null &&
        !ClintTranslations.sameLanguage(from, to) && !pageState.isTranslating
    val fromName = from?.let { code -> loadedSupport?.fromLanguages?.firstOrNull { it.code == code }?.name ?: ClintTranslations.displayName(code, locale) }
    val toName = to?.let { code -> loadedSupport?.toLanguages?.firstOrNull { it.code == code }?.name ?: ClintTranslations.displayName(code, locale) }
    val sentenceName = from?.let { ClintTranslations.displayName(it, locale, capitalize = false) }.orEmpty()
    val hasError = pageState.error != null

    val title = when {
        hasError -> stringResource(R.string.translation_failed)
        pageState.isTranslating -> stringResource(R.string.translation_translating)
        pageState.isTranslated -> stringResource(R.string.translation_translated)
        else -> stringResource(R.string.translation_bar_prompt)
    }
    val size = downloadSize
    val subtitle = when {
        loading -> null
        !engineSupported -> stringResource(R.string.translation_engine_unsupported)
        loadedSupport == null -> stringResource(R.string.translation_languages_unavailable)
        hasError -> stringResource(R.string.translation_error_hint)
        !pageState.isRequested && size != null && size > 0L -> stringResource(R.string.translation_download_short, formatFileSize(size))
        else -> null
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.popupBackground,
        border = BorderStroke(1.dp, colors.popupStroke),
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = TranslateBarMaxWidth)
            .graphicsLayer {
                translationY = dragOffset
                alpha = 1f - (-dragOffset / (dismissThresholdPx * 2f)).coerceIn(0f, 0.6f)
            }
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta -> dragOffset = (dragOffset + delta).coerceAtMost(0f) },
                onDragStarted = { dragging = true },
                onDragStopped = {
                    dragging = false
                    if (dragOffset <= -dismissThresholdPx) {
                        onDismiss()
                    } else {
                        androidx.compose.animation.core.animate(dragOffset, 0f, animationSpec = tween(180)) { value, _ -> dragOffset = value }
                    }
                }
            )
    ) {
        Column(Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(34.dp).clip(CircleShape).background(colors.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Translate, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                }
                Column(Modifier.weight(1f).padding(start = 12.dp, end = 4.dp)) {
                    Text(
                        title,
                        color = if (hasError) colors.colorError else colors.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle != null) {
                        Text(
                            subtitle,
                            color = colors.secondaryText,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                    }
                }
                Box {
                    IconButton(onClick = { overflowOpen = true }, enabled = engineSupported && !loading) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.translation_more_options),
                            tint = colors.iconTint
                        )
                    }
                    DropdownMenu(
                        expanded = overflowOpen,
                        onDismissRequest = { overflowOpen = false },
                        shape = PopupShape,
                        containerColor = colors.popupBackground,
                        border = BorderStroke(1.dp, colors.popupStroke)
                    ) {
                        val currentSetting = languageSetting
                        if (from != null && currentSetting != null) {
                            ListMenuItem(
                                Icons.Filled.Translate,
                                stringResource(R.string.translation_always_language, sentenceName),
                                currentSetting == TranslationLanguageSetting.ALWAYS
                            ) {
                                overflowOpen = false
                                val next = if (currentSetting == TranslationLanguageSetting.ALWAYS) TranslationLanguageSetting.OFFER else TranslationLanguageSetting.ALWAYS
                                scope.launch { if (ClintTranslations.setLanguageSetting(from, next)) languageSetting = next }
                            }
                            ListMenuItem(
                                Icons.Filled.Block,
                                stringResource(R.string.translation_never_language, sentenceName),
                                currentSetting == TranslationLanguageSetting.NEVER
                            ) {
                                overflowOpen = false
                                val next = if (currentSetting == TranslationLanguageSetting.NEVER) TranslationLanguageSetting.OFFER else TranslationLanguageSetting.NEVER
                                scope.launch {
                                    if (ClintTranslations.setLanguageSetting(from, next)) {
                                        languageSetting = next
                                        if (next == TranslationLanguageSetting.NEVER && !pageState.isRequested) onDismiss()
                                    }
                                }
                            }
                        }
                        val siteValue = neverSite
                        if (request.allowSiteSetting && siteValue != null) {
                            ListMenuItem(
                                Icons.Filled.Language,
                                stringResource(R.string.translation_never_site),
                                siteValue
                            ) {
                                overflowOpen = false
                                val next = !siteValue
                                scope.launch {
                                    if (request.webView.translator.setNeverTranslateSite(next)) {
                                        neverSite = next
                                        if (next && !pageState.isRequested) onDismiss()
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = colors.popupStroke, modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp))
                        ListMenuItem(Icons.Filled.Settings, stringResource(R.string.translation_open_settings), false) {
                            overflowOpen = false
                            onDismiss()
                            onOpenSettings()
                        }
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_close), tint = colors.iconTint)
                }
            }
            if (engineSupported && (loading || loadedSupport != null)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        TranslateLanguageChip(
                            label = stringResource(R.string.translation_from),
                            value = fromName ?: stringResource(R.string.translation_choose_language),
                            languages = loadedSupport?.fromLanguages.orEmpty(),
                            selected = from,
                            enabled = loadedSupport != null && !pageState.isTranslating,
                            expanded = fromMenuOpen,
                            onExpandedChange = { fromMenuOpen = it },
                            onSelect = { code ->
                                userPicked = true
                                fromCode = code
                            },
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = colors.secondaryText,
                            modifier = Modifier.padding(horizontal = 6.dp).size(16.dp)
                        )
                        TranslateLanguageChip(
                            label = stringResource(R.string.translation_to),
                            value = toName ?: stringResource(R.string.translation_choose_language),
                            languages = loadedSupport?.toLanguages.orEmpty(),
                            selected = to,
                            enabled = loadedSupport != null && !pageState.isTranslating,
                            expanded = toMenuOpen,
                            onExpandedChange = { toMenuOpen = it },
                            onSelect = { code ->
                                userPicked = true
                                toCode = code
                            },
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    Box(Modifier.heightIn(min = 48.dp).padding(start = 4.dp), contentAlignment = Alignment.Center) {
                        when {
                            loading || pageState.isTranslating -> CircularProgressIndicator(
                                color = colors.primary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.padding(horizontal = 20.dp).size(20.dp)
                            )
                            pageState.isTranslated && from != null && to != null &&
                                pageState.requestedTo?.let { ClintTranslations.sameLanguage(it, to) } == true &&
                                pageState.requestedFrom?.let { ClintTranslations.sameLanguage(it, from) } == true -> TextButton(onClick = { onRestore(request.webView) }) {
                                Text(stringResource(R.string.translation_show_original), color = colors.primary, fontWeight = FontWeight.Medium)
                            }
                            else -> TextButton(
                                onClick = {
                                    if (from != null && to != null) onTranslate(request.webView, from, to)
                                },
                                enabled = canTranslate
                            ) {
                                Text(
                                    stringResource(R.string.translation_action_translate),
                                    color = if (canTranslate) colors.primary else colors.secondaryText,
                                    fontWeight = FontWeight.Medium
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
private fun TranslateLanguageChip(
    label: String,
    value: String,
    languages: List<TranslationLanguage>,
    selected: String?,
    enabled: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalClintColors.current
    Box(modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(colors.surfaceVariant)
                .clickable(enabled = enabled, onClickLabel = label) { onExpandedChange(true) }
                .heightIn(min = 36.dp)
                .padding(start = 10.dp, end = 4.dp)
        ) {
            Text(
                value,
                color = colors.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = colors.iconTint, modifier = Modifier.size(20.dp))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.heightIn(max = 360.dp),
            shape = PopupShape,
            containerColor = colors.popupBackground,
            border = BorderStroke(1.dp, colors.popupStroke)
        ) {
            languages.forEach { language ->
                val isSelected = language.code == selected
                DropdownMenuItem(
                    text = { Text(language.name, color = colors.popupText, fontSize = 14.sp) },
                    trailingIcon = if (isSelected) {
                        { Icon(Icons.Filled.Check, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp)) }
                    } else null,
                    onClick = {
                        onExpandedChange(false)
                        onSelect(language.code)
                    }
                )
            }
        }
    }
}
