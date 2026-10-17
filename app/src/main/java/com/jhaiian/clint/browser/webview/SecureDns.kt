package com.jhaiian.clint.browser.webview

import android.content.Context
import android.net.Uri
import androidx.preference.PreferenceManager
import org.mozilla.geckoview.GeckoPreferenceController

object SecureDns {

    const val PREF_MODE = "secure_dns_mode"
    const val PREF_PROVIDER = "secure_dns_provider"
    const val PREF_CUSTOM_URL = "secure_dns_custom_url"

    const val MODE_OFF = "off"
    const val MODE_INCREASED = "increased"
    const val MODE_MAX = "max"

    const val PROVIDER_CLOUDFLARE = "cloudflare"
    const val PROVIDER_GOOGLE = "google"
    const val PROVIDER_QUAD9 = "quad9"
    const val PROVIDER_CUSTOM = "custom"

    private const val TRR_MODE_FIRST = 2
    private const val TRR_MODE_ONLY = 3
    private const val TRR_MODE_OFF_BY_CHOICE = 5

    private val providerUris = mapOf(
        PROVIDER_CLOUDFLARE to "https://mozilla.cloudflare-dns.com/dns-query",
        PROVIDER_GOOGLE to "https://dns.google/dns-query",
        PROVIDER_QUAD9 to "https://dns.quad9.net/dns-query"
    )

    fun isValidDohUri(value: String): Boolean {
        val uri = runCatching { Uri.parse(value.trim()) }.getOrNull() ?: return false
        return uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()
    }

    fun resolveUri(provider: String, customUrl: String): String {
        if (provider == PROVIDER_CUSTOM && isValidDohUri(customUrl)) return customUrl.trim()
        return providerUris[provider] ?: providerUris.getValue(PROVIDER_CLOUDFLARE)
    }

    fun apply(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val mode = prefs.getString(PREF_MODE, MODE_OFF) ?: MODE_OFF
        val provider = prefs.getString(PREF_PROVIDER, PROVIDER_CLOUDFLARE) ?: PROVIDER_CLOUDFLARE
        val customUrl = prefs.getString(PREF_CUSTOM_URL, "") ?: ""
        val trrMode = when (mode) {
            MODE_INCREASED -> TRR_MODE_FIRST
            MODE_MAX -> TRR_MODE_ONLY
            else -> TRR_MODE_OFF_BY_CHOICE
        }
        runCatching {
            if (mode != MODE_OFF) {
                GeckoPreferenceController.setGeckoPref("network.trr.uri", resolveUri(provider, customUrl), GeckoPreferenceController.PREF_BRANCH_USER)
            }
            GeckoPreferenceController.setGeckoPref("network.trr.mode", trrMode, GeckoPreferenceController.PREF_BRANCH_USER)
        }
    }
}
