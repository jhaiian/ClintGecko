package com.jhaiian.clint.browser.menu

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.bookmarks.BookmarkManager
import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.browser.delegates.openSiteInfo
import com.jhaiian.clint.browser.home.isClintHomeUrl
import com.jhaiian.clint.browser.sheets.LongPressContentMaxWidth
import com.jhaiian.clint.downloads.ClintDownloadManager
import com.jhaiian.clint.downloads.DownloadStatus
import com.jhaiian.clint.settings.datasaver.DataSaverSiteException
import com.jhaiian.clint.ui.ClintDialogStatusBarEffect
import com.jhaiian.clint.ui.ClintSheetWindowInsets
import com.jhaiian.clint.ui.ClintSwitch
import com.jhaiian.clint.ui.listscreen.PopupShape
import com.jhaiian.clint.ui.rememberClintFavicon
import com.jhaiian.clint.ui.rememberSheetFlingConnection
import com.jhaiian.clint.ui.theme.LocalClintColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class BrowserMenuPageState(
    val isBookmarked: Boolean,
    val isDataSaverExceptionForSite: Boolean,
    val openInAppEnabled: Boolean,
    val openInAppLabel: String?
)

internal data class BrowserMenuSnapshot(
    val url: String,
    val title: String,
    val isSecure: Boolean,
    val isWebPage: Boolean,
    val showNavRow: Boolean,
    val canGoBack: Boolean,
    val canGoForward: Boolean,
    val page: BrowserMenuPageState,
    val isLoading: Boolean,
    val isDesktopMode: Boolean,
    val isDataSaverEnabled: Boolean,
    val pendingDownloadCount: Long,
    val isMediaCaptureEnabled: Boolean,
    val isWebsiteBlockerEnabled: Boolean,
    val canCreateShortcut: Boolean,
    val canToggleTabMute: Boolean,
    val isTabMuted: Boolean,
    val canUseCookiesControl: Boolean,
    val canTranslatePage: Boolean,
    val isPageTranslated: Boolean,
    val layout: BrowserMenuLayout
)

internal class BrowserMenuActions(
    val onGoBack: () -> Unit = {},
    val onGoForward: () -> Unit = {},
    val onHome: () -> Unit = {},
    val onRefreshOrStop: () -> Unit = {},
    val onToggleBookmark: () -> Unit = {},
    val onNewTab: () -> Unit = {},
    val onIncognito: () -> Unit = {},
    val onNewProfileTab: () -> Unit = {},
    val onOpenProfiles: () -> Unit = {},
    val onShare: () -> Unit = {},
    val onOpenInApp: () -> Unit = {},
    val onCreateShortcut: () -> Unit = {},
    val onCreateFavorite: () -> Unit = {},
    val onOpenShortcutManager: () -> Unit = {},
    val onDownloads: () -> Unit = {},
    val onOpenDownloadSettings: () -> Unit = {},
    val onMediaCapture: () -> Unit = {},
    val onExtensionClick: (String, Boolean) -> Unit = { _, _ -> },
    val onOpenExtensionSettings: (String) -> Unit = {},
    val onOpenExtensionsSettings: () -> Unit = {},
    val onBookmarks: () -> Unit = {},
    val onHistory: () -> Unit = {},
    val onDesktopMode: () -> Unit = {},
    val onSettings: () -> Unit = {},
    val onReaderMode: () -> Unit = {},
    val onFindInPage: () -> Unit = {},
    val onDataSaver: () -> Unit = {},
    val onOpenDataSaverSettings: () -> Unit = {},
    val onOpenDesktopModeSettings: () -> Unit = {},
    val onOpenDataSaverSiteSettings: () -> Unit = {},
    val onDisableDataSaverForSite: () -> Unit = {},
    val onToggleTabMute: () -> Unit = {},
    val onWebsiteBlocker: () -> Unit = {},
    val onOpenWebsiteBlockerSettings: () -> Unit = {},
    val onCookiesControl: () -> Unit = {},
    val onTranslate: () -> Unit = {},
    val onOpenTranslationSettings: () -> Unit = {},
    val onSiteInfo: () -> Unit = {},
    val onCustomize: () -> Unit = {}
)

