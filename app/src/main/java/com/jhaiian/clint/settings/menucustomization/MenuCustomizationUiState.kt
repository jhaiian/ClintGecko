package com.jhaiian.clint.settings.menucustomization

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jhaiian.clint.browser.menu.BrowserMenuLayout
import com.jhaiian.clint.browser.menu.CustomizableMenuItem
import com.jhaiian.clint.browser.menu.MenuNavSlot
import com.jhaiian.clint.browser.menu.MenuSection

sealed interface MenuCustomizeRow {
    val key: String
}

class MenuSectionHeaderRow(val section: MenuSection) : MenuCustomizeRow {
    override val key: String = "menu_section:${section.id}"
}

class MenuItemEntryRow(val item: CustomizableMenuItem) : MenuCustomizeRow {
    override val key: String = menuItemDragKey(item.id)
}

class MenuCustomizationUiState(layout: BrowserMenuLayout, menuStyle: String) {
    val rows = mutableStateListOf<MenuCustomizeRow>()
    val navSlots = mutableStateListOf<MenuNavSlot>()
    var gridColumns by mutableIntStateOf(layout.gridColumns)
    var compact by mutableStateOf(layout.compact)
    var showHeader by mutableStateOf(layout.showHeader)
    var gridLabels by mutableStateOf(layout.gridLabels)
    var showNavBar by mutableStateOf(layout.showNavBar)
    var menuStyle by mutableStateOf(menuStyle)

    init {
        load(layout)
    }

    fun load(layout: BrowserMenuLayout) {
        rows.clear()
        MenuSection.entries.forEach { section ->
            rows.add(MenuSectionHeaderRow(section))
            layout.itemsIn(section).forEach { rows.add(MenuItemEntryRow(it)) }
        }
        navSlots.clear()
        navSlots.addAll(layout.navSlots)
        gridColumns = layout.gridColumns
        compact = layout.compact
        showHeader = layout.showHeader
        gridLabels = layout.gridLabels
        showNavBar = layout.showNavBar
    }

    fun rowSections(): List<MenuSection> {
        var current = MenuSection.GRID
        return rows.map { row ->
            if (row is MenuSectionHeaderRow) current = row.section
            current
        }
    }

    fun arrangement(): Pair<List<CustomizableMenuItem>, Map<CustomizableMenuItem, MenuSection>> {
        val sections = rowSections()
        val order = mutableListOf<CustomizableMenuItem>()
        val map = mutableMapOf<CustomizableMenuItem, MenuSection>()
        rows.forEachIndexed { index, row ->
            if (row is MenuItemEntryRow) {
                order += row.item
                map[row.item] = sections[index]
            }
        }
        return order to map
    }

    fun toLayout(): BrowserMenuLayout {
        val (order, sections) = arrangement()
        return BrowserMenuLayout(order, sections, gridColumns, compact, showHeader, gridLabels, navSlots.toList(), showNavBar)
    }

    fun moveToSection(item: CustomizableMenuItem, section: MenuSection) {
        val from = rows.indexOfFirst { it is MenuItemEntryRow && it.item == item }
        if (from == -1) return
        val row = rows.removeAt(from)
        var insert = rows.indexOfFirst { it is MenuSectionHeaderRow && it.section == section } + 1
        while (insert < rows.size && rows[insert] is MenuItemEntryRow) insert++
        rows.add(insert, row)
    }

    fun setNavSlot(index: Int, slot: MenuNavSlot) {
        val existing = navSlots.indexOf(slot)
        if (existing == index) return
        if (existing != -1) navSlots[existing] = navSlots[index]
        navSlots[index] = slot
    }
}
