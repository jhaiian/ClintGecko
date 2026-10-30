package com.jhaiian.clint.browser
import com.jhaiian.clint.browser.home.ClintHomeScreen
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward

import com.jhaiian.clint.browser.delegates.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jhaiian.clint.tabs.TabMenuScreen
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.viewinterop.AndroidView
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
internal fun MainScreen(activity: MainActivity, state: MainUiState) {
    val density = LocalDensity.current
    val colors = LocalClintColors.current
    var tabSwitcherOpen by remember { mutableStateOf(false) }
    val hideStatusBar = state.hideStatusBar
    val hideSystemNavigation = state.hideSystemNavigation
    val rawStatusBarPx = WindowInsets.statusBars.getTop(density)
    val rawNavBarPx = WindowInsets.navigationBars.getBottom(density)
    val rawImePx = WindowInsets.ime.getBottom(density)
    @OptIn(ExperimentalLayoutApi::class)
    val imeTargetPx = WindowInsets.imeAnimationTarget.getBottom(density)

    LaunchedEffect(rawStatusBarPx) {
        if (rawStatusBarPx > 0) state.cachedStatusBarInsetPx = rawStatusBarPx
    }
    LaunchedEffect(imeTargetPx, state.searchOverlayOpen) {
        state.imeInsetPx = if (state.searchOverlayOpen) 0 else imeTargetPx
        activity.updateMainContentInsets()
    }
    LaunchedEffect(rawNavBarPx) {
        if (rawNavBarPx > 0) state.navBarInsetPx = rawNavBarPx
    }
    val effectiveStatusBarPx = if (hideStatusBar) {
        0
    } else {
        rawStatusBarPx.takeIf { it > 0 } ?: state.cachedStatusBarInsetPx
    }
    val effectiveNavBarPx = if (hideSystemNavigation) 0 else state.navBarInsetPx
    val systemNavPx = if (hideSystemNavigation || state.isFullscreen) 0 else rawNavBarPx
    LaunchedEffect(systemNavPx) {
        state.systemNavInsetPx = systemNavPx
        activity.updateMainContentInsets()
    }
    LaunchedEffect(effectiveStatusBarPx) {
        state.statusBarInsetPx = effectiveStatusBarPx
        activity.updateMainContentInsets()
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.surface)) {

        AndroidView(
            factory = { activity.swipeRefreshView },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                view.setPadding(0, state.contentPaddingTopPx, 0, if (state.isFullscreen) 0 else maxOf(state.contentPaddingBottomPx, state.imeInsetPx))
            }
        )

        if (state.isHomePage && !state.isFullscreen) {
            ClintHomeScreen(
                activity = activity,
                state = state,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = with(density) { state.contentPaddingTopPx.toDp() },
                        bottom = with(density) { state.contentPaddingBottomPx.toDp() }
                    )
            )
        }

        val openTabSwitcher: () -> Unit = {
            activity.captureActiveTabThumbnail()
            tabSwitcherOpen = true
        }
        val openTabGroup: (String) -> Unit = { groupId ->
            state.tabMenuFocusGroupId = groupId
            openTabSwitcher()
        }
        val tabStripAtTop = resolveTabBarAtTop(state.tabStripPosition, state.addressBarPosition)
        val tabGroupBarAtTop = resolveTabBarAtTop(state.tabGroupBarPosition, state.addressBarPosition)
        val chromeVisible = !state.isFullscreen && !state.isShortcutFrameless

        if (!state.isFullscreen && !state.isShortcutFrameless && (state.addressBarPosition == AddressBarPosition.TOP || state.addressBarPosition == AddressBarPosition.SPLIT)) {
            TopToolbar(
                activity = activity,
                state = state,
                statusBarPaddingPx = effectiveStatusBarPx,
                onTabCountClick = openTabSwitcher,
                onOpenTabGroup = openTabGroup,
                showTabStrip = tabStripAtTop,
                showTabGroupBar = tabGroupBarAtTop,
                modifier = Modifier.align(Alignment.TopStart)
            )
        }

        if (!state.isFullscreen && !state.isShortcutFrameless && state.addressBarPosition == AddressBarPosition.SPLIT) {
            BottomNavBar(
                activity = activity,
                state = state,
                navBarPaddingPx = effectiveNavBarPx,
                showTabStrip = !tabStripAtTop,
                showTabGroupBar = !tabGroupBarAtTop,
                onOpenTabGroup = openTabGroup,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }

        if (!state.isFullscreen && !state.isShortcutFrameless && state.addressBarPosition == AddressBarPosition.BOTTOM) {
            BottomToolbar(
                activity = activity,
                state = state,
                bottomPaddingPx = maxOf(rawImePx, effectiveNavBarPx),
                onTabCountClick = openTabSwitcher,
                onOpenTabGroup = openTabGroup,
                showTabStrip = !tabStripAtTop,
                showTabGroupBar = !tabGroupBarAtTop,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }

        if (chromeVisible && state.addressBarPosition == AddressBarPosition.BOTTOM && (tabStripAtTop || tabGroupBarAtTop)) {
            TopTabBarsAccessory(
                activity = activity,
                state = state,
                statusBarPaddingPx = effectiveStatusBarPx,
                showTabStrip = tabStripAtTop,
                showTabGroupBar = tabGroupBarAtTop,
                onOpenTabGroup = openTabGroup,
                modifier = Modifier.align(Alignment.TopStart)
            )
        }

        if (chromeVisible && state.addressBarPosition == AddressBarPosition.TOP && (!tabStripAtTop || !tabGroupBarAtTop)) {
            BottomTabBarsAccessory(
                activity = activity,
                state = state,
                navBarPaddingPx = effectiveNavBarPx,
                showTabStrip = !tabStripAtTop,
                showTabGroupBar = !tabGroupBarAtTop,
                onOpenTabGroup = openTabGroup,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }

        com.jhaiian.clint.translation.TranslateBar(
            request = state.translateBarRequest,
            visible = !state.isFullscreen && !state.searchOverlayOpen && !tabSwitcherOpen,
            activeTabId = state.activeTabId,
            onTranslate = { view, from, to -> activity.translatePage(view, from, to) },
            onRestore = { view -> activity.restoreTranslatedPage(view) },
            onOpenSettings = { activity.openTranslationSettings() },
            onDismiss = { state.translateBarRequest = null },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(
                    top = with(density) { state.contentPaddingTopPx.toDp() } + 8.dp,
                    start = 12.dp,
                    end = 12.dp
                )
        )

        AnimatedVisibility(
            visible = state.searchOverlayOpen,
            enter = fadeIn(tween(200)) + expandVertically(
                animationSpec = tween(300),
                expandFrom = if (state.searchOverlayIsBottom) Alignment.Bottom else Alignment.Top
            ),
            exit = fadeOut(tween(150)) + shrinkVertically(
                animationSpec = tween(250),
                shrinkTowards = if (state.searchOverlayIsBottom) Alignment.Bottom else Alignment.Top
            ),
            modifier = Modifier.align(if (state.searchOverlayIsBottom) Alignment.BottomStart else Alignment.TopStart)
        ) {
            SearchOverlay(
                initialText = state.searchQuery,
                isBottom = state.searchOverlayIsBottom,
                hint = stringResource(
                    R.string.search_bar_hint,
                    engineDisplayName(
                        searchEngineFor(activity.prefs, state.isIncognito),
                        customSearchEngineName(activity.prefs, state.isIncognito)
                    )
                ),
                suggestions = state.suggestions,
                statusBarPaddingPx = effectiveStatusBarPx,
                navBarPaddingPx = effectiveNavBarPx,
                onQueryChange = { activity.onSearchQueryChanged(it) },
                onSubmit = { activity.onSearchSubmitted(it) },
                onVoiceSearch = { activity.handleVoiceSearchTap() },
                onClose = { activity.closeSearchOverlay() },
                onSuggestionClick = { activity.onSuggestionChosen(it) },
                onSuggestionDelete = { activity.onSuggestionHistoryDelete(it) },
                modifier = Modifier.fillMaxSize(),
                incognitoKeyboard = state.isIncognito && com.jhaiian.clint.privacy.IncognitoKeyboard.isEnabled(activity)
            )
        }

        AnimatedVisibility(
            visible = tabSwitcherOpen,
            enter = fadeIn(tween(220)) + scaleIn(initialScale = 0.92f, animationSpec = tween(220)),
            exit = fadeOut(tween(160)) + scaleOut(targetScale = 0.94f, animationSpec = tween(160))
        ) {
            TabMenuScreen(activity = activity, onDismiss = { tabSwitcherOpen = false })
        }

        state.imageLongPressRequest?.let { req ->
            com.jhaiian.clint.browser.sheets.ImageLongPressSheet(request = req, activity = activity, onDismiss = { state.imageLongPressRequest = null })
        }
        state.linkLongPressRequest?.let { req ->
            com.jhaiian.clint.browser.sheets.LinkLongPressSheet(request = req, activity = activity, onDismiss = { state.linkLongPressRequest = null })
        }
        state.contentPreviewRequest?.let { req ->
            com.jhaiian.clint.browser.sheets.ContentPreviewSheet(request = req, activity = activity, onDismiss = { state.contentPreviewRequest = null })
        }

        val hideStatusBarPref = remember { androidx.preference.PreferenceManager.getDefaultSharedPreferences(activity).getBoolean("hide_status_bar", false) }
        val hideSystemNavigationPref = remember { androidx.preference.PreferenceManager.getDefaultSharedPreferences(activity).getBoolean("hide_system_navigation", false) }
        com.jhaiian.clint.ui.listscreen.ConfirmDialogHost(state.confirmDialogConfig, hideStatusBarPref, hideSystemNavigationPref) { state.confirmDialogConfig = null }
        state.conflictDialogRequest?.let { req ->
            com.jhaiian.clint.downloads.DownloadConflictDialog(req, hideStatusBarPref, hideSystemNavigationPref) { state.conflictDialogRequest = null }
        }
        state.webPermissionDialogRequest?.let { req ->
            com.jhaiian.clint.ui.WebPermissionDialog(req, hideStatusBarPref, hideSystemNavigationPref) { state.webPermissionDialogRequest = null }
        }
        state.selectPickerRequest?.let { req ->
            com.jhaiian.clint.browser.dialogs.SelectPickerDialog(req, hideStatusBarPref, hideSystemNavigationPref) {
                req.onCancel?.invoke()
                if (state.selectPickerRequest === req) state.selectPickerRequest = null
            }
        }
        state.dateTimePickerRequest?.let { req ->
            com.jhaiian.clint.browser.dialogs.DateTimePickerDialog(req, hideStatusBarPref, hideSystemNavigationPref) { state.dateTimePickerRequest = null }
        }
        state.colorPickerRequest?.let { req ->
            com.jhaiian.clint.browser.dialogs.ColorPickerDialog(req, hideStatusBarPref, hideSystemNavigationPref) { state.colorPickerRequest = null }
        }
        state.jsDialogRequest?.let { req ->
            com.jhaiian.clint.browser.dialogs.JsDialog(req, hideStatusBarPref, hideSystemNavigationPref) { state.jsDialogRequest = null }
        }
        state.httpAuthRequest?.let { req ->
            com.jhaiian.clint.browser.dialogs.HttpAuthDialog(req, hideStatusBarPref, hideSystemNavigationPref) { state.httpAuthRequest = null }
        }
        state.sslWarningRequest?.let { req ->
            com.jhaiian.clint.browser.dialogs.SslWarningDialog(req, hideStatusBarPref, hideSystemNavigationPref) { state.sslWarningRequest = null }
        }
        state.httpsOnlyRequest?.let { req ->
            com.jhaiian.clint.browser.dialogs.HttpsOnlyDialog(req, hideStatusBarPref, hideSystemNavigationPref) { state.httpsOnlyRequest = null }
        }
        if (!state.isFullscreen) {
            state.popupAlertRequest?.let { req ->
                com.jhaiian.clint.browser.dialogs.PopupAlertDialog(req, hideStatusBarPref, hideSystemNavigationPref) { state.popupAlertRequest = null }
            }
        }
        state.refreshLinkDialogRequest?.let { req ->
            com.jhaiian.clint.browser.dialogs.RefreshLinkDialog(req, hideStatusBarPref, hideSystemNavigationPref) { state.refreshLinkDialogRequest = null }
        }
        state.siteInfoRequest?.let { req ->
            com.jhaiian.clint.browser.siteinfo.SiteInfoSheet(
                request = req,
                hideStatusBar = hideStatusBarPref,
                hideSystemNavigation = hideSystemNavigationPref,
                onPermissionChange = { type, newState -> activity.changeSiteInfoPermission(req.host, type, newState) },
                onResetPermissions = { activity.resetSiteInfoPermissions(req.host) },
                onTrackingProtectionChange = { enabled -> activity.setSiteInfoTrackingProtection(req.host, enabled) },
                onCookiesClick = { activity.openSiteInfoCookies() },
                onSiteSettingsClick = { activity.openSiteInfoSiteSettings() },
                onDismiss = { state.siteInfoRequest = null }
            )
        }
        state.cookiesControlRequest?.let { req ->
            com.jhaiian.clint.browser.dialogs.CookiesControlDialog(req, hideStatusBarPref, hideSystemNavigationPref) { state.cookiesControlRequest = null }
        }
        state.cookiesControlExportRequest?.let { req ->
            com.jhaiian.clint.browser.dialogs.CookiesControlExportDialog(req, hideStatusBarPref, hideSystemNavigationPref) { state.cookiesControlExportRequest = null }
        }
        state.cookiesControlImportRequest?.let { req ->
            com.jhaiian.clint.browser.dialogs.CookiesControlImportDialog(req, hideStatusBarPref, hideSystemNavigationPref) { state.cookiesControlImportRequest = null }
        }
        if (state.profilePickerOpen) {
            com.jhaiian.clint.profiles.ProfilePickerDialog(
                profiles = remember { com.jhaiian.clint.profiles.ProfileRepository.all(activity) },
                hideStatusBar = hideStatusBarPref,
                hideSystemNavigation = hideSystemNavigationPref,
                onPick = { id -> state.profilePickerOpen = false; activity.onNewTabInProfile(id) },
                onManage = {
                    state.profilePickerOpen = false
                    activity.startActivity(android.content.Intent(activity, com.jhaiian.clint.profiles.ProfilesActivity::class.java))
                },
                onDismiss = { state.profilePickerOpen = false }
            )
        }
        state.tabStripNewGroupTabId?.let { tabId ->
            com.jhaiian.clint.tabs.TabGroupEditorDialog(
                title = stringResource(R.string.tab_group_new),
                positiveLabel = stringResource(R.string.tab_group_create),
                initialName = "",
                initialColor = remember(tabId) { com.jhaiian.clint.tabs.TabGroupColor.next(activity.tabManager.groups.map { it.color }) },
                hideStatusBar = hideStatusBarPref,
                hideSystemNavigation = hideSystemNavigationPref,
                onDismiss = { state.tabStripNewGroupTabId = null },
                onConfirm = { name, color ->
                    state.tabStripNewGroupTabId = null
                    activity.addTabToNewGroup(tabId, name, color)
                }
            )
        }
        state.createShortcutRequest?.let { req ->
            com.jhaiian.clint.browser.dialogs.CreateShortcutDialog(req, activity, hideStatusBarPref, hideSystemNavigationPref) { state.createShortcutRequest = null }
        }
        state.openInAppRequest?.let { req ->
            com.jhaiian.clint.browser.webview.OpenInAppDialog(req, hideStatusBarPref, hideSystemNavigationPref) { state.openInAppRequest = null }
        }
        if (state.bookmarkFolderDialogOpen) {
            com.jhaiian.clint.bookmarks.MoveToFolderDialog(
                tree = state.bookmarkFolderTree,
                hideStatusBar = hideStatusBarPref, hideSystemNavigation = hideSystemNavigationPref,
                onDismiss = { activity.dismissBookmarkFolderDialog() },
                onSelect = { folderId -> activity.confirmPendingBookmark(folderId) },
                title = stringResource(R.string.bookmarks_save_to_title)
            )
        }
        state.websiteBlockedRequest?.let { req ->
            com.jhaiian.clint.blocker.blockedpage.WebsiteBlockedOverlay(
                request = req,
                statusBarPaddingPx = effectiveStatusBarPx,
                navBarPaddingPx = effectiveNavBarPx,
                onReturnToPrevious = { activity.dismissWebsiteBlockedOverlay() }
            )
        }

        if (!state.isFullscreen && effectiveStatusBarPx > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .height(with(density) { effectiveStatusBarPx.toDp() })
                    .background(colors.surface)
            )
        }
    }
}

