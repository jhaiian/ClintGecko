package com.jhaiian.clint.ui

import android.content.Context
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.theme.LocalClintColors
import java.text.DateFormatSymbols
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.launch

private val CalendarRowHeight = 40.dp
private val CalendarBodyHeight = CalendarRowHeight * 7
private val WeekNumberWidth = 36.dp
private val MonthGridHeight = 232.dp
private val WheelItemHeight = 44.dp
private const val WheelVisibleItems = 5
private const val MinYear = 1
private const val MaxYear = 9999

@Composable
internal fun ClintPickerDialogShell(
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    expandable: Boolean = false,
    content: @Composable (wide: Boolean) -> Unit
) {
    val colors = LocalClintColors.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ClintDialogStatusBarEffect(hideStatusBar, hideSystemNavigation)
        BoxWithConstraints(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            val wide = expandable && maxWidth >= 620.dp
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = colors.popupBackground,
                modifier = Modifier.widthIn(max = if (wide) 640.dp else 380.dp).fillMaxWidth()
            ) {
                Column(Modifier.heightIn(max = maxHeight)) {
                    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                        content(wide)
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.action_cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                        }
                        TextButton(onClick = onConfirm) {
                            Text(stringResource(R.string.action_ok), color = colors.primary, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun PickerTitle(text: String) {
    Text(
        text,
        color = LocalClintColors.current.secondaryText,
        fontSize = 14.sp,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp)
    )
}

@Composable
private fun PickerHeadline(text: String, subtitle: String? = null) {
    val colors = LocalClintColors.current
    Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 8.dp)) {
        Text(text, color = colors.onSurface, fontSize = 26.sp)
        if (subtitle != null) {
            Text(subtitle, color = colors.secondaryText, fontSize = 14.sp)
        }
    }
}

@Composable
private fun PickerNavRow(
    label: String,
    yearsVisible: Boolean,
    onToggleYears: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    previousDescription: String,
    nextDescription: String
) {
    val colors = LocalClintColors.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick = onToggleYears)
                .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = colors.iconTint,
                modifier = Modifier.rotate(if (yearsVisible) 180f else 0f)
            )
        }
        Spacer(Modifier.weight(1f))
        if (!yearsVisible) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = previousDescription, tint = colors.iconTint)
            }
            IconButton(onClick = onNext) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = nextDescription, tint = colors.iconTint)
            }
        }
    }
}

