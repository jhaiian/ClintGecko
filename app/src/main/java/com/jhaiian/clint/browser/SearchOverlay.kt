package com.jhaiian.clint.browser

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.listscreen.ListMenuItem
import com.jhaiian.clint.ui.listscreen.PopupShape
import com.jhaiian.clint.ui.rememberClintFavicon
import com.jhaiian.clint.ui.theme.LocalClintColors
import kotlinx.coroutines.flow.drop
import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.unit.Dp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Tab
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import com.jhaiian.clint.browser.home.HomeSiteTile
import com.jhaiian.clint.browser.home.HomeTileStore
import com.jhaiian.clint.browser.home.HomeTileVisual
import com.jhaiian.clint.browser.home.allSites
import com.jhaiian.clint.browser.home.label
import com.jhaiian.clint.browser.suggestions.SUGGESTIONS_ORDER_HISTORY
import com.jhaiian.clint.browser.suggestions.SUGGESTIONS_ORDER_MIXED
import com.jhaiian.clint.browser.suggestions.SUGGESTIONS_ORDER_WEB
import com.jhaiian.clint.browser.suggestions.SUGGESTIONS_URL_DOMAIN
import com.jhaiian.clint.browser.suggestions.SUGGESTIONS_URL_OFF
import com.jhaiian.clint.browser.suggestions.SuggestionDisplay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.sign

private val SuggestionContentMaxWidth = 720.dp
private val SuggestionCardShape = RoundedCornerShape(18.dp)
private val SuggestionRowShape = RoundedCornerShape(14.dp)

internal data class SearchEngineOption(val key: String, val name: String, val homeUrl: String)

private data class SuggestionSection(val type: SuggestionType?, val items: List<SuggestionItem>)