@Composable
private fun TopToolbar(
    activity: MainActivity,
    state: MainUiState,
    statusBarPaddingPx: Int,
    onTabCountClick: () -> Unit,
    onOpenTabGroup: (String) -> Unit,
    showTabStrip: Boolean,
    showTabGroupBar: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalClintColors.current
    val density = LocalDensity.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                val contentBarHeight = (state.topBarFullHeightPx - state.statusBarInsetPx).toFloat()
                translationY = -state.topBarFraction * contentBarHeight
            }
            .onGloballyPositioned { coordinates ->
                val h = coordinates.size.height
                if (h > 0 && state.topBarFullHeightPx != h) {
                    state.topBarFullHeightPx = h
                    activity.swipeRefreshView.setProgressViewOffset(false, h + 4, h + 72)
                    activity.updateMainContentInsets()
                }
            }
            .background(colors.surface)
            .padding(top = with(density) { statusBarPaddingPx.toDp() })
    ) {
        if (showTabStrip) TabStripSlot(activity, state, onOpenTabGroup)
        AddressBarRow(
            activity = activity,
            isIncognito = state.isIncognito,
            addressBarText = state.addressBarTextTop,
            isSecure = state.addressBarSecureTop,
            tabCountText = state.tabCountText,
            isMediaCaptureEnabled = state.isMediaCaptureEnabled,
            activeTabId = state.activeTabId,
            onAddressBarClick = { activity.openSearchOverlay(isBottom = false) },
            onTabCountClick = onTabCountClick,
            onMediaCaptureClick = { activity.mountMediaCaptureDialog() },
            showHomeButton = state.showHomeButton,
            onHomeClick = { activity.navGoHome() },
            onSwipeTabChange = { direction -> activity.onSwipeTabChange(direction) },
            onSiteInfoClick = { if (!activity.openSiteInfo()) activity.openSearchOverlay(isBottom = false) },
            openTabsSwipeDirection = 1
        )
        if (showTabGroupBar) TabGroupBarSlot(activity, state, onOpenTabGroup)
        PageLoadProgress(state)
    }
}

