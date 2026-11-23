package com.jhaiian.clint.browser.home

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.layer.*
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.AddressBarPosition
import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.browser.MainUiState
import com.jhaiian.clint.browser.customSearchEngineName
import com.jhaiian.clint.browser.delegates.handleVoiceSearchTap
import com.jhaiian.clint.browser.delegates.loadUrl
import com.jhaiian.clint.browser.delegates.openNewTab
import com.jhaiian.clint.browser.delegates.openSearchOverlay
import com.jhaiian.clint.browser.delegates.switchToTabMode
import com.jhaiian.clint.browser.engineDisplayName
import com.jhaiian.clint.history.SearchHistoryManager
import com.jhaiian.clint.profiles.WebProfiles
import com.jhaiian.clint.downloads.ClintDownloadManager
import com.jhaiian.clint.downloads.DownloadStatus
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.jhaiian.clint.ui.rememberClintFavicon
import com.jhaiian.clint.tabs.TabThumbnailCache
import com.jhaiian.clint.ui.theme.LocalClintColors
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val PREF_HOMEPAGE = "homepage"
const val HOMEPAGE_CLINT = "clint"
const val HOMEPAGE_SEARCH_ENGINE = "search_engine"
const val PREF_HOMEPAGE_DESIGN = "homepage_design"
const val PREF_HOMEPAGE_SHOW_FAVORITES = "homepage_show_favorites"
const val PREF_HOMEPAGE_SHOW_QUICK_ACTIONS = "homepage_show_quick_actions"
const val PREF_HOMEPAGE_SHOW_RECENT = "homepage_show_recent"
const val PREF_HOMEPAGE_CENTER_CONTENT = "homepage_center_content"
const val PREF_HOMEPAGE_IMAGE_FULL_BRIGHTNESS = "homepage_image_full_brightness"
const val HOMEPAGE_DESIGN_GRADIENT = "gradient"
const val HOMEPAGE_DESIGN_PLAIN = "plain"

internal const val CLINT_HOME_HOST = "appassets.androidplatform.net"
internal const val CLINT_HOME_PATH = "/clint/home"
internal const val CLINT_HOME_URL = "https://$CLINT_HOME_HOST$CLINT_HOME_PATH"

private const val RECENT_LOAD_LIMIT = 20
private val ContentMaxWidth = 640.dp
private val DesktopContentMaxWidth = 960.dp

internal fun isClintHomeUrl(url: String?): Boolean {
    if (url.isNullOrEmpty()) return false
    return url.substringBefore('#').substringBefore('?').trimEnd('/') == CLINT_HOME_URL
}

internal class HomeSite(val url: String, val title: String, val faviconUrl: String)

internal class HomeAction(val icon: ImageVector, val labelRes: Int, val onClick: () -> Unit)

internal fun hostOf(url: String): String =
    runCatching { Uri.parse(url).host.orEmpty().removePrefix("www.") }.getOrDefault("")

internal fun labelOf(site: HomeSite): String = site.title.ifBlank { hostOf(site.url) }.ifBlank { site.url }

internal suspend fun loadRecentSites(context: Context): List<HomeSite> =
    withContext(Dispatchers.IO) {
        val history = runCatching { SearchHistoryManager.getRecent(context) }.getOrElse { emptyList() }
        history
            .filter { it.query.startsWith("http") && !isClintHomeUrl(it.query) }
            .distinctBy { hostOf(it.query) }
            .take(RECENT_LOAD_LIMIT)
            .map { HomeSite(it.query, it.title, "") }
    }

