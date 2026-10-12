package com.jhaiian.clint.browser.home

import android.content.Context
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Bookmark
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
import androidx.compose.ui.draw.drawWithContent
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
import com.jhaiian.clint.bookmarks.BookmarkManager
import com.jhaiian.clint.browser.AddressBarPosition
import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.browser.MainUiState
import com.jhaiian.clint.browser.customSearchEngineName
import com.jhaiian.clint.browser.delegates.handleVoiceSearchTap
import com.jhaiian.clint.browser.delegates.loadUrl
import com.jhaiian.clint.browser.delegates.openSearchOverlay
import com.jhaiian.clint.browser.delegates.switchToTabMode
import com.jhaiian.clint.browser.engineDisplayName
import com.jhaiian.clint.history.SearchHistoryManager
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
const val PREF_HOMEPAGE_SHOW_RECENT = "homepage_show_recent"
const val PREF_HOMEPAGE_CENTER_CONTENT = "homepage_center_content"
const val HOMEPAGE_DESIGN_GRADIENT = "gradient"
const val HOMEPAGE_DESIGN_PLAIN = "plain"

internal const val CLINT_HOME_HOST = "appassets.androidplatform.net"
internal const val CLINT_HOME_PATH = "/clint/home"
internal const val CLINT_HOME_URL = "https://$CLINT_HOME_HOST$CLINT_HOME_PATH"

private const val FAVORITES_LOAD_LIMIT = 12
private const val RECENT_LIMIT = 5
private val ContentMaxWidth = 640.dp
private val DesktopContentMaxWidth = 960.dp

internal fun isClintHomeUrl(url: String?): Boolean {
    if (url.isNullOrEmpty()) return false
    return url.substringBefore('#').substringBefore('?').trimEnd('/') == CLINT_HOME_URL
}

private class HomeSite(val url: String, val title: String, val faviconUrl: String)

private class HomeAction(val icon: ImageVector, val labelRes: Int, val onClick: () -> Unit)

private fun hostOf(url: String): String =
    runCatching { Uri.parse(url).host.orEmpty().removePrefix("www.") }.getOrDefault("")

private fun labelOf(site: HomeSite): String = site.title.ifBlank { hostOf(site.url) }.ifBlank { site.url }

