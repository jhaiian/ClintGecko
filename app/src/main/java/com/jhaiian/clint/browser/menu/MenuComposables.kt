package com.jhaiian.clint.browser.menu
import android.content.res.Configuration
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.ChromeReaderMode
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DataSaverOn
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.ArrowForward

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Velocity
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.bookmarks.BookmarkManager
import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.browser.sheets.LongPressContentMaxWidth
import com.jhaiian.clint.downloads.ClintDownloadManager
import com.jhaiian.clint.downloads.DownloadStatus
import com.jhaiian.clint.settings.sitepermissions.SitePermissionDatabase
import com.jhaiian.clint.settings.sitepermissions.SitePermissionManager
import com.jhaiian.clint.settings.datasaver.DataSaverSiteException
import com.jhaiian.clint.ui.ClintDialogStatusBarEffect
import com.jhaiian.clint.ui.listscreen.PopupShape
import com.jhaiian.clint.ui.theme.LocalClintColors

internal data class BrowserMenuSnapshot(
    val showNavRow: Boolean,
    val canGoBack: Boolean,
    val canGoForward: Boolean,
    val isBookmarked: Boolean,
    val isLoading: Boolean,
    val isDesktopMode: Boolean,
    val isDataSaverEnabled: Boolean,
    val pendingDownloadCount: Long,
    val mediaCaptureCount: Long,
    val isMediaCaptureEnabled: Boolean,
    val isDataSaverExceptionForSite: Boolean,
    val isWebsiteBlockerEnabled: Boolean,
    val openInAppEnabled: Boolean,
    val openInAppLabel: String?,
    val canCreateShortcut: Boolean,
    val canToggleTabMute: Boolean,
    val isTabMuted: Boolean,
    val canUseCookiesControl: Boolean,
    val visibleMenuItems: List<CustomizableMenuItem>
)

internal class BrowserMenuActions(
    val onGoBack: () -> Unit,
    val onGoForward: () -> Unit,
    val onHome: () -> Unit,
    val onRefreshOrStop: () -> Unit,
    val onToggleBookmark: () -> Unit,
    val onNewTab: () -> Unit,
    val onIncognito: () -> Unit,
    val onNewProfileTab: () -> Unit,
    val onOpenProfiles: () -> Unit,
    val onShare: () -> Unit,
    val onOpenInApp: () -> Unit,
    val onCreateShortcut: () -> Unit,
    val onOpenShortcutManager: () -> Unit,
    val onDownloads: () -> Unit,
    val onOpenDownloadSettings: () -> Unit,
    val onMediaCapture: () -> Unit,
    val onExtensionClick: (String, Boolean) -> Unit,
    val onOpenExtensionSettings: (String) -> Unit,
    val onOpenExtensionsSettings: () -> Unit,
    val onBookmarks: () -> Unit,
    val onHistory: () -> Unit,
    val onDesktopMode: () -> Unit,
    val onSettings: () -> Unit,
    val onReaderMode: () -> Unit,
    val onDataSaver: () -> Unit,
    val onOpenDataSaverSettings: () -> Unit,
    val onOpenDesktopModeSettings: () -> Unit,
    val onOpenDataSaverSiteSettings: () -> Unit,
    val onDisableDataSaverForSite: () -> Unit,
    val onToggleTabMute: () -> Unit,
    val onWebsiteBlocker: () -> Unit,
    val onOpenWebsiteBlockerSettings: () -> Unit,
    val onCookiesControl: () -> Unit
)

