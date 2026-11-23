package com.jhaiian.clint.settings.toolbarcustomization

import android.content.SharedPreferences
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.home.HOMEPAGE_SHAPE_PILL
import com.jhaiian.clint.browser.home.HOMEPAGE_SHAPE_ROUNDED
import com.jhaiian.clint.browser.home.HOMEPAGE_SHAPE_SQUARE
import com.jhaiian.clint.browser.toolbar.BottomBarAction
import com.jhaiian.clint.browser.toolbar.PROGRESS_CLASSIC
import com.jhaiian.clint.browser.toolbar.PROGRESS_SQUIGGLE
import com.jhaiian.clint.settings.browser.HomepageChoiceDialog
import com.jhaiian.clint.browser.toolbar.ToolbarButton
import com.jhaiian.clint.browser.toolbar.ToolbarCustomizationStore
import com.jhaiian.clint.browser.toolbar.ToolbarLayout
import com.jhaiian.clint.browser.toolbar.icon
import com.jhaiian.clint.browser.toolbar.titleRes
import com.jhaiian.clint.translation.PREF_TRANSLATION_SHOW_BUTTON
import com.jhaiian.clint.ui.AdaptiveWidthContainer
import com.jhaiian.clint.ui.ClintSwitch
import com.jhaiian.clint.ui.listscreen.PopupShape
import com.jhaiian.clint.ui.theme.LocalClintColors
import kotlin.math.roundToInt

private val ReorderRowHeight = 56.dp

class ToolbarCustomizationUiState(layout: ToolbarLayout) {
    val order = mutableStateListOf<ToolbarButton>()
    val hidden = mutableStateListOf<ToolbarButton>()
    val bottom = mutableStateListOf<BottomBarAction>()
    val longPress = mutableStateMapOf<BottomBarAction, BottomBarAction>()
    var compact by mutableStateOf(false)
    var showLabels by mutableStateOf(false)
    var showInBottomPosition by mutableStateOf(false)
    var searchShape by mutableStateOf(HOMEPAGE_SHAPE_PILL)
    var progressStyle by mutableStateOf(PROGRESS_CLASSIC)

    init {
        load(layout)
    }

    fun load(layout: ToolbarLayout) {
        order.clear()
        order.addAll(layout.order)
        hidden.clear()
        hidden.addAll(layout.hidden)
        bottom.clear()
        bottom.addAll(layout.bottomActions)
        longPress.clear()
        longPress.putAll(layout.longPress)
        compact = layout.compact
        showLabels = layout.showLabels
        showInBottomPosition = layout.showInBottomPosition
        searchShape = layout.searchShape
        progressStyle = layout.progressStyle
    }

    fun toLayout() = ToolbarLayout(
        order.toList(),
        hidden.toSet(),
        bottom.toList(),
        longPress.filterKeys { it in bottom },
        compact,
        showLabels,
        showInBottomPosition,
        searchShape,
        progressStyle
    )

    fun toggleHidden(button: ToolbarButton) {
        if (!hidden.remove(button)) hidden.add(button)
    }
}

