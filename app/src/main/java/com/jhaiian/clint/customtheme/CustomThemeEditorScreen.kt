package com.jhaiian.clint.customtheme

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.settings.common.RowDivider
import com.jhaiian.clint.settings.common.SettingsSection
import com.jhaiian.clint.ui.AdaptiveWidthContainer
import com.jhaiian.clint.ui.ClintColorPickerDialog
import com.jhaiian.clint.ui.ClintOutlinedTextField
import com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig
import com.jhaiian.clint.ui.listscreen.ConfirmDialogHost
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
fun CustomThemeEditorScreen(
    initial: CustomTheme,
    isNew: Boolean,
    maxContentWidth: Dp?,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    takenNames: List<String>,
    onSave: (CustomTheme) -> Unit,
    onExit: () -> Unit
) {
    val colors = LocalClintColors.current
    var name by remember { mutableStateOf(initial.name) }
    var description by remember { mutableStateOf(initial.description) }
    var light by remember { mutableStateOf(initial.light) }
    var dark by remember { mutableStateOf(initial.dark) }
    var editingLight by remember { mutableStateOf(colors.isLight) }
    var tab by remember { mutableIntStateOf(0) }
    var pickerSlot by remember { mutableStateOf<ThemeSlot?>(null) }
    var confirmDiscard by remember { mutableStateOf(false) }

    val palette = if (editingLight) light else dark
    val nameTaken = (isNew || name.trim() != initial.name.trim()) &&
        takenNames.any { it.trim().equals(name.trim(), ignoreCase = true) }
    val dirty = name != initial.name || description != initial.description || light != initial.light || dark != initial.dark
    val modeDirty = if (editingLight) light != initial.light else dark != initial.dark
    val canSave = name.isNotBlank() && !nameTaken && (dirty || isNew)

    BackHandler { if (dirty) confirmDiscard = true else onExit() }

    val discardConfig = if (confirmDiscard) {
        ConfirmDialogConfig(
            title = stringResource(R.string.custom_theme_discard_title),
            message = stringResource(R.string.custom_theme_discard_message),
            positiveLabel = stringResource(R.string.custom_theme_discard_action),
            onPositive = onExit,
            negativeLabel = stringResource(R.string.action_cancel)
        )
    } else {
        null
    }

    val modeLabels = listOf(
        stringResource(R.string.custom_theme_mode_light),
        stringResource(R.string.custom_theme_mode_dark)
    )
    val tabLabels = listOf(
        stringResource(R.string.custom_theme_tab_colors),
        stringResource(R.string.custom_theme_tab_preview)
    )

    val header: @Composable () -> Unit = {
        ClintOutlinedTextField(
            value = name,
            onValueChange = { if (it.length <= MAX_CUSTOM_THEME_NAME_LENGTH) name = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.custom_theme_name_label)) },
            singleLine = true,
            isError = name.isBlank() || nameTaken
        )
        if (name.isBlank() || nameTaken) {
            Text(
                stringResource(
                    if (name.isBlank()) R.string.custom_theme_name_required else R.string.custom_theme_name_duplicate
                ),
                color = colors.colorError,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        ClintOutlinedTextField(
            value = description,
            onValueChange = { input ->
                val clean = input.replace('\n', ' ')
                if (clean.length <= MAX_CUSTOM_THEME_DESCRIPTION_LENGTH) description = clean
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.custom_theme_description_label)) },
            minLines = 2,
            maxLines = 3,
            supportingText = {
                Text(
                    "${description.length}/$MAX_CUSTOM_THEME_DESCRIPTION_LENGTH",
                    color = colors.secondaryText,
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
        Spacer(Modifier.height(14.dp))
        SegmentedToggle(
            options = modeLabels,
            selected = if (editingLight) 0 else 1,
            onSelect = { editingLight = it == 0 }
        )
        Text(
            stringResource(R.string.custom_theme_palette_hint),
            color = colors.secondaryText,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp)
        )
    }

    Surface(color = colors.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ThemeTopBar(
                title = stringResource(
                    if (isNew) R.string.custom_theme_editor_new_title else R.string.custom_theme_editor_edit_title
                ),
                onBack = { if (dirty) confirmDiscard = true else onExit() },
                actions = {
                    IconButton(
                        enabled = modeDirty,
                        onClick = {
                            if (editingLight) light = initial.light else dark = initial.dark
                        }
                    ) {
                        Icon(
                            Icons.Filled.Restore,
                            contentDescription = stringResource(R.string.custom_theme_reset),
                            tint = if (modeDirty) colors.iconTint else colors.iconTint.copy(alpha = 0.38f)
                        )
                    }
                    TextButton(
                        enabled = canSave,
                        onClick = { onSave(CustomTheme(initial.id, name.trim(), light, dark, description.trim())) }
                    ) {
                        Text(
                            stringResource(R.string.action_save),
                            color = if (canSave) colors.primary else colors.secondaryText.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            )
            BoxWithConstraints(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                if (maxWidth >= 840.dp) {
                    Row(Modifier.fillMaxSize()) {
                        Column(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        ) {
                            header()
                            Spacer(Modifier.height(8.dp))
                            ColorSlotsList(palette) { pickerSlot = it }
                        }
                        Box(Modifier.width(1.dp).fillMaxHeight().background(colors.divider))
                        Column(
                            Modifier
                                .weight(1.2f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        ) {
                            ThemePreviewPage(palette, editingLight)
                        }
                    }
                } else {
                    AdaptiveWidthContainer(maxContentWidth) {
                        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                            Spacer(Modifier.height(14.dp))
                            header()
                            Spacer(Modifier.height(14.dp))
                            SegmentedToggle(
                                options = tabLabels,
                                selected = tab,
                                onSelect = { tab = it }
                            )
                            Column(
                                Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .padding(top = 8.dp, bottom = 16.dp)
                            ) {
                                if (tab == 0) {
                                    ColorSlotsList(palette) { pickerSlot = it }
                                } else {
                                    Spacer(Modifier.height(8.dp))
                                    ThemePreviewPage(palette, editingLight)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pickerSlot?.let { slot ->
        ClintColorPickerDialog(
            initial = palette.color(slot),
            hideStatusBar = hideStatusBar,
            hideSystemNavigation = hideSystemNavigation,
            onDismiss = { pickerSlot = null },
            onPicked = { picked ->
                val argb = picked.copy(alpha = 1f).toArgb()
                if (editingLight) light = light.with(slot, argb) else dark = dark.with(slot, argb)
            }
        )
    }

    ConfirmDialogHost(
        config = discardConfig,
        hideStatusBar = hideStatusBar,
        hideSystemNavigation = hideSystemNavigation,
        onDismiss = { confirmDiscard = false }
    )
}

@Composable
internal fun SegmentedToggle(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalClintColors.current
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surfaceVariant)
            .padding(4.dp)
    ) {
        options.forEachIndexed { index, label ->
            val isSelected = index == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) colors.primary else Color.Transparent)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(index) }),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (isSelected) colors.onPrimary else colors.secondaryText,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ColorSlotsList(palette: CustomPalette, onPick: (ThemeSlot) -> Unit) {
    val colors = LocalClintColors.current
    ThemeSlotGroup.entries.forEach { group ->
        Text(
            stringResource(group.titleRes),
            color = colors.primary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 6.dp, top = 14.dp, bottom = 8.dp)
        )
        SettingsSection(colors.cardBackground) {
            val slots = ThemeSlot.entries.filter { it.group == group }
            slots.forEachIndexed { index, slot ->
                if (index > 0) RowDivider(colors.divider)
                SlotRow(slot, palette[slot]) { onPick(slot) }
            }
        }
    }
}

@Composable
private fun SlotRow(slot: ThemeSlot, argb: Int, onClick: () -> Unit) {
    val colors = LocalClintColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(argb))
                .border(1.dp, colors.divider, CircleShape)
        )
        Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Text(stringResource(slot.titleRes), color = colors.onSurface, fontSize = 15.sp)
            Text(
                formatArgb(argb),
                color = colors.secondaryText,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 1.dp)
            )
        }
        Icon(
            Icons.Filled.Edit,
            contentDescription = null,
            tint = colors.iconTint,
            modifier = Modifier.size(20.dp)
        )
    }
}