private suspend fun loadHomeSites(context: Context): Pair<List<HomeSite>, List<HomeSite>> =
    withContext(Dispatchers.IO) {
        val marks = runCatching { BookmarkManager.getAll(context) }.getOrElse { emptyList() }
        val favs = marks
            .filter { it.url.startsWith("http") }
            .sortedByDescending { maxOf(it.lastVisit, it.addedAt) }
            .take(FAVORITES_LOAD_LIMIT)
            .map { HomeSite(it.url, it.title, it.faviconUrl) }
        val favUrls = marks.map { it.url }.toSet()
        val history = runCatching { SearchHistoryManager.getAll(context) }.getOrElse { emptyList() }
        val recents = history
            .filter { it.query.startsWith("http") && !isClintHomeUrl(it.query) && it.query !in favUrls }
            .distinctBy { hostOf(it.query) }
            .take(RECENT_LIMIT)
            .map { HomeSite(it.query, it.title, "") }
        favs to recents
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ClintHomeScreen(activity: MainActivity, state: MainUiState, modifier: Modifier = Modifier) {
    val colors = LocalClintColors.current
    val context = LocalContext.current
    val isIncognito = state.isIncognito
    val isDesktop = state.isDesktopMode
    var favorites by remember { mutableStateOf<List<HomeSite>>(emptyList()) }
    var recent by remember { mutableStateOf<List<HomeSite>>(emptyList()) }
    var revealed by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var refreshTick by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val pullState = rememberPullToRefreshState()
    val scrollState = rememberScrollState()
    val thumbnailLayer = rememberGraphicsLayer()

    LaunchedEffect(state.activeTabId, isIncognito) {
        if (isIncognito) {
            favorites = emptyList()
            recent = emptyList()
            return@LaunchedEffect
        }
        val loaded = loadHomeSites(context)
        favorites = loaded.first
        recent = loaded.second
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
        activity.prefs.getString("search_engine", "duckduckgo") ?: "duckduckgo",
        customSearchEngineName(activity.prefs)
    )
    val hint = stringResource(R.string.search_bar_hint, engineName)
    val isBottomBar = state.addressBarPosition == AddressBarPosition.BOTTOM

    val design = if (isIncognito && state.homepageDesign == HOMEPAGE_DESIGN_IMAGE) HOMEPAGE_DESIGN_PLAIN else state.homepageDesign
    val bgImage by produceState<ImageBitmap?>(null, design, state.homepageImageVersion, refreshTick) {
        value = if (design == HOMEPAGE_DESIGN_IMAGE) withContext(Dispatchers.IO) { loadHomepageBackground(context) } else null
    }
    val bgAlpha by animateFloatAsState(
        targetValue = if (bgImage != null) 1f else 0f,
        animationSpec = tween(320),
        label = "homeBackground"
    )
    val cardAlpha = if (design == HOMEPAGE_DESIGN_IMAGE) 0.86f else 1f
    val scrimTop = if (colors.isLight) 0.60f else 0.55f
    val scrimBottom = if (colors.isLight) 0.84f else 0.80f
    val captureThumbnail: suspend () -> Unit = {
        val tabId = state.activeTabId
        if (tabId != null) {
            val bitmap = runCatching { thumbnailLayer.toImageBitmap().asAndroidBitmap() }.getOrNull()
            if (bitmap != null) {
                withContext(Dispatchers.Default) { TabThumbnailCache.put(context, tabId, bitmap, isIncognito) }
            }
        }
    }
    LaunchedEffect(state.activeTabId, isIncognito, design, bgImage, favorites, recent, refreshTick, colors.background) {
        delay(800)
        captureThumbnail()
    }
    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.isScrollInProgress }
            .drop(1)
            .filter { !it }
            .collect { captureThumbnail() }
    }
    CompositionLocalProvider(LocalHomeCardAlpha provides cardAlpha) {
    BoxWithConstraints(
        modifier = modifier
            .drawWithContent {
                thumbnailLayer.record { this@drawWithContent.drawContent() }
                drawLayer(thumbnailLayer)
            }
            .background(colors.background)
            .then(
                if (design == HOMEPAGE_DESIGN_GRADIENT) Modifier.background(Brush.verticalGradient(listOf(topTint, Color.Transparent)))
                else Modifier
            )
            .pointerInput(Unit) {}
    ) {
        bgImage?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = bgAlpha }
            )
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
        val viewportHeight = maxHeight
        val compact = viewportHeight < 480.dp
        val maxContent = if (isDesktop) DesktopContentMaxWidth else ContentMaxWidth
        val contentWidth = if (maxWidth < maxContent) maxWidth else maxContent
        val centered = state.homepageCenterContent
        val showFavorites = state.homepageShowFavorites
        val showRecent = state.homepageShowRecent && recent.isNotEmpty()
        val sideBySide = isDesktop && !isIncognito && showFavorites && showRecent && contentWidth >= 800.dp
        val gridWidth = if (sideBySide) (contentWidth - 64.dp) * (1.6f / 2.6f) else contentWidth - 40.dp
        val tileWidth = if (isDesktop) 128.dp else 84.dp
        val columns = (gridWidth / tileWidth).toInt().coerceIn(if (isDesktop) 2 else 3, 6)
        val actionsPerRow = if (contentWidth >= 520.dp) 4 else 2
        val actions = listOf(
            HomeAction(Icons.Filled.Bookmark, R.string.menu_bookmarks) { activity.onMenuBookmarks() },
            HomeAction(Icons.Filled.History, R.string.menu_history) { activity.onMenuHistory() },
            HomeAction(Icons.Filled.Download, R.string.menu_downloads) { activity.onMenuDownloads() },
            if (isIncognito) {
                HomeAction(Icons.Filled.Tab, R.string.home_normal_mode) { activity.switchToTabMode(false) }
            } else {
                HomeAction(Icons.Filled.VisibilityOff, R.string.incognito) { activity.switchToTabMode(true) }
            }
        )
        val openSearch: () -> Unit = { activity.openSearchOverlay(isBottom = isBottomBar) }
        val openVoice: () -> Unit = { activity.handleVoiceSearchTap() }
        val openSite: (HomeSite) -> Unit = { activity.loadUrl(it.url) }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                if (!isRefreshing) {
                    isRefreshing = true
                    scope.launch {
                        if (!isIncognito) {
                            val loaded = loadHomeSites(context)
                            favorites = loaded.first
                            recent = loaded.second
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
                    if (isDesktop) {
                        if (!centered) Spacer(Modifier.height(if (compact) 16.dp else 56.dp))
                        HomeDesktopHero(
                            isIncognito = isIncognito,
                            showGlow = design == HOMEPAGE_DESIGN_GRADIENT,
                            compact = compact,
                            greeting = stringResource(greetingRes),
                            date = dateText
                        )
                        Spacer(Modifier.height(if (compact) 18.dp else 32.dp))
                        HomeSearchPill(
                            hint = hint,
                            onClick = openSearch,
                            onVoice = openVoice,
                            modifier = Modifier.widthIn(max = 680.dp),
                            height = if (compact) 52.dp else 60.dp
                        )
                        Spacer(Modifier.height(if (compact) 16.dp else 24.dp))
                        HomeDesktopActions(actions, actionsPerRow)
                    } else {
                        if (!centered) Spacer(Modifier.height(if (compact) 12.dp else 40.dp))
                        HomeHero(
                            isIncognito = isIncognito,
                            showGlow = design == HOMEPAGE_DESIGN_GRADIENT,
                            compact = compact,
                            greeting = stringResource(greetingRes)
                        )
                        Spacer(Modifier.height(if (compact) 16.dp else 28.dp))
                        HomeSearchPill(hint = hint, onClick = openSearch, onVoice = openVoice)
                        Spacer(Modifier.height(if (compact) 16.dp else 28.dp))
                        HomeQuickActions(actions)
                    }
                    if (!isIncognito && (showFavorites || showRecent)) {
                        Spacer(Modifier.height(if (compact) 20.dp else 32.dp))
                        if (sideBySide) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                Column(modifier = Modifier.weight(1.6f)) {
                                    HomeFavoritesSection(favorites, columns, isDesktop, openSite)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    HomeRecentSection(recent, openSite)
                                }
                            }
                        } else {
                            if (showFavorites) {
                                HomeFavoritesSection(favorites, columns, isDesktop, openSite)
                            }
                            if (showRecent) {
                                if (showFavorites) Spacer(Modifier.height(if (compact) 20.dp else 32.dp))
                                HomeRecentSection(recent, openSite)
                            }
                        }
                    }
                    Spacer(Modifier.height(if (centered) 16.dp else 32.dp))
                }
            }
        }
    }
    }
}