private fun resolveTabBarAtTop(position: String, addressBarPosition: AddressBarPosition): Boolean =
    when (com.jhaiian.clint.tabs.TabBarPlacement.normalize(position)) {
        com.jhaiian.clint.tabs.TabBarPlacement.TOP -> true
        com.jhaiian.clint.tabs.TabBarPlacement.BOTTOM -> false
        else -> addressBarPosition != AddressBarPosition.BOTTOM
    }

@Composable
private fun TopTabBarsAccessory(
    activity: MainActivity,
    state: MainUiState,
    statusBarPaddingPx: Int,
    showTabStrip: Boolean,
    showTabGroupBar: Boolean,
    onOpenTabGroup: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalClintColors.current
    val density = LocalDensity.current
    DisposableEffect(Unit) {
        onDispose {
            state.topBarFullHeightPx = 0
            activity.updateMainContentInsets()
        }
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                val contentBarHeight = (state.topBarFullHeightPx - state.statusBarInsetPx).toFloat()
                translationY = -state.topBarFraction * contentBarHeight
            }
            .onGloballyPositioned { coordinates ->
                val h = coordinates.size.height
                if (state.topBarFullHeightPx != h) {
                    state.topBarFullHeightPx = h
                    activity.swipeRefreshView.setProgressViewOffset(false, h + 4, h + 72)
                    activity.updateMainContentInsets()
                }
            }
            .background(colors.surface)
            .padding(top = with(density) { statusBarPaddingPx.toDp() })
    ) {
        if (showTabStrip) TabStripSlot(activity, state, onOpenTabGroup)
        if (showTabGroupBar) TabGroupBarSlot(activity, state, onOpenTabGroup)
    }
}