internal suspend fun loadHomeTiles(context: Context, profileId: String?): List<HomeTile> =
    withContext(Dispatchers.IO) { HomeTileStore.load(context, profileId) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ClintHomeScreen(activity: MainActivity, state: MainUiState, modifier: Modifier = Modifier) {
    val colors = LocalClintColors.current
    val context = LocalContext.current
    val isIncognito = state.isIncognito
    val isDesktop = state.isDesktopMode
    val profileId = remember(state.activeTabId) { state.activeTabId?.let { WebProfiles.profileOfTab(it) } ?: WebProfiles.DEFAULT_ID }
    val layout = remember(state.homepageRefresh, profileId) { readHomepageLayout(context, activity.prefs, profileId) }
    val tileProfile = if (layout.separateTiles) profileId else null
    var tiles by remember { mutableStateOf<List<HomeTile>>(emptyList()) }
    var recent by remember { mutableStateOf<List<HomeSite>>(emptyList()) }
    var iconVersion by remember { mutableIntStateOf(0) }
    var editRequest by remember { mutableStateOf<HomeEditRequest?>(null) }
    var openFolderId by remember { mutableStateOf<String?>(null) }
    var renameFolderId by remember { mutableStateOf<String?>(null) }
    var moveSiteId by remember { mutableStateOf<String?>(null) }
    var revealed by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var refreshTick by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val pullState = rememberPullToRefreshState()
    val scrollState = rememberScrollState()
    val thumbnailLayer = rememberGraphicsLayer()

    LaunchedEffect(state.activeTabId, isIncognito, tileProfile) {
        if (isIncognito) {
            tiles = emptyList()
            recent = emptyList()
            return@LaunchedEffect
        }
        tiles = loadHomeTiles(context, tileProfile)
        recent = loadRecentSites(context)
    }
    LaunchedEffect(Unit) { revealed = true }
    val reveal by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(420),
        label = "homeReveal"
    )

    val greetingRes = remember(refreshTick) {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> R.string.home_greeting_morning
            in 12..17 -> R.string.home_greeting_afternoon
            else -> R.string.home_greeting_evening
        }
    }
    val dateText = remember(refreshTick) {
        val locale = java.util.Locale.getDefault()
        val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "EEEEMMMMd")
        java.text.SimpleDateFormat(pattern, locale).format(java.util.Date())
    }
    val topTint = colors.primary.copy(alpha = if (colors.isLight) 0.10f else 0.18f)
    val engineName = engineDisplayName(
        com.jhaiian.clint.browser.searchEngineFor(activity.prefs, isIncognito),
        customSearchEngineName(activity.prefs, isIncognito)
    )
    val hint = stringResource(R.string.search_bar_hint, engineName)
    val isBottomBar = state.addressBarPosition == AddressBarPosition.BOTTOM

    val design = if (isIncognito && layout.design == HOMEPAGE_DESIGN_IMAGE) HOMEPAGE_DESIGN_PLAIN else layout.design
    val darkImage = layout.imageSeparateDark && !colors.isLight
    val bgImage by produceState<ImageBitmap?>(null, design, layout.imageVersion, refreshTick, profileId, darkImage, layout.imageBlur) {
        value = if (design == HOMEPAGE_DESIGN_IMAGE) {
            withContext(Dispatchers.IO) { loadHomepageBackground(context, profileId, darkImage, layout.imageBlur) }
        } else null
    }
    val bgAlpha by animateFloatAsState(
        targetValue = if (bgImage != null) 1f else 0f,
        animationSpec = tween(320),
        label = "homeBackground"
    )
    val cardAlpha = (if (design == HOMEPAGE_DESIGN_IMAGE) 0.86f else 1f) * layout.cardOpacity / 100f
    val dimFactor = layout.imageDim / 100f
    val scrimTop = (if (colors.isLight) 0.60f else 0.55f) * dimFactor
    val scrimBottom = (if (colors.isLight) 0.84f else 0.80f) * dimFactor
    val captureThumbnail: suspend () -> Unit = {
        val tabId = state.activeTabId
        if (tabId != null) {
            val bitmap = runCatching { thumbnailLayer.toImageBitmap().asAndroidBitmap() }.getOrNull()
            if (bitmap != null) {
                withContext(Dispatchers.Default) { TabThumbnailCache.put(context, tabId, bitmap, isIncognito) }
            }
        }
    }
    LaunchedEffect(state.activeTabId, isIncognito, design, bgImage, tiles, recent, iconVersion, refreshTick, colors.background, layout) {
        delay(800)
        captureThumbnail()
    }
    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.isScrollInProgress }
            .drop(1)
            .filter { !it }
            .collect { captureThumbnail() }
    }
    val tileUrls = remember(tiles) { tiles.allSites().map { it.url }.toSet() }
    val recentShown = remember(recent, tileUrls, layout.recentCount) { recent.filter { it.url !in tileUrls }.take(layout.recentCount) }
    val hideStatusBar = remember { activity.prefs.getBoolean("hide_status_bar", false) }
    val hideSystemNavigation = remember { activity.prefs.getBoolean("hide_system_navigation", false) }
    val defaultFolderName = stringResource(R.string.home_folder_default_name)
    val openNewTabLabel = stringResource(R.string.link_open_in_new_tab)
    val editLabel = stringResource(R.string.action_edit)
    val moveToFolderLabel = stringResource(R.string.home_tile_move_to_folder)
    val moveOutLabel = stringResource(R.string.home_tile_move_out_of_folder)
    val removeLabel = stringResource(R.string.home_tile_remove)
    val renameLabel = stringResource(R.string.home_folder_rename)
    val ungroupLabel = stringResource(R.string.home_folder_ungroup)
    val addToFavoritesLabel = stringResource(R.string.home_tile_add_to_favorites)

    fun commitTiles(updated: List<HomeTile>, icon: Pair<String, Bitmap>? = null) {
        tiles = updated
        HomeTileStore.saveAsync(context, updated, icon, tileProfile) { if (icon != null) iconVersion++ }
    }

    fun siteMenu(site: HomeSiteTile): List<HomeMenuEntry> = buildList {
        add(HomeMenuEntry(Icons.AutoMirrored.Filled.OpenInNew, openNewTabLabel) { activity.openNewTab(false, site.url) })
        add(HomeMenuEntry(Icons.Filled.Edit, editLabel) { editRequest = HomeEditRequest(site) })
        if (tiles.folderOf(site.id) != null) {
            add(HomeMenuEntry(Icons.AutoMirrored.Filled.DriveFileMove, moveOutLabel) { commitTiles(tiles.moveSiteOutOfFolder(site.id)) })
        } else {
            add(HomeMenuEntry(Icons.Filled.Folder, moveToFolderLabel) { moveSiteId = site.id })
        }
        add(HomeMenuEntry(Icons.Filled.Delete, removeLabel) { commitTiles(tiles.removeSite(site.id)) })
    }

    fun tileMenu(tile: HomeTile): List<HomeMenuEntry> = when (tile) {
        is HomeSiteTile -> siteMenu(tile)
        is HomeFolderTile -> listOf(
            HomeMenuEntry(Icons.Filled.Edit, renameLabel) { renameFolderId = tile.id },
            HomeMenuEntry(Icons.Filled.FolderOpen, ungroupLabel) { commitTiles(tiles.ungroup(tile.id)) }
        )
    }

    val openTile: (HomeTile) -> Unit = { tile ->
        when (tile) {
            is HomeSiteTile -> activity.loadUrl(tile.url)
            is HomeFolderTile -> openFolderId = tile.id
        }
    }
    val dropTiles: (List<HomeTile>, String, String?) -> Unit = { order, draggedId, targetId ->
        commitTiles(if (targetId == null) order else order.mergeTiles(draggedId, targetId, defaultFolderName))
    }
    val addTile: () -> Unit = { editRequest = HomeEditRequest(null) }
    val pinRecent: (HomeSite) -> Unit = { site -> commitTiles(tiles + HomeSiteTile(newHomeTileId(), site.url, site.title)) }
    val recentMenu: (HomeSite) -> List<HomeMenuEntry> = { site ->
        listOf(
            HomeMenuEntry(Icons.Filled.Add, addToFavoritesLabel) { pinRecent(site) },
            HomeMenuEntry(Icons.AutoMirrored.Filled.OpenInNew, openNewTabLabel) { activity.openNewTab(false, site.url) }
        )
    }

    val wantsDownloads = HOMEPAGE_WIDGET_DOWNLOADS in layout.widgets
    val downloadsActive by produceState(false, wantsDownloads) {
        if (wantsDownloads) {
            ClintDownloadManager.downloadsFlow
                .map { list -> list.any { it.status in DownloadStatus.ACTIVELY_WORKING } }
                .distinctUntilChanged()
                .collect { value = it }
        } else {
            value = false
        }
    }
    val continueSite = recent.firstOrNull()
    val solidBackground = layout.solidColor(colors)
    val gradientStart = layout.gradientStartColor(colors)
    val gradientEnd = layout.gradientEndColor(colors)
    val tileStyle = HomeTileStyle(layout.tileShape, layout.tileScale / 100f, layout.showTileLabels, layout.folderStyle == HOMEPAGE_FOLDER_PLAIN)

    CompositionLocalProvider(
        LocalHomeCardAlpha provides cardAlpha,
        LocalHomeCardCorner provides layout.cardCorner.dp,
        LocalHomeTileStyle provides tileStyle
    ) {
    BoxWithConstraints(
        modifier = modifier
            .drawWithContent {
                thumbnailLayer.record { this@drawWithContent.drawContent() }
                drawLayer(thumbnailLayer)
            }
            .background(colors.background)
            .then(
                when (design) {
                    HOMEPAGE_DESIGN_GRADIENT -> Modifier.background(Brush.verticalGradient(listOf(topTint, Color.Transparent)))
                    HOMEPAGE_DESIGN_SOLID -> Modifier.background(solidBackground)
                    HOMEPAGE_DESIGN_CUSTOM_GRADIENT -> Modifier.drawBehind {
                        val radians = Math.toRadians(layout.gradientAngle.toDouble())
                        val dx = kotlin.math.sin(radians).toFloat()
                        val dy = kotlin.math.cos(radians).toFloat()
                        val half = (kotlin.math.abs(size.width * dx) + kotlin.math.abs(size.height * dy)) / 2f
                        val center = Offset(size.width / 2f, size.height / 2f)
                        drawRect(
                            Brush.linearGradient(
                                listOf(gradientStart, gradientEnd),
                                start = Offset(center.x - dx * half, center.y - dy * half),
                                end = Offset(center.x + dx * half, center.y + dy * half)
                            )
                        )
                    }
                    else -> Modifier
                }
            )
            .pointerInput(Unit) {}
    ) {
        bgImage?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .graphicsLayer {
                        alpha = bgAlpha
                        if (layout.imageParallax) {
                            scaleX = 1.15f
                            scaleY = 1.15f
                            translationY = -(scrollState.value * 0.15f).coerceAtMost(size.height * 0.07f)
                        }
                    }
            )
            if (!layout.fullBrightness && dimFactor > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = bgAlpha }
                        .background(
                            Brush.verticalGradient(
                                listOf(colors.background.copy(alpha = scrimTop), colors.background.copy(alpha = scrimBottom))
                            )
                        )
                )
            }
        }
        val viewportHeight = maxHeight
        val compact = viewportHeight < 480.dp
        val maxContent = if (isDesktop) DesktopContentMaxWidth else ContentMaxWidth
        val contentWidth = if (maxWidth < maxContent) maxWidth else maxContent
        val centered = layout.centerContent
        val showFavorites = layout.showFavorites
        val actions = layout.quickActions.map { key ->
            when (key) {
                HOMEPAGE_ACTION_BOOKMARKS -> HomeAction(homepageActionIcon(key), homepageActionLabel(key)) { activity.onMenuBookmarks() }
                HOMEPAGE_ACTION_HISTORY -> HomeAction(homepageActionIcon(key), homepageActionLabel(key)) { activity.onMenuHistory() }
                HOMEPAGE_ACTION_DOWNLOADS -> HomeAction(homepageActionIcon(key), homepageActionLabel(key)) { activity.onMenuDownloads() }
                HOMEPAGE_ACTION_SETTINGS -> HomeAction(homepageActionIcon(key), homepageActionLabel(key)) { activity.onMenuSettings() }
                HOMEPAGE_ACTION_EXTENSIONS -> HomeAction(homepageActionIcon(key), homepageActionLabel(key)) { activity.onMenuOpenExtensionsSettings() }
                HOMEPAGE_ACTION_PROFILES -> HomeAction(homepageActionIcon(key), homepageActionLabel(key)) { activity.onMenuOpenProfiles() }
                else -> if (isIncognito) {
                    HomeAction(Icons.Filled.Tab, R.string.home_normal_mode) { activity.switchToTabMode(false) }
                } else {
                    HomeAction(Icons.Filled.VisibilityOff, R.string.incognito) { activity.switchToTabMode(true) }
                }
            }
        }
        val showQuickActions = layout.showQuickActions && actions.isNotEmpty()
        val showRecent = layout.showRecent && recentShown.isNotEmpty()
        val sideBySide = isDesktop && !isIncognito && showFavorites && showRecent && contentWidth >= 800.dp
        val gridWidth = if (sideBySide) (contentWidth - 64.dp) * (1.6f / 2.6f) else contentWidth - 40.dp
        val tileWidth = if (isDesktop) 128.dp else 84.dp
        val columns = if (layout.columns > 0) layout.columns else (gridWidth / tileWidth).toInt().coerceIn(if (isDesktop) 2 else 3, 6)
        val actionsPerRow = if (contentWidth >= 520.dp) 4 else 2
        val heroStyle = if (isIncognito) HOMEPAGE_HERO_FULL else layout.heroStyle
        val showHero = heroStyle != HOMEPAGE_HERO_NONE
        val heroTitle = layout.title.ifBlank { stringResource(R.string.app_name) }
        val heroGreeting = if (layout.showGreeting) stringResource(greetingRes) else ""
        val searchCorner = when (layout.searchShape) {
            HOMEPAGE_SHAPE_ROUNDED -> 16.dp
            HOMEPAGE_SHAPE_SQUARE -> 6.dp
            else -> 28.dp
        }
        val openSearch: () -> Unit = { activity.openSearchOverlay(isBottom = isBottomBar) }
        val openVoice: () -> Unit = { activity.handleVoiceSearchTap() }
        val openSite: (HomeSite) -> Unit = { activity.loadUrl(it.url) }
        val favoritesSection: @Composable () -> Unit = {
            HomeTilesSection(tiles, columns, isDesktop, iconVersion, openTile, dropTiles, addTile, { tileMenu(it) })
        }
        val recentSection: @Composable () -> Unit = {
            HomeRecentSection(recentShown, openSite, recentMenu)
        }
        val visibleWidgets = layout.widgets.filter {
            when (it) {
                HOMEPAGE_WIDGET_CLOCK -> true
                HOMEPAGE_WIDGET_CONTINUE -> continueSite != null
                else -> downloadsActive
            }
        }
        val blocks = layout.sectionOrder.filter {
            when (it) {
                HOMEPAGE_BLOCK_SEARCH -> true
                HOMEPAGE_BLOCK_ACTIONS -> showQuickActions
                HOMEPAGE_BLOCK_FAVORITES -> !isIncognito && showFavorites
                HOMEPAGE_BLOCK_RECENT -> !isIncognito && showRecent
                else -> !isIncognito && visibleWidgets.isNotEmpty()
            }
        }
        val renderBlock: @Composable (String) -> Unit = { key ->
            when (key) {
                HOMEPAGE_BLOCK_SEARCH -> if (isDesktop) {
                    HomeSearchPill(
                        hint = hint,
                        onClick = openSearch,
                        onVoice = openVoice,
                        modifier = Modifier.widthIn(max = 680.dp),
                        height = if (compact) 52.dp else 60.dp,
                        corner = searchCorner,
                        showVoice = layout.showVoice
                    )
                } else {
                    HomeSearchPill(hint = hint, onClick = openSearch, onVoice = openVoice, corner = searchCorner, showVoice = layout.showVoice)
                }
                HOMEPAGE_BLOCK_ACTIONS -> if (isDesktop) HomeDesktopActions(actions, actionsPerRow) else HomeQuickActions(actions)
                HOMEPAGE_BLOCK_FAVORITES -> favoritesSection()
                HOMEPAGE_BLOCK_RECENT -> recentSection()
                else -> HomeWidgets(
                    items = visibleWidgets,
                    dateText = dateText,
                    continueTitle = continueSite?.let { labelOf(it) },
                    continueHost = continueSite?.let { hostOf(it.url) },
                    downloadsActive = downloadsActive,
                    onContinue = { continueSite?.let { openSite(it) } },
                    onDownloads = { activity.onMenuDownloads() }
                )
            }
        }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                if (!isRefreshing) {
                    isRefreshing = true
                    scope.launch {
                        if (!isIncognito) {
                            tiles = loadHomeTiles(context, tileProfile)
                            recent = loadRecentSites(context)
                        }
                        refreshTick++
                        delay(450)
                        isRefreshing = false
                    }
                }
            },
            state = pullState,
            modifier = Modifier.fillMaxSize(),
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullState,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                    containerColor = colors.addressBarColor,
                    color = colors.primary
                )
            }
        ) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .then(if (centered) Modifier.heightIn(min = viewportHeight) else Modifier)
                        .widthIn(max = maxContent)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .graphicsLayer {
                            alpha = reveal
                            translationY = (1f - reveal) * 18.dp.toPx()
                        },
                    verticalArrangement = if (centered) Arrangement.Center else Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (centered) Spacer(Modifier.height(16.dp))
                    if (!centered) Spacer(Modifier.height(if (isDesktop) (if (compact) 16.dp else 56.dp) else (if (compact) 12.dp else 40.dp)))
                    if (showHero) {
                        if (isDesktop) {
                            HomeDesktopHero(
                                isIncognito = isIncognito,
                                showGlow = design == HOMEPAGE_DESIGN_GRADIENT,
                                compact = compact,
                                greeting = heroGreeting,
                                date = dateText,
                                style = heroStyle,
                                title = heroTitle
                            )
                            Spacer(Modifier.height(if (compact) 18.dp else 32.dp))
                        } else {
                            HomeHero(
                                isIncognito = isIncognito,
                                showGlow = design == HOMEPAGE_DESIGN_GRADIENT,
                                compact = compact,
                                greeting = heroGreeting,
                                style = heroStyle,
                                title = heroTitle
                            )
                            Spacer(Modifier.height(if (compact) 16.dp else 28.dp))
                        }
                    }
                    var blockIndex = 0
                    while (blockIndex < blocks.size) {
                        val block = blocks[blockIndex]
                        val next = blocks.getOrNull(blockIndex + 1)
                        if (blockIndex > 0) Spacer(Modifier.height(if (compact) 18.dp else 28.dp))
                        val paired = sideBySide && next != null &&
                            (block == HOMEPAGE_BLOCK_FAVORITES || block == HOMEPAGE_BLOCK_RECENT) &&
                            (next == HOMEPAGE_BLOCK_FAVORITES || next == HOMEPAGE_BLOCK_RECENT)
                        if (paired) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                Column(modifier = Modifier.weight(if (block == HOMEPAGE_BLOCK_FAVORITES) 1.6f else 1f)) { renderBlock(block) }
                                Column(modifier = Modifier.weight(if (next == HOMEPAGE_BLOCK_FAVORITES) 1.6f else 1f)) { renderBlock(next) }
                            }
                            blockIndex += 2
                        } else {
                            renderBlock(block)
                            blockIndex += 1
                        }
                    }
                    Spacer(Modifier.height(if (centered) 16.dp else 32.dp))
                }
            }
        }
    }
    }

    editRequest?.let { request ->
        HomeSiteEditDialog(
            activity = activity,
            request = request,
            iconVersion = iconVersion,
            hideStatusBar = hideStatusBar,
            hideSystemNavigation = hideSystemNavigation,
            onDismiss = { editRequest = null }
        ) { url, title, icon, reset ->
            val existing = request.site
            if (existing == null) {
                val site = HomeSiteTile(newHomeTileId(), url, title, icon != null)
                commitTiles(tiles + site, icon?.let { site.id to it })
            } else {
                val updated = existing.copy(
                    url = url,
                    title = title,
                    customIcon = when {
                        icon != null -> true
                        reset -> false
                        else -> existing.customIcon
                    }
                )
                commitTiles(tiles.replaceSite(updated), icon?.let { existing.id to it })
            }
        }
    }

    openFolderId?.let { folderId ->
        val folder = tiles.firstOrNull { it.id == folderId } as? HomeFolderTile
        if (folder == null) {
            LaunchedEffect(folderId) { openFolderId = null }
        } else {
            HomeFolderDialog(
                folder = folder,
                iconVersion = iconVersion,
                hideStatusBar = hideStatusBar,
                hideSystemNavigation = hideSystemNavigation,
                onDismiss = { openFolderId = null },
                onOpen = { site ->
                    openFolderId = null
                    activity.loadUrl(site.url)
                },
                onReorder = { sites ->
                    (tiles.firstOrNull { it.id == folderId } as? HomeFolderTile)?.let {
                        commitTiles(tiles.replaceFolder(it.copy(sites = sites)))
                    }
                },
                onRename = { renameFolderId = folderId },
                siteMenu = { siteMenu(it) }
            )
        }
    }

    renameFolderId?.let { folderId ->
        val folder = tiles.firstOrNull { it.id == folderId } as? HomeFolderTile
        if (folder == null) {
            LaunchedEffect(folderId) { renameFolderId = null }
        } else {
            HomeFolderNameDialog(
                initialName = folder.name.ifBlank { defaultFolderName },
                hideStatusBar = hideStatusBar,
                hideSystemNavigation = hideSystemNavigation,
                onDismiss = { renameFolderId = null }
            ) { name ->
                (tiles.firstOrNull { it.id == folderId } as? HomeFolderTile)?.let {
                    commitTiles(tiles.replaceFolder(it.copy(name = name)))
                }
            }
        }
    }

    moveSiteId?.let { siteId ->
        val currentFolder = tiles.folderOf(siteId)?.id
        HomeMoveToFolderDialog(
            folders = tiles.filterIsInstance<HomeFolderTile>().filter { it.id != currentFolder },
            hideStatusBar = hideStatusBar,
            hideSystemNavigation = hideSystemNavigation,
            onDismiss = { moveSiteId = null }
        ) { folderId ->
            val target = folderId ?: newHomeTileId()
            commitTiles(tiles.moveSiteToFolder(siteId, target, defaultFolderName))
            if (folderId == null) renameFolderId = target
        }
    }
}