@Composable
private fun HomeHero(isIncognito: Boolean, showGlow: Boolean, compact: Boolean, greeting: String) {
    val colors = LocalClintColors.current
    val glowSize = if (compact) 72.dp else 96.dp
    val cardSize = if (compact) 52.dp else 68.dp
    val shape = RoundedCornerShape(22.dp)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(glowSize), contentAlignment = Alignment.Center) {
            if (showGlow) {
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
        Spacer(Modifier.height(if (compact) 8.dp else 14.dp))
        if (isIncognito) {
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
        } else {
            Text(
                text = greeting,
                color = colors.secondaryText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(R.string.app_name),
                color = colors.onSurface,
                fontSize = if (compact) 28.sp else 36.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
        }
    }
}

@Composable
private fun HomeSearchPill(
    hint: String,
    onClick: () -> Unit,
    onVoice: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp
) {
    val colors = LocalClintColors.current
    Surface(
        onClick = onClick,
        color = colors.addressBarColor,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, colors.divider),
        shadowElevation = if (colors.isLight) 2.dp else 0.dp,
        modifier = modifier.fillMaxWidth().height(height)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 18.dp, end = 6.dp)
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

@Composable
private fun HomeQuickActions(actions: List<HomeAction>) {
    val colors = LocalClintColors.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        actions.forEach { action ->
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
                        .clip(RoundedCornerShape(18.dp))
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
    }
}

@Composable
private fun HomeSectionTitle(text: String) {
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
private fun HomeEmptyFavorites() {
    val colors = LocalClintColors.current
    Surface(
        color = colors.cardBackground.copy(alpha = LocalHomeCardAlpha.current),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Bookmark,
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

@Composable
private fun HomeFavoritesGrid(sites: List<HomeSite>, columns: Int, onOpen: (HomeSite) -> Unit) {
    val colors = LocalClintColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        sites.chunked(columns).forEach { rowSites ->
            Row(modifier = Modifier.fillMaxWidth()) {
                rowSites.forEach { site ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onOpen(site) }
                            .padding(vertical = 8.dp)
                    ) {
                        SiteIcon(site, 56.dp, 28.dp, 18.dp, colors.cardBackground.copy(alpha = LocalHomeCardAlpha.current))
                        Text(
                            text = labelOf(site),
                            color = colors.onSurface,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 6.dp, start = 4.dp, end = 4.dp)
                        )
                    }
                }
                repeat(columns - rowSites.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun HomeRecentCard(sites: List<HomeSite>, onOpen: (HomeSite) -> Unit) {
    val colors = LocalClintColors.current
    Surface(
        color = colors.cardBackground.copy(alpha = LocalHomeCardAlpha.current),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            sites.forEachIndexed { index, site ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(site) }
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
private fun HomeFavoritesSection(
    favorites: List<HomeSite>,
    columns: Int,
    desktop: Boolean,
    onOpen: (HomeSite) -> Unit
) {
    HomeSectionTitle(stringResource(R.string.home_favorites))
    if (favorites.isEmpty()) {
        HomeEmptyFavorites()
    } else if (desktop) {
        HomeDesktopFavoritesGrid(favorites.take(columns * 2), columns, onOpen)
    } else {
        HomeFavoritesGrid(favorites.take(columns * 2), columns, onOpen)
    }
}

@Composable
private fun HomeRecentSection(recent: List<HomeSite>, onOpen: (HomeSite) -> Unit) {
    HomeSectionTitle(stringResource(R.string.home_recent))
    HomeRecentCard(sites = recent, onOpen = onOpen)
}

@Composable
private fun HomeDesktopHero(isIncognito: Boolean, showGlow: Boolean, compact: Boolean, greeting: String, date: String) {
    val colors = LocalClintColors.current
    val cardSize = if (compact) 56.dp else 76.dp
    val shape = RoundedCornerShape(22.dp)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(cardSize + 24.dp), contentAlignment = Alignment.Center) {
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
            Spacer(Modifier.width(8.dp))
            Column {
                if (!isIncognito) {
                    Text(
                        text = greeting,
                        color = colors.secondaryText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = stringResource(if (isIncognito) R.string.incognito else R.string.app_name),
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
                        shape = RoundedCornerShape(16.dp),
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

@Composable
private fun HomeDesktopFavoritesGrid(sites: List<HomeSite>, columns: Int, onOpen: (HomeSite) -> Unit) {
    val colors = LocalClintColors.current
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        sites.chunked(columns).forEach { rowSites ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowSites.forEach { site ->
                    Surface(
                        onClick = { onOpen(site) },
                        color = colors.cardBackground.copy(alpha = LocalHomeCardAlpha.current),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 16.dp)
                        ) {
                            SiteIcon(site, 52.dp, 28.dp, 16.dp, colors.surfaceVariant)
                            Text(
                                text = labelOf(site),
                                color = colors.onSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                        }
                    }
                }
                repeat(columns - rowSites.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