internal fun MainActivity.buildMenuSnapshot(previous: BrowserMenuSnapshot?): BrowserMenuSnapshot {
    val position = prefs.getString("address_bar_position", "top") ?: "top"
    val tab = tabManager.activeTab
    val wv = tab?.webView
    val currentUrl = wv?.url ?: ""
    val cachedPage = previous?.takeIf { it.url == currentUrl }?.page
    val page = BrowserMenuPageState(
        isBookmarked = currentUrl.isNotEmpty() && uiState.hasActiveUrl && uiState.isBookmarked,
        isDataSaverExceptionForSite = cachedPage?.isDataSaverExceptionForSite ?: false,
        openInAppEnabled = cachedPage?.openInAppEnabled ?: false,
        openInAppLabel = cachedPage?.openInAppLabel
    )
    val isHttp = currentUrl.startsWith("http://") || currentUrl.startsWith("https://")

    return BrowserMenuSnapshot(
        url = currentUrl,
        title = tab?.title.orEmpty(),
        isSecure = currentUrl.startsWith("https://"),
        isWebPage = isHttp && !isClintHomeUrl(currentUrl),
        showNavRow = position != "split",
        canGoBack = wv?.canGoBack() == true,
        canGoForward = wv?.canGoForward() == true,
        page = page,
        isLoading = uiState.isPageLoading,
        isDesktopMode = isDesktopMode,
        isDataSaverEnabled = prefs.getBoolean("data_saver_enabled", false),
        pendingDownloadCount = ClintDownloadManager.downloadsFlow.value
            .count { it.status in DownloadStatus.RUNNING_OR_QUEUED }.toLong(),
        isMediaCaptureEnabled = prefs.getBoolean(com.jhaiian.clint.mediacapture.MEDIA_CAPTURE_ENABLED_PREF, true),
        isWebsiteBlockerEnabled = prefs.getBoolean("website_blocker_enabled", false),
        canCreateShortcut = currentUrl.isNotEmpty(),
        canToggleTabMute = wv != null && (wv.audioState.audible || wv.audioState.muted),
        isTabMuted = wv?.audioState?.muted == true,
        canUseCookiesControl = tab?.isIncognito == false && isHttp,
        canTranslatePage = isHttp,
        isPageTranslated = wv != null && wv.translator.state.value.isRequested,
        layout = BrowserMenuCustomizationStore.readLayout(prefs)
    )
}

internal suspend fun MainActivity.loadMenuPageState(url: String): BrowserMenuPageState {
    val webClient = tabManager.activeTab?.webView?.webViewClient
    val appContext = applicationContext
    return withContext(Dispatchers.IO) {
        val uri = url.takeIf { it.isNotEmpty() }?.let { runCatching { Uri.parse(it) }.getOrNull() }
        val appMatches = if (uri != null && webClient != null && (uri.scheme == "http" || uri.scheme == "https")) {
            runCatching { webClient.resolveAppMatches(uri, appContext) }.getOrDefault(emptyList())
        } else emptyList()
        val openInAppLabel = if (appMatches.size == 1) {
            val appLabel = runCatching { appMatches[0].loadLabel(appContext.packageManager).toString() }.getOrNull()
            appLabel?.let { getString(R.string.menu_open_in_named_app, it) }
        } else null
        BrowserMenuPageState(
            isBookmarked = url.isNotEmpty() && runCatching { BookmarkManager.isBookmarked(appContext, url) }.getOrDefault(false),
            isDataSaverExceptionForSite = runCatching { DataSaverSiteException.isExceptedUrl(appContext, url) }.getOrDefault(false),
            openInAppEnabled = appMatches.isNotEmpty(),
            openInAppLabel = openInAppLabel
        )
    }
}