internal fun MainActivity.buildMenuSnapshot(): BrowserMenuSnapshot {
    val position = prefs.getString("address_bar_position", "top") ?: "top"
    val wv = tabManager.activeTab?.webView
    val currentUrl = wv?.url ?: ""
    val currentUri = currentUrl.takeIf { it.isNotEmpty() }
        ?.let { runCatching { android.net.Uri.parse(it) }.getOrNull() }
    val webClient = wv?.webViewClient
    val appMatches = if (currentUri != null && webClient != null &&
        (currentUri.scheme == "http" || currentUri.scheme == "https")
    ) {
        webClient.resolveAppMatches(currentUri, this)
    } else emptyList()
    val openInAppLabel = if (appMatches.size == 1) {
        getString(R.string.menu_open_in_named_app, appMatches[0].loadLabel(packageManager).toString())
    } else null

    val dataSaverExceptionForSite = DataSaverSiteException.isExceptedUrl(this, wv?.url)

    return BrowserMenuSnapshot(
        showNavRow = position != "split",
        canGoBack = wv?.canGoBack() == true,
        canGoForward = wv?.canGoForward() == true,
        isBookmarked = currentUrl.isNotEmpty() && BookmarkManager.isBookmarked(this, currentUrl),
        isLoading = uiState.isPageLoading,
        isDesktopMode = isDesktopMode,
        isDataSaverEnabled = prefs.getBoolean("data_saver_enabled", false),
        pendingDownloadCount = ClintDownloadManager.downloadsFlow.value
            .count { it.status in DownloadStatus.RUNNING_OR_QUEUED }.toLong(),
        mediaCaptureCount = com.jhaiian.clint.mediacapture.MediaCaptureStore.countForTab(tabManager.activeTab?.id).toLong(),
        isMediaCaptureEnabled = prefs.getBoolean(com.jhaiian.clint.mediacapture.MEDIA_CAPTURE_ENABLED_PREF, true),
        isDataSaverExceptionForSite = dataSaverExceptionForSite,
        isWebsiteBlockerEnabled = prefs.getBoolean("website_blocker_enabled", false),
        openInAppEnabled = appMatches.isNotEmpty(),
        openInAppLabel = openInAppLabel,
        canCreateShortcut = currentUrl.isNotEmpty(),
        canToggleTabMute = wv != null && (wv.audioState.audible || wv.audioState.muted),
        isTabMuted = wv?.audioState?.muted == true,
        canUseCookiesControl = tabManager.activeTab?.isIncognito == false &&
            (currentUrl.startsWith("http://") || currentUrl.startsWith("https://")),
        visibleMenuItems = BrowserMenuCustomizationStore.visibleOrderedItems(prefs)
    )
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
    onDataSaver = { dismiss(); onMenuDataSaver() },
    onOpenDataSaverSettings = { dismiss(); onMenuOpenDataSaverSettings() },
    onOpenDesktopModeSettings = { dismiss(); onMenuOpenDesktopModeSettings() },
    onOpenDataSaverSiteSettings = { dismiss(); onMenuOpenDataSaverSiteSettings() },
    onDisableDataSaverForSite = { dismiss(); onMenuDisableDataSaverForSite() },
    onToggleTabMute = { dismiss(); onMenuToggleTabMute() },
    onWebsiteBlocker = { dismiss(); onMenuWebsiteBlocker() },
    onOpenWebsiteBlockerSettings = { dismiss(); onMenuOpenWebsiteBlockerSettings() },
    onCookiesControl = { dismiss(); onMenuCookiesControl() }
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
        if (expanded) snapshot = activity.buildMenuSnapshot()
    }

    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Filled.MoreVert,
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
                modifier = Modifier.width(240.dp).heightIn(max = maxMenuHeight),
                shape = PopupShape,
                containerColor = colors.popupBackground,
                border = BorderStroke(1.dp, colors.popupStroke)
            ) {
                if (currentSnapshot != null) BrowserMenuContent(currentSnapshot, actions)
            }
        }
    }
}

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

    val flingBoundaryConnection = remember {
        object : NestedScrollConnection {
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = available
        }
    }

    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    val maxSheetHeight = if (isPortrait) {
        (configuration.screenHeightDp.dp * 0.8f).coerceAtLeast(320.dp)
    } else {
        (configuration.screenHeightDp.dp - 96.dp).coerceAtLeast(320.dp)
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
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
                    BrowserMenuContent(snapshot, actions, modifier = Modifier.fillMaxWidth().widthIn(max = LongPressContentMaxWidth))
                }
            }
        }
    }
}