@Composable
private fun ClintYearGrid(selectedYear: Int, onYearSelected: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalClintColors.current
    val minYear = minOf(1900, selectedYear)
    val maxYear = maxOf(2100, selectedYear)
    val count = maxYear - minYear + 1
    val currentYear = remember { LocalDate.now().year }
    val selectedRow = (selectedYear - minYear) / 3
    val state = rememberLazyGridState(initialFirstVisibleItemIndex = (selectedRow - 1).coerceAtLeast(0) * 3)
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        state = state,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(count) { index ->
            val year = minYear + index
            val selected = year == selectedYear
            val shape = RoundedCornerShape(20.dp)
            Box(
                Modifier.height(40.dp).clip(shape)
                    .background(if (selected) colors.primary else Color.Transparent)
                    .then(if (!selected && year == currentYear) Modifier.border(1.dp, colors.primary, shape) else Modifier)
                    .clickable { onYearSelected(year) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    year.toString(),
                    color = when {
                        selected -> colors.onPrimary
                        year == currentYear -> colors.primary
                        else -> colors.onSurface
                    },
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun ClintCalendar(
    displayedMonth: YearMonth,
    onDisplayedMonthChange: (YearMonth) -> Unit,
    selectedDate: LocalDate,
    weekSelection: Boolean,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalClintColors.current
    val locale = LocalConfiguration.current.locales[0]
    val today = remember { LocalDate.now() }
    var showYears by remember { mutableStateOf(false) }
    val firstDay = if (weekSelection) DayOfWeek.MONDAY else WeekFields.of(locale).firstDayOfWeek
    val gridStart = remember(displayedMonth, firstDay) {
        displayedMonth.atDay(1).with(TemporalAdjusters.previousOrSame(firstDay))
    }
    val selectedMonday = remember(selectedDate) {
        selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }
    val monthLabel = remember(displayedMonth, locale) {
        DateTimeFormatter.ofPattern("LLLL y", locale).format(displayedMonth)
    }

    Column(modifier) {
        PickerNavRow(
            label = monthLabel,
            yearsVisible = showYears,
            onToggleYears = { showYears = !showYears },
            onPrevious = { onDisplayedMonthChange(displayedMonth.minusMonths(1)) },
            onNext = { onDisplayedMonthChange(displayedMonth.plusMonths(1)) },
            previousDescription = stringResource(R.string.date_time_picker_previous_month),
            nextDescription = stringResource(R.string.date_time_picker_next_month)
        )
        if (showYears) {
            ClintYearGrid(
                selectedYear = displayedMonth.year,
                onYearSelected = { year ->
                    onDisplayedMonthChange(displayedMonth.withYear(year))
                    showYears = false
                },
                modifier = Modifier.fillMaxWidth().height(CalendarBodyHeight)
            )
        } else {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(CalendarBodyHeight)) {
                Row(Modifier.fillMaxWidth().height(CalendarRowHeight), verticalAlignment = Alignment.CenterVertically) {
                    if (weekSelection) {
                        Text(
                            stringResource(R.string.date_time_picker_week_abbrev),
                            color = colors.secondaryText,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(WeekNumberWidth)
                        )
                    }
                    for (i in 0 until 7) {
                        Text(
                            firstDay.plus(i.toLong()).getDisplayName(TextStyle.NARROW, locale),
                            color = colors.secondaryText,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                for (row in 0 until 6) {
                    val rowStart = gridStart.plusWeeks(row.toLong())
                    val rowSelected = weekSelection && rowStart == selectedMonday
                    val rowModifier = Modifier.fillMaxWidth().height(CalendarRowHeight)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (rowSelected) colors.primary.copy(alpha = 0.18f) else Color.Transparent)
                    Row(
                        if (weekSelection) rowModifier.clickable { onDateSelected(rowStart) } else rowModifier,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (weekSelection) {
                            Text(
                                rowStart.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR).toString(),
                                color = if (rowSelected) colors.primary else colors.secondaryText,
                                fontSize = 12.sp,
                                fontWeight = if (rowSelected) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(WeekNumberWidth)
                            )
                        }
                        for (d in 0 until 7) {
                            val date = rowStart.plusDays(d.toLong())
                            val inMonth = YearMonth.from(date) == displayedMonth
                            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                                if (weekSelection || inMonth) {
                                    val daySelected = !weekSelection && date == selectedDate
                                    val isToday = date == today
                                    val cellModifier = Modifier.size(36.dp).clip(CircleShape)
                                        .background(if (daySelected) colors.primary else Color.Transparent)
                                        .then(
                                            if (isToday && !daySelected) Modifier.border(1.dp, colors.primary, CircleShape) else Modifier
                                        )
                                    Box(
                                        if (weekSelection) cellModifier else cellModifier.clickable { onDateSelected(date) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            date.dayOfMonth.toString(),
                                            color = when {
                                                daySelected -> colors.onPrimary
                                                isToday -> colors.primary
                                                !inMonth -> colors.secondaryText.copy(alpha = 0.6f)
                                                else -> colors.onSurface
                                            },
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClintWheelColumn(
    items: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    onSelected: (Int) -> Unit
) {
    val colors = LocalClintColors.current
    val scope = rememberCoroutineScope()
    val state = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex.coerceIn(0, items.lastIndex))
    val fling = rememberSnapFlingBehavior(lazyListState = state, snapPosition = SnapPosition.Center)
    val currentOnSelected by rememberUpdatedState(onSelected)
    val currentSelectedIndex by rememberUpdatedState(selectedIndex)
    val centerIndex by remember(state) {
        derivedStateOf {
            val info = state.layoutInfo
            val center = info.viewportSize.height / 2
            info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - center) }?.index ?: currentSelectedIndex
        }
    }

    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }.collect { scrolling ->
            if (!scrolling) currentOnSelected(centerIndex)
        }
    }

    Box(modifier.height(WheelItemHeight * WheelVisibleItems), contentAlignment = Alignment.Center) {
        Box(
            Modifier.fillMaxWidth().height(WheelItemHeight)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.primary.copy(alpha = 0.12f))
        )
        LazyColumn(
            state = state,
            flingBehavior = fling,
            contentPadding = PaddingValues(vertical = WheelItemHeight * (WheelVisibleItems / 2)),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(items) { index, label ->
                val selected = index == centerIndex
                Box(
                    Modifier.fillMaxWidth().height(WheelItemHeight)
                        .clickable { scope.launch { state.animateScrollToItem(index) } },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color = if (selected) colors.onSurface else colors.secondaryText,
                        fontSize = if (selected) 22.sp else 18.sp,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun ClintTimeWheels(
    hour: Int,
    minute: Int,
    is24Hour: Boolean,
    onTimeChange: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalClintColors.current
    val locale = LocalConfiguration.current.locales[0]
    val periods = remember(locale) { DateFormatSymbols.getInstance(locale).amPmStrings.toList() }
    val minutes = remember { (0..59).map { String.format(Locale.ROOT, "%02d", it) } }
    val hours = remember(is24Hour) {
        if (is24Hour) (0..23).map { String.format(Locale.ROOT, "%02d", it) } else (1..12).map { it.toString() }
    }
    val isPm = hour >= 12
    val hourIndex = if (is24Hour) hour else (hour + 11) % 12
    val wheelWidth = 72.dp

    Row(modifier, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        ClintWheelColumn(hours, hourIndex, Modifier.width(wheelWidth)) { index ->
            val newHour = if (is24Hour) index else (index + 1) % 12 + if (isPm) 12 else 0
            onTimeChange(newHour, minute)
        }
        Text(":", color = colors.onSurface, fontSize = 22.sp, modifier = Modifier.padding(horizontal = 4.dp))
        ClintWheelColumn(minutes, minute, Modifier.width(wheelWidth)) { index ->
            onTimeChange(hour, index)
        }
        if (!is24Hour) {
            Spacer(Modifier.width(8.dp))
            ClintWheelColumn(periods, if (isPm) 1 else 0, Modifier.width(wheelWidth)) { index ->
                onTimeChange(hour % 12 + if (index == 1) 12 else 0, minute)
            }
        }
    }
}

@Composable
private fun PickerChip(text: String, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = LocalClintColors.current
    Box(
        modifier.clip(RoundedCornerShape(12.dp))
            .background(if (active) colors.primary.copy(alpha = 0.24f) else colors.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (active) colors.primary else colors.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun weekRangeText(context: Context, monday: LocalDate): String {
    val zone = ZoneId.systemDefault()
    val start = monday.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
    val end = monday.plusDays(6).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
    return DateUtils.formatDateRange(
        context,
        start,
        end,
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_MONTH
    )
}

@Composable
fun ClintMonthPickerDialog(
    initial: YearMonth,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onDismiss: () -> Unit,
    onPicked: (YearMonth) -> Unit
) {
    val colors = LocalClintColors.current
    val locale = LocalConfiguration.current.locales[0]
    var year by remember(initial) { mutableIntStateOf(initial.year) }
    var month by remember(initial) { mutableIntStateOf(initial.monthValue) }
    var showYears by remember { mutableStateOf(false) }
    val currentMonth = remember { YearMonth.now() }
    val monthNames = remember(locale) {
        (1..12).map { Month.of(it).getDisplayName(TextStyle.SHORT_STANDALONE, locale) }
    }
    val headline = remember(year, month, locale) {
        DateTimeFormatter.ofPattern("LLLL y", locale).format(YearMonth.of(year, month))
    }

    ClintPickerDialogShell(
        hideStatusBar = hideStatusBar,
        hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        onConfirm = {
            onPicked(YearMonth.of(year, month))
            onDismiss()
        }
    ) {
        PickerTitle(stringResource(R.string.date_time_picker_select_month))
        PickerHeadline(headline)
        PickerNavRow(
            label = year.toString(),
            yearsVisible = showYears,
            onToggleYears = { showYears = !showYears },
            onPrevious = { if (year > MinYear) year -= 1 },
            onNext = { if (year < MaxYear) year += 1 },
            previousDescription = stringResource(R.string.date_time_picker_previous_year),
            nextDescription = stringResource(R.string.date_time_picker_next_year)
        )
        Box(Modifier.fillMaxWidth().height(MonthGridHeight)) {
            if (showYears) {
                ClintYearGrid(
                    selectedYear = year,
                    onYearSelected = {
                        year = it
                        showYears = false
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (r in 0 until 4) {
                        Row(
                            Modifier.fillMaxWidth().weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (c in 0 until 3) {
                                val m = r * 3 + c + 1
                                val selected = m == month
                                val isCurrent = year == currentMonth.year && m == currentMonth.monthValue
                                val shape = RoundedCornerShape(24.dp)
                                Box(
                                    Modifier.weight(1f).fillMaxHeight().clip(shape)
                                        .background(if (selected) colors.primary else Color.Transparent)
                                        .then(
                                            if (!selected && isCurrent) Modifier.border(1.dp, colors.primary, shape) else Modifier
                                        )
                                        .clickable { month = m },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        monthNames[m - 1],
                                        color = when {
                                            selected -> colors.onPrimary
                                            isCurrent -> colors.primary
                                            else -> colors.onSurface
                                        },
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun ClintWeekPickerDialog(
    initial: LocalDate,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onDismiss: () -> Unit,
    onPicked: (monday: LocalDate) -> Unit
) {
    val context = LocalContext.current
    var selected by remember(initial) {
        mutableStateOf(initial.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
    }
    var displayed by remember(initial) { mutableStateOf(YearMonth.from(selected)) }
    val headline = stringResource(
        R.string.date_time_picker_week_label,
        selected.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR),
        selected.get(IsoFields.WEEK_BASED_YEAR)
    )
    val range = remember(selected) { weekRangeText(context, selected) }

    ClintPickerDialogShell(
        hideStatusBar = hideStatusBar,
        hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        onConfirm = {
            onPicked(selected)
            onDismiss()
        }
    ) {
        PickerTitle(stringResource(R.string.date_time_picker_select_week))
        PickerHeadline(headline, range)
        ClintCalendar(
            displayedMonth = displayed,
            onDisplayedMonthChange = { displayed = it },
            selectedDate = selected,
            weekSelection = true,
            onDateSelected = { selected = it },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun ClintDateTimeLocalPickerDialog(
    initial: LocalDateTime,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onDismiss: () -> Unit,
    onPicked: (LocalDateTime) -> Unit
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val is24Hour = remember { DateFormat.is24HourFormat(context) }
    var date by remember(initial) { mutableStateOf(initial.toLocalDate()) }
    var displayed by remember(initial) { mutableStateOf(YearMonth.from(initial.toLocalDate())) }
    var hour by remember(initial) { mutableIntStateOf(initial.hour) }
    var minute by remember(initial) { mutableIntStateOf(initial.minute) }
    var showTime by remember { mutableStateOf(false) }
    val dateText = remember(date, locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(date)
    }
    val timeText = remember(hour, minute, is24Hour) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        DateFormat.getTimeFormat(context).format(calendar.time)
    }

    ClintPickerDialogShell(
        hideStatusBar = hideStatusBar,
        hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        onConfirm = {
            onPicked(LocalDateTime.of(date, java.time.LocalTime.of(hour, minute)))
            onDismiss()
        },
        expandable = true
    ) { wide ->
        PickerTitle(stringResource(R.string.date_time_picker_select_date_time))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PickerChip(dateText, active = wide || !showTime, modifier = Modifier.weight(1f)) { showTime = false }
            PickerChip(timeText, active = wide || showTime, modifier = Modifier.weight(1f)) { showTime = true }
        }
        if (wide) {
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                ClintCalendar(
                    displayedMonth = displayed,
                    onDisplayedMonthChange = { displayed = it },
                    selectedDate = date,
                    weekSelection = false,
                    onDateSelected = { date = it },
                    modifier = Modifier.weight(1.3f)
                )
                ClintTimeWheels(
                    hour = hour,
                    minute = minute,
                    is24Hour = is24Hour,
                    onTimeChange = { h, m ->
                        hour = h
                        minute = m
                    },
                    modifier = Modifier.weight(1f).padding(end = 16.dp)
                )
            }
        } else if (showTime) {
            Box(
                Modifier.fillMaxWidth().height(CalendarBodyHeight + 48.dp),
                contentAlignment = Alignment.Center
            ) {
                ClintTimeWheels(
                    hour = hour,
                    minute = minute,
                    is24Hour = is24Hour,
                    onTimeChange = { h, m ->
                        hour = h
                        minute = m
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            ClintCalendar(
                displayedMonth = displayed,
                onDisplayedMonthChange = { displayed = it },
                selectedDate = date,
                weekSelection = false,
                onDateSelected = { date = it },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}
