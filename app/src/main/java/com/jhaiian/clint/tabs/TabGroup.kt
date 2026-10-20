package com.jhaiian.clint.tabs

import androidx.annotation.StringRes
import com.jhaiian.clint.R
import java.util.UUID

data class TabGroup(
    val id: String = newTabGroupId(),
    val name: String,
    val color: Int,
    val isIncognito: Boolean = false,
    val collapsed: Boolean = false
)

fun newTabGroupId(): String = "g" + UUID.randomUUID().toString().replace("-", "").take(12)

enum class TabGroupColor(val argb: Int, @StringRes val labelRes: Int) {
    BLUE(0xFF4A86E8.toInt(), R.string.tab_group_color_blue),
    RED(0xFFE5534B.toInt(), R.string.tab_group_color_red),
    YELLOW(0xFFF2B33D.toInt(), R.string.tab_group_color_yellow),
    GREEN(0xFF3FB27F.toInt(), R.string.tab_group_color_green),
    PINK(0xFFE0559B.toInt(), R.string.tab_group_color_pink),
    PURPLE(0xFF9B6BF2.toInt(), R.string.tab_group_color_purple),
    CYAN(0xFF2BB3C9.toInt(), R.string.tab_group_color_cyan),
    ORANGE(0xFFF08A3C.toInt(), R.string.tab_group_color_orange),
    GREY(0xFF8A8F98.toInt(), R.string.tab_group_color_grey);

    companion object {
        fun of(argb: Int): TabGroupColor? = entries.firstOrNull { it.argb == argb }

        fun next(used: Collection<Int>): Int =
            entries.firstOrNull { it.argb !in used }?.argb
                ?: entries[used.size % entries.size].argb
    }
}