@Composable
private fun BrowserMenuContent(
    snapshot: BrowserMenuSnapshot,
    actions: BrowserMenuActions,
    modifier: Modifier = Modifier
) {
    val colors = LocalClintColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colors.popupBackground)
            .padding(vertical = 6.dp)
    ) {
        if (snapshot.showNavRow) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surfaceVariant)
                    .height(52.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MenuNavIcon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), snapshot.canGoBack, actions.onGoBack)
                MenuNavIcon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowForward, stringResource(R.string.forward), snapshot.canGoForward, actions.onGoForward)
                MenuNavIcon(androidx.compose.material.icons.Icons.Filled.Home, stringResource(R.string.home), true, actions.onHome)
                MenuNavIcon(
                    if (snapshot.isLoading) androidx.compose.material.icons.Icons.Filled.Close else androidx.compose.material.icons.Icons.Filled.Refresh,
                    stringResource(R.string.refresh), true, actions.onRefreshOrStop
                )
                MenuNavIcon(
                    if (snapshot.isBookmarked) androidx.compose.material.icons.Icons.Filled.Bookmark else androidx.compose.material.icons.Icons.Filled.BookmarkBorder,
                    stringResource(R.string.content_desc_bookmark), true, actions.onToggleBookmark
                )
            }
            MenuDivider()
        }
        snapshot.visibleMenuItems.forEach { item -> MenuItemRowFor(item, snapshot, actions) }
        MenuDivider()
        MenuItemRow(androidx.compose.material.icons.Icons.Filled.Settings, stringResource(R.string.settings), onClick = actions.onSettings)
    }
}

@Composable
private fun MenuItemRowFor(item: CustomizableMenuItem, snapshot: BrowserMenuSnapshot, actions: BrowserMenuActions) {
    when (item) {
        CustomizableMenuItem.NEW_TAB -> MenuItemRow(item.icon(), stringResource(item.titleRes()), onClick = actions.onNewTab)
        CustomizableMenuItem.NEW_INCOGNITO_TAB -> MenuItemRow(item.icon(), stringResource(item.titleRes()), onClick = actions.onIncognito)
        CustomizableMenuItem.NEW_PROFILE_TAB -> if (com.jhaiian.clint.profiles.WebProfiles.isSupported()) {
            MenuItemRow(
                item.icon(),
                stringResource(item.titleRes()),
                onClick = actions.onNewProfileTab,
                onLongClick = actions.onOpenProfiles
            )
        }
        CustomizableMenuItem.SHARE -> MenuItemRow(item.icon(), stringResource(item.titleRes()), onClick = actions.onShare)
        CustomizableMenuItem.OPEN_IN_APP -> MenuItemRow(
            item.icon(),
            snapshot.openInAppLabel ?: stringResource(item.titleRes()),
            enabled = snapshot.openInAppEnabled,
            onClick = actions.onOpenInApp
        )
        CustomizableMenuItem.CREATE_SHORTCUT -> MenuItemRow(
            item.icon(),
            stringResource(item.titleRes()),
            enabled = snapshot.canCreateShortcut,
            onClick = actions.onCreateShortcut,
            onLongClick = actions.onOpenShortcutManager
        )
        CustomizableMenuItem.DOWNLOADS -> MenuItemRow(
            item.icon(),
            stringResource(item.titleRes()),
            badge = if (snapshot.pendingDownloadCount > 0L) com.jhaiian.clint.ui.status.formatCompactCount(snapshot.pendingDownloadCount) else null,
            onClick = actions.onDownloads,
            onLongClick = actions.onOpenDownloadSettings
        )
        CustomizableMenuItem.MEDIA_CAPTURE -> MenuItemRow(
            item.icon(),
            stringResource(item.titleRes()),
            checked = snapshot.isMediaCaptureEnabled,
            onClick = actions.onMediaCapture
        )
        CustomizableMenuItem.EXTENSIONS -> {
            val context = LocalContext.current
            val menuPrefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
            var expanded by remember { mutableStateOf(menuPrefs.getBoolean("extensions_menu_expanded", false)) }
            MenuItemRow(
                item.icon(),
                stringResource(item.titleRes()),
                onClick = {
                    expanded = !expanded
                    menuPrefs.edit().putBoolean("extensions_menu_expanded", expanded).apply()
                },
                onLongClick = actions.onOpenExtensionsSettings,
                trailing = { ExtensionsChevron(expanded) }
            )
            ExtensionsMenuPanel(
                expanded = expanded,
                onExtensionClick = actions.onExtensionClick,
                onExtensionSettings = actions.onOpenExtensionSettings,
                onManage = actions.onOpenExtensionsSettings
            )
        }
        CustomizableMenuItem.WEBSITE_BLOCKER -> MenuItemRow(
            item.icon(),
            stringResource(item.titleRes()),
            checked = snapshot.isWebsiteBlockerEnabled,
            onClick = actions.onWebsiteBlocker,
            onLongClick = actions.onOpenWebsiteBlockerSettings
        )
        CustomizableMenuItem.BOOKMARKS -> MenuItemRow(item.icon(), stringResource(item.titleRes()), onClick = actions.onBookmarks)
        CustomizableMenuItem.HISTORY -> MenuItemRow(item.icon(), stringResource(item.titleRes()), onClick = actions.onHistory)
        CustomizableMenuItem.READER_MODE -> MenuItemRow(item.icon(), stringResource(item.titleRes()), onClick = actions.onReaderMode)
        CustomizableMenuItem.DESKTOP_MODE -> MenuItemRow(
            item.icon(),
            stringResource(item.titleRes()),
            checked = snapshot.isDesktopMode,
            onClick = actions.onDesktopMode,
            onLongClick = actions.onOpenDesktopModeSettings
        )
        CustomizableMenuItem.DATA_SAVER -> MenuItemRow(
            item.icon(),
            stringResource(item.titleRes()),
            checked = snapshot.isDataSaverEnabled,
            onClick = actions.onDataSaver,
            onLongClick = actions.onOpenDataSaverSettings
        )
        CustomizableMenuItem.MUTE_TAB -> if (snapshot.canToggleTabMute) {
            MenuItemRow(
                if (snapshot.isTabMuted) androidx.compose.material.icons.Icons.AutoMirrored.Filled.VolumeUp else item.icon(),
                stringResource(if (snapshot.isTabMuted) R.string.menu_unmute_tab else item.titleRes()),
                onClick = actions.onToggleTabMute
            )
        }
        CustomizableMenuItem.COOKIES_CONTROL -> MenuItemRow(
            item.icon(),
            stringResource(item.titleRes()),
            enabled = snapshot.canUseCookiesControl,
            onClick = actions.onCookiesControl
        )
        CustomizableMenuItem.DISABLE_DATA_SAVER_FOR_SITE -> MenuItemRow(
            item.icon(),
            stringResource(item.titleRes()),
            checked = snapshot.isDataSaverExceptionForSite,
            onClick = actions.onDisableDataSaverForSite,
            onLongClick = actions.onOpenDataSaverSiteSettings
        )
    }
}