@Composable
internal fun HomeHero(isIncognito: Boolean, showGlow: Boolean, compact: Boolean, greeting: String, style: String, title: String) {
    val colors = LocalClintColors.current
    val glowSize = if (compact) 72.dp else 96.dp
    val cardSize = if (compact) 52.dp else 68.dp
    val shape = RoundedCornerShape(22.dp)
    val showLogo = style != HOMEPAGE_HERO_TEXT
    val showText = style != HOMEPAGE_HERO_LOGO
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (showLogo) Box(modifier = Modifier.size(glowSize), contentAlignment = Alignment.Center) {
            if (showGlow) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.radialGradient(listOf(colors.primary.copy(alpha = 0.30f), Color.Transparent)),
                            CircleShape
                        )
                )
            }
            Box(
                modifier = Modifier
                    .size(cardSize)
                    .clip(shape)
                    .background(colors.cardBackground.copy(alpha = LocalHomeCardAlpha.current))
                    .border(1.dp, colors.primary.copy(alpha = 0.25f), shape),
                contentAlignment = Alignment.Center
            ) {
                if (isIncognito) {
                    Icon(
                        imageVector = Icons.Filled.VisibilityOff,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(if (compact) 28.dp else 36.dp)
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.ic_clint_logo),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(colors.primary),
                        modifier = Modifier.size(if (compact) 40.dp else 52.dp)
                    )
                }
            }
        }
        if (showLogo && showText) Spacer(Modifier.height(if (compact) 8.dp else 14.dp))
        if (showText && isIncognito) {
            Text(
                text = stringResource(R.string.incognito),
                color = colors.onSurface,
                fontSize = if (compact) 26.sp else 32.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.home_incognito_message),
                color = colors.secondaryText,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, start = 12.dp, end = 12.dp)
            )
        } else if (showText) {
            if (greeting.isNotEmpty()) {
                Text(
                    text = greeting,
                    color = colors.secondaryText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = title,
                color = colors.onSurface,
                fontSize = if (compact) 28.sp else 36.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.5).sp
            )
        }
    }
}