internal fun MainActivity.buildMenuActions(dismiss: () -> Unit): BrowserMenuActions = BrowserMenuActions(
    onGoBack = { dismiss(); onMenuGoBack() },
    onGoForward = { dismiss(); onMenuGoForward() },
    onHome = { dismiss(); onMenuHome() },
    onRefreshOrStop = { dismiss(); onMenuRefreshOrStop() },
    onToggleBookmark = { dismiss(); onMenuToggleBookmark() },
    onNewTab = { dismiss(); onMenuNewTab() },
    onIncognito = { dismiss(); onMenuIncognito() },
    onNewProfileTab = { dismiss(); onMenuNewProfileTab() },
    onOpenProfiles = { dismiss(); onMenuOpenProfiles() },
    onShare = { dismiss(); onMenuShare() },
    onOpenInApp = { dismiss(); onMenuOpenInApp() },
    onCreateShortcut = { dismiss(); onMenuCreateShortcut() },
    onCreateFavorite = { dismiss(); onMenuCreateFavorite() },
    onOpenShortcutManager = { dismiss(); onMenuOpenShortcutManager() },
    onDownloads = { dismiss(); onMenuDownloads() },
    onOpenDownloadSettings = { dismiss(); onMenuOpenDownloadSettings() },
    onMediaCapture = { dismiss(); onMenuMediaCapture() },
    onExtensionClick = { id, actionable -> dismiss(); onMenuExtensionClick(id, actionable) },
    onOpenExtensionSettings = { id -> dismiss(); onMenuOpenExtensionSettings(id) },
    onOpenExtensionsSettings = { dismiss(); onMenuOpenExtensionsSettings() },
    onBookmarks = { dismiss(); onMenuBookmarks() },
    onHistory = { dismiss(); onMenuHistory() },
    onDesktopMode = { dismiss(); onMenuDesktopMode() },
    onSettings = { dismiss(); onMenuSettings() },
    onReaderMode = { dismiss(); onMenuReaderMode() },
    onFindInPage = { dismiss(); onMenuFindInPage() },
    onDataSaver = { dismiss(); onMenuDataSaver() },
    onOpenDataSaverSettings = { dismiss(); onMenuOpenDataSaverSettings() },
    onOpenDesktopModeSettings = { dismiss(); onMenuOpenDesktopModeSettings() },
    onOpenDataSaverSiteSettings = { dismiss(); onMenuOpenDataSaverSiteSettings() },
    onDisableDataSaverForSite = { dismiss(); onMenuDisableDataSaverForSite() },
    onToggleTabMute = { dismiss(); onMenuToggleTabMute() },
    onWebsiteBlocker = { dismiss(); onMenuWebsiteBlocker() },
    onOpenWebsiteBlockerSettings = { dismiss(); onMenuOpenWebsiteBlockerSettings() },
    onCookiesControl = { dismiss(); onMenuCookiesControl() },
    onTranslate = { dismiss(); onMenuTranslate() },
    onOpenTranslationSettings = { dismiss(); onMenuOpenTranslationSettings() },
    onSiteInfo = { dismiss(); openSiteInfo() },
    onCustomize = { dismiss(); onMenuCustomize() }
)

@Composable
internal fun MenuTriggerButton(activity: MainActivity, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    var snapshot by remember { mutableStateOf<BrowserMenuSnapshot?>(null) }
    val colors = LocalClintColors.current
    val configuration = LocalConfiguration.current
    val maxMenuHeight = configuration.screenHeightDp.dp * 0.9f
    val actions = remember(activity) { activity.buildMenuActions(dismiss = { expanded = false }) }

    LaunchedEffect(expanded) {
        if (!expanded) return@LaunchedEffect
        val url = snapshot?.url ?: return@LaunchedEffect
        val page = activity.loadMenuPageState(url)
        snapshot = snapshot?.let { current -> if (current.url == url) current.copy(page = page) else current }
    }

    Box(modifier = modifier) {
        IconButton(onClick = {
            snapshot = activity.buildMenuSnapshot(snapshot)
            expanded = true
        }) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.menu),
                tint = colors.iconTint
            )
        }

        val menuStyle = activity.prefs.getString("menu_style", "popup") ?: "popup"
        val currentSnapshot = snapshot
        if (menuStyle == "bottom_sheet") {
            if (expanded && currentSnapshot != null) {
                BrowserMenuBottomSheet(currentSnapshot, actions, onDismissRequest = { expanded = false })
            }
        } else {
            DropdownMenu(
                expanded = expanded && currentSnapshot != null,
                onDismissRequest = { expanded = false },
                modifier = Modifier.width(PopupMenuWidth).heightIn(max = maxMenuHeight),
                shape = PopupShape,
                containerColor = colors.popupBackground,
                border = BorderStroke(1.dp, colors.popupStroke)
            ) {
                if (currentSnapshot != null) BrowserMenuContent(currentSnapshot, actions, popup = true)
            }
        }
    }
}

