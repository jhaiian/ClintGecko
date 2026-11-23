package com.jhaiian.clint.parental

import android.content.Context

object ParentalControls {

    const val AREA_BLOCKER = "area_website_blocker"
    const val AREA_EXTENSIONS = "area_extensions"
    const val AREA_HISTORY = "area_history"
    const val AREA_RESTORE = "area_backup_restore"

    private const val PREFS = "parental_controls"

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isEnabled(context: Context, area: String): Boolean = prefs(context).getBoolean(area, true)

    fun setEnabled(context: Context, area: String, enabled: Boolean) {
        prefs(context).edit().putBoolean(area, enabled).commit()
    }

    fun isProtected(context: Context, area: String): Boolean =
        ParentalPassword.isSet(context) && isEnabled(context, area)

    fun gate(
        context: Context,
        area: String,
        title: String,
        message: String,
        positiveLabel: String,
        show: (ParentalGateRequest) -> Unit,
        action: () -> Unit
    ) {
        if (!isProtected(context, area)) {
            action()
            return
        }
        show(ParentalGateRequest(title, message, positiveLabel, action))
    }
}
