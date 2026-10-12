package com.jhaiian.clint.ui

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerLayoutType
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.theme.LocalClintColors
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClintDatePickerDialog(
    currentMillis: Long,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    title: String? = null,
    onDismiss: () -> Unit,
    onPicked: (year: Int, month: Int, dayOfMonth: Int) -> Unit
) {
    val colors = LocalClintColors.current
    val initialUtc = remember(currentMillis) {
        val local = Calendar.getInstance().apply { timeInMillis = currentMillis }
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
    }
    val state = rememberDatePickerState(initialSelectedDateMillis = initialUtc)
    val pickerColors = DatePickerDefaults.colors(
        containerColor = colors.popupBackground,
        titleContentColor = colors.secondaryText,
        headlineContentColor = colors.onSurface,
        weekdayContentColor = colors.secondaryText,
        subheadContentColor = colors.secondaryText,
        navigationContentColor = colors.iconTint,
        yearContentColor = colors.onSurface,
        currentYearContentColor = colors.primary,
        selectedYearContentColor = colors.onPrimary,
        selectedYearContainerColor = colors.primary,
        dayContentColor = colors.onSurface,
        disabledDayContentColor = colors.secondaryText,
        selectedDayContentColor = colors.onPrimary,
        selectedDayContainerColor = colors.primary,
        todayContentColor = colors.primary,
        todayDateBorderColor = colors.primary,
        dividerColor = colors.divider,
        dateTextFieldColors = clintPickerTextFieldColors()
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ClintDialogStatusBarEffect(hideStatusBar, hideSystemNavigation)
        BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp), contentAlignment = Alignment.Center) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = colors.popupBackground,
                modifier = Modifier.widthIn(max = 412.dp).fillMaxWidth()
            ) {
                Column(Modifier.heightIn(max = maxHeight)) {
                    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                        DatePicker(
                            state = state,
                            colors = pickerColors,
                            title = {
                                Text(
                                    title ?: stringResource(R.string.download_schedule_date_picker_title),
                                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                                    fontSize = 14.sp
                                )
                            }
                        )
                    }
                    Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.action_cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                        }
                        TextButton(
                            onClick = {
                                val selected = state.selectedDateMillis
                                if (selected != null) {
                                    val picked = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = selected }
                                    onPicked(picked.get(Calendar.YEAR), picked.get(Calendar.MONTH), picked.get(Calendar.DAY_OF_MONTH))
                                }
                                onDismiss()
                            },
                            enabled = state.selectedDateMillis != null
                        ) {
                            Text(
                                stringResource(R.string.action_ok),
                                color = if (state.selectedDateMillis != null) colors.primary else colors.secondaryText,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClintTimePickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onDismiss: () -> Unit,
    onPicked: (hour: Int, minute: Int) -> Unit
) {
    val colors = LocalClintColors.current
    val context = LocalContext.current
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = DateFormat.is24HourFormat(context)
    )
    val pickerColors = TimePickerDefaults.colors(
        clockDialColor = colors.surfaceVariant,
        clockDialSelectedContentColor = colors.onPrimary,
        clockDialUnselectedContentColor = colors.onSurface,
        selectorColor = colors.primary,
        containerColor = colors.popupBackground,
        periodSelectorBorderColor = colors.divider,
        periodSelectorSelectedContainerColor = colors.primary.copy(alpha = 0.24f),
        periodSelectorUnselectedContainerColor = colors.popupBackground,
        periodSelectorSelectedContentColor = colors.primary,
        periodSelectorUnselectedContentColor = colors.secondaryText,
        timeSelectorSelectedContainerColor = colors.primary.copy(alpha = 0.24f),
        timeSelectorUnselectedContainerColor = colors.surfaceVariant,
        timeSelectorSelectedContentColor = colors.primary,
        timeSelectorUnselectedContentColor = colors.onSurface
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ClintDialogStatusBarEffect(hideStatusBar, hideSystemNavigation)
        BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp), contentAlignment = Alignment.Center) {
            val horizontal = maxHeight < 560.dp && maxWidth > maxHeight
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = colors.popupBackground,
                modifier = Modifier.widthIn(max = if (horizontal) 600.dp else 380.dp).fillMaxWidth()
            ) {
                Column(Modifier.heightIn(max = maxHeight)) {
                    Column(
                        Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(top = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                            Text(title, color = colors.secondaryText, fontSize = 14.sp)
                        }
                        TimePicker(
                            state = state,
                            colors = pickerColors,
                            layoutType = if (horizontal) TimePickerLayoutType.Horizontal else TimePickerLayoutType.Vertical
                        )
                    }
                    Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.action_cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                        }
                        TextButton(onClick = {
                            onPicked(state.hour, state.minute)
                            onDismiss()
                        }) {
                            Text(stringResource(R.string.action_ok), color = colors.primary, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun clintPickerTextFieldColors() = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
    focusedTextColor = LocalClintColors.current.onSurface,
    unfocusedTextColor = LocalClintColors.current.onSurface,
    focusedBorderColor = LocalClintColors.current.primary,
    unfocusedBorderColor = LocalClintColors.current.divider,
    focusedLabelColor = LocalClintColors.current.primary,
    unfocusedLabelColor = LocalClintColors.current.secondaryText,
    cursorColor = LocalClintColors.current.primary,
    focusedSupportingTextColor = LocalClintColors.current.secondaryText,
    unfocusedSupportingTextColor = LocalClintColors.current.secondaryText
)

@Composable
fun ClintScheduleDateTimeRows(
    scheduledMillis: Long,
    onScheduledMillisChange: (Long) -> Unit,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    compact: Boolean = false
) {
    val colors = LocalClintColors.current
    val context = LocalContext.current
    val rowVerticalPadding = if (compact) 4.dp else 6.dp
    val titleSize = if (compact) 13.sp else 14.sp
    val valueSize = if (compact) 12.sp else 13.sp
    val dateFormat = remember { DateFormat.getMediumDateFormat(context) }
    val timeFormat = remember { DateFormat.getTimeFormat(context) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Row(
        Modifier.fillMaxWidth().clickable { showDatePicker = true }.padding(vertical = rowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.download_schedule_date_title), color = colors.onSurface, fontSize = titleSize, modifier = Modifier.weight(1f))
        Text(dateFormat.format(java.util.Date(scheduledMillis)), color = colors.secondaryText, fontSize = valueSize)
    }
    Row(
        Modifier.fillMaxWidth().clickable { showTimePicker = true }.padding(vertical = rowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.download_schedule_time_title), color = colors.onSurface, fontSize = titleSize, modifier = Modifier.weight(1f))
        Text(timeFormat.format(java.util.Date(scheduledMillis)), color = colors.secondaryText, fontSize = valueSize)
    }

    if (showDatePicker) {
        ClintDatePickerDialog(
            currentMillis = scheduledMillis,
            hideStatusBar = hideStatusBar,
            hideSystemNavigation = hideSystemNavigation,
            onDismiss = { showDatePicker = false }
        ) { year, month, day ->
            onScheduledMillisChange(
                Calendar.getInstance().apply {
                    timeInMillis = scheduledMillis
                    set(Calendar.YEAR, year); set(Calendar.MONTH, month); set(Calendar.DAY_OF_MONTH, day)
                }.timeInMillis
            )
        }
    }
    if (showTimePicker) {
        val current = Calendar.getInstance().apply { timeInMillis = scheduledMillis }
        ClintTimePickerDialog(
            title = stringResource(R.string.download_schedule_time_picker_title),
            initialHour = current.get(Calendar.HOUR_OF_DAY),
            initialMinute = current.get(Calendar.MINUTE),
            hideStatusBar = hideStatusBar,
            hideSystemNavigation = hideSystemNavigation,
            onDismiss = { showTimePicker = false }
        ) { hour, minute ->
            onScheduledMillisChange(
                Calendar.getInstance().apply {
                    timeInMillis = scheduledMillis
                    set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute); set(Calendar.SECOND, 0)
                }.timeInMillis
            )
        }
    }
}