@Composable
private fun MenuDivider() {
    HorizontalDivider(
        color = LocalClintColors.current.popupStroke,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
    )
}

@Composable
private fun RowScope.MenuNavIcon(iconRes: androidx.compose.ui.graphics.vector.ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = LocalClintColors.current
    IconButton(onClick = onClick, modifier = Modifier.weight(1f).fillMaxHeight()) {
        Icon(
            imageVector = iconRes,
            contentDescription = description,
            tint = colors.iconTint,
            modifier = Modifier.alpha(if (enabled) 1.0f else 0.38f)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MenuItemRow(
    iconRes: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean = true,
    checked: Boolean? = null,
    badge: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val colors = LocalClintColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .let { if (enabled) it.combinedClickable(onClick = onClick, onLongClick = onLongClick) else it }
            .alpha(if (enabled) 1f else 0.38f)
            .padding(horizontal = 16.dp)
    ) {
        Box(modifier = Modifier.size(34.dp)) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconRes,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            if (badge != null) {

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 5.dp, y = (-5).dp)
                        .defaultMinSize(minWidth = 13.dp)
                        .height(13.dp)
                        .clip(RoundedCornerShape(6.5.dp))
                        .background(colors.primary)
                        .padding(horizontal = 3.dp)
                ) {
                    Text(
                        text = badge,
                        color = colors.onPrimary,
                        fontSize = 7.sp,
                        lineHeight = 7.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        style = LocalTextStyle.current.copy(
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
            }
        }
        Text(
            text = label,
            color = colors.popupText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f).padding(start = 14.dp)
        )
        if (checked == true) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Filled.Check,
                contentDescription = null,
                tint = colors.popupCheck,
                modifier = Modifier.size(18.dp)
            )
        }
        trailing?.invoke()
    }
}
