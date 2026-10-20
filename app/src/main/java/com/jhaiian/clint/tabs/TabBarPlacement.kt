package com.jhaiian.clint.tabs

object TabBarPlacement {
    const val PREF_TAB_STRIP_POSITION = "tab_strip_position"
    const val PREF_TAB_GROUP_BAR_POSITION = "tab_group_bar_position"
    const val FOLLOW = "follow"
    const val TOP = "top"
    const val BOTTOM = "bottom"

    fun normalize(value: String?): String = when (value) {
        TOP, BOTTOM -> value
        else -> FOLLOW
    }
}
