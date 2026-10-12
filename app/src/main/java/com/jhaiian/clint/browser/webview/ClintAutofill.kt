package com.jhaiian.clint.browser.webview

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.View
import android.view.autofill.AutofillManager

object ClintAutofill {
    const val PREF_AUTOFILL_ENABLED = "autofill_enabled"
    const val DEFAULT_AUTOFILL_ENABLED = true

    private fun manager(context: Context): AutofillManager? =
        context.getSystemService(AutofillManager::class.java)

    fun isSupported(context: Context): Boolean =
        manager(context)?.isAutofillSupported == true

    fun hasService(context: Context): Boolean {
        val autofillManager = manager(context) ?: return false
        if (!autofillManager.isAutofillSupported) return false
        val configured = runCatching {
            Settings.Secure.getString(context.contentResolver, "autofill_service")
        }.getOrNull()
        return !configured.isNullOrBlank() || autofillManager.isEnabled
    }

    fun apply(webView: ClintWebView, isIncognito: Boolean, enabled: Boolean) {
        webView.autofillAllowed = enabled && !isIncognito
        webView.importantForAutofill = if (enabled && !isIncognito) {
            View.IMPORTANT_FOR_AUTOFILL_YES
        } else {
            View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        }
    }

    fun cancel(context: Context) {
        runCatching { manager(context)?.cancel() }
    }

    fun openServicePicker(context: Context): Boolean {
        val picker = Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE, Uri.parse("package:android"))
        val fallback = Intent(Settings.ACTION_SETTINGS)
        return listOf(picker, fallback).any { intent ->
            if (context !is android.app.Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
                true
            } catch (_: ActivityNotFoundException) {
                false
            } catch (_: SecurityException) {
                false
            }
        }
    }
}