@Composable
fun ToolbarCustomizationScreen(
    state: ToolbarCustomizationUiState,
    prefs: SharedPreferences,
    maxContentWidth: Dp?,
    onExit: () -> Unit,
    onChanged: () -> Unit,
    onResetClick: () -> Unit,
    onRestartRequired: (revert: () -> Unit) -> Unit
) {
    val colors = LocalClintColors.current
    var showHome by remember { mutableStateOf(prefs.getBoolean("show_home_button", true)) }
    var showTranslate by remember { mutableStateOf(prefs.getBoolean(PREF_TRANSLATION_SHOW_BUTTON, true)) }
    var dialog by remember { mutableStateOf<String?>(null) }
    val shapeOptions = listOf(
        HOMEPAGE_SHAPE_PILL to stringResource(R.string.homepage_shape_pill),
        HOMEPAGE_SHAPE_ROUNDED to stringResource(R.string.homepage_shape_rounded),
        HOMEPAGE_SHAPE_SQUARE to stringResource(R.string.homepage_shape_square)
    )
    val progressOptions = listOf(
        PROGRESS_CLASSIC to stringResource(R.string.toolbar_customization_progress_classic),
        PROGRESS_SQUIGGLE to stringResource(R.string.toolbar_customization_progress_squiggle)
    )
    val hideStatusBar = prefs.getBoolean("hide_status_bar", false)
    val hideSystemNavigation = prefs.getBoolean("hide_system_navigation", false)

    Surface(color = colors.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Surface(color = colors.background, modifier = Modifier.statusBarsPadding()) {
                Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onExit) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = colors.onSurface)
                    }
                    Text(
                        stringResource(R.string.toolbar_customization_title),
                        color = colors.onSurface,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(start = 4.dp)
                    )
                    IconButton(onClick = onResetClick) {
                        Icon(Icons.Filled.RestartAlt, contentDescription = stringResource(R.string.menu_customization_reset_desc), tint = colors.iconTint)
                    }
                }
            }
            HorizontalDivider(color = colors.divider, thickness = 1.dp)
            when (dialog) {
                "shape" -> HomepageChoiceDialog(
                    title = stringResource(R.string.homepage_search_shape_title), options = shapeOptions, current = state.searchShape,
                    hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                    onConfirm = { picked ->
                        state.searchShape = picked
                        dialog = null
                        onChanged()
                    },
                    onDismiss = { dialog = null }
                )
                "progress" -> HomepageChoiceDialog(
                    title = stringResource(R.string.toolbar_customization_progress_title), options = progressOptions, current = state.progressStyle,
                    hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                    onConfirm = { picked ->
                        state.progressStyle = picked
                        dialog = null
                        onChanged()
                    },
                    onDismiss = { dialog = null }
                )
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                AdaptiveWidthContainer(maxContentWidth) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(start = 12.dp, end = 12.dp, top = 4.dp)
                            .navigationBarsPadding()
                            .padding(bottom = 24.dp)
                    ) {
                        SectionLabel(stringResource(R.string.toolbar_customization_layout_section))
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.cardBackground).padding(12.dp)
                        ) {
                            ToggleRow(
                                title = stringResource(R.string.toolbar_customization_compact),
                                summary = stringResource(R.string.toolbar_customization_compact_desc),
                                checked = state.compact
                            ) {
                                state.compact = !state.compact
                                onRestartRequired { state.compact = !state.compact }
                            }
                        }

                        SectionLabel(stringResource(R.string.toolbar_customization_style_section))
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.cardBackground).padding(12.dp)
                        ) {
                            ChoiceRow(
                                title = stringResource(R.string.homepage_search_shape_title),
                                value = shapeOptions.first { it.first == state.searchShape }.second
                            ) { dialog = "shape" }
                            ChoiceRow(
                                title = stringResource(R.string.toolbar_customization_progress_title),
                                value = progressOptions.first { it.first == state.progressStyle }.second
                            ) { dialog = "progress" }
                            Text(
                                stringResource(R.string.toolbar_customization_shape_hint),
                                color = colors.secondaryText,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 6.dp)
                            )
                        }

                        SectionLabel(stringResource(R.string.toolbar_customization_top_section))
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.cardBackground).padding(12.dp)
                        ) {
                            ToggleRow(
                                title = stringResource(R.string.pref_show_home_button_title),
                                summary = stringResource(R.string.pref_show_home_button_summary),
                                checked = showHome
                            ) {
                                showHome = !showHome
                                prefs.edit().putBoolean("show_home_button", showHome).apply()
                            }
                            ToggleRow(
                                title = stringResource(R.string.translation_button_title),
                                summary = stringResource(R.string.translation_button_summary),
                                checked = showTranslate
                            ) {
                                showTranslate = !showTranslate
                                prefs.edit().putBoolean(PREF_TRANSLATION_SHOW_BUTTON, showTranslate).apply()
                            }
                            Text(
                                stringResource(R.string.toolbar_customization_top_desc),
                                color = colors.secondaryText,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 10.dp, bottom = 6.dp)
                            )
                            ReorderableList(
                                items = state.order,
                                onChanged = onChanged,
                                icon = { it.icon() },
                                title = { stringResource(it.titleRes()) }
                            ) { button ->
                                if (button.canHide) {
                                    val on = button !in state.hidden
                                    Box(
                                        Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .toggleable(value = on, role = Role.Switch, onValueChange = {
                                                state.toggleHidden(button)
                                                onChanged()
                                            })
                                            .padding(8.dp)
                                    ) { ClintSwitch(checked = on) }
                                } else {
                                    Text(
                                        stringResource(R.string.toolbar_customization_always_shown),
                                        color = colors.secondaryText,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                }
                            }
                        }

                        SectionLabel(stringResource(R.string.toolbar_customization_bottom_section))
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.cardBackground).padding(12.dp)
                        ) {
                            ToggleRow(
                                title = stringResource(R.string.toolbar_customization_labels),
                                summary = stringResource(R.string.toolbar_customization_labels_desc),
                                checked = state.showLabels
                            ) {
                                state.showLabels = !state.showLabels
                                onRestartRequired { state.showLabels = !state.showLabels }
                            }
                            ToggleRow(
                                title = stringResource(R.string.toolbar_customization_bottom_position),
                                summary = stringResource(R.string.toolbar_customization_bottom_position_desc),
                                checked = state.showInBottomPosition
                            ) {
                                state.showInBottomPosition = !state.showInBottomPosition
                                onRestartRequired { state.showInBottomPosition = !state.showInBottomPosition }
                            }
                            Text(
                                stringResource(R.string.toolbar_customization_bottom_desc, stringResource(R.string.address_bar_position_split)),
                                color = colors.secondaryText,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 10.dp, bottom = 6.dp)
                            )
                            ReorderableList(
                                items = state.bottom,
                                onChanged = onChanged,
                                icon = { it.icon() },
                                title = { stringResource(it.titleRes()) }
                            ) { action ->
                                val label = stringResource(action.titleRes())
                                if (action.canLongPress) LongPressPicker(action, state, onChanged)
                                IconButton(
                                    modifier = Modifier.size(40.dp),
                                    enabled = state.bottom.size > ToolbarCustomizationStore.MIN_BOTTOM_ACTIONS,
                                    onClick = {
                                        state.bottom.remove(action)
                                        onChanged()
                                    }
                                ) {
                                    Icon(
                                        Icons.Filled.RemoveCircleOutline,
                                        contentDescription = stringResource(R.string.toolbar_customization_remove_action, label),
                                        tint = colors.iconTint
                                    )
                                }
                            }
                            AddActionButton(state.bottom) {
                                state.bottom.add(it)
                                onChanged()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChoiceRow(title: String, value: String, onClick: () -> Unit) {
    val colors = LocalClintColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Text(value, color = colors.secondaryText, fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    val colors = LocalClintColors.current
    Text(
        text,
        color = colors.primary,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 18.dp, bottom = 8.dp)
    )
}

@Composable
private fun ToggleRow(title: String, summary: String, checked: Boolean, onToggle: () -> Unit) {
    val colors = LocalClintColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .toggleable(value = checked, role = Role.Switch, onValueChange = { onToggle() })
            .heightIn(min = 56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(summary, color = colors.secondaryText, fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 2.dp))
        }
        ClintSwitch(checked = checked)
    }
}

