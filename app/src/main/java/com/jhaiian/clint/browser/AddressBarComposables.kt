package com.jhaiian.clint.browser
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.automirrored.filled.ArrowBack

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.rememberClintFavicon
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
    showHomeButton: Boolean = false,
    onHomeClick: () -> Unit = {},
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
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var swipeDirection by remember { mutableIntStateOf(0) }
    val swipeThresholdPx = with(density) { 56.dp.toPx() }
    val maxDragPx = with(density) { 72.dp.toPx() }
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
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .graphicsLayer { translationX = dragOffset }
            ) {
                Icon(
                    imageVector = if (addressBarText.isEmpty()) androidx.compose.material.icons.Icons.Filled.Search else if (isSecure) androidx.compose.material.icons.Icons.Filled.Lock else androidx.compose.material.icons.Icons.Filled.LockOpen,
                    contentDescription = null,
                    tint = colors.iconTint,
                    modifier = Modifier.size(20.dp)
                )
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
                    modifier = Modifier.padding(start = 12.dp).weight(1f)
                ) { text ->
                    Text(
                        text = text.ifEmpty { addressBarHint },
                        color = if (text.isEmpty()) colors.secondaryText else colors.onSurface,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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

@Composable
internal fun SearchOverlay(
    initialText: String,
    isBottom: Boolean,
    hint: String,
    suggestions: List<SuggestionItem>,
    statusBarPaddingPx: Int,
    navBarPaddingPx: Int = 0,
    onQueryChange: (String) -> Unit,
    onSubmit: (String) -> Unit,
    onVoiceSearch: () -> Unit,
    onClose: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    onSuggestionDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalClintColors.current
    val density = LocalDensity.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var fieldValue by remember {
        mutableStateOf(TextFieldValue(initialText, TextRange(0, initialText.length)))
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    fun fill(text: String) {
        fieldValue = TextFieldValue(text, TextRange(text.length))
        onQueryChange(text)
    }

    fun withoutAutoSpaceAfterPeriod(new: TextFieldValue): TextFieldValue {
        val old = fieldValue
        val newText = new.text
        val cursor = new.selection.end
        val insertedAutoSpace = newText.length == old.text.length + 1 &&
            new.selection.collapsed &&
            cursor == newText.length &&
            cursor >= 2 &&
            newText[cursor - 1] == ' ' &&
            newText[cursor - 2] == '.' &&
            (cursor < 3 || newText[cursor - 3] != ' ') &&
            newText.regionMatches(0, old.text, 0, old.text.length)
        if (!insertedAutoSpace) return new
        val fixedText = newText.removeRange(cursor - 1, cursor)
        return TextFieldValue(fixedText, TextRange(fixedText.length))
    }

    val fieldRow = @Composable {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.addressBarColor)
                    .border(1.dp, colors.divider, RoundedCornerShape(24.dp))
                    .padding(horizontal = 14.dp)
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.Search,
                    contentDescription = null,
                    tint = colors.secondaryText,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (fieldValue.text.isEmpty()) {
                        Text(
                            text = hint,
                            color = colors.secondaryText,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = fieldValue,
                        onValueChange = {
                            val corrected = withoutAutoSpaceAfterPeriod(it)
                            fieldValue = corrected
                            onQueryChange(corrected.text)
                        },
                        singleLine = true,
                        textStyle = TextStyle(color = colors.onSurface, fontSize = 16.sp),
                        cursorBrush = SolidColor(colors.primary),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { onSubmit(fieldValue.text.trim()) }),
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                    )
                }
                if (fieldValue.text.isNotEmpty()) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Filled.Close,
                        contentDescription = stringResource(R.string.action_clear_search),
                        tint = colors.secondaryText,
                        modifier = Modifier.size(18.dp).clip(CircleShape).clickable { fill("") }
                    )
                } else {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Filled.Mic,
                        contentDescription = stringResource(R.string.voice_search),
                        tint = colors.secondaryText,
                        modifier = Modifier.size(18.dp).clip(CircleShape).clickable(onClick = onVoiceSearch)
                    )
                }
            }
        }
    }

    val suggestionsList = @Composable {
        LazyColumn(
            reverseLayout = isBottom,
            verticalArrangement = if (isBottom) Arrangement.Bottom else Arrangement.Top,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(suggestions, key = { it.type.name + it.query }) { item ->
                SuggestionRow(
                    item = item,
                    onClick = { onSuggestionClick(item.query) },
                    onFill = { fill(item.query) },
                    onDelete = { onSuggestionDelete(item.query) }
                )
            }
        }
    }

    val bottomInsetPx = maxOf(WindowInsets.ime.getBottom(density), navBarPaddingPx)
    Surface(color = colors.background, modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(
                top = with(density) { statusBarPaddingPx.toDp() },
                bottom = with(density) { bottomInsetPx.toDp() }
            )
        ) {
            if (isBottom) {
                Box(modifier = Modifier.weight(1f, fill = true), contentAlignment = Alignment.BottomStart) { suggestionsList() }
                fieldRow()
            } else {
                fieldRow()
                Box(modifier = Modifier.weight(1f, fill = true), contentAlignment = Alignment.TopStart) { suggestionsList() }
            }
        }
    }
}

@Composable
private fun SuggestionRow(
    item: SuggestionItem,
    onClick: () -> Unit,
    onFill: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalClintColors.current
    val isUrl = item.query.startsWith("http")
    val showSubtitle = item.type != SuggestionType.SUGGESTION && isUrl && item.displayText != item.query
    val favicon = if (isUrl) rememberClintFavicon(item.query) else null
    val suggestionUrlDesc = stringResource(R.string.suggestion_url_desc)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 4.dp)
            .height(58.dp)
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(colors.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (favicon != null) {
                androidx.compose.foundation.Image(
                    bitmap = favicon.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp).clip(RoundedCornerShape(4.dp))
                )
            } else {
                val fallbackIcon = when (item.type) {
                    SuggestionType.BOOKMARK -> androidx.compose.material.icons.Icons.Filled.Bookmark
                    SuggestionType.HISTORY -> androidx.compose.material.icons.Icons.Filled.History
                    SuggestionType.SUGGESTION -> androidx.compose.material.icons.Icons.Filled.Search
                }
                Icon(
                    imageVector = fallbackIcon,
                    contentDescription = stringResource(R.string.suggestion_icon_desc),
                    tint = if (item.type == SuggestionType.BOOKMARK) colors.primary else colors.iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            Text(
                item.displayText,
                color = colors.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (showSubtitle) {
                Text(
                    item.query.removePrefix("https://").removePrefix("http://"),
                    color = colors.secondaryText,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics {
                        contentDescription = suggestionUrlDesc
                    }
                )
            }
        }

        if (item.type == SuggestionType.HISTORY) {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.Close,
                    contentDescription = stringResource(R.string.history_delete_desc),
                    tint = colors.secondaryText,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        IconButton(onClick = onFill) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.suggestion_fill_desc),
                tint = colors.secondaryText,
                modifier = Modifier.size(18.dp).rotate(135f)
            )
        }
    }
}