internal val PopupMenuWidth = 288.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowserMenuBottomSheet(
    snapshot: BrowserMenuSnapshot,
    actions: BrowserMenuActions,
    onDismissRequest: () -> Unit
) {
    val colors = LocalClintColors.current
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val hideStatusBar = remember {
        PreferenceManager.getDefaultSharedPreferences(context).getBoolean("hide_status_bar", false)
    }
    val hideSystemNavigation = remember {
        PreferenceManager.getDefaultSharedPreferences(context).getBoolean("hide_system_navigation", false)
    }
    val listState = rememberLazyListState()

    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    val maxSheetHeight = if (isPortrait) {
        (configuration.screenHeightDp.dp * 0.85f).coerceAtLeast(320.dp)
    } else {
        (configuration.screenHeightDp.dp - 96.dp).coerceAtLeast(320.dp)
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val flingBoundaryConnection = rememberSheetFlingConnection(sheetState)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        contentWindowInsets = { ClintSheetWindowInsets },
        containerColor = colors.popupBackground,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.divider) }
    ) {
        ClintDialogStatusBarEffect(hideStatusBar, hideSystemNavigation)
        LazyColumn(
            Modifier.fillMaxWidth().heightIn(max = maxSheetHeight).nestedScroll(flingBoundaryConnection),
            state = listState
        ) {
            item {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    BrowserMenuContent(
                        snapshot,
                        actions,
                        popup = false,
                        modifier = Modifier.fillMaxWidth().widthIn(max = LongPressContentMaxWidth)
                    )
                }
            }
        }
    }
}

@Composable
internal fun BrowserMenuPreview(layout: BrowserMenuLayout, popup: Boolean, modifier: Modifier = Modifier) {
    val colors = LocalClintColors.current
    val snapshot = BrowserMenuSnapshot(
        url = "https://example.com",
        title = stringResource(R.string.app_name),
        isSecure = true,
        isWebPage = true,
        showNavRow = true,
        canGoBack = true,
        canGoForward = false,
        page = BrowserMenuPageState(isBookmarked = false, isDataSaverExceptionForSite = false, openInAppEnabled = false, openInAppLabel = null),
        isLoading = false,
        isDesktopMode = false,
        isDataSaverEnabled = false,
        pendingDownloadCount = 2L,
        isMediaCaptureEnabled = true,
        isWebsiteBlockerEnabled = true,
        canCreateShortcut = true,
        canToggleTabMute = true,
        isTabMuted = false,
        canUseCookiesControl = true,
        canTranslatePage = true,
        isPageTranslated = false,
        layout = layout
    )
    val actions = remember { BrowserMenuActions() }
    Box(modifier.clearAndSetSemantics {}) {
        if (popup) {
            Box(
                Modifier
                    .widthIn(max = PopupMenuWidth)
                    .fillMaxWidth()
                    .clip(PopupShape)
                    .background(colors.popupBackground)
                    .border(1.dp, colors.popupStroke, PopupShape)
                    .padding(vertical = 8.dp)
            ) {
                BrowserMenuContent(snapshot, actions, popup = true, preview = true)
            }
        } else {
            BrowserMenuContent(snapshot, actions, popup = false, preview = true, modifier = Modifier.fillMaxWidth())
        }
        Box(
            Modifier.matchParentSize().pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent()
                }
            }
        )
    }
}

internal class MenuEntryUi(
    val item: CustomizableMenuItem?,
    val icon: ImageVector,
    val label: String,
    val enabled: Boolean = true,
    val checked: Boolean? = null,
    val expanded: Boolean? = null,
    val badge: String? = null,
    val onClick: () -> Unit,
    val onLongClick: (() -> Unit)? = null
) {
    val active: Boolean get() = checked == true || expanded == true
}

