package com.jhaiian.clint.browser.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.customSearchEngineName
import com.jhaiian.clint.browser.engineDisplayName
import com.jhaiian.clint.browser.searchEngineFor
import com.jhaiian.clint.ui.theme.LocalClintColors
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val PREVIEW_WIDTH = 360
private const val PREVIEW_HEIGHT = 680
private const val PREVIEW_COLUMNS = 3

internal const val HOMEPAGE_PREVIEW_RATIO = PREVIEW_HEIGHT.toFloat() / PREVIEW_WIDTH

@Composable
internal fun HomepagePreview(layout: HomepageLayout, profileId: String?, width: Dp, modifier: Modifier = Modifier) {
    val colors = LocalClintColors.current
    val baseDensity = LocalDensity.current
    val scale = width.value / PREVIEW_WIDTH
    val scaledDensity = remember(baseDensity, scale) { Density(baseDensity.density * scale, baseDensity.fontScale) }
    val shape = RoundedCornerShape(width * 0.08f)
    Box(
        modifier
            .size(width, width * HOMEPAGE_PREVIEW_RATIO)
            .clip(shape)
            .border(1.dp, colors.divider, shape)
    ) {
        CompositionLocalProvider(LocalDensity provides scaledDensity) {
            HomepagePreviewContent(layout, profileId)
        }
        Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { } })
    }
}

