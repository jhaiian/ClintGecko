package com.jhaiian.clint.tabs

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.ClintCheckbox
import com.jhaiian.clint.ui.FaviconCache
import com.jhaiian.clint.ui.theme.LocalClintColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

private val IncognitoCardBackground = Color(0xFF2A2A31)
private val IncognitoOnCard = Color(0xFFEDEDF2)
private val IncognitoOnCardSecondary = Color(0xFFAEAEBB)
private val IncognitoAccent = Color(0xFF9D8CF5)

@Composable
internal fun TabMenuCard(
    preview: TabPreview,
    isActive: Boolean,
    thumbnail: Bitmap?,
    selectionMode: Boolean,
    selected: Boolean,
    dragHandleModifier: Modifier,
    onOpen: () -> Unit,
    onEnterSelection: () -> Unit,
    onToggleSelect: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleMute: () -> Unit = {},
    mergeHighlighted: Boolean = false,
    showDragHandle: Boolean = true,
    longPressDragModifier: Modifier? = null
) {
    val colors = LocalClintColors.current
    val favicon = rememberTabFavicon(preview)
    val profile = rememberTabProfile(preview)
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberTabPressScale(interactionSource, mergeHighlighted)
    val borderWidth = rememberTabBorderWidth(isActive, mergeHighlighted)
    val accentColor = if (preview.isIncognito) IncognitoAccent else colors.primary
    val cardBg = if (preview.isIncognito) IncognitoCardBackground else colors.cardBackground
    val onCard = if (preview.isIncognito) IncognitoOnCard else colors.onSurface
    val onCardSecondary = if (preview.isIncognito) IncognitoOnCardSecondary else colors.secondaryText

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.76f)
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .border(borderWidth, accentColor, RoundedCornerShape(16.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { if (selectionMode) onToggleSelect() else onOpen() },
                onLongClick = if (longPressDragModifier == null) ({ if (!selectionMode) onEnterSelection() }) else null
            )
            .then(longPressDragModifier ?: Modifier),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 8.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedVisibility(
                    visible = selectionMode,
                    enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.6f),
                    exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.6f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ClintCheckbox(checked = selected, onCheckedChange = { onToggleSelect() }, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                    }
                }
                AnimatedVisibility(
                    visible = !selectionMode,
                    enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.6f),
                    exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.6f)
                ) {
                    if (favicon != null && !preview.isIncognito) {
                        Image(favicon.asImageBitmap(), contentDescription = null, modifier = Modifier.size(16.dp))
                    } else {
                        Icon(
                            if (preview.isIncognito) Icons.Filled.VisibilityOff else Icons.Filled.Public,
                            contentDescription = null,
                            tint = if (preview.isIncognito) IncognitoAccent else onCardSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    preview.title.ifBlank { stringResource(R.string.new_tab) },
                    color = onCard,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 6.dp)
                )
                AnimatedVisibility(visible = !selectionMode, enter = fadeIn(tween(150)), exit = fadeOut(tween(120))) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val audio = preview.audio
                        if (audio != null && (audio.audible || audio.muted)) {
                            IconButton(onClick = onToggleMute, modifier = Modifier.size(26.dp)) {
                                Icon(
                                    if (audio.muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = stringResource(if (audio.muted) R.string.tab_unmute_desc else R.string.tab_mute_desc),
                                    tint = if (audio.muted) onCardSecondary else accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        if (showDragHandle) {
                            Icon(
                                Icons.Filled.DragIndicator,
                                contentDescription = stringResource(R.string.tab_menu_drag_handle_desc),
                                tint = onCardSecondary,
                                modifier = dragHandleModifier.size(22.dp).padding(2.dp)
                            )
                        }
                        IconButton(onClick = onClose, modifier = Modifier.size(26.dp)) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.close_tab),
                                tint = onCardSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp))
                    .background(if (preview.isIncognito) Color(0xFF1C1C21) else colors.surfaceVariant)
            ) {
                if (thumbnail != null) {
                    Image(
                        thumbnail.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        filterQuality = FilterQuality.High
                    )
                } else {
                    Icon(
                        if (preview.isIncognito) Icons.Filled.VisibilityOff else Icons.Filled.Public,
                        contentDescription = null,
                        tint = onCardSecondary,
                        modifier = Modifier.size(36.dp).align(Alignment.Center)
                    )
                }

                if (profile != null) {
                    Row(
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(9.dp).clip(CircleShape).background(Color(profile.color)))
                        Text(
                            profile.name,
                            color = Color.White,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 5.dp).widthIn(max = 90.dp)
                        )
                    }
                }

                if (preview.isIncognito) {
                    Box(
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.VisibilityOff,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun TabMenuListRow(
    preview: TabPreview,
    isActive: Boolean,
    selectionMode: Boolean,
    selected: Boolean,
    dragHandleModifier: Modifier,
    onOpen: () -> Unit,
    onEnterSelection: () -> Unit,
    onToggleSelect: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleMute: () -> Unit = {},
    mergeHighlighted: Boolean = false,
    showDragHandle: Boolean = true,
    longPressDragModifier: Modifier? = null
) {
    val colors = LocalClintColors.current
    val favicon = rememberTabFavicon(preview)
    val profile = rememberTabProfile(preview)
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberTabPressScale(interactionSource, mergeHighlighted)
    val borderWidth = rememberTabBorderWidth(isActive, mergeHighlighted)
    val accentColor = if (preview.isIncognito) IncognitoAccent else colors.primary
    val rowBg = if (preview.isIncognito) IncognitoCardBackground else colors.cardBackground
    val onRow = if (preview.isIncognito) IncognitoOnCard else colors.onSurface
    val onRowSecondary = if (preview.isIncognito) IncognitoOnCardSecondary else colors.secondaryText
    val host = remember(preview.url) {
        runCatching { android.net.Uri.parse(preview.url).host }.getOrNull()?.removePrefix("www.").orEmpty()
    }
    val shape = RoundedCornerShape(14.dp)

    Row(
        modifier
            .fillMaxWidth()
            .height(TabListRowHeight)
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .clip(shape)
            .background(rowBg)
            .border(borderWidth, accentColor, shape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { if (selectionMode) onToggleSelect() else onOpen() },
                onLongClick = if (longPressDragModifier == null) ({ if (!selectionMode) onEnterSelection() }) else null
            )
            .then(longPressDragModifier ?: Modifier)
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(
            visible = selectionMode,
            enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.6f),
            exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.6f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ClintCheckbox(checked = selected, onCheckedChange = { onToggleSelect() }, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
            }
        }
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (preview.isIncognito) Color(0xFF1C1C21) else colors.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (favicon != null && !preview.isIncognito) {
                Image(favicon.asImageBitmap(), contentDescription = null, modifier = Modifier.size(20.dp))
            } else {
                Icon(
                    if (preview.isIncognito) Icons.Filled.VisibilityOff else Icons.Filled.Public,
                    contentDescription = null,
                    tint = if (preview.isIncognito) IncognitoAccent else onRowSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Column(Modifier.weight(1f).padding(start = 12.dp, end = 4.dp)) {
            Text(
                preview.title.ifBlank { stringResource(R.string.new_tab) },
                color = onRow,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (host.isNotEmpty() || profile != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (profile != null) {
                        Box(Modifier.padding(end = 5.dp).size(8.dp).clip(CircleShape).background(Color(profile.color)))
                    }
                    Text(
                        host.ifEmpty { profile?.name.orEmpty() },
                        color = onRowSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        AnimatedVisibility(visible = !selectionMode, enter = fadeIn(tween(150)), exit = fadeOut(tween(120))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val audio = preview.audio
                if (audio != null && (audio.audible || audio.muted)) {
                    IconButton(onClick = onToggleMute, modifier = Modifier.size(36.dp)) {
                        Icon(
                            if (audio.muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = stringResource(if (audio.muted) R.string.tab_unmute_desc else R.string.tab_mute_desc),
                            tint = if (audio.muted) onRowSecondary else accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (showDragHandle) {
                    Icon(
                        Icons.Filled.DragIndicator,
                        contentDescription = stringResource(R.string.tab_menu_drag_handle_desc),
                        tint = onRowSecondary,
                        modifier = dragHandleModifier.size(36.dp).padding(8.dp)
                    )
                }
                IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.close_tab),
                        tint = onRowSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

internal val TabListRowHeight = 64.dp

@Composable
internal fun SwipeToCloseContainer(
    enabled: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var offset by remember { mutableFloatStateOf(0f) }
    var width by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    var settleJob by remember { mutableStateOf<Job?>(null) }
    val flingThreshold = with(LocalDensity.current) { 900.dp.toPx() }

    fun settleTo(target: Float, onSettled: (() -> Unit)? = null) {
        settleJob?.cancel()
        settleJob = scope.launch {
            animate(offset, target, animationSpec = if (target == 0f) spring(dampingRatio = 0.7f) else tween(160)) { value, _ -> offset = value }
            if (onSettled != null) {
                onSettled()
                delay(600)
                offset = 0f
            }
        }
    }

    LaunchedEffect(enabled) {
        if (!enabled && offset != 0f) settleTo(0f)
    }

    val draggableState = rememberDraggableState { delta -> offset += delta }
    Box(
        modifier
            .onSizeChanged { width = it.width }
            .graphicsLayer {
                translationX = offset
                alpha = if (width > 0) (1f - abs(offset) / width).coerceIn(0f, 1f) else 1f
            }
            .draggable(
                state = draggableState,
                orientation = Orientation.Horizontal,
                enabled = enabled,
                onDragStarted = { settleJob?.cancel() },
                onDragStopped = { velocity ->
                    val direction = if (offset != 0f) sign(offset) else sign(velocity)
                    val flung = abs(velocity) > flingThreshold && sign(velocity) == direction
                    if (width > 0 && direction != 0f && (abs(offset) > width * 0.4f || flung)) {
                        settleTo(direction * width, onClose)
                    } else {
                        settleTo(0f)
                    }
                }
            )
    ) {
        content()
    }
}

@Composable
private fun rememberTabProfile(preview: TabPreview): com.jhaiian.clint.profiles.BrowserProfile? {
    val appContext = LocalContext.current
    return remember(preview.profileId) {
        if (preview.profileId == com.jhaiian.clint.profiles.WebProfiles.DEFAULT_ID) null
        else com.jhaiian.clint.profiles.ProfileRepository.get(appContext, preview.profileId)
    }
}

@Composable
private fun rememberTabPressScale(interactionSource: MutableInteractionSource, mergeHighlighted: Boolean): Float {
    var pressed by remember { mutableStateOf(false) }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> pressed = true
                is PressInteraction.Release, is PressInteraction.Cancel -> pressed = false
            }
        }
    }
    val scale by animateFloatAsState(
        when {
            mergeHighlighted -> 0.9f
            pressed -> 0.95f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.6f),
        label = "cardPressScale"
    )
    return scale
}

@Composable
private fun rememberTabBorderWidth(isActive: Boolean, mergeHighlighted: Boolean): Dp {
    val width by animateDpAsState(
        when {
            mergeHighlighted -> 3.dp
            isActive -> 2.5.dp
            else -> 0.dp
        },
        label = "cardBorderWidth"
    )
    return width
}

@Composable
internal fun TabMenuSectionHeader(isIncognitoSection: Boolean, modifier: Modifier = Modifier) {
    val colors = LocalClintColors.current
    Row(
        modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp, start = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (isIncognitoSection) Icons.Filled.VisibilityOff else Icons.Filled.Public,
            contentDescription = null,
            tint = if (isIncognitoSection) IncognitoAccent else colors.secondaryText,
            modifier = Modifier.size(14.dp)
        )
        Text(
            stringResource(if (isIncognitoSection) R.string.tabs_section_incognito else R.string.tabs_section_normal),
            color = if (isIncognitoSection) IncognitoAccent else colors.secondaryText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

@Composable
internal fun NewTabButton(text: String, iconRes: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = LocalClintColors.current
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(22.dp),
        colors = ButtonDefaults.buttonColors(containerColor = colors.buttonBackground, contentColor = colors.buttonTextColor),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)
    ) {
        Icon(iconRes, contentDescription = null, tint = colors.buttonIconTint, modifier = Modifier.size(18.dp))
        Text(text, fontSize = 13.sp, color = colors.buttonTextColor, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
internal fun rememberTabFavicon(tab: TabPreview): Bitmap? {
    val context = LocalContext.current
    var bitmap by remember(tab.url, tab.isIncognito) { mutableStateOf<Bitmap?>(null) }
    var sharp by remember(tab.url, tab.isIncognito) {
        mutableStateOf(if (tab.isIncognito) null else com.jhaiian.clint.ui.SharpFavicon.cached(tab.url))
    }
    LaunchedEffect(tab.url, tab.isIncognito) {
        val faviconUrl = FaviconCache.faviconUrlFor(tab.url)
        if (faviconUrl.isEmpty()) return@LaunchedEffect
        if (tab.isIncognito) {
            FaviconCache.loadMemoryOnly(context, faviconUrl) { bmp -> bitmap = bmp }
        } else {
            FaviconCache.load(context, faviconUrl) { bmp -> bitmap = bmp }
        }
    }
    LaunchedEffect(tab.url, tab.isIncognito) {
        if (!tab.isIncognito && sharp == null) sharp = com.jhaiian.clint.ui.SharpFavicon.load(context, tab.url)
    }
    return com.jhaiian.clint.ui.SharpFavicon.pick(bitmap, sharp)
}