@Composable
private fun menuEntryFor(
    item: CustomizableMenuItem,
    snapshot: BrowserMenuSnapshot,
    actions: BrowserMenuActions,
    extensionsExpanded: Boolean,
    onToggleExtensions: () -> Unit
): MenuEntryUi? {
    val label = stringResource(item.titleRes())
    return when (item) {
        CustomizableMenuItem.NEW_TAB -> MenuEntryUi(item, item.icon(), label, onClick = actions.onNewTab)
        CustomizableMenuItem.NEW_INCOGNITO_TAB -> MenuEntryUi(item, item.icon(), label, onClick = actions.onIncognito)
        CustomizableMenuItem.NEW_PROFILE_TAB -> if (com.jhaiian.clint.profiles.WebProfiles.isSupported()) {
            MenuEntryUi(item, item.icon(), label, onClick = actions.onNewProfileTab, onLongClick = actions.onOpenProfiles)
        } else null
        CustomizableMenuItem.SHARE -> MenuEntryUi(item, item.icon(), label, enabled = snapshot.isWebPage, onClick = actions.onShare)
        CustomizableMenuItem.OPEN_IN_APP -> MenuEntryUi(
            item, item.icon(), snapshot.page.openInAppLabel ?: label,
            enabled = snapshot.page.openInAppEnabled,
            onClick = actions.onOpenInApp
        )
        CustomizableMenuItem.CREATE_SHORTCUT -> MenuEntryUi(
            item, item.icon(), label,
            enabled = snapshot.canCreateShortcut,
            onClick = actions.onCreateShortcut,
            onLongClick = actions.onOpenShortcutManager
        )
        CustomizableMenuItem.CREATE_FAVORITE -> MenuEntryUi(
            item, item.icon(), label,
            enabled = snapshot.isWebPage,
            onClick = actions.onCreateFavorite
        )
        CustomizableMenuItem.DOWNLOADS -> MenuEntryUi(
            item, item.icon(), label,
            badge = if (snapshot.pendingDownloadCount > 0L) com.jhaiian.clint.ui.status.formatCompactCount(snapshot.pendingDownloadCount) else null,
            onClick = actions.onDownloads,
            onLongClick = actions.onOpenDownloadSettings
        )
        CustomizableMenuItem.MEDIA_CAPTURE -> MenuEntryUi(
            item, item.icon(), label,
            checked = snapshot.isMediaCaptureEnabled,
            onClick = actions.onMediaCapture
        )
        CustomizableMenuItem.EXTENSIONS -> MenuEntryUi(
            item, item.icon(), label,
            expanded = extensionsExpanded,
            onClick = onToggleExtensions,
            onLongClick = actions.onOpenExtensionsSettings
        )
        CustomizableMenuItem.WEBSITE_BLOCKER -> MenuEntryUi(
            item, item.icon(), label,
            checked = snapshot.isWebsiteBlockerEnabled,
            onClick = actions.onWebsiteBlocker,
            onLongClick = actions.onOpenWebsiteBlockerSettings
        )
        CustomizableMenuItem.BOOKMARKS -> MenuEntryUi(item, item.icon(), label, onClick = actions.onBookmarks)
        CustomizableMenuItem.HISTORY -> MenuEntryUi(item, item.icon(), label, onClick = actions.onHistory)
        CustomizableMenuItem.READER_MODE -> MenuEntryUi(item, item.icon(), label, onClick = actions.onReaderMode)
        CustomizableMenuItem.FIND_IN_PAGE -> MenuEntryUi(item, item.icon(), label, enabled = snapshot.isWebPage, onClick = actions.onFindInPage)
        CustomizableMenuItem.TRANSLATE -> MenuEntryUi(
            item, item.icon(), label,
            enabled = snapshot.canTranslatePage,
            checked = snapshot.isPageTranslated,
            onClick = actions.onTranslate,
            onLongClick = actions.onOpenTranslationSettings
        )
        CustomizableMenuItem.DESKTOP_MODE -> MenuEntryUi(
            item, item.icon(), label,
            checked = snapshot.isDesktopMode,
            onClick = actions.onDesktopMode,
            onLongClick = actions.onOpenDesktopModeSettings
        )
        CustomizableMenuItem.DATA_SAVER -> MenuEntryUi(
            item, item.icon(), label,
            checked = snapshot.isDataSaverEnabled,
            onClick = actions.onDataSaver,
            onLongClick = actions.onOpenDataSaverSettings
        )
        CustomizableMenuItem.MUTE_TAB -> if (snapshot.canToggleTabMute) {
            MenuEntryUi(
                item,
                if (snapshot.isTabMuted) Icons.AutoMirrored.Filled.VolumeUp else item.icon(),
                stringResource(if (snapshot.isTabMuted) R.string.menu_unmute_tab else item.titleRes()),
                onClick = actions.onToggleTabMute
            )
        } else null
        CustomizableMenuItem.COOKIES_CONTROL -> MenuEntryUi(
            item, item.icon(), label,
            enabled = snapshot.canUseCookiesControl,
            onClick = actions.onCookiesControl
        )
        CustomizableMenuItem.DISABLE_DATA_SAVER_FOR_SITE -> MenuEntryUi(
            item, item.icon(), label,
            checked = snapshot.page.isDataSaverExceptionForSite,
            onClick = actions.onDisableDataSaverForSite,
            onLongClick = actions.onOpenDataSaverSiteSettings
        )
    }
}

