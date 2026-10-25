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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
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
            activity.prefs.getString("search_engine", "duckduckgo") ?: "duckduckgo",
            customSearchEngineName(activity.prefs)
        )
    )
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
                .clickable(onClick = onAddressBarClick)
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
                .size(width = 34.dp, height = 26.dp)
                .border(2.dp, colors.primary, RoundedCornerShape(5.dp))
                .clickable(onClick = onTabCountClick)
        ) {
            Text(
                text = tabCountText,
                color = colors.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
        Box(modifier = Modifier.padding(start = 4.dp).size(40.dp), contentAlignment = Alignment.Center) {
            com.jhaiian.clint.browser.menu.MenuTriggerButton(activity)
        }
    }
}