@Composable
private fun BottomTabBarsAccessory(
    activity: MainActivity,
    state: MainUiState,
    navBarPaddingPx: Int,
    showTabStrip: Boolean,
    showTabGroupBar: Boolean,
    onOpenTabGroup: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalClintColors.current
    val density = LocalDensity.current
    DisposableEffect(Unit) {
        onDispose {
            state.bottomBarFullHeightPx = 0
            activity.updateMainContentInsets()
        }
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = state.bottomBarFraction * state.bottomBarFullHeightPx }
            .onGloballyPositioned { coordinates ->
                val h = coordinates.size.height
                if (state.bottomBarFullHeightPx != h) {
                    state.bottomBarFullHeightPx = h
                    activity.updateMainContentInsets()
                }
            }
            .background(colors.surface)
            .padding(bottom = with(density) { navBarPaddingPx.toDp() })
    ) {
        if (showTabGroupBar) TabGroupBarSlot(activity, state, onOpenTabGroup)
        if (showTabStrip) TabStripSlot(activity, state, onOpenTabGroup)
    }
}

@Composable
private fun TabStripSlot(activity: MainActivity, state: MainUiState, onOpenTabGroup: (String) -> Unit) {
    val strip = state.tabStrip
    AnimatedVisibility(
        visible = strip != null,
        enter = fadeIn(tween(180)) + expandVertically(tween(220)),
        exit = fadeOut(tween(140)) + shrinkVertically(tween(200))
    ) {
        val lastStrip = remember { mutableStateOf(strip) }
        if (strip != null) lastStrip.value = strip
        lastStrip.value?.let { shown ->
            com.jhaiian.clint.tabs.TabStrip(
                state = shown,
                onSelectTab = { id -> activity.focusTabById(id) },
                onCloseTab = { id -> activity.closeTabFromStrip(id) },
                onCloseOtherTabs = { id -> activity.closeOtherTabsFromStrip(id) },
                onNewTab = { activity.openNewTabFromStrip() },
                onNewTabInGroup = { groupId -> activity.openNewTabInGroup(groupId) },
                onAddToNewGroup = { id -> state.tabStripNewGroupTabId = id },
                onRemoveFromGroup = { id -> activity.removeTabFromGroup(id) },
                onToggleGroup = { groupId -> activity.toggleTabGroupCollapsed(groupId) },
                onOpenGroup = onOpenTabGroup
            )
        }
    }
}