@Composable
internal fun HomeSearchPill(
    hint: String,
    onClick: () -> Unit,
    onVoice: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp,
    corner: Dp = 28.dp,
    showVoice: Boolean = true
) {
    val colors = LocalClintColors.current
    Surface(
        onClick = onClick,
        color = colors.addressBarColor,
        shape = RoundedCornerShape(corner),
        border = BorderStroke(1.dp, colors.divider),
        shadowElevation = if (colors.isLight) 2.dp else 0.dp,
        modifier = modifier.fillMaxWidth().height(height)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 18.dp, end = if (showVoice) 6.dp else 18.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = hint,
                color = colors.secondaryText,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)
            )
            if (showVoice) {
                IconButton(onClick = onVoice) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = stringResource(R.string.voice_search),
                        tint = colors.iconTint
                    )
                }
            }
        }
    }
}

@Composable
internal fun HomeQuickActions(actions: List<HomeAction>) {
    val colors = LocalClintColors.current
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        actions.chunked(4).forEach { rowActions ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                rowActions.forEach { action ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(onClick = action.onClick)
                            .padding(vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(LocalHomeCardCorner.current * 0.9f))
                                .background(colors.cardBackground.copy(alpha = LocalHomeCardAlpha.current)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = stringResource(action.labelRes),
                            color = colors.secondaryText,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp)
                        )
                    }
                }
                if (actions.size > 4) repeat(4 - rowActions.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
internal fun HomeSectionTitle(text: String) {
    val colors = LocalClintColors.current
    Text(
        text = text.uppercase(),
        color = colors.primary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 10.dp)
    )
}