@Composable
private fun <T : Any> ReorderableList(
    items: MutableList<T>,
    onChanged: () -> Unit,
    icon: (T) -> ImageVector,
    title: @Composable (T) -> String,
    trailing: @Composable (T) -> Unit
) {
    val colors = LocalClintColors.current
    val rowHeightPx = with(LocalDensity.current) { ReorderRowHeight.toPx() }
    var dragging by remember { mutableStateOf<T?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    Column {
        items.toList().forEach { item ->
            key(item) {
                val isDragging = dragging == item
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(ReorderRowHeight)
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (isDragging) dragOffset else 0f
                            val lift = if (isDragging) 1.03f else 1f
                            scaleX = lift
                            scaleY = lift
                        }
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isDragging) colors.surface else colors.surfaceVariant)
                        .padding(start = 4.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .pointerInput(item) {
                                detectDragGestures(
                                    onDragStart = {
                                        dragging = item
                                        dragOffset = 0f
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragOffset += amount.y
                                        val current = items.indexOf(item)
                                        val target = (current + (dragOffset / rowHeightPx).roundToInt()).coerceIn(0, items.lastIndex)
                                        if (current != -1 && target != current) {
                                            items.add(target, items.removeAt(current))
                                            dragOffset -= (target - current) * rowHeightPx
                                        }
                                    },
                                    onDragEnd = {
                                        dragging = null
                                        dragOffset = 0f
                                        onChanged()
                                    },
                                    onDragCancel = {
                                        dragging = null
                                        dragOffset = 0f
                                        onChanged()
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.DragIndicator,
                            contentDescription = stringResource(R.string.menu_customization_drag_handle_desc),
                            tint = colors.secondaryText,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Box(
                        Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(colors.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon(item), contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        title(item),
                        color = colors.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                    )
                    trailing(item)
                }
            }
        }
    }
}

@Composable
private fun LongPressPicker(action: BottomBarAction, state: ToolbarCustomizationUiState, onChanged: () -> Unit) {
    val colors = LocalClintColors.current
    var open by remember { mutableStateOf(false) }
    val target = state.longPress[action]
    Box {
        IconButton(modifier = Modifier.size(40.dp), onClick = { open = true }) {
            Icon(
                target?.icon() ?: Icons.Filled.TouchApp,
                contentDescription = stringResource(R.string.toolbar_customization_long_press, stringResource(action.titleRes())),
                tint = if (target != null) colors.primary else colors.secondaryText
            )
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = PopupShape,
            containerColor = colors.popupBackground,
            border = BorderStroke(1.dp, colors.popupStroke)
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.toolbar_customization_long_press_none), color = colors.popupText, fontSize = 14.sp, fontWeight = FontWeight.Medium) },
                leadingIcon = { Icon(Icons.Filled.Block, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp)) },
                trailingIcon = if (target == null) {
                    { Icon(Icons.Filled.Check, contentDescription = null, tint = colors.popupCheck, modifier = Modifier.size(18.dp)) }
                } else null,
                onClick = {
                    open = false
                    state.longPress.remove(action)
                    onChanged()
                }
            )
            BottomBarAction.entries.filter { it.canLongPress && it != action }.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.titleRes()), color = colors.popupText, fontSize = 14.sp, fontWeight = FontWeight.Medium) },
                    leadingIcon = { Icon(option.icon(), contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp)) },
                    trailingIcon = if (target == option) {
                        { Icon(Icons.Filled.Check, contentDescription = null, tint = colors.popupCheck, modifier = Modifier.size(18.dp)) }
                    } else null,
                    onClick = {
                        open = false
                        state.longPress[action] = option
                        onChanged()
                    }
                )
            }
        }
    }
}

@Composable
private fun AddActionButton(current: List<BottomBarAction>, onPick: (BottomBarAction) -> Unit) {
    val colors = LocalClintColors.current
    var open by remember { mutableStateOf(false) }
    val available = BottomBarAction.entries.filter { it !in current }
    val enabled = current.size < ToolbarCustomizationStore.MAX_BOTTOM_ACTIONS && available.isNotEmpty()
    Box(Modifier.padding(top = 6.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable(enabled = enabled, role = Role.Button) { open = true }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = if (enabled) colors.primary else colors.secondaryText, modifier = Modifier.size(20.dp))
            Text(
                stringResource(R.string.toolbar_customization_add_action),
                color = if (enabled) colors.primary else colors.secondaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = PopupShape,
            containerColor = colors.popupBackground,
            border = BorderStroke(1.dp, colors.popupStroke)
        ) {
            available.forEach { action ->
                DropdownMenuItem(
                    text = { Text(stringResource(action.titleRes()), color = colors.popupText, fontSize = 14.sp, fontWeight = FontWeight.Medium) },
                    leadingIcon = { Icon(action.icon(), contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp)) },
                    onClick = {
                        open = false
                        onPick(action)
                    }
                )
            }
        }
    }
}