@Composable
private fun TabGroupBarSlot(activity: MainActivity, state: MainUiState, onOpenTabGroup: (String) -> Unit) {
    val bar = state.tabGroupBar
    AnimatedVisibility(
        visible = bar != null,
        enter = fadeIn(tween(180)) + expandVertically(tween(220)),
        exit = fadeOut(tween(140)) + shrinkVertically(tween(200))
    ) {
        val lastBar = remember { mutableStateOf(bar) }
        if (bar != null) lastBar.value = bar
        lastBar.value?.let { shown ->
            com.jhaiian.clint.tabs.TabGroupBar(
                state = shown,
                onSelectTab = { id -> activity.focusTabById(id) },
                onNewTab = { activity.openNewTabInGroup(shown.group.id) },
                onOpenGroup = { onOpenTabGroup(shown.group.id) }
            )
        }
    }
}

@Composable
private fun PageLoadProgress(state: MainUiState) {
    val colors = LocalClintColors.current
    Box(modifier = Modifier.fillMaxWidth().height(4.dp)) {
        if (state.isPageLoading) {
            val shownProgress by animateFloatAsState(
                targetValue = maxOf(state.pageLoadProgress, 20) / 100f,
                animationSpec = tween(250),
                label = "pageLoadProgress"
            )
            LinearProgressIndicator(
                progress = { shownProgress },
                color = colors.primary,
                trackColor = colors.surface,
                modifier = Modifier.fillMaxWidth().height(4.dp)
            )
        }
    }
}