@Composable
internal fun HomeEmptyFavorites(onAdd: () -> Unit) {
    val colors = LocalClintColors.current
    Surface(
        onClick = onAdd,
        color = colors.cardBackground.copy(alpha = LocalHomeCardAlpha.current),
        shape = RoundedCornerShape(LocalHomeCardCorner.current),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = stringResource(R.string.home_favorites_empty),
                color = colors.secondaryText,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 14.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun HomeRecentCard(sites: List<HomeSite>, onOpen: (HomeSite) -> Unit, menuFor: (HomeSite) -> List<HomeMenuEntry>) {
    val colors = LocalClintColors.current
    var menuUrl by remember { mutableStateOf<String?>(null) }
    Surface(
        color = colors.cardBackground.copy(alpha = LocalHomeCardAlpha.current),
        shape = RoundedCornerShape(LocalHomeCardCorner.current),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            sites.forEachIndexed { index, site ->
                Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(onClick = { onOpen(site) }, onLongClick = { menuUrl = site.url })
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    SiteIcon(site, 40.dp, 22.dp, 12.dp, colors.surfaceVariant)
                    Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(
                            text = labelOf(site),
                            color = colors.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = hostOf(site.url),
                            color = colors.secondaryText,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (menuUrl == site.url) {
                    HomeTileMenu(menuFor(site)) { menuUrl = null }
                }
                }
                if (index < sites.lastIndex) {
                    HorizontalDivider(
                        color = colors.divider,
                        thickness = 1.dp,
                        modifier = Modifier.padding(start = 68.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SiteIcon(site: HomeSite, boxSize: Dp, iconSize: Dp, corner: Dp, background: Color) {
    val colors = LocalClintColors.current
    val bitmap = rememberClintFavicon(site.url, site.faviconUrl)
    Box(
        modifier = Modifier
            .size(boxSize)
            .clip(RoundedCornerShape(corner))
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(iconSize).clip(RoundedCornerShape(6.dp))
            )
        } else {
            Text(
                text = labelOf(site).take(1).uppercase(),
                color = colors.primary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun HomeTilesSection(
    tiles: List<HomeTile>,
    columns: Int,
    desktop: Boolean,
    iconVersion: Int,
    onOpen: (HomeTile) -> Unit,
    onDrop: (List<HomeTile>, String, String?) -> Unit,
    onAdd: () -> Unit,
    menuFor: (HomeTile) -> List<HomeMenuEntry>
) {
    HomeSectionTitle(stringResource(R.string.home_favorites))
    if (tiles.isEmpty()) {
        HomeEmptyFavorites(onAdd)
    } else {
        HomeTileGrid(
            tiles = tiles,
            columns = columns,
            desktop = desktop,
            iconVersion = iconVersion,
            allowMerge = true,
            showAdd = true,
            onOpen = onOpen,
            onDrop = onDrop,
            onAdd = onAdd,
            menu = { tile, dismiss -> HomeTileMenu(menuFor(tile), dismiss) }
        )
    }
}

@Composable
private fun HomeRecentSection(recent: List<HomeSite>, onOpen: (HomeSite) -> Unit, menuFor: (HomeSite) -> List<HomeMenuEntry>) {
    HomeSectionTitle(stringResource(R.string.home_recent))
    HomeRecentCard(sites = recent, onOpen = onOpen, menuFor = menuFor)
}

@Composable
private fun HomeDesktopHero(isIncognito: Boolean, showGlow: Boolean, compact: Boolean, greeting: String, date: String, style: String, title: String) {
    val colors = LocalClintColors.current
    val cardSize = if (compact) 56.dp else 76.dp
    val shape = RoundedCornerShape(22.dp)
    val showLogo = style != HOMEPAGE_HERO_TEXT
    val showText = style != HOMEPAGE_HERO_LOGO
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showLogo) Box(modifier = Modifier.size(cardSize + 24.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.radialGradient(listOf(colors.primary.copy(alpha = 0.30f), Color.Transparent)),
                            CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(cardSize)
                        .clip(shape)
                        .background(colors.cardBackground.copy(alpha = LocalHomeCardAlpha.current))
                        .border(1.dp, colors.primary.copy(alpha = 0.25f), shape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isIncognito) {
                        Icon(
                            imageVector = Icons.Filled.VisibilityOff,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(if (compact) 28.dp else 38.dp)
                        )
                    } else {
                        Image(
                            painter = painterResource(R.drawable.ic_clint_logo),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(colors.primary),
                            modifier = Modifier.size(if (compact) 40.dp else 54.dp)
                        )
                    }
                }
            }
            if (showLogo && showText) Spacer(Modifier.width(8.dp))
            if (showText) Column {
                if (!isIncognito && greeting.isNotEmpty()) {
                    Text(
                        text = greeting,
                        color = colors.secondaryText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = if (isIncognito) stringResource(R.string.incognito) else title,
                    color = colors.onSurface,
                    fontSize = if (compact) 32.sp else 44.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                if (!isIncognito) {
                    Text(
                        text = date,
                        color = colors.secondaryText,
                        fontSize = 14.sp
                    )
                }
            }
        }
        if (isIncognito) {
            Text(
                text = stringResource(R.string.home_incognito_message),
                color = colors.secondaryText,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 480.dp).padding(top = 8.dp, start = 12.dp, end = 12.dp)
            )
        }
    }
}

@Composable
private fun HomeDesktopActions(actions: List<HomeAction>, perRow: Int) {
    val colors = LocalClintColors.current
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        actions.chunked(perRow).forEach { rowActions ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowActions.forEach { action ->
                    Surface(
                        onClick = action.onClick,
                        color = colors.cardBackground.copy(alpha = LocalHomeCardAlpha.current),
                        shape = RoundedCornerShape(LocalHomeCardCorner.current * 0.8f),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(action.labelRes),
                                color = colors.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = 10.dp)
                            )
                        }
                    }
                }
                repeat(perRow - rowActions.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