@Composable
internal fun SearchOverlay(
    fieldShape: androidx.compose.ui.graphics.Shape,
    initialText: String,
    isBottom: Boolean,
    hint: String,
    suggestions: List<SuggestionItem>,
    statusBarPaddingPx: Int,
    display: SuggestionDisplay,
    navBarPaddingPx: Int = 0,
    onQueryChange: (String) -> Unit,
    onSubmit: (String) -> Unit,
    onVoiceSearch: () -> Unit,
    onClose: () -> Unit,
    onSuggestionClick: (SuggestionItem) -> Unit,
    onSuggestionDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
    incognitoKeyboard: Boolean = false,
    engines: List<SearchEngineOption> = emptyList(),
    currentEngine: String = "",
    onSearchWithEngine: (query: String, engine: String) -> Unit = { _, _ -> },
    canPaste: Boolean = false,
    onPasteAndGo: () -> Unit = {}
) {
    val colors = LocalClintColors.current
    val density = LocalDensity.current
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val fieldState = remember { TextFieldState(initialText, TextRange(0, initialText.length)) }
    val currentOnQueryChange by rememberUpdatedState(onQueryChange)
    val query by remember { derivedStateOf { fieldState.text.toString().trim() } }
    var oneTimeEngine by remember { mutableStateOf<String?>(null) }
    var engineMenuOpen by remember { mutableStateOf(false) }
    val activeEngine = engines.firstOrNull { it.key == (oneTimeEngine ?: currentEngine) }
    val fieldHint = if (oneTimeEngine != null && activeEngine != null) stringResource(R.string.search_bar_hint, activeEngine.name) else hint

    fun submit(text: String) {
        val engine = oneTimeEngine
        if (engine != null && text.isNotEmpty()) onSearchWithEngine(text, engine) else onSubmit(text)
    }

    fun choose(item: SuggestionItem) {
        val engine = oneTimeEngine
        val isSearchTerm = !item.query.startsWith("http://") && !item.query.startsWith("https://") &&
            (item.type == SuggestionType.SUGGESTION || item.type == SuggestionType.HISTORY)
        if (engine != null && isSearchTerm) onSearchWithEngine(item.query, engine) else onSuggestionClick(item)
    }

    LaunchedEffect(fieldState) {
        snapshotFlow { fieldState.text.toString() }.drop(1).collect { currentOnQueryChange(it) }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    val favorites by produceState(emptyList<HomeSiteTile>(), display.showFavorites) {
        if (display.showFavorites) value = withContext(Dispatchers.IO) { HomeTileStore.load(context).allSites() }
    }

    fun fill(text: String) {
        fieldState.setTextAndPlaceCursorAtEnd(text)
    }

    val withoutAutoSpaceAfterPeriod = remember {
        InputTransformation {
            val newText = asCharSequence()
            val cursor = selection.end
            val insertedAutoSpace = length == originalText.length + 1 &&
                selection.collapsed &&
                cursor == length &&
                cursor >= 2 &&
                newText[cursor - 1] == ' ' &&
                newText[cursor - 2] == '.' &&
                (cursor < 3 || newText[cursor - 3] != ' ') &&
                newText.startsWith(originalText)
            if (insertedAutoSpace) delete(cursor - 1, cursor)
        }
    }

    val sections = remember(suggestions, display.order) {
        if (display.order == SUGGESTIONS_ORDER_MIXED) {
            listOf(SuggestionSection(null, rankMixed(suggestions, fieldState.text.toString().trim()))).filter { it.items.isNotEmpty() }
        } else {
            sectionOrder(display.order).mapNotNull { type ->
                suggestions.filter { it.type == type }.takeIf { it.isNotEmpty() }?.let { SuggestionSection(type, it) }
            }
        }
    }

    val hideKeyboardOnDrag = remember(keyboardController) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y != 0f) keyboardController?.hide()
                return Offset.Zero
            }
        }
    }

    val fieldRow = @Composable {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = SuggestionContentMaxWidth)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val showEngineButton = display.showEngines && activeEngine != null
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(fieldShape)
                    .background(colors.addressBarColor)
                    .padding(start = if (showEngineButton) 6.dp else 18.dp, end = 4.dp)
            ) {
                if (showEngineButton) {
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(colors.background)
                                .clickable { engineMenuOpen = true }
                                .semantics { contentDescription = activeEngine.name }
                                .padding(start = 10.dp, end = 6.dp)
                        ) {
                            EngineIcon(activeEngine, 24.dp)
                            Icon(
                                imageVector = Icons.Filled.ExpandMore,
                                contentDescription = null,
                                tint = colors.secondaryText,
                                modifier = Modifier.padding(start = 2.dp).size(18.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = engineMenuOpen,
                            onDismissRequest = { engineMenuOpen = false },
                            shape = PopupShape,
                            containerColor = colors.popupBackground,
                            border = BorderStroke(1.dp, colors.popupStroke)
                        ) {
                            Text(
                                text = stringResource(R.string.suggestion_this_time_search_in),
                                color = colors.popupText,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                            )
                            engines.forEach { engine ->
                                DropdownMenuItem(
                                    text = { Text(engine.name, color = colors.popupText, fontSize = 15.sp) },
                                    leadingIcon = { EngineIcon(engine, 24.dp) },
                                    trailingIcon = if (engine.key == activeEngine.key) {
                                        { Icon(Icons.Filled.Check, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp)) }
                                    } else null,
                                    onClick = {
                                        oneTimeEngine = engine.key.takeIf { it != currentEngine }
                                        engineMenuOpen = false
                                        focusRequester.requestFocus()
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.size(10.dp))
                }
                Box(modifier = Modifier.weight(1f)) {
                    if (fieldState.text.isEmpty()) {
                        Text(
                            text = fieldHint,
                            color = colors.secondaryText,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    com.jhaiian.clint.privacy.IncognitoKeyboardScope(enabled = incognitoKeyboard) {
                        BasicTextField(
                            state = fieldState,
                            inputTransformation = withoutAutoSpaceAfterPeriod,
                            lineLimits = TextFieldLineLimits.SingleLine,
                            textStyle = TextStyle(color = colors.onSurface, fontSize = 16.sp),
                            cursorBrush = SolidColor(colors.primary),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                            onKeyboardAction = { submit(fieldState.text.toString().trim()) },
                            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                        )
                    }
                }
                if (fieldState.text.isNotEmpty()) {
                    FieldIconButton(Icons.Filled.Close, stringResource(R.string.action_clear_search)) { fill("") }
                } else {
                    FieldIconButton(Icons.Filled.Mic, stringResource(R.string.voice_search), onVoiceSearch)
                }
            }
        }
    }

    val showPaste = canPaste && display.showPaste && (query.isEmpty() || query == initialText.trim())
    val showFavorites = display.showFavorites && query.isEmpty() && favorites.isNotEmpty()

    val suggestionsList = @Composable {
        LazyColumn(
            reverseLayout = isBottom,
            verticalArrangement = if (isBottom) Arrangement.Bottom else Arrangement.Top,
            contentPadding = PaddingValues(horizontal = if (display.compact) 8.dp else 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().nestedScroll(hideKeyboardOnDrag)
        ) {
            if (showPaste) {
                item(key = "paste") {
                    PasteChip(onClick = onPasteAndGo, modifier = Modifier.animateItem())
                }
            }
            if (showFavorites) {
                item(key = "favorites") {
                    FavoritesGrid(
                        sites = favorites,
                        display = display,
                        onClick = { choose(SuggestionItem(it.url, it.label(), SuggestionType.BOOKMARK)) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
            items(sections, key = { it.type?.name ?: "mixed" }) { section ->
                SuggestionSectionCard(
                    section = section,
                    query = query,
                    isBottom = isBottom,
                    display = display,
                    onClick = { choose(it) },
                    onFill = { fill(it.query) },
                    onDelete = { onSuggestionDelete(it.query) },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }

    val bottomInsetPx = maxOf(WindowInsets.ime.getBottom(density), navBarPaddingPx)
    Surface(color = colors.background, modifier = modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().padding(
                top = with(density) { statusBarPaddingPx.toDp() },
                bottom = with(density) { bottomInsetPx.toDp() }
            )
        ) {
            if (isBottom) {
                Box(modifier = Modifier.weight(1f, fill = true), contentAlignment = Alignment.BottomCenter) { suggestionsList() }
                fieldRow()
            } else {
                fieldRow()
                Box(modifier = Modifier.weight(1f, fill = true), contentAlignment = Alignment.TopCenter) { suggestionsList() }
            }
        }
    }
}

private fun sectionOrder(order: String): List<SuggestionType> = when (order) {
    SUGGESTIONS_ORDER_HISTORY -> listOf(SuggestionType.FIND_IN_PAGE, SuggestionType.CLINT_PAGE, SuggestionType.TAB, SuggestionType.HISTORY, SuggestionType.BOOKMARK, SuggestionType.SUGGESTION)
    SUGGESTIONS_ORDER_WEB -> listOf(SuggestionType.FIND_IN_PAGE, SuggestionType.CLINT_PAGE, SuggestionType.TAB, SuggestionType.SUGGESTION, SuggestionType.BOOKMARK, SuggestionType.HISTORY)
    else -> listOf(SuggestionType.FIND_IN_PAGE, SuggestionType.CLINT_PAGE, SuggestionType.TAB, SuggestionType.BOOKMARK, SuggestionType.HISTORY, SuggestionType.SUGGESTION)
}

private fun rankMixed(items: List<SuggestionItem>, query: String): List<SuggestionItem> {
    val q = query.lowercase()
    return items.sortedBy { item ->
        val texts = listOf(item.displayText.lowercase(), readableUrl(item.query).lowercase())
        when {
            item.type == SuggestionType.FIND_IN_PAGE || item.type == SuggestionType.CLINT_PAGE || item.type == SuggestionType.TAB -> 0
            q.isEmpty() -> 1
            texts.any { it.startsWith(q) } -> 1
            texts.any { text -> text.split(' ', '.', '/', '-', '_').any { it.startsWith(q) } } -> 2
            else -> 3
        }
    }
}

@Composable
private fun FieldIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    val colors = LocalClintColors.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = colors.secondaryText,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun EngineIcon(engine: SearchEngineOption, size: Dp) {
    val colors = LocalClintColors.current
    val favicon = rememberClintFavicon(engine.homeUrl)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(size).clip(RoundedCornerShape(size * 0.25f))
    ) {
        if (favicon != null) {
            Image(
                bitmap = favicon.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(size * 0.8f)
            )
        }
    }
}

@Composable
private fun PasteChip(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalClintColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = SuggestionContentMaxWidth)
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(CircleShape)
                .background(colors.surfaceVariant)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.ContentPaste,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = stringResource(R.string.address_bar_paste_and_go),
                color = colors.onSurface,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String, compact: Boolean) {
    val colors = LocalClintColors.current
    Text(
        text = text,
        color = if (compact) colors.secondaryText else colors.primary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.4.sp,
        modifier = Modifier.padding(start = 14.dp, bottom = if (compact) 2.dp else 6.dp)
    )
}

@Composable
private fun FavoritesGrid(
    sites: List<HomeSiteTile>,
    display: SuggestionDisplay,
    onClick: (HomeSiteTile) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalClintColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = SuggestionContentMaxWidth)
            .padding(vertical = 4.dp)
    ) {
        if (display.showHeaders) SectionHeader(stringResource(R.string.home_favorites), display.compact)
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val columns = (maxWidth / 80.dp).toInt().coerceIn(4, 8)
            Column {
                sites.take(columns * 2).chunked(columns).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        row.forEach { site ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(SuggestionRowShape)
                                    .clickable { onClick(site) }
                                    .padding(vertical = 8.dp)
                            ) {
                                HomeTileVisual(
                                    tile = site,
                                    size = 44.dp,
                                    corner = 14.dp,
                                    background = colors.surfaceVariant,
                                    iconVersion = 0
                                )
                                Text(
                                    text = site.label(),
                                    color = colors.secondaryText,
                                    fontSize = (11 * display.textScale).sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 6.dp, start = 4.dp, end = 4.dp)
                                )
                            }
                        }
                        repeat(columns - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionSectionCard(
    section: SuggestionSection,
    query: String,
    isBottom: Boolean,
    display: SuggestionDisplay,
    onClick: (SuggestionItem) -> Unit,
    onFill: (SuggestionItem) -> Unit,
    onDelete: (SuggestionItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalClintColors.current
    val compact = display.compact
    val rows = if (isBottom) section.items.asReversed() else section.items

    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = SuggestionContentMaxWidth)
            .padding(vertical = if (compact) 4.dp else 6.dp)
    ) {
        if (display.showHeaders && section.type != null) {
            SectionHeader(
                text = stringResource(
                    when (section.type) {
                        SuggestionType.FIND_IN_PAGE -> R.string.suggestion_section_find
                        SuggestionType.CLINT_PAGE -> R.string.suggestion_section_clint
                        SuggestionType.TAB -> R.string.suggestion_section_tabs
                        SuggestionType.BOOKMARK -> R.string.bookmarks_title
                        SuggestionType.HISTORY -> R.string.history_title
                        SuggestionType.SUGGESTION -> R.string.suggestion_section_search
                    }
                ),
                compact = compact
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (compact) Modifier else Modifier.clip(SuggestionCardShape).background(colors.cardBackground))
        ) {
            rows.forEachIndexed { index, item ->
                key(item.type, item.query, item.tabId) {
                    if (index > 0 && !compact) {
                        HorizontalDivider(
                            color = colors.divider.copy(alpha = 0.5f),
                            modifier = Modifier.padding(start = 64.dp, end = 16.dp)
                        )
                    }
                    SuggestionRow(
                        item = item,
                        query = query,
                        isBottom = isBottom,
                        display = display,
                        onClick = { onClick(item) },
                        onFill = { onFill(item) },
                        onDelete = { onDelete(item) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SuggestionRow(
    item: SuggestionItem,
    query: String,
    isBottom: Boolean,
    display: SuggestionDisplay,
    onClick: () -> Unit,
    onFill: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalClintColors.current
    val compact = display.compact
    val isUrl = item.type != SuggestionType.FIND_IN_PAGE && (item.query.startsWith("http://") || item.query.startsWith("https://"))
    val isClintPage = item.type == SuggestionType.CLINT_PAGE
    val isTab = item.type == SuggestionType.TAB
    val readableUrl = remember(item.query, isClintPage) { if (isClintPage) item.query else readableUrl(item.query) }
    val hasTitle = (isUrl || isClintPage) && item.displayText != item.query
    val titleText = if (isUrl && !hasTitle) readableUrl else item.displayText
    val addressText = when {
        !hasTitle || display.urlMode == SUGGESTIONS_URL_OFF -> null
        display.urlMode == SUGGESTIONS_URL_DOMAIN && isUrl -> domainOf(item.query).ifEmpty { readableUrl }
        else -> readableUrl
    }
    val switchLabel = stringResource(R.string.suggestion_switch_to_tab)
    val subtitle = if (isTab) listOfNotNull(switchLabel, addressText).joinToString(" · ") else addressText
    val isSearchTerm = !isUrl && !isClintPage && (item.type == SuggestionType.SUGGESTION || item.type == SuggestionType.HISTORY)
    val favicon = if (isUrl) rememberClintFavicon(item.query) else null
    val suggestionUrlDesc = stringResource(R.string.suggestion_url_desc)
    val removeLabel = stringResource(R.string.history_delete_desc)
    val fillLabel = stringResource(R.string.suggestion_fill_desc)
    var menuOpen by remember { mutableStateOf(false) }
    val canRemove = item.type == SuggestionType.HISTORY
    val isFind = item.type == SuggestionType.FIND_IN_PAGE
    val canFill = !isTab && !isFind

    var swipeOffset by remember { mutableFloatStateOf(0f) }
    var rowWidth by remember { mutableIntStateOf(0) }
    val swipeState = rememberDraggableState { delta -> swipeOffset += delta }
    val swiping = swipeOffset != 0f

    Box(modifier = Modifier.onSizeChanged { rowWidth = it.width }) {
        if (canRemove && swiping) {
            Box(
                contentAlignment = if (swipeOffset > 0f) AbsoluteAlignment.CenterLeft else AbsoluteAlignment.CenterRight,
                modifier = Modifier
                    .matchParentSize()
                    .clip(SuggestionRowShape)
                    .background(colors.colorErrorContainer)
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = null,
                    tint = colors.colorOnErrorContainer,
                    modifier = Modifier.padding(horizontal = 20.dp).size(22.dp)
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = swipeOffset }
                .heightIn(min = if (compact) 48.dp else 56.dp)
                .then(if (compact || swiping) Modifier.clip(SuggestionRowShape) else Modifier)
                .then(if (swiping) Modifier.background(if (compact) colors.background else colors.cardBackground) else Modifier)
                .then(
                    if (canRemove) {
                        Modifier.draggable(
                            state = swipeState,
                            orientation = Orientation.Horizontal,
                            onDragStopped = { velocity ->
                                val distance = abs(swipeOffset)
                                val dismiss = rowWidth > 0 && (distance > rowWidth * 0.4f || (abs(velocity) > 2500f && distance > rowWidth * 0.1f))
                                val target = if (dismiss) sign(swipeOffset) * rowWidth else 0f
                                animate(swipeOffset, target) { value, _ -> swipeOffset = value }
                                if (dismiss) onDelete()
                            }
                        )
                    } else Modifier
                )
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = if (canFill || canRemove) ({ menuOpen = true }) else null,
                    onLongClickLabel = if (canFill || canRemove) fillLabel else null
                )
                .padding(start = 14.dp, end = 4.dp, top = if (compact) 4.dp else 8.dp, bottom = if (compact) 4.dp else 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(if (compact) 24.dp else 36.dp)
                    .then(if (compact) Modifier else Modifier.clip(RoundedCornerShape(10.dp)).background(colors.surfaceVariant)),
                contentAlignment = Alignment.Center
            ) {
                if (favicon != null) {
                    Image(
                        bitmap = favicon.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp).clip(RoundedCornerShape(4.dp))
                    )
                } else {
                    val fallbackIcon = when {
                        isFind -> Icons.Filled.FindInPage
                        isClintPage -> Icons.Filled.SportsEsports
                        isTab -> Icons.Filled.Tab
                        item.type == SuggestionType.BOOKMARK -> Icons.Filled.Bookmark
                        item.type == SuggestionType.HISTORY && !isUrl -> Icons.Filled.History
                        isUrl -> Icons.Filled.Public
                        else -> Icons.Filled.Search
                    }
                    Icon(
                        imageVector = fallbackIcon,
                        contentDescription = stringResource(R.string.suggestion_icon_desc),
                        tint = if (item.type == SuggestionType.BOOKMARK || isClintPage || isTab || isFind) colors.primary else if (compact) colors.secondaryText else colors.iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f).padding(start = if (compact) 16.dp else 14.dp, end = 4.dp)) {
                Text(
                    text = highlightMatch(titleText, query, boldUntyped = isSearchTerm),
                    color = colors.onSurface,
                    fontSize = (15 * display.textScale).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = if (isTab) colors.primary else colors.secondaryText,
                        fontSize = (12 * display.textScale).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { contentDescription = suggestionUrlDesc }
                    )
                }
            }

            if (display.showFill && canFill) {
                IconButton(onClick = onFill) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = fillLabel,
                        tint = if (compact) colors.secondaryText.copy(alpha = 0.7f) else colors.secondaryText,
                        modifier = Modifier.size(if (compact) 18.dp else 20.dp).rotate(if (isBottom) -45f else 45f)
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(12.dp))
            }
        }

        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            shape = PopupShape,
            containerColor = colors.popupBackground,
            border = BorderStroke(1.dp, colors.popupStroke)
        ) {
            if (canFill) {
                ListMenuItem(Icons.Filled.Edit, fillLabel, checked = false) {
                    menuOpen = false
                    onFill()
                }
            }
            if (canRemove) {
                ListMenuItem(Icons.Filled.DeleteOutline, removeLabel, checked = false) {
                    menuOpen = false
                    onDelete()
                }
            }
        }
    }
}

private fun readableUrl(url: String): String =
    url.removePrefix("https://").removePrefix("http://").removePrefix("www.").removeSuffix("/")

private fun domainOf(url: String): String =
    runCatching { android.net.Uri.parse(url).host.orEmpty().removePrefix("www.") }.getOrDefault("")

@Composable
private fun highlightMatch(text: String, query: String, boldUntyped: Boolean): AnnotatedString {
    val colors = LocalClintColors.current
    return remember(text, query, boldUntyped, colors.onSurface) {
        val start = if (query.isEmpty()) -1 else text.indexOf(query, ignoreCase = true)
        val bold = SpanStyle(fontWeight = FontWeight.SemiBold, color = colors.onSurface)
        buildAnnotatedString {
            when {
                start < 0 -> append(text)
                boldUntyped -> {
                    withStyle(bold) { append(text.substring(0, start)) }
                    append(text.substring(start, start + query.length))
                    withStyle(bold) { append(text.substring(start + query.length)) }
                }
                else -> {
                    append(text.substring(0, start))
                    withStyle(bold) { append(text.substring(start, start + query.length)) }
                    append(text.substring(start + query.length))
                }
            }
        }
    }
}