@Composable
private fun HomepagePreviewContent(layout: HomepageLayout, profileId: String?) {
    val colors = LocalClintColors.current
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    val tileProfile = if (layout.separateTiles) profileId else null
    val tiles by produceState<List<HomeTile>>(emptyList(), tileProfile) { value = loadHomeTiles(context, tileProfile) }
    val recent by produceState<List<HomeSite>>(emptyList()) { value = loadRecentSites(context) }
    val design = layout.design
    val darkImage = layout.imageSeparateDark && !colors.isLight
    val bgImage by produceState<ImageBitmap?>(null, design, layout.imageVersion, profileId, darkImage, layout.imageBlur) {
        value = if (design == HOMEPAGE_DESIGN_IMAGE) {
            withContext(Dispatchers.IO) { loadHomepageBackground(context, profileId, darkImage, layout.imageBlur) }
        } else null
    }
    val tileUrls = remember(tiles) { tiles.allSites().map { it.url }.toSet() }
    val recentShown = remember(recent, tileUrls, layout.recentCount) { recent.filter { it.url !in tileUrls }.take(layout.recentCount) }
    val engineName = engineDisplayName(searchEngineFor(prefs, false), customSearchEngineName(prefs, false))
    val hint = stringResource(R.string.search_bar_hint, engineName)
    val greeting = if (layout.showGreeting) {
        stringResource(
            when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
                in 5..11 -> R.string.home_greeting_morning
                in 12..17 -> R.string.home_greeting_afternoon
                else -> R.string.home_greeting_evening
            }
        )
    } else ""
    val dateText = remember {
        val locale = java.util.Locale.getDefault()
        val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "EEEEMMMMd")
        java.text.SimpleDateFormat(pattern, locale).format(java.util.Date())
    }
    val cardAlpha = (if (design == HOMEPAGE_DESIGN_IMAGE) 0.86f else 1f) * layout.cardOpacity / 100f
    val dimFactor = layout.imageDim / 100f
    val scrimTop = (if (colors.isLight) 0.60f else 0.55f) * dimFactor
    val scrimBottom = (if (colors.isLight) 0.84f else 0.80f) * dimFactor
    val topTint = colors.primary.copy(alpha = if (colors.isLight) 0.10f else 0.18f)
    val solidBackground = layout.solidColor(colors)
    val gradientStart = layout.gradientStartColor(colors)
    val gradientEnd = layout.gradientEndColor(colors)
    val tileStyle = HomeTileStyle(layout.tileShape, layout.tileScale / 100f, layout.showTileLabels, layout.folderStyle == HOMEPAGE_FOLDER_PLAIN)
    val actions = layout.quickActions.map { key ->
        if (key == HOMEPAGE_ACTION_MODE) HomeAction(Icons.Filled.VisibilityOff, R.string.incognito) {}
        else HomeAction(homepageActionIcon(key), homepageActionLabel(key)) {}
    }
    val columns = if (layout.columns > 0) layout.columns else PREVIEW_COLUMNS
    val searchCorner = when (layout.searchShape) {
        HOMEPAGE_SHAPE_ROUNDED -> 16.dp
        HOMEPAGE_SHAPE_SQUARE -> 6.dp
        else -> 28.dp
    }
    val continueSite = recent.firstOrNull()
    val visibleWidgets = layout.widgets.filter { it != HOMEPAGE_WIDGET_CONTINUE || continueSite != null }
    val blocks = layout.sectionOrder.filter {
        when (it) {
            HOMEPAGE_BLOCK_SEARCH -> true
            HOMEPAGE_BLOCK_ACTIONS -> layout.showQuickActions && actions.isNotEmpty()
            HOMEPAGE_BLOCK_FAVORITES -> layout.showFavorites
            HOMEPAGE_BLOCK_RECENT -> layout.showRecent && recentShown.isNotEmpty()
            else -> visibleWidgets.isNotEmpty()
        }
    }
    val heroTitle = layout.title.ifBlank { stringResource(R.string.app_name) }

    CompositionLocalProvider(
        LocalHomeCardAlpha provides cardAlpha,
        LocalHomeCardCorner provides layout.cardCorner.dp,
        LocalHomeTileStyle provides tileStyle
    ) {
        Box(
            Modifier
                .fillMaxSize()
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
                .clipToBounds()
        ) {
            bgImage?.let { bitmap ->
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (!layout.fullBrightness && dimFactor > 0f) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(colors.background.copy(alpha = scrimTop), colors.background.copy(alpha = scrimBottom))
                                )
                            )
                    )
                }
            }
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = if (layout.centerContent) Arrangement.Center else Arrangement.Top
            ) {
                if (!layout.centerContent) Spacer(Modifier.height(40.dp))
                if (layout.heroStyle != HOMEPAGE_HERO_NONE) {
                    HomeHero(
                        isIncognito = false,
                        showGlow = design == HOMEPAGE_DESIGN_GRADIENT,
                        compact = false,
                        greeting = greeting,
                        style = layout.heroStyle,
                        title = heroTitle
                    )
                    Spacer(Modifier.height(28.dp))
                }
                blocks.forEachIndexed { index, block ->
                    if (index > 0) Spacer(Modifier.height(28.dp))
                    when (block) {
                        HOMEPAGE_BLOCK_SEARCH -> HomeSearchPill(
                            hint = hint, onClick = {}, onVoice = {}, corner = searchCorner, showVoice = layout.showVoice
                        )
                        HOMEPAGE_BLOCK_ACTIONS -> HomeQuickActions(actions)
                        HOMEPAGE_BLOCK_FAVORITES -> Column(Modifier.fillMaxWidth()) {
                            HomeSectionTitle(stringResource(R.string.home_favorites))
                            if (tiles.isEmpty()) {
                                HomeEmptyFavorites {}
                            } else {
                                tiles.take(columns * 2).chunked(columns).forEach { row ->
                                    Row(Modifier.fillMaxWidth()) {
                                        row.forEach { tile ->
                                            Box(Modifier.weight(1f)) {
                                                HomeTileCell(tile, false, 0, false, {}, {}) {}
                                            }
                                        }
                                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                                    }
                                }
                            }
                        }
                        HOMEPAGE_BLOCK_RECENT -> Column(Modifier.fillMaxWidth()) {
                            HomeSectionTitle(stringResource(R.string.home_recent))
                            HomeRecentCard(recentShown, {}, { emptyList() })
                        }
                        else -> HomeWidgets(
                            items = visibleWidgets,
                            dateText = dateText,
                            continueTitle = continueSite?.let { labelOf(it) },
                            continueHost = continueSite?.let { hostOf(it.url) },
                            downloadsActive = true,
                            onContinue = {},
                            onDownloads = {}
                        )
                    }
                }
            }
        }
    }
}
