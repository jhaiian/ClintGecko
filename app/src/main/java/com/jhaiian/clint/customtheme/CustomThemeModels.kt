package com.jhaiian.clint.customtheme

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.theme.ClintColors
import com.jhaiian.clint.ui.theme.ThemeMode
import com.jhaiian.clint.ui.theme.resolveClintTheme
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

const val CUSTOM_ACCENT_PREFIX = "custom:"
const val CUSTOM_THEME_REVISION_PREF = "custom_theme_revision"
const val MAX_CUSTOM_THEME_NAME_LENGTH = 40
const val MAX_CUSTOM_THEME_DESCRIPTION_LENGTH = 80
private const val CUSTOM_THEMES_PREF = "custom_themes"

enum class ThemeSlotGroup(val titleRes: Int) {
    SURFACES(R.string.custom_theme_group_surfaces),
    CONTENT(R.string.custom_theme_group_content),
    ACCENT(R.string.custom_theme_group_accent),
    BUTTONS(R.string.custom_theme_group_buttons),
    MENUS(R.string.custom_theme_group_menus),
    ERRORS(R.string.custom_theme_group_errors),
    SYSTEM_BARS(R.string.custom_theme_group_system_bars)
}

enum class ThemeSlot(val group: ThemeSlotGroup, val titleRes: Int) {
    BACKGROUND(ThemeSlotGroup.SURFACES, R.string.custom_theme_slot_background),
    SURFACE(ThemeSlotGroup.SURFACES, R.string.custom_theme_slot_surface),
    CARD_BACKGROUND(ThemeSlotGroup.SURFACES, R.string.custom_theme_slot_card_background),
    SURFACE_VARIANT(ThemeSlotGroup.SURFACES, R.string.custom_theme_slot_surface_variant),
    ADDRESS_BAR(ThemeSlotGroup.SURFACES, R.string.custom_theme_slot_address_bar),
    DIVIDER(ThemeSlotGroup.SURFACES, R.string.custom_theme_slot_divider),
    ON_SURFACE(ThemeSlotGroup.CONTENT, R.string.custom_theme_slot_on_surface),
    SECONDARY_TEXT(ThemeSlotGroup.CONTENT, R.string.custom_theme_slot_secondary_text),
    ICON_TINT(ThemeSlotGroup.CONTENT, R.string.custom_theme_slot_icon_tint),
    PRIMARY(ThemeSlotGroup.ACCENT, R.string.custom_theme_slot_primary),
    ON_PRIMARY(ThemeSlotGroup.ACCENT, R.string.custom_theme_slot_on_primary),
    BUTTON_BACKGROUND(ThemeSlotGroup.BUTTONS, R.string.custom_theme_slot_button_background),
    BUTTON_ICON_TINT(ThemeSlotGroup.BUTTONS, R.string.custom_theme_slot_button_icon_tint),
    BUTTON_TEXT(ThemeSlotGroup.BUTTONS, R.string.custom_theme_slot_button_text),
    POPUP_BACKGROUND(ThemeSlotGroup.MENUS, R.string.custom_theme_slot_popup_background),
    POPUP_TEXT(ThemeSlotGroup.MENUS, R.string.custom_theme_slot_popup_text),
    POPUP_STROKE(ThemeSlotGroup.MENUS, R.string.custom_theme_slot_popup_stroke),
    POPUP_CHECK(ThemeSlotGroup.MENUS, R.string.custom_theme_slot_popup_check),
    ERROR(ThemeSlotGroup.ERRORS, R.string.custom_theme_slot_error),
    ERROR_CONTAINER(ThemeSlotGroup.ERRORS, R.string.custom_theme_slot_error_container),
    ON_ERROR_CONTAINER(ThemeSlotGroup.ERRORS, R.string.custom_theme_slot_on_error_container),
    STATUS_BAR(ThemeSlotGroup.SYSTEM_BARS, R.string.custom_theme_slot_status_bar),
    NAVIGATION_BAR(ThemeSlotGroup.SYSTEM_BARS, R.string.custom_theme_slot_navigation_bar)
}

data class CustomPalette(val colors: Map<ThemeSlot, Int>) {

    operator fun get(slot: ThemeSlot): Int = colors[slot] ?: FALLBACK

    fun color(slot: ThemeSlot): Color = Color(get(slot))

    fun with(slot: ThemeSlot, argb: Int): CustomPalette = CustomPalette(colors + (slot to argb))

    fun toJson(): JSONObject {
        val json = JSONObject()
        ThemeSlot.entries.forEach { json.put(it.name, get(it)) }
        return json
    }

    companion object {
        private val FALLBACK = Color(0xFF808080).toArgb()

        val EMPTY = CustomPalette(emptyMap())

        fun fromJson(json: JSONObject?, fallback: CustomPalette = EMPTY): CustomPalette =
            CustomPalette(
                ThemeSlot.entries.associateWith { slot ->
                    if (json != null && json.has(slot.name)) json.getInt(slot.name) else fallback[slot]
                }
            )
    }
}

