package com.jhaiian.clint.browser.find

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.browser.MainUiState
import com.jhaiian.clint.browser.delegates.updateMainContentInsets
import com.jhaiian.clint.privacy.IncognitoKeyboard
import com.jhaiian.clint.privacy.IncognitoKeyboardScope
import com.jhaiian.clint.ui.rememberSyncedTextFieldState
import com.jhaiian.clint.ui.theme.LocalClintColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop

private val FindBarMaxWidth = 640.dp
private val FindBarShape = RoundedCornerShape(18.dp)

@Composable
internal fun FindBar(activity: MainActivity, state: MainUiState, tabSwitcherOpen: Boolean) {
    val find = state.find
    val colors = LocalClintColors.current
    val density = LocalDensity.current
    val visible = find.open && !state.searchOverlayOpen && !state.isFullscreen && !tabSwitcherOpen

    LaunchedEffect(find.open, find.barAtTop, find.barHeightPx) { activity.updateMainContentInsets() }
    LaunchedEffect(find.open) {
        if (!find.open) return@LaunchedEffect
        snapshotFlow { state.isPageLoading }.drop(1).collect { activity.onFindPageLoading(it) }
    }

    val topBasePx = state.contentPaddingTopPx
    val bottomBasePx = maxOf(state.contentPaddingBottomPx, state.imeInsetPx)
    val barHeightDp = with(density) { find.barHeightPx.toDp() }
    val topDp = with(density) { topBasePx.toDp() }
    val bottomDp = with(density) { bottomBasePx.toDp() }
    val atTop = find.barAtTop

    Box(Modifier.fillMaxSize()) {
        if (visible && find.showScrollMarkers && find.ticks.isNotEmpty()) {
            val markerColor = androidx.compose.ui.graphics.Color((0xFF shl 24) or FIND_MATCH_RGB)
            val currentColor = androidx.compose.ui.graphics.Color((0xFF shl 24) or FIND_CURRENT_RGB)
            val currentIndex = find.current - 1
            Canvas(
                Modifier
                    .align(Alignment.TopEnd)
                    .fillMaxHeight()
                    .padding(
                        top = topDp + if (atTop) barHeightDp else 0.dp,
                        bottom = bottomDp + if (atTop) 0.dp else barHeightDp
                    )
                    .width(6.dp)
            ) {
                val tick = 2.dp.toPx()
                val lastRow = HashSet<Int>()
                find.ticks.forEachIndexed { index, y ->
                    if (index == currentIndex) return@forEachIndexed
                    val row = (y * size.height / tick).toInt()
                    if (!lastRow.add(row)) return@forEachIndexed
                    drawRect(
                        color = markerColor,
                        topLeft = Offset(0f, y * size.height - tick / 2f),
                        size = Size(size.width, tick)
                    )
                }
                find.ticks.getOrNull(currentIndex)?.let { y ->
                    val big = 4.dp.toPx()
                    drawRect(
                        color = currentColor,
                        topLeft = Offset(0f, y * size.height - big / 2f),
                        size = Size(size.width, big)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = visible && find.listOpen,
            enter = fadeIn(tween(160)) + expandVertically(tween(200)),
            exit = fadeOut(tween(120)) + shrinkVertically(tween(160)),
            modifier = Modifier
                .align(if (atTop) Alignment.TopCenter else Alignment.BottomCenter)
                .padding(
                    top = if (atTop) topDp + barHeightDp else 0.dp,
                    bottom = if (atTop) 0.dp else bottomDp + barHeightDp,
                    start = 8.dp,
                    end = 8.dp
                )
        ) {
            FindResultsPanel(activity, state)
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(tween(200)) { if (atTop) -it else it } + fadeIn(tween(200)),
            exit = slideOutVertically(tween(160)) { if (atTop) -it else it } + fadeOut(tween(160)),
            modifier = Modifier
                .align(if (atTop) Alignment.TopCenter else Alignment.BottomCenter)
                .padding(top = if (atTop) topDp else 0.dp, bottom = if (atTop) 0.dp else bottomDp)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = FindBarMaxWidth + 16.dp)
                    .onSizeChanged { find.barHeightPx = it.height }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                FindBarContent(activity, state)
            }
        }
    }
}

@Composable
private fun FindBarContent(activity: MainActivity, state: MainUiState) {
    val find = state.find
    val colors = LocalClintColors.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val fieldState = rememberSyncedTextFieldState(find.query) { activity.setFindQuery(it) }
    val incognitoKeyboard = state.isIncognito && IncognitoKeyboard.isEnabled(activity)

    val view = LocalView.current
    LaunchedEffect(Unit) {
        if (find.query.isEmpty()) {
            delay(150)
            runCatching { focusRequester.requestFocus() }
            keyboardController?.show()
            runCatching { WindowCompat.getInsetsController(activity.window, view).show(WindowInsetsCompat.Type.ime()) }
        }
    }

    Surface(
        shape = FindBarShape,
        color = colors.popupBackground,
        border = BorderStroke(1.dp, colors.popupStroke),
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth().widthIn(max = FindBarMaxWidth)
    ) {
        Column(Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(colors.addressBarColor)
                        .padding(start = 14.dp, end = 2.dp)
                ) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (fieldState.text.isEmpty()) {
                            Text(
                                stringResource(R.string.find_hint),
                                color = colors.secondaryText,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IncognitoKeyboardScope(enabled = incognitoKeyboard) {
                            BasicTextField(
                                state = fieldState,
                                lineLimits = TextFieldLineLimits.SingleLine,
                                textStyle = TextStyle(color = colors.onSurface, fontSize = 15.sp),
                                cursorBrush = SolidColor(colors.primary),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.None,
                                    autoCorrectEnabled = false,
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Search
                                ),
                                onKeyboardAction = { activity.findStep(forward = true) },
                                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                            )
                        }
                    }
                    FindCounter(state)
                    if (fieldState.text.isEmpty()) {
                        FindIconButton(Icons.Filled.Mic, stringResource(R.string.voice_search), size = 36) { activity.findVoiceInput() }
                    } else {
                        FindIconButton(Icons.Filled.Close, stringResource(R.string.action_clear_search), size = 36) { activity.setFindQuery("") }
                    }
                }
                FindIconButton(Icons.Filled.KeyboardArrowUp, stringResource(R.string.find_previous), enabled = find.total > 0) {
                    activity.findStep(forward = false)
                }
                FindIconButton(Icons.Filled.KeyboardArrowDown, stringResource(R.string.find_next), enabled = find.total > 0) {
                    activity.findStep(forward = true)
                }
                FindIconButton(
                    Icons.Filled.Tune,
                    stringResource(R.string.find_options),
                    tint = if (find.optionsOpen) colors.primary else colors.iconTint
                ) { find.optionsOpen = !find.optionsOpen }
                FindIconButton(Icons.Filled.Close, stringResource(R.string.find_close)) { activity.closeFind() }
            }

            AnimatedVisibility(
                visible = find.optionsOpen,
                enter = fadeIn(tween(140)) + expandVertically(tween(180)),
                exit = fadeOut(tween(100)) + shrinkVertically(tween(140))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).horizontalScroll(rememberScrollState())
                ) {
                    FindChip(stringResource(R.string.find_match_case), selected = find.matchCase) { activity.setFindMatchCase(!find.matchCase) }
                    Spacer(Modifier.width(6.dp))
                    FindChip(stringResource(R.string.find_whole_word), selected = find.wholeWord, enabled = !find.regex) {
                        activity.setFindWholeWord(!find.wholeWord)
                    }
                    Spacer(Modifier.width(6.dp))
                    FindChip(stringResource(R.string.find_links_only), selected = find.linksOnly, enabled = !find.regex) {
                        activity.setFindLinksOnly(!find.linksOnly)
                    }
                    Spacer(Modifier.width(6.dp))
                    FindChip(stringResource(R.string.find_regex), selected = find.regex) { activity.setFindRegex(!find.regex) }
                    Spacer(Modifier.width(6.dp))
                    FindChip(
                        label = stringResource(R.string.find_results),
                        selected = null,
                        leading = Icons.AutoMirrored.Filled.List,
                        enabled = find.query.isNotEmpty()
                    ) { find.listOpen = !find.listOpen }
                    Spacer(Modifier.width(6.dp))
                    FindChip(label = stringResource(R.string.find_paste), selected = null, leading = Icons.Filled.ContentPaste) {
                        activity.findPaste()
                    }
                }
            }

            AnimatedVisibility(
                visible = find.query.isEmpty() && find.recent.isNotEmpty(),
                enter = fadeIn(tween(140)) + expandVertically(tween(180)),
                exit = fadeOut(tween(100)) + shrinkVertically(tween(140))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).horizontalScroll(rememberScrollState())
                ) {
                    Icon(
                        Icons.Filled.History,
                        contentDescription = stringResource(R.string.find_recent_desc),
                        tint = colors.secondaryText,
                        modifier = Modifier.padding(horizontal = 6.dp).size(18.dp)
                    )
                    find.recent.forEach { item ->
                        FindChip(label = item, selected = null) { activity.findUseRecent(item) }
                        Spacer(Modifier.width(6.dp))
                    }
                    FindIconButton(
                        Icons.Filled.DeleteOutline,
                        stringResource(R.string.find_clear_recent),
                        size = 34
                    ) { activity.clearFindRecent() }
                }
            }
        }
    }
}

