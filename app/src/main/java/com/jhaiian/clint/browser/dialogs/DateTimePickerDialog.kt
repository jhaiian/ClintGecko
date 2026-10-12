package com.jhaiian.clint.browser.dialogs

import com.jhaiian.clint.browser.webview.ClintWebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.ClintDatePickerDialog
import com.jhaiian.clint.ui.ClintDateTimeLocalPickerDialog
import com.jhaiian.clint.ui.ClintMonthPickerDialog
import com.jhaiian.clint.ui.ClintTimePickerDialog
import com.jhaiian.clint.ui.ClintWeekPickerDialog
import java.lang.ref.WeakReference
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoField
import java.time.temporal.IsoFields
import java.util.Locale

enum class DateTimePickerType(val inputType: String) {
    Date("date"),
    Time("time"),
    DateTimeLocal("datetime-local"),
    Month("month"),
    Week("week");

    companion object {
        fun fromInputType(inputType: String): DateTimePickerType? = values().firstOrNull { it.inputType == inputType }
    }
}

class DateTimePickerRequest(
    val id: String,
    val type: DateTimePickerType,
    val value: String,
    val webView: WeakReference<ClintWebView>
)

private fun parseIsoWeek(value: String): LocalDate {
    val match = Regex("""^(\d{4,})-W(\d{2})$""").matchEntire(value) ?: throw IllegalArgumentException()
    val year = match.groupValues[1].toInt()
    val week = match.groupValues[2].toLong()
    return LocalDate.of(year, 1, 4)
        .with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, week)
        .with(ChronoField.DAY_OF_WEEK, 1)
}

private fun parseInitial(type: DateTimePickerType, value: String): LocalDateTime {
    val now = LocalDateTime.now()
    return runCatching {
        when (type) {
            DateTimePickerType.Date -> LocalDate.parse(value).atStartOfDay()
            DateTimePickerType.Time -> LocalTime.parse(value).atDate(now.toLocalDate())
            DateTimePickerType.DateTimeLocal -> LocalDateTime.parse(value)
            DateTimePickerType.Month -> YearMonth.parse(value).atDay(1).atStartOfDay()
            DateTimePickerType.Week -> parseIsoWeek(value).atStartOfDay()
        }
    }.getOrDefault(now)
}

private fun formatMonth(month: YearMonth): String =
    String.format(Locale.ROOT, "%04d-%02d", month.year, month.monthValue)

private fun formatWeek(date: LocalDate): String =
    String.format(
        Locale.ROOT, "%04d-W%02d",
        date.get(IsoFields.WEEK_BASED_YEAR), date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
    )

private fun formatTime(hour: Int, minute: Int): String =
    String.format(Locale.ROOT, "%02d:%02d", hour, minute)

@Composable
internal fun DateTimePickerDialog(request: DateTimePickerRequest, hideStatusBar: Boolean, hideSystemNavigation: Boolean, onDismiss: () -> Unit) {
    val initial = remember(request) { parseInitial(request.type, request.value) }

    fun applyValue(value: String) {
        val webView = request.webView.get() ?: return
        val safeId = request.id.replace("'", "")
        val quoted = org.json.JSONObject.quote(value)
        webView.evaluateJavascript("window.__clintApplyDateTime && window.__clintApplyDateTime('$safeId', $quoted)", null)
    }

    when (request.type) {
        DateTimePickerType.Date -> {
            val currentMillis = remember(request) {
                initial.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
            ClintDatePickerDialog(
                currentMillis = currentMillis,
                hideStatusBar = hideStatusBar,
                hideSystemNavigation = hideSystemNavigation,
                title = stringResource(R.string.date_time_picker_select_date),
                onDismiss = onDismiss
            ) { year, month, day ->
                applyValue(LocalDate.of(year, month + 1, day).toString())
            }
        }
        DateTimePickerType.Time -> {
            ClintTimePickerDialog(
                title = stringResource(R.string.date_time_picker_select_time),
                initialHour = initial.hour,
                initialMinute = initial.minute,
                hideStatusBar = hideStatusBar,
                hideSystemNavigation = hideSystemNavigation,
                onDismiss = onDismiss
            ) { hour, minute ->
                applyValue(formatTime(hour, minute))
            }
        }
        DateTimePickerType.DateTimeLocal -> {
            ClintDateTimeLocalPickerDialog(
                initial = initial,
                hideStatusBar = hideStatusBar,
                hideSystemNavigation = hideSystemNavigation,
                onDismiss = onDismiss
            ) { picked ->
                applyValue("${picked.toLocalDate()}T${formatTime(picked.hour, picked.minute)}")
            }
        }
        DateTimePickerType.Month -> {
            ClintMonthPickerDialog(
                initial = YearMonth.from(initial),
                hideStatusBar = hideStatusBar,
                hideSystemNavigation = hideSystemNavigation,
                onDismiss = onDismiss
            ) { picked ->
                applyValue(formatMonth(picked))
            }
        }
        DateTimePickerType.Week -> {
            ClintWeekPickerDialog(
                initial = initial.toLocalDate(),
                hideStatusBar = hideStatusBar,
                hideSystemNavigation = hideSystemNavigation,
                onDismiss = onDismiss
            ) { monday ->
                applyValue(formatWeek(monday))
            }
        }
    }
}
