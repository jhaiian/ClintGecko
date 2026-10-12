package com.jhaiian.clint.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.theme.LocalClintColors
import java.util.Locale

private fun colorHexDigits(color: Color): String =
    String.format(Locale.ROOT, "%06X", color.toArgb() and 0xFFFFFF)

@Composable
fun ClintColorPickerDialog(
    initial: Color,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onDismiss: () -> Unit,
    onPicked: (Color) -> Unit
) {
    val colors = LocalClintColors.current
    val controller = rememberColorPickerController()
    val selected by controller.selectedColor
    var exactColor by remember(initial) { mutableStateOf<Color?>(initial) }
    var hexText by remember(initial) { mutableStateOf(colorHexDigits(initial)) }
    val current = exactColor ?: if (selected == Color.Transparent) initial else selected.copy(alpha = 1f)

    LaunchedEffect(selected) {
        if (exactColor == null && selected != Color.Transparent) {
            hexText = colorHexDigits(selected)
        }
    }

    fun onHexChanged(raw: String) {
        val digits = raw
            .filter { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
            .take(6)
            .uppercase(Locale.ROOT)
        hexText = digits
        if (digits.length == 6) {
            val parsed = Color(0xFF000000L or digits.toLong(16))
            exactColor = parsed
            controller.selectByColor(parsed, fromUser = false)
        }
    }

    fun resetToInitial() {
        exactColor = initial
        hexText = colorHexDigits(initial)
        controller.selectByColor(initial, fromUser = false)
    }

    ClintPickerDialogShell(
        hideStatusBar = hideStatusBar,
        hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        onConfirm = {
            onPicked(exactColor ?: selected.copy(alpha = 1f))
            onDismiss()
        },
        expandable = true
    ) { wide ->
        val wheel: @Composable () -> Unit = {
            Box(
                Modifier.widthIn(max = if (wide) 240.dp else 280.dp).fillMaxWidth().aspectRatio(1f).padding(8.dp)
            ) {
                HsvColorPicker(
                    modifier = Modifier.fillMaxSize(),
                    controller = controller,
                    initialColor = initial,
                    onColorChanged = { if (it.fromUser) exactColor = null }
                )
            }
        }
        val controls: @Composable () -> Unit = {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                Text(
                    stringResource(R.string.color_picker_brightness),
                    color = colors.secondaryText,
                    fontSize = 12.sp
                )
                BrightnessSlider(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(36.dp),
                    controller = controller,
                    borderRadius = 12.dp,
                    borderSize = 4.dp,
                    borderColor = colors.divider,
                    wheelRadius = 10.dp,
                    initialColor = initial,
                    onColorChanged = { if (it.fromUser) exactColor = null }
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val previewShape = RoundedCornerShape(12.dp)
                    Row(
                        Modifier.width(72.dp).height(56.dp).clip(previewShape).border(1.dp, colors.divider, previewShape)
                    ) {
                        Box(Modifier.weight(1f).fillMaxHeight().background(initial).clickable { resetToInitial() })
                        Box(Modifier.weight(1f).fillMaxHeight().background(current))
                    }
                    OutlinedTextField(
                        value = hexText,
                        onValueChange = { onHexChanged(it) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text(stringResource(R.string.color_picker_hex_label)) },
                        prefix = { Text("#") },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Done
                        ),
                        colors = clintPickerTextFieldColors()
                    )
                }
            }
        }

        PickerTitle(stringResource(R.string.color_picker_select_color))
        if (wide) {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { wheel() }
                Box(Modifier.weight(1f)) { controls() }
            }
        } else {
            Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) { wheel() }
            controls()
        }
        Spacer(Modifier.height(8.dp))
    }
}