@Composable
private fun BottomToolbar(
    activity: MainActivity,
    state: MainUiState,
    bottomPaddingPx: Int,
    onTabCountClick: () -> Unit,
    onOpenTabGroup: (String) -> Unit,
    showTabStrip: Boolean,
    showTabGroupBar: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalClintColors.current
    val density = LocalDensity.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = state.bottomBarFraction * state.bottomBarFullHeightPx }
            .onGloballyPositioned { coordinates ->
                val h = coordinates.size.height
                if (h > 0 && state.bottomBarFullHeightPx != h) {
                    state.bottomBarFullHeightPx = h
                    activity.updateMainContentInsets()
                }
            }
            .background(colors.surface)
            .padding(bottom = with(density) { bottomPaddingPx.toDp() })
    ) {
        PageLoadProgress(state)
        if (showTabGroupBar) TabGroupBarSlot(activity, state, onOpenTabGroup)
        if (showTabStrip) TabStripSlot(activity, state, onOpenTabGroup)
        AddressBarRow(
            activity = activity,
            isIncognito = state.isIncognito,
            addressBarText = state.addressBarTextBottom,
            isSecure = state.addressBarSecureBottom,
            tabCountText = state.tabCountText,
            isMediaCaptureEnabled = state.isMediaCaptureEnabled,
            activeTabId = state.activeTabId,
            onAddressBarClick = { activity.openSearchOverlay(isBottom = true) },
            onTabCountClick = onTabCountClick,
            onMediaCaptureClick = { activity.mountMediaCaptureDialog() },
            showHomeButton = state.showHomeButton,
            onHomeClick = { activity.navGoHome() },
            onSwipeTabChange = { direction -> activity.onSwipeTabChange(direction) },
            onSiteInfoClick = { if (!activity.openSiteInfo()) activity.openSearchOverlay(isBottom = true) },
            openTabsSwipeDirection = -1,
            isBottomBar = true
        )
    }
}

