package com.jhaiian.clint.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

fun formatRelativeTimestamp(millis: Long): String {
    val itemCal = Calendar.getInstance().apply { timeInMillis = millis }
    val nowCal = Calendar.getInstance()

    val isSameDay = itemCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
        itemCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)

    val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val isYesterday = itemCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
        itemCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR)

    val timePart = SimpleDateFormat("h:mm a", Locale.getDefault()).format(itemCal.time)

    return when {
        isSameDay -> timePart
        isYesterday -> "Yesterday, $timePart"
        itemCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) ->
            SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(itemCal.time)
        else ->
            SimpleDateFormat("MMM d yyyy, h:mm a", Locale.getDefault()).format(itemCal.time)
    }
}