@Composable
private fun BrowserMenuContent(
    snapshot: BrowserMenuSnapshot,
    actions: BrowserMenuActions,
    popup: Boolean,
    modifier: Modifier = Modifier,
    preview: Boolean = false
) {
    val context = LocalContext.current
    val menuPrefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    var extensionsExpanded by remember { mutableStateOf(!preview && menuPrefs.getBoolean("extensions_menu_expanded", false)) }
    val toggleExtensions: () -> Unit = {
        extensionsExpanded = !extensionsExpanded
        if (!preview) menuPrefs.edit().putBoolean("extensions_menu_expanded", extensionsExpanded).apply()
    }
    val layout = snapshot.layout
    val grid = layout.itemsIn(MenuSection.GRID).mapNotNull { menuEntryFor(it, snapshot, actions, extensionsExpanded, toggleExtensions) }
    val toggles = layout.itemsIn(MenuSection.TOGGLES).mapNotNull { menuEntryFor(it, snapshot, actions, extensionsExpanded, toggleExtensions) }
    val list = layout.itemsIn(MenuSection.LIST).mapNotNull { menuEntryFor(it, snapshot, actions, extensionsExpanded, toggleExtensions) }
    val extensionsPanel: @Composable () -> Unit = {
        if (!preview) {
            ExtensionsMenuPanel(
                expanded = extensionsExpanded,
                onExtensionClick = actions.onExtensionClick,
                onExtensionSettings = actions.onOpenExtensionSettings,
                onManage = actions.onOpenExtensionsSettings
            )
        }
    }
    if (popup) {
        PopupMenuLayout(snapshot, actions, grid, toggles, list, extensionsPanel, modifier)
    } else {
        SheetMenuLayout(snapshot, actions, grid, toggles, list, extensionsPanel, preview, modifier)
    }
}

private fun List<MenuEntryUi>.hasExtensions(): Boolean = any { it.item == CustomizableMenuItem.EXTENSIONS }

