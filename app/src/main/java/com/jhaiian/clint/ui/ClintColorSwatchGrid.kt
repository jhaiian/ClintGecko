package com.jhaiian.clint.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.theme.LocalClintColors

private val CustomSwatchBrush = Brush.sweepGradient(
    listOf(
        Color(0xFFE53935), Color(0xFFFDD835), Color(0xFF43A047), Color(0xFF00ACC1),
        Color(0xFF1E88E5), Color(0xFF8E24AA), Color(0xFFE53935)
    )
)

fun swatchContentColor(color: Int): Color =
    if (Color(color).luminance() > 0.6f) Color(0xFF1B1B1F) else Color.White

@Composable
fun ClintColorSwatchGrid(
    presets: List<Int>,
    selected: Int,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    labels: List<String> = emptyList(),
    columns: Int = 5,
    swatchSize: Dp = 42.dp
) {
    val colors = LocalClintColors.current
    var pickerOpen by remember { mutableStateOf(false) }
    val isCustom = presets.none { it == selected }
    val customLabel = stringResource(R.string.color_custom)
    val cells: List<Int?> = presets + listOf(null)
    Column(modifier) {
        cells.chunked(columns).forEach { rowCells ->
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowCells.forEach { cell ->
                    if (cell != null) {
                        val label = labels.getOrNull(presets.indexOf(cell))
                        val isSelected = cell == selected
                        Box(
                            Modifier
                                .size(swatchSize)
                                .clip(CircleShape)
                                .background(Color(cell))
                                .clickable { onSelect(cell) }
                                .semantics {
                                    if (label != null) contentDescription = label
                                    this.selected = isSelected
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = swatchContentColor(cell), modifier = Modifier.size(22.dp))
                            }
                        }
                    } else {
                        Box(
                            Modifier
                                .size(swatchSize)
                                .clip(CircleShape)
                                .then(if (isCustom) Modifier.background(Color(selected)) else Modifier.background(CustomSwatchBrush))
                                .clickable { pickerOpen = true }
                                .semantics {
                                    contentDescription = customLabel
                                    this.selected = isCustom
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCustom) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = swatchContentColor(selected), modifier = Modifier.size(22.dp))
                            } else {
                                Box(
                                    Modifier.size(swatchSize - 14.dp).clip(CircleShape).background(colors.popupBackground),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Palette, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (pickerOpen) {
        ClintColorPickerDialog(
            initial = Color(selected),
            hideStatusBar = hideStatusBar,
            hideSystemNavigation = hideSystemNavigation,
            onDismiss = { pickerOpen = false }
        ) { picked ->
            onSelect(picked.copy(alpha = 1f).toArgb())
        }
    }
}
