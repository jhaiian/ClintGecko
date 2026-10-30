package com.jhaiian.clint.browser
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.VisibilityOff

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.delegates.clipboardHasText
import com.jhaiian.clint.browser.delegates.copyCurrentPageLink
import com.jhaiian.clint.browser.delegates.pasteAndGo
import com.jhaiian.clint.browser.delegates.pasteIntoSearch
import com.jhaiian.clint.browser.delegates.openTranslatePage
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
internal fun AddressBarRow(
    activity: MainActivity,
    isIncognito: Boolean,
    addressBarText: String,
    isSecure: Boolean,
    tabCountText: String,
    isMediaCaptureEnabled: Boolean,
    activeTabId: String?,
    onAddressBarClick: () -> Unit,
    onTabCountClick: () -> Unit,
    onMediaCaptureClick: () -> Unit,
    onSwipeTabChange: (Int) -> Boolean,
    onSiteInfoClick: () -> Unit = {},
    showHomeButton: Boolean = false,
    onHomeClick: () -> Unit = {},
    openTabsSwipeDirection: Int = 0,
    isBottomBar: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = LocalClintColors.current
    val density = LocalDensity.current
    val mediaCaptureCount = if (isMediaCaptureEnabled && activeTabId != null) {
        val items by com.jhaiian.clint.mediacapture.MediaCaptureStore.observe(activeTabId).collectAsState()
        items.size
    } else 0
    val addressBarHint = stringResource(
        R.string.search_bar_hint,
        engineDisplayName(
            searchEngineFor(activity.prefs, activity.uiState.isIncognito),
            customSearchEngineName(activity.prefs, activity.uiState.isIncognito)
        )
    )
    val haptic = LocalHapticFeedback.current
    var showEditMenu by remember { mutableStateOf(false) }
    var canPaste by remember { mutableStateOf(false) }
    val activeWebView = remember(activeTabId, activity.uiState.tabsRevision) {
        activity.tabManager.tabs.firstOrNull { it.id == activeTabId }?.webView
    }
    val translationState by (activeWebView?.translator?.state ?: com.jhaiian.clint.translation.EmptyPageTranslationState).collectAsState()
    val showTranslateButton = activity.uiState.showTranslateButton && addressBarText.isNotEmpty() &&
        (translationState.canTranslate || translationState.isRequested)
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var swipeDirection by remember { mutableIntStateOf(0) }
    val swipeThresholdPx = with(density) { 56.dp.toPx() }
    val maxDragPx = with(density) { 72.dp.toPx() }
    var verticalDrag by remember { mutableFloatStateOf(0f) }
    val openTabsThresholdPx = with(density) { 40.dp.toPx() }
    val openTabsFlingPx = with(density) { 600.dp.toPx() }
    val maxVerticalNudgePx = with(density) { 10.dp.toPx() }
    val verticalDragState = rememberDraggableState { delta -> verticalDrag += delta }
    LaunchedEffect(addressBarText) {
        if (swipeDirection != 0) {
            kotlinx.coroutines.delay(240)
            swipeDirection = 0
        }
    }
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isIncognito) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Filled.VisibilityOff,
                contentDescription = stringResource(R.string.incognito),
                tint = colors.iconTint,
                modifier = Modifier.padding(end = 8.dp).size(20.dp)
            )
        }
        if (showHomeButton) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onHomeClick)
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.Home,
                    contentDescription = stringResource(R.string.home),
                    tint = colors.iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Surface(
            color = colors.addressBarColor,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .combinedClickable(
                    onClick = onAddressBarClick,
                    onLongClick = {
                        canPaste = activity.clipboardHasText()
                        if (addressBarText.isNotEmpty() || canPaste) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showEditMenu = true
                        }
                    }
                )
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        dragOffset = (dragOffset + delta).coerceIn(-maxDragPx, maxDragPx)
                    },
                    onDragStopped = {
                        val value = dragOffset
                        val settle: suspend (Float) -> Unit = { start ->
                            androidx.compose.animation.core.animate(start, 0f, animationSpec = tween(220)) { v, _ -> dragOffset = v }
                        }
                        when {
                            value <= -swipeThresholdPx -> {
                                swipeDirection = -1
                                if (onSwipeTabChange(-1)) dragOffset = 0f else settle(value)
                            }
                            value >= swipeThresholdPx -> {
                                swipeDirection = 1
                                if (onSwipeTabChange(1)) dragOffset = 0f else settle(value)
                            }
                            else -> settle(value)
                        }
                    }
                )
                .draggable(
                    orientation = Orientation.Vertical,
                    enabled = openTabsSwipeDirection != 0,
                    state = verticalDragState,
                    onDragStopped = { velocity ->
                        val distance = verticalDrag * openTabsSwipeDirection
                        val fling = velocity * openTabsSwipeDirection
                        verticalDrag = 0f
                        if (distance >= openTabsThresholdPx || (distance > 0f && fling >= openTabsFlingPx)) onTabCountClick()
                    }
                )
        ) {
            if (showEditMenu) {
                AddressBarEditMenu(
                    above = isBottomBar,
                    showCopy = addressBarText.isNotEmpty(),
                    showPaste = canPaste,
                    onCopy = {
                        showEditMenu = false
                        activity.copyCurrentPageLink(addressBarText)
                    },
                    onPaste = {
                        showEditMenu = false
                        activity.pasteIntoSearch(isBottomBar)
                    },
                    onPasteAndGo = {
                        showEditMenu = false
                        activity.pasteAndGo(isBottomBar)
                    },
                    onDismiss = { showEditMenu = false }
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(start = 6.dp, end = 16.dp)
                    .graphicsLayer {
                        translationX = dragOffset
                        translationY = (verticalDrag * openTabsSwipeDirection).coerceIn(0f, maxVerticalNudgePx) * openTabsSwipeDirection
                    }
            ) {
                val hasSiteInfo = addressBarText.isNotEmpty()
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .then(
                            if (hasSiteInfo) {
                                Modifier.clickable(
                                    onClickLabel = stringResource(R.string.site_info_title),
                                    onClick = onSiteInfoClick
                                )
                            } else Modifier
                        )
                ) {
                    Icon(
                        imageVector = if (!hasSiteInfo) androidx.compose.material.icons.Icons.Filled.Search else if (isSecure) androidx.compose.material.icons.Icons.Filled.Lock else androidx.compose.material.icons.Icons.Filled.LockOpen,
                        contentDescription = if (hasSiteInfo) stringResource(R.string.site_info_title) else null,
                        tint = colors.iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                AnimatedContent(
                    targetState = addressBarText,
                    transitionSpec = {
                        if (swipeDirection < 0) {
                            (slideInHorizontally(tween(220)) { it } + fadeIn(tween(220))) togetherWith
                                (slideOutHorizontally(tween(220)) { -it } + fadeOut(tween(220)))
                        } else if (swipeDirection > 0) {
                            (slideInHorizontally(tween(220)) { -it } + fadeIn(tween(220))) togetherWith
                                (slideOutHorizontally(tween(220)) { it } + fadeOut(tween(220)))
                        } else {
                            fadeIn(tween(180)) togetherWith fadeOut(tween(180))
                        }
                    },
                    modifier = Modifier.padding(start = 4.dp).weight(1f)
                ) { text ->
                    Text(
                        text = text.ifEmpty { addressBarHint },
                        color = if (text.isEmpty()) colors.secondaryText else colors.onSurface,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (showTranslateButton) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable(
                                onClickLabel = stringResource(R.string.translation_menu_translate),
                                onClick = { activity.openTranslatePage() }
                            )
                    ) {
                        if (translationState.isTranslating) {
                            CircularProgressIndicator(
                                color = colors.primary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Filled.Translate,
                                contentDescription = stringResource(R.string.translation_menu_translate),
                                tint = if (translationState.isTranslated) colors.primary else colors.iconTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
        if (isMediaCaptureEnabled) {
            Box(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .size(34.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onMediaCaptureClick)
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Filled.VideoLibrary,
                        contentDescription = stringResource(R.string.menu_media_capture),
                        tint = colors.iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (mediaCaptureCount > 0) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-2).dp)
                            .defaultMinSize(minWidth = 13.dp)
                            .height(13.dp)
                            .clip(RoundedCornerShape(6.5.dp))
                            .background(colors.primary)
                            .padding(horizontal = 3.dp)
                    ) {
                        Text(
                            text = com.jhaiian.clint.ui.status.formatCompactCount(mediaCaptureCount.toLong()),
                            color = colors.onPrimary,
                            fontSize = 9.sp,
                            lineHeight = 9.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(start = 10.dp)
                .defaultMinSize(minWidth = 34.dp, minHeight = 26.dp)
                .clip(RoundedCornerShape(5.dp))
                .border(2.dp, colors.primary, RoundedCornerShape(5.dp))
                .combinedClickable(
                    onClick = onTabCountClick,
                    onLongClickLabel = stringResource(if (isIncognito) R.string.new_incognito_tab else R.string.new_tab),
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (isIncognito) activity.onNewIncognitoTab() else activity.onNewTab()
                    }
                )
                .padding(horizontal = 5.dp, vertical = 4.dp)
        ) {
            Text(
                text = tabCountText,
                color = colors.primary,
                fontSize = 12.sp,
                lineHeight = 12.sp,
                letterSpacing = 0.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                style = LocalTextStyle.current.copy(
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.Both
                    )
                )
            )
        }
        Box(modifier = Modifier.padding(start = 4.dp).size(40.dp), contentAlignment = Alignment.Center) {
            com.jhaiian.clint.browser.menu.MenuTriggerButton(activity)
        }
    }
}

private class AddressBarEditMenuPositionProvider(
    private val above: Boolean,
    private val gapPx: Int,
    private val insetPx: Int
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val x = if (layoutDirection == LayoutDirection.Ltr) {
            anchorBounds.left + insetPx
        } else {
            anchorBounds.right - insetPx - popupContentSize.width
        }
        val y = if (above) anchorBounds.top - gapPx - popupContentSize.height else anchorBounds.bottom + gapPx
        return IntOffset(
            x.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)),
            y.coerceIn(0, (windowSize.height - popupContentSize.height).coerceAtLeast(0))
        )
    }
}

@Composable
private fun AddressBarEditMenu(
    above: Boolean,
    showCopy: Boolean,
    showPaste: Boolean,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onPasteAndGo: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    val density = LocalDensity.current
    val positionProvider = remember(above, density) {
        with(density) {
            AddressBarEditMenuPositionProvider(above, 6.dp.roundToPx(), 16.dp.roundToPx())
        }
    }
    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Surface(
            color = colors.popupBackground,
            contentColor = colors.popupText,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, colors.popupStroke),
            shadowElevation = 6.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                if (showCopy) AddressBarEditMenuAction(stringResource(R.string.action_copy), onCopy)
                if (showPaste) {
                    AddressBarEditMenuAction(stringResource(R.string.address_bar_paste), onPaste)
                    AddressBarEditMenuAction(stringResource(R.string.address_bar_paste_and_go), onPasteAndGo)
                }
            }
        }
    }
}

@Composable
private fun AddressBarEditMenuAction(text: String, onClick: () -> Unit) {
    val colors = LocalClintColors.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = text,
            color = colors.popupText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