@Composable
private fun SheetMenuLayout(
    snapshot: BrowserMenuSnapshot,
    actions: BrowserMenuActions,
    grid: List<MenuEntryUi>,
    toggles: List<MenuEntryUi>,
    list: List<MenuEntryUi>,
    extensionsPanel: @Composable () -> Unit,
    preview: Boolean,
    modifier: Modifier
) {
    val layout = snapshot.layout
    val compact = layout.compact
    Column(
        modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp)
    ) {
        if (layout.showHeader && snapshot.isWebPage) {
            MenuPageHeader(snapshot, preview, actions.onSiteInfo)
        }
        if (snapshot.showNavRow && snapshot.layout.showNavBar) {
            MenuNavRow(snapshot, actions, height = if (compact) 48.dp else 56.dp, corner = 20.dp)
        }
        if (grid.isNotEmpty()) {
            MenuGrid(
                grid,
                columns = layout.gridColumns,
                iconBox = if (compact) 44.dp else 52.dp,
                labels = layout.gridLabels,
                rowGap = if (compact) 4.dp else 8.dp
            )
            if (grid.hasExtensions()) extensionsPanel()
        }
        if (toggles.isNotEmpty()) {
            MenuChips(toggles, compact)
            if (toggles.hasExtensions()) extensionsPanel()
        }
        if (list.isNotEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(LocalClintColors.current.surfaceVariant.copy(alpha = 0.5f))
                    .padding(vertical = 4.dp)
            ) {
                list.forEach { entry ->
                    MenuItemRow(entry, height = if (compact) 46.dp else 52.dp)
                    if (entry.item == CustomizableMenuItem.EXTENSIONS) extensionsPanel()
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MenuFooterButton(Icons.Filled.Settings, stringResource(R.string.settings), filled = true, onClick = actions.onSettings, modifier = Modifier.weight(1f))
            MenuFooterButton(Icons.Filled.Edit, stringResource(R.string.menu_customize), filled = false, onClick = actions.onCustomize, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PopupMenuLayout(
    snapshot: BrowserMenuSnapshot,
    actions: BrowserMenuActions,
    grid: List<MenuEntryUi>,
    toggles: List<MenuEntryUi>,
    list: List<MenuEntryUi>,
    extensionsPanel: @Composable () -> Unit,
    modifier: Modifier
) {
    val colors = LocalClintColors.current
    val layout = snapshot.layout
    val rowHeight = if (layout.compact) 42.dp else 46.dp
    Column(modifier.padding(horizontal = 8.dp)) {
        if (snapshot.showNavRow && snapshot.layout.showNavBar) {
            MenuNavRow(snapshot, actions, height = 48.dp, corner = 16.dp)
        }
        if (grid.isNotEmpty()) {
            Box(Modifier.padding(top = 6.dp, bottom = 2.dp)) {
                MenuGrid(
                    grid,
                    columns = layout.gridColumns.coerceAtMost(4),
                    iconBox = if (layout.compact) 38.dp else 44.dp,
                    labels = layout.gridLabels,
                    rowGap = 2.dp
                )
            }
            if (grid.hasExtensions()) extensionsPanel()
        }
        if (toggles.isNotEmpty()) {
            MenuDivider()
            toggles.forEach { entry ->
                MenuItemRow(entry, height = rowHeight, switchStyle = true)
                if (entry.item == CustomizableMenuItem.EXTENSIONS) extensionsPanel()
            }
        }
        if (list.isNotEmpty()) {
            MenuDivider()
            list.forEach { entry ->
                MenuItemRow(entry, height = rowHeight)
                if (entry.item == CustomizableMenuItem.EXTENSIONS) extensionsPanel()
            }
        }
        MenuDivider()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                MenuItemRow(
                    MenuEntryUi(null, Icons.Filled.Settings, stringResource(R.string.settings), onClick = actions.onSettings),
                    height = rowHeight
                )
            }
            IconButton(
                onClick = actions.onCustomize,
                modifier = Modifier.padding(start = 4.dp).size(42.dp).clip(CircleShape).background(colors.surfaceVariant)
            ) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.menu_customize), tint = colors.iconTint, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun MenuPageHeader(snapshot: BrowserMenuSnapshot, preview: Boolean, onSiteInfo: () -> Unit) {
    val colors = LocalClintColors.current
    val favicon = if (preview) null else rememberClintFavicon(snapshot.url)
    val host = remember(snapshot.url) {
        runCatching { Uri.parse(snapshot.url).host }.getOrNull().orEmpty().removePrefix("www.")
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(colors.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (favicon != null) {
                Image(favicon.asImageBitmap(), contentDescription = null, modifier = Modifier.size(24.dp))
            } else {
                Text(
                    host.firstOrNull()?.uppercase().orEmpty(),
                    color = colors.primary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                snapshot.title.ifBlank { host },
                color = colors.popupText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                Icon(
                    if (snapshot.isSecure) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    contentDescription = null,
                    tint = if (snapshot.isSecure) colors.secondaryText else colors.colorError,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    host,
                    color = colors.secondaryText,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
        IconButton(
            onClick = onSiteInfo,
            modifier = Modifier.size(44.dp).clip(CircleShape).background(colors.surfaceVariant)
        ) {
            Icon(Icons.Filled.Info, contentDescription = stringResource(R.string.site_info_title), tint = colors.iconTint, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun MenuNavRow(snapshot: BrowserMenuSnapshot, actions: BrowserMenuActions, height: Dp, corner: Dp) {
    val colors = LocalClintColors.current
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(corner))
            .background(colors.surfaceVariant)
            .height(height),
        verticalAlignment = Alignment.CenterVertically
    ) {
        snapshot.layout.navSlots.forEach { slot ->
            val label = stringResource(slot.titleRes())
            when (slot) {
                MenuNavSlot.BACK -> MenuNavIcon(slot.icon(), label, snapshot.canGoBack, actions.onGoBack)
                MenuNavSlot.FORWARD -> MenuNavIcon(slot.icon(), label, snapshot.canGoForward, actions.onGoForward)
                MenuNavSlot.HOME -> MenuNavIcon(slot.icon(), label, true, actions.onHome)
                MenuNavSlot.RELOAD -> MenuNavIcon(if (snapshot.isLoading) Icons.Filled.Close else slot.icon(), label, true, actions.onRefreshOrStop)
                MenuNavSlot.BOOKMARK -> MenuNavIcon(if (snapshot.page.isBookmarked) Icons.Filled.Bookmark else slot.icon(), label, true, actions.onToggleBookmark)
                MenuNavSlot.SHARE -> MenuNavIcon(slot.icon(), label, snapshot.isWebPage, actions.onShare)
                MenuNavSlot.NEW_TAB -> MenuNavIcon(slot.icon(), label, true, actions.onNewTab)
            }
        }
    }
}

@Composable
private fun RowScope.MenuNavIcon(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = LocalClintColors.current
    IconButton(onClick = onClick, modifier = Modifier.weight(1f).fillMaxHeight()) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = colors.iconTint,
            modifier = Modifier.alpha(if (enabled) 1.0f else 0.38f)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.menuClickable(entry: MenuEntryUi): Modifier {
    val toggle = entry.checked
    val base = if (toggle != null) semantics { toggleableState = ToggleableState(toggle) } else this
    return if (entry.enabled) {
        base.combinedClickable(
            role = if (toggle != null) Role.Switch else Role.Button,
            onClick = entry.onClick,
            onLongClick = entry.onLongClick
        )
    } else base
}

@Composable
private fun MenuGrid(entries: List<MenuEntryUi>, columns: Int, iconBox: Dp, labels: Boolean, rowGap: Dp) {
    Column(verticalArrangement = Arrangement.spacedBy(rowGap)) {
        entries.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { entry -> MenuGridTile(entry, iconBox, labels, Modifier.weight(1f)) }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun MenuGridTile(entry: MenuEntryUi, iconBox: Dp, labels: Boolean, modifier: Modifier) {
    val colors = LocalClintColors.current
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .menuClickable(entry)
            .alpha(if (entry.enabled) 1f else 0.38f)
            .padding(vertical = 6.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.size(iconBox)) {
            Box(
                Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(iconBox * 0.34f))
                    .background(if (entry.active) colors.primary else colors.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    entry.icon,
                    contentDescription = if (labels) null else entry.label,
                    tint = if (entry.active) colors.onPrimary else colors.primary,
                    modifier = Modifier.size(iconBox * 0.42f)
                )
            }
            entry.badge?.let { MenuBadge(it, Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-4).dp)) }
        }
        if (labels) {
            Text(
                entry.label,
                color = colors.popupText,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MenuChips(entries: List<MenuEntryUi>, compact: Boolean) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        entries.forEach { MenuChip(it, compact) }
    }
}

@Composable
private fun MenuChip(entry: MenuEntryUi, compact: Boolean) {
    val colors = LocalClintColors.current
    val shape = RoundedCornerShape(50)
    val content = if (entry.active) colors.onPrimary else colors.popupText
    Row(
        Modifier
            .height(if (compact) 36.dp else 40.dp)
            .clip(shape)
            .then(if (entry.active) Modifier.background(colors.primary) else Modifier.border(1.dp, colors.popupStroke, shape))
            .menuClickable(entry)
            .alpha(if (entry.enabled) 1f else 0.38f)
            .padding(start = 12.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(entry.icon, contentDescription = null, tint = if (entry.active) colors.onPrimary else colors.primary, modifier = Modifier.size(18.dp))
        Text(entry.label, color = content, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        entry.badge?.let { MenuBadge(it) }
    }
}

@Composable
private fun MenuFooterButton(icon: ImageVector, label: String, filled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = LocalClintColors.current
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .height(48.dp)
            .clip(shape)
            .then(if (filled) Modifier.background(colors.primary.copy(alpha = 0.14f)) else Modifier.border(1.dp, colors.popupStroke, shape))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
        Text(
            label,
            color = colors.popupText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun MenuDivider() {
    HorizontalDivider(
        color = LocalClintColors.current.popupStroke,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun MenuBadge(text: String, modifier: Modifier = Modifier) {
    val colors = LocalClintColors.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .defaultMinSize(minWidth = 18.dp)
            .height(18.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(colors.primary)
            .border(2.dp, colors.popupBackground, RoundedCornerShape(9.dp))
            .padding(horizontal = 4.dp)
    ) {
        Text(
            text = text,
            color = colors.onPrimary,
            fontSize = 10.sp,
            lineHeight = 10.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            style = LocalTextStyle.current.copy(platformStyle = PlatformTextStyle(includeFontPadding = false))
        )
    }
}

@Composable
private fun MenuItemRow(entry: MenuEntryUi, height: Dp, switchStyle: Boolean = false) {
    val colors = LocalClintColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(14.dp))
            .menuClickable(entry)
            .alpha(if (entry.enabled) 1f else 0.38f)
            .padding(horizontal = 10.dp)
    ) {
        Box(modifier = Modifier.size(34.dp)) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(entry.icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
            }
            entry.badge?.let { MenuBadge(it, Modifier.align(Alignment.TopEnd).offset(x = 5.dp, y = (-5).dp)) }
        }
        Text(
            text = entry.label,
            color = colors.popupText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 14.dp)
        )
        when {
            entry.expanded != null -> ExtensionsChevron(entry.expanded)
            switchStyle && entry.checked != null -> ClintSwitch(
                checked = entry.checked,
                modifier = Modifier.graphicsLayer { scaleX = 0.8f; scaleY = 0.8f }
            )
            entry.checked == true -> Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = colors.popupCheck,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
