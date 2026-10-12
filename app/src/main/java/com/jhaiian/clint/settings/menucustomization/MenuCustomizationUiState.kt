package com.jhaiian.clint.settings.menucustomization

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jhaiian.clint.browser.menu.CustomizableMenuItem

class MenuCustomizationEntry(val item: CustomizableMenuItem, visible: Boolean) {
    var visible by mutableStateOf(visible)
}

class MenuCustomizationUiState(initialEntries: List<MenuCustomizationEntry>) {
    val entries = mutableStateListOf<MenuCustomizationEntry>().apply { addAll(initialEntries) }

    fun replaceAll(newEntries: List<MenuCustomizationEntry>) {
        entries.clear()
        entries.addAll(newEntries)
    }
}