@Composable
private fun BottomNavBar(
    activity: MainActivity,
    state: MainUiState,
    navBarPaddingPx: Int,
    showTabStrip: Boolean,
    showTabGroupBar: Boolean,
    onOpenTabGroup: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalClintColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = state.bottomBarFraction * state.bottomBarFullHeightPx }
            .onGloballyPositioned { coordinates ->
                val h = coordinates.size.height
                if (h > 0 && state.bottomBarFullHeightPx != h) {
                    state.bottomBarFullHeightPx = h
                    activity.updateMainContentInsets()
                }
            }
            .background(colors.surface)
    ) {
        if (showTabGroupBar) TabGroupBarSlot(activity, state, onOpenTabGroup)
        if (showTabStrip) TabStripSlot(activity, state, onOpenTabGroup)
        BottomNavButtons(activity, state, navBarPaddingPx)
    }
}

@Composable
private fun BottomNavButtons(activity: MainActivity, state: MainUiState, navBarPaddingPx: Int) {
    val density = LocalDensity.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(bottom = with(density) { navBarPaddingPx.toDp() }),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly
    ) {
        NavIconButton(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), enabled = state.canGoBack) { activity.navGoBack() }
        NavIconButton(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowForward, stringResource(R.string.forward), enabled = state.canGoForward) { activity.navGoForward() }
        NavIconButton(androidx.compose.material.icons.Icons.Filled.Home, stringResource(R.string.home), enabled = true) { activity.navGoHome() }
        NavIconButton(
            if (state.isPageLoading) androidx.compose.material.icons.Icons.Filled.Close else androidx.compose.material.icons.Icons.Filled.Refresh,
            stringResource(R.string.refresh),
            enabled = true
        ) { activity.navRefreshOrStop() }
        NavIconButton(
            if (state.isBookmarked) androidx.compose.material.icons.Icons.Filled.Bookmark else androidx.compose.material.icons.Icons.Filled.BookmarkBorder,
            stringResource(R.string.content_desc_bookmark),
            enabled = state.hasActiveUrl
        ) { activity.navToggleBookmark() }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NavIconButton(
    iconRes: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalClintColors.current
    IconButton(
        onClick = onClick,
        modifier = Modifier.weight(1f).fillMaxSize()
    ) {
        Icon(
            imageVector = iconRes,
            contentDescription = description,
            tint = colors.iconTint,
            modifier = Modifier.alpha(if (enabled) 1.0f else 0.38f)
        )
    }
}

@Composable
internal fun engineDisplayName(engine: String, customName: String): String = when (engine) {
    "brave" -> stringResource(R.string.engine_brave)
    "ecosia" -> stringResource(R.string.engine_ecosia)
    "google" -> stringResource(R.string.engine_google)
    "custom" -> customName.ifBlank { stringResource(R.string.engine_custom) }
    else -> stringResource(R.string.engine_duckduckgo)
}