data class CustomTheme(
    val id: String,
    val name: String,
    val light: CustomPalette,
    val dark: CustomPalette,
    val description: String = ""
) {
    fun palette(isLight: Boolean): CustomPalette = if (isLight) light else dark
}

data class ThemeChrome(
    val background: Int,
    val statusBar: Int,
    val navigationBar: Int,
    val cardBackground: Int,
    val primary: Int,
    val lightStatusBar: Boolean,
    val lightNavigationBar: Boolean
)

fun isCustomAccent(accent: String): Boolean = accent.startsWith(CUSTOM_ACCENT_PREFIX)

fun customAccentFor(id: String): String = CUSTOM_ACCENT_PREFIX + id

fun customThemeId(accent: String): String = accent.removePrefix(CUSTOM_ACCENT_PREFIX)

fun resolveCustomPalette(context: Context, accent: String, isLight: Boolean): CustomPalette? {
    if (!isCustomAccent(accent)) return null
    return CustomThemeRepository.find(context, customThemeId(accent))?.palette(isLight)
}

fun customAccentLabel(context: Context, accent: String): String =
    CustomThemeRepository.find(context, customThemeId(accent))?.name
        ?: context.getString(R.string.accent_material_you)

fun CustomPalette.toClintColors(isLight: Boolean): ClintColors = ClintColors(
    background = color(ThemeSlot.BACKGROUND),
    onSurface = color(ThemeSlot.ON_SURFACE),
    secondaryText = color(ThemeSlot.SECONDARY_TEXT),
    cardBackground = color(ThemeSlot.CARD_BACKGROUND),
    buttonBackground = color(ThemeSlot.BUTTON_BACKGROUND),
    surfaceVariant = color(ThemeSlot.SURFACE_VARIANT),
    popupBackground = color(ThemeSlot.POPUP_BACKGROUND),
    primary = color(ThemeSlot.PRIMARY),
    iconTint = color(ThemeSlot.ICON_TINT),
    divider = color(ThemeSlot.DIVIDER),
    popupText = color(ThemeSlot.POPUP_TEXT),
    surface = color(ThemeSlot.SURFACE),
    buttonIconTint = color(ThemeSlot.BUTTON_ICON_TINT),
    popupStroke = color(ThemeSlot.POPUP_STROKE),
    popupCheck = color(ThemeSlot.POPUP_CHECK),
    onPrimary = color(ThemeSlot.ON_PRIMARY),
    colorError = color(ThemeSlot.ERROR),
    colorErrorContainer = color(ThemeSlot.ERROR_CONTAINER),
    colorOnErrorContainer = color(ThemeSlot.ON_ERROR_CONTAINER),
    buttonTextColor = color(ThemeSlot.BUTTON_TEXT),
    addressBarColor = color(ThemeSlot.ADDRESS_BAR),
    isLight = isLight
)

fun seedPalette(context: Context, isLight: Boolean, accent: String, intensity: String): CustomPalette {
    val safeAccent = if (isCustomAccent(accent)) "material_you" else accent
    val r = resolveClintTheme(context, if (isLight) "light" else "dark", safeAccent, intensity)
    return CustomPalette(
        mapOf(
            ThemeSlot.BACKGROUND to r.background.toArgb(),
            ThemeSlot.SURFACE to r.surface.toArgb(),
            ThemeSlot.CARD_BACKGROUND to r.cardBackground.toArgb(),
            ThemeSlot.SURFACE_VARIANT to r.surfaceVariant.toArgb(),
            ThemeSlot.ADDRESS_BAR to r.addressBar.toArgb(),
            ThemeSlot.DIVIDER to r.divider.toArgb(),
            ThemeSlot.ON_SURFACE to r.onSurface.toArgb(),
            ThemeSlot.SECONDARY_TEXT to r.secondaryText.toArgb(),
            ThemeSlot.ICON_TINT to r.iconTint.toArgb(),
            ThemeSlot.PRIMARY to r.primary.toArgb(),
            ThemeSlot.ON_PRIMARY to r.onPrimary.toArgb(),
            ThemeSlot.BUTTON_BACKGROUND to r.buttonBackground.toArgb(),
            ThemeSlot.BUTTON_ICON_TINT to r.buttonIconTint.toArgb(),
            ThemeSlot.BUTTON_TEXT to r.buttonTextColor.toArgb(),
            ThemeSlot.POPUP_BACKGROUND to r.popupBackground.toArgb(),
            ThemeSlot.POPUP_TEXT to r.popupText.toArgb(),
            ThemeSlot.POPUP_STROKE to r.popupStroke.toArgb(),
            ThemeSlot.POPUP_CHECK to r.popupCheck.toArgb(),
            ThemeSlot.ERROR to r.error.toArgb(),
            ThemeSlot.ERROR_CONTAINER to r.errorContainer.toArgb(),
            ThemeSlot.ON_ERROR_CONTAINER to r.onErrorContainer.toArgb(),
            ThemeSlot.STATUS_BAR to r.statusBar.toArgb(),
            ThemeSlot.NAVIGATION_BAR to r.navigationBar.toArgb()
        )
    )
}

