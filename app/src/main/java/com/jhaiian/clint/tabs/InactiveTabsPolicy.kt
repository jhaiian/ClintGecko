package com.jhaiian.clint.tabs

object InactiveTabsPolicy {
    const val PREF_DELETE_INACTIVE_TABS = "delete_inactive_tabs"
    const val NEVER = "never"
    const val AFTER_1_DAY = "1_day"
    const val AFTER_1_WEEK = "1_week"
    const val AFTER_1_MONTH = "1_month"
    const val DEFAULT = NEVER

    private const val DAY_MILLIS = 24L * 60L * 60L * 1000L

    fun normalize(value: String?): String = when (value) {
        NEVER, AFTER_1_DAY, AFTER_1_WEEK, AFTER_1_MONTH -> value
        else -> DEFAULT
    }

    fun thresholdMillis(value: String?): Long = when (normalize(value)) {
        AFTER_1_DAY -> DAY_MILLIS
        AFTER_1_WEEK -> 7L * DAY_MILLIS
        AFTER_1_MONTH -> 30L * DAY_MILLIS
        else -> Long.MAX_VALUE
    }
}