@Composable
private fun FindCounter(state: MainUiState) {
    val find = state.find
    val colors = LocalClintColors.current
    val noResults = stringResource(R.string.find_no_results)
    val invalid = stringResource(R.string.find_invalid_pattern)
    when {
        find.query.isEmpty() -> Unit
        find.invalidPattern -> Icon(
            Icons.Filled.ErrorOutline,
            contentDescription = invalid,
            tint = colors.colorError,
            modifier = Modifier.padding(horizontal = 6.dp).size(18.dp)
        )
        find.total > 0 -> {
            val text = stringResource(R.string.find_counter, find.current, find.total)
            Text(
                text,
                color = colors.secondaryText,
                fontSize = 13.sp,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 6.dp).semantics { liveRegion = LiveRegionMode.Polite; contentDescription = text }
            )
        }
        !find.searching -> {
            val text = stringResource(R.string.find_counter, 0, 0)
            Text(
                text,
                color = colors.colorError,
                fontSize = 13.sp,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 6.dp).semantics { liveRegion = LiveRegionMode.Polite; contentDescription = noResults }
            )
        }
    }
}

@Composable
private fun FindIconButton(
    icon: ImageVector,
    description: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Int = 40,
    tint: androidx.compose.ui.graphics.Color = LocalClintColors.current.iconTint,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .alpha(if (enabled) 1f else 0.38f)
            .clickable(enabled = enabled, onClickLabel = description, role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun FindChip(
    label: String,
    selected: Boolean?,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val colors = LocalClintColors.current
    val active = selected == true
    val background = if (active) colors.primary.copy(alpha = 0.16f) else colors.surfaceVariant
    val foreground = if (active) colors.primary else colors.onSurface
    val interaction = if (selected == null) {
        Modifier.clickable(enabled = enabled, role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)
    } else {
        Modifier.toggleable(value = selected, enabled = enabled, role = androidx.compose.ui.semantics.Role.Switch, onValueChange = { onClick() })
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(background)
            .alpha(if (enabled) 1f else 0.4f)
            .then(interaction)
            .padding(horizontal = 12.dp)
    ) {
        val icon = if (active) Icons.Filled.Check else leading
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = foreground, modifier = Modifier.padding(end = 6.dp).size(16.dp))
        }
        Text(label, color = foreground, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 180.dp))
    }
}