fun resolveThemeChrome(context: Context, theme: String, accent: String, intensity: String): ThemeChrome {
    val isLight = ThemeMode.isLight(theme)
    val palette = resolveCustomPalette(context, accent, isLight)
    if (palette != null) {
        return ThemeChrome(
            background = palette[ThemeSlot.BACKGROUND],
            statusBar = palette[ThemeSlot.STATUS_BAR],
            navigationBar = palette[ThemeSlot.NAVIGATION_BAR],
            cardBackground = palette[ThemeSlot.CARD_BACKGROUND],
            primary = palette[ThemeSlot.PRIMARY],
            lightStatusBar = palette.color(ThemeSlot.STATUS_BAR).luminance() > 0.5f,
            lightNavigationBar = palette.color(ThemeSlot.NAVIGATION_BAR).luminance() > 0.5f
        )
    }
    val safeAccent = if (isCustomAccent(accent)) "material_you" else accent
    val r = resolveClintTheme(context, theme, safeAccent, intensity)
    return ThemeChrome(
        background = r.background.toArgb(),
        statusBar = r.statusBar.toArgb(),
        navigationBar = r.navigationBar.toArgb(),
        cardBackground = r.cardBackground.toArgb(),
        primary = r.primary.toArgb(),
        lightStatusBar = r.isLight,
        lightNavigationBar = r.isLight
    )
}

fun contrastRatio(a: Color, b: Color): Float {
    val l1 = a.luminance()
    val l2 = b.luminance()
    return (maxOf(l1, l2) + 0.05f) / (minOf(l1, l2) + 0.05f)
}

object CustomThemeRepository {

    private fun prefs(context: Context) = PreferenceManager.getDefaultSharedPreferences(context)

    fun newId(): String = UUID.randomUUID().toString().replace("-", "").take(12)

    fun all(context: Context): List<CustomTheme> {
        val raw = prefs(context).getString(CUSTOM_THEMES_PREF, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                try {
                    parse(array.getJSONObject(index))
                } catch (_: Exception) {
                    null
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun find(context: Context, id: String): CustomTheme? = all(context).firstOrNull { it.id == id }

    fun isNameTaken(context: Context, name: String, excludeId: String? = null): Boolean =
        all(context).any { it.id != excludeId && it.name.trim().equals(name.trim(), ignoreCase = true) }

    fun uniqueName(context: Context, base: String): String {
        val trimmed = base.trim().take(MAX_CUSTOM_THEME_NAME_LENGTH)
        if (!isNameTaken(context, trimmed)) return trimmed
        var n = 2
        while (true) {
            val suffix = " ($n)"
            val candidate = trimmed.take(MAX_CUSTOM_THEME_NAME_LENGTH - suffix.length).trimEnd() + suffix
            if (!isNameTaken(context, candidate)) return candidate
            n++
        }
    }

    fun save(context: Context, theme: CustomTheme): Boolean {
        val current = all(context)
        val existing = current.firstOrNull { it.id == theme.id }
        val nameChanged = existing == null || !existing.name.trim().equals(theme.name.trim(), ignoreCase = true)
        if (nameChanged && isNameTaken(context, theme.name, theme.id)) return false
        val updated = if (existing != null) {
            current.map { if (it.id == theme.id) theme else it }
        } else {
            current + theme
        }
        write(context, updated)
        return true
    }

    fun delete(context: Context, id: String) {
        write(context, all(context).filterNot { it.id == id })
    }

    fun createFromCurrent(context: Context, name: String): CustomTheme {
        val prefs = prefs(context)
        val accent = prefs.getString("accent_color", "material_you") ?: "material_you"
        val intensity = prefs.getString("surface_intensity", "soft_tint") ?: "soft_tint"
        val existing = if (isCustomAccent(accent)) find(context, customThemeId(accent)) else null
        return if (existing != null) {
            CustomTheme(newId(), name, existing.light, existing.dark)
        } else {
            CustomTheme(
                newId(),
                name,
                seedPalette(context, true, accent, intensity),
                seedPalette(context, false, accent, intensity)
            )
        }
    }

    private fun parse(json: JSONObject): CustomTheme = CustomTheme(
        id = json.getString("id"),
        name = json.getString("name"),
        light = CustomPalette.fromJson(json.optJSONObject("light")),
        dark = CustomPalette.fromJson(json.optJSONObject("dark")),
        description = json.optString("description", "")
    )

    private fun write(context: Context, themes: List<CustomTheme>) {
        val array = JSONArray()
        themes.forEach { theme ->
            array.put(
                JSONObject()
                    .put("id", theme.id)
                    .put("name", theme.name)
                    .put("description", theme.description)
                    .put("light", theme.light.toJson())
                    .put("dark", theme.dark.toJson())
            )
        }
        val prefs = prefs(context)
        prefs.edit()
            .putString(CUSTOM_THEMES_PREF, array.toString())
            .putLong(CUSTOM_THEME_REVISION_PREF, prefs.getLong(CUSTOM_THEME_REVISION_PREF, 0L) + 1L)
            .commit()
    }
}