@Composable
private fun FindResultsPanel(activity: MainActivity, state: MainUiState) {
    val find = state.find
    val colors = LocalClintColors.current
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.4f).dp
    Surface(
        shape = FindBarShape,
        color = colors.popupBackground,
        border = BorderStroke(1.dp, colors.popupStroke),
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth().widthIn(max = FindBarMaxWidth).heightIn(max = maxHeight)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp)) {
                Text(
                    stringResource(R.string.find_results_title),
                    color = colors.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                FindIconButton(Icons.Filled.Close, stringResource(R.string.action_close), size = 36) { find.listOpen = false }
            }
            HorizontalDivider(color = colors.popupStroke)
            if (find.hits.isEmpty()) {
                Text(
                    stringResource(R.string.find_results_empty),
                    color = colors.secondaryText,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(Modifier.weight(1f, fill = false)) {
                    itemsIndexed(find.hits) { index, hit ->
                        val selected = index == find.current - 1
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .background(if (selected) colors.primary.copy(alpha = 0.12f) else androidx.compose.ui.graphics.Color.Transparent)
                                .clickable { activity.findJumpTo(index) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                (index + 1).toString(),
                                color = colors.secondaryText,
                                fontSize = 12.sp,
                                modifier = Modifier.width(32.dp)
                            )
                            Text(
                                buildAnnotatedString {
                                    append(hit.before)
                                    withStyle(
                                        SpanStyle(
                                            fontWeight = FontWeight.Bold,
                                            color = colors.primary,
                                            background = colors.primary.copy(alpha = 0.14f)
                                        )
                                    ) { append(hit.match) }
                                    append(hit.after)
                                },
                                color = colors.onSurface,
                                fontSize = 14.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    if (find.hitTotal > find.hits.size) {
                        item {
                            Text(
                                stringResource(R.string.find_results_limited, find.hits.size, find.hitTotal),
                                color = colors.secondaryText,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
