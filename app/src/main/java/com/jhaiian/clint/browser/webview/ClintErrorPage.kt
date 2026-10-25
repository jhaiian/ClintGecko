package com.jhaiian.clint.browser.webview

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.text.TextUtils
import android.util.Base64
import android.view.View
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.longshot.LongShot
import com.jhaiian.clint.privacy.PrivacyPrefs
import com.jhaiian.clint.ui.theme.ClintColors
import com.jhaiian.clint.ui.theme.ThemeMode
import com.jhaiian.clint.ui.theme.resolveClintColors
import com.jhaiian.clint.util.LocaleHelper
import org.json.JSONObject
import org.mozilla.geckoview.WebRequestError

internal object ClintErrorPage {

    private enum class Icon(val path: String) {
        OFFLINE("M23.64 7c-.45-.34-4.93-4-11.64-4-1.5 0-2.89.19-4.15.48L18.18 13.8 23.64 7zm-6.6 8.22L3.27 1.44 2 2.72l2.05 2.06C1.91 5.76.59 6.82.36 7l11.63 14.49.01.01.01-.01 3.9-4.86 3.32 3.32 1.27-1.27-3.46-3.46z"),
        GLOBE("M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z"),
        CLOCK("M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67z"),
        LOCK("M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"),
        WARNING("M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z"),
        LOOP("M3 12c0 2.21.91 4.2 2.36 5.64L3 20h6v-6l-2.24 2.24C5.68 15.15 5 13.66 5 12c0-2.61 1.67-4.83 4-5.65V4.26C5.55 5.15 3 8.27 3 12zm8 5h2v-2h-2v2zM21 4h-6v6l2.24-2.24C18.32 8.85 19 10.34 19 12c0 2.61-1.67 4.83-4 5.65v2.09c3.45-.89 6-4.01 6-7.74 0-2.21-.91-4.2-2.36-5.64L21 4zm-10 9h2V7h-2v6z"),
        LINK_OFF("M17 7h-4v1.9h4c1.71 0 3.1 1.39 3.1 3.1 0 1.43-.98 2.63-2.31 2.98l1.46 1.46C20.88 15.61 22 13.95 22 12c0-2.76-2.24-5-5-5zm-1 4h-2.19l2 2H16zM2 4.27l3.11 3.11C3.29 8.12 2 9.91 2 12c0 2.76 2.24 5 5 5h4v-1.9H7c-1.71 0-3.1-1.39-3.1-3.1 0-1.59 1.21-2.9 2.76-3.07L8.73 11H8v2h2.73L13 15.27V17h1.73l4.01 4L20 19.74 3.27 3 2 4.27z"),
        FILE("M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"),
        ERROR("M11 15h2v2h-2zm0-8h2v6h-2zm.99-5C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8z")
    }

    private class Spec(
        val code: String,
        val icon: Icon,
        val title: Int,
        val message: Int,
        val messageHasHost: Boolean = true,
        val tips: List<Int> = emptyList(),
        val danger: Boolean = false,
        val proceed: Boolean = false,
        val game: Boolean = false
    )

    class ThemeStyle(val palette: String, val colorScheme: String)

    private val connectionTips = listOf(R.string.error_page_tip_connection, R.string.error_page_tip_later)

    private fun offlineSpec(code: String) = Spec(
        code, Icon.OFFLINE, R.string.error_page_offline_title, R.string.error_page_offline_message,
        messageHasHost = false,
        tips = listOf(R.string.error_page_tip_airplane, R.string.error_page_tip_connection),
        game = true
    )

    private fun isDeviceOffline(context: Context): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
        return runCatching {
            val capabilities = manager.activeNetwork?.let { manager.getNetworkCapabilities(it) }
            capabilities == null || !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }.getOrDefault(false)
    }

    private fun specFor(error: WebRequestError, strictCertificates: Boolean, deviceOffline: Boolean): Spec {
        if (deviceOffline && error.code == WebRequestError.ERROR_UNKNOWN_HOST) return offlineSpec("ERROR_UNKNOWN_HOST")
        return specForCode(error, strictCertificates)
    }

    private fun specForCode(error: WebRequestError, strictCertificates: Boolean): Spec = when (error.code) {
        WebRequestError.ERROR_OFFLINE -> offlineSpec("ERROR_OFFLINE")
        WebRequestError.ERROR_UNKNOWN_HOST -> Spec(
            "ERROR_UNKNOWN_HOST", Icon.GLOBE, R.string.error_page_dns_title, R.string.error_page_dns_message,
            tips = listOf(R.string.error_page_tip_spelling, R.string.error_page_tip_connection, R.string.error_page_tip_secure_dns)
        )
        WebRequestError.ERROR_CONNECTION_REFUSED -> Spec(
            "ERROR_CONNECTION_REFUSED", Icon.GLOBE, R.string.error_page_connection_title, R.string.error_page_refused_message,
            tips = connectionTips
        )
        WebRequestError.ERROR_NET_RESET -> Spec(
            "ERROR_NET_RESET", Icon.GLOBE, R.string.error_page_connection_title, R.string.error_page_reset_message,
            tips = connectionTips
        )
        WebRequestError.ERROR_NET_INTERRUPT -> Spec(
            "ERROR_NET_INTERRUPT", Icon.GLOBE, R.string.error_page_connection_title, R.string.error_page_interrupted_message,
            tips = connectionTips
        )
        WebRequestError.ERROR_UNKNOWN_SOCKET_TYPE -> Spec(
            "ERROR_UNKNOWN_SOCKET_TYPE", Icon.GLOBE, R.string.error_page_connection_title, R.string.error_page_message,
            tips = connectionTips
        )
        WebRequestError.ERROR_NET_TIMEOUT -> Spec(
            "ERROR_NET_TIMEOUT", Icon.CLOCK, R.string.error_page_timeout_title, R.string.error_page_timeout_message,
            tips = connectionTips
        )
        WebRequestError.ERROR_REDIRECT_LOOP -> Spec(
            "ERROR_REDIRECT_LOOP", Icon.LOOP, R.string.error_page_redirect_title, R.string.error_page_redirect_message,
            tips = listOf(R.string.error_page_tip_cookies)
        )
        WebRequestError.ERROR_PROXY_CONNECTION_REFUSED -> Spec(
            "ERROR_PROXY_CONNECTION_REFUSED", Icon.GLOBE, R.string.error_page_proxy_title, R.string.error_page_proxy_message,
            messageHasHost = false,
            tips = listOf(R.string.error_page_tip_proxy)
        )
        WebRequestError.ERROR_UNKNOWN_PROXY_HOST -> Spec(
            "ERROR_UNKNOWN_PROXY_HOST", Icon.GLOBE, R.string.error_page_proxy_title, R.string.error_page_proxy_message,
            messageHasHost = false,
            tips = listOf(R.string.error_page_tip_proxy)
        )
        WebRequestError.ERROR_SECURITY_BAD_CERT -> Spec(
            "ERROR_SECURITY_BAD_CERT", Icon.LOCK, R.string.error_page_cert_title,
            if (strictCertificates) R.string.error_page_cert_strict_message else R.string.error_page_cert_message,
            danger = true,
            proceed = !strictCertificates
        )
        WebRequestError.ERROR_SECURITY_SSL -> Spec(
            "ERROR_SECURITY_SSL", Icon.LOCK, R.string.error_page_ssl_title, R.string.error_page_ssl_message,
            danger = true
        )
        WebRequestError.ERROR_BAD_HSTS_CERT -> Spec(
            "ERROR_BAD_HSTS_CERT", Icon.LOCK, R.string.error_page_ssl_title, R.string.error_page_hsts_message,
            danger = true
        )
        WebRequestError.ERROR_HTTPS_ONLY -> Spec(
            "ERROR_HTTPS_ONLY", Icon.LOCK, R.string.error_page_https_only_title, R.string.error_page_https_only_message
        )
        WebRequestError.ERROR_SAFEBROWSING_MALWARE_URI -> Spec(
            "ERROR_SAFEBROWSING_MALWARE_URI", Icon.WARNING, R.string.error_page_malware_title, R.string.error_page_malware_message,
            danger = true
        )
        WebRequestError.ERROR_SAFEBROWSING_UNWANTED_URI -> Spec(
            "ERROR_SAFEBROWSING_UNWANTED_URI", Icon.WARNING, R.string.error_page_malware_title, R.string.error_page_unwanted_message,
            danger = true
        )
        WebRequestError.ERROR_SAFEBROWSING_HARMFUL_URI -> Spec(
            "ERROR_SAFEBROWSING_HARMFUL_URI", Icon.WARNING, R.string.error_page_malware_title, R.string.error_page_harmful_message,
            danger = true
        )
        WebRequestError.ERROR_SAFEBROWSING_PHISHING_URI -> Spec(
            "ERROR_SAFEBROWSING_PHISHING_URI", Icon.WARNING, R.string.error_page_phishing_title, R.string.error_page_phishing_message,
            danger = true
        )
        WebRequestError.ERROR_HARMFULADDON_URI -> Spec(
            "ERROR_HARMFULADDON_URI", Icon.WARNING, R.string.error_page_harmful_addon_title, R.string.error_page_harmful_addon_message,
            messageHasHost = false,
            danger = true
        )
        WebRequestError.ERROR_CORRUPTED_CONTENT -> Spec(
            "ERROR_CORRUPTED_CONTENT", Icon.ERROR, R.string.error_page_content_title, R.string.error_page_content_message
        )
        WebRequestError.ERROR_INVALID_CONTENT_ENCODING -> Spec(
            "ERROR_INVALID_CONTENT_ENCODING", Icon.ERROR, R.string.error_page_content_title, R.string.error_page_content_message
        )
        WebRequestError.ERROR_UNSAFE_CONTENT_TYPE -> Spec(
            "ERROR_UNSAFE_CONTENT_TYPE", Icon.ERROR, R.string.error_page_content_title, R.string.error_page_unsafe_type_message
        )
        WebRequestError.ERROR_CONTENT_CRASHED -> Spec(
            "ERROR_CONTENT_CRASHED", Icon.ERROR, R.string.error_page_crashed_title, R.string.error_page_crashed_message,
            messageHasHost = false
        )
        WebRequestError.ERROR_MALFORMED_URI -> Spec(
            "ERROR_MALFORMED_URI", Icon.LINK_OFF, R.string.error_page_invalid_address_title, R.string.error_page_invalid_address_message,
            messageHasHost = false
        )
        WebRequestError.ERROR_UNKNOWN_PROTOCOL -> Spec(
            "ERROR_UNKNOWN_PROTOCOL", Icon.LINK_OFF, R.string.error_page_protocol_title, R.string.error_page_protocol_message,
            messageHasHost = false
        )
        WebRequestError.ERROR_DATA_URI_TOO_LONG -> Spec(
            "ERROR_DATA_URI_TOO_LONG", Icon.LINK_OFF, R.string.error_page_too_long_title, R.string.error_page_too_long_message,
            messageHasHost = false
        )
        WebRequestError.ERROR_FILE_NOT_FOUND -> Spec(
            "ERROR_FILE_NOT_FOUND", Icon.FILE, R.string.error_page_file_title, R.string.error_page_file_message,
            messageHasHost = false
        )
        WebRequestError.ERROR_FILE_ACCESS_DENIED -> Spec(
            "ERROR_FILE_ACCESS_DENIED", Icon.FILE, R.string.error_page_file_denied_title, R.string.error_page_file_denied_message,
            messageHasHost = false
        )
        WebRequestError.ERROR_PORT_BLOCKED -> Spec(
            "ERROR_PORT_BLOCKED", Icon.WARNING, R.string.error_page_port_title, R.string.error_page_port_message,
            messageHasHost = false
        )
        WebRequestError.ERROR_LOCAL_NETWORK_ACCESS_DENIED -> Spec(
            "ERROR_LOCAL_NETWORK_ACCESS_DENIED", Icon.WARNING, R.string.error_page_local_network_title, R.string.error_page_local_network_message
        )
        else -> Spec(
            "ERROR_UNKNOWN", Icon.ERROR, R.string.error_page_title, R.string.error_page_message
        )
    }

    fun build(context: Context, uri: String, error: WebRequestError): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val spec = specFor(
            error,
            prefs.getBoolean(PrivacyPrefs.STRICT_CERTIFICATES, PrivacyPrefs.DEFAULT_STRICT_CERTIFICATES),
            error.code == WebRequestError.ERROR_UNKNOWN_HOST && isDeviceOffline(context)
        )
        val host = runCatching { Uri.parse(uri).host }.getOrNull().orEmpty().ifEmpty { uri }
        val title = context.getString(spec.title)
        val message = if (spec.messageHasHost) context.getString(spec.message, host) else context.getString(spec.message)
        val locale = LocaleHelper.resolveEffectiveLocale(context)
        val dir = if (TextUtils.getLayoutDirectionFromLocale(locale) == View.LAYOUT_DIRECTION_RTL) "rtl" else "ltr"

        val theme = themeStyle(context)

        val retry = "<a id=\"retry\" class=\"btn ${if (spec.danger) "tonal" else "primary"}\" href=\"${enc(uri)}\">${enc(context.getString(R.string.error_page_retry))}</a>"
        val back = "<button id=\"back\" class=\"btn ${if (spec.danger) "primary" else "tonal"}\" hidden>${enc(context.getString(R.string.error_page_back))}</button>"
        val actions = if (spec.danger) back + retry else retry + back
        val tips = if (spec.tips.isEmpty()) "" else {
            "<section class=\"tips\"><p>${enc(context.getString(R.string.error_page_tips_title))}</p><ul>" +
                spec.tips.joinToString("") { "<li>${enc(context.getString(it))}</li>" } +
                "</ul></section>"
        }
        val proceed = if (spec.proceed) {
            "<button id=\"proceed\" class=\"btn link\" hidden>${enc(context.getString(R.string.error_page_proceed))}</button>"
        } else ""

        val html = "<!doctype html><html lang=\"${enc(locale.toLanguageTag())}\" dir=\"$dir\"><head><meta charset=\"utf-8\">" +
            "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,viewport-fit=cover\">" +
            "<meta name=\"color-scheme\" content=\"${theme.colorScheme}\">" +
            "<title>${enc(title)}</title><style>${theme.palette}$STYLE${if (spec.game) LongShot.style(context) else ""}</style></head><body><main>" +
            "<div class=\"icon${if (spec.danger) " danger" else ""}\"><svg viewBox=\"0 0 24 24\" aria-hidden=\"true\"><path d=\"${spec.icon.path}\"/></svg></div>" +
            "<h1>${enc(title)}</h1>" +
            "<p class=\"msg\">${enc(message)}</p>" +
            (if (spec.game) LongShot.markup(context) else "") +
            tips +
            "<div class=\"actions\">$actions</div>" +
            proceed +
            "<details><summary>${enc(context.getString(R.string.error_page_details))}</summary>" +
            "<p class=\"code\">${enc(context.getString(R.string.error_page_error_code, spec.code))}</p>" +
            "<p class=\"url\">${enc(uri)}</p></details>" +
            "</main><script>${script(uri)}</script>" +
            (if (spec.game) "<script>${LongShot.script(context, false)}</script>" else "") +
            "</body></html>"
        return ClintWebView.ERROR_PAGE_PREFIX + Base64.encodeToString(html.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    fun themeStyle(context: Context): ThemeStyle {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val theme = prefs.getString("app_theme", ThemeMode.SYSTEM) ?: ThemeMode.SYSTEM
        val accent = prefs.getString("accent_color", "material_you") ?: "material_you"
        val intensity = prefs.getString("surface_intensity", "soft_tint") ?: "soft_tint"
        val palette = when (theme) {
            ThemeMode.LIGHT -> ":root{${cssVars(resolveClintColors(context, true, accent, intensity))}}"
            ThemeMode.DARK -> ":root{${cssVars(resolveClintColors(context, false, accent, intensity))}}"
            else -> ":root{${cssVars(resolveClintColors(context, true, accent, intensity))}}" +
                "@media (prefers-color-scheme:dark){:root{${cssVars(resolveClintColors(context, false, accent, intensity))}}}"
        }
        val colorScheme = when (theme) {
            ThemeMode.LIGHT -> "light"
            ThemeMode.DARK -> "dark"
            else -> "light dark"
        }
        return ThemeStyle(palette, colorScheme)
    }

    private fun enc(value: String): String = TextUtils.htmlEncode(value)

    private fun css(color: Color): String {
        val argb = color.toArgb()
        val alpha = ((argb ushr 24) and 0xFF) / 255f
        return "rgba(${(argb shr 16) and 0xFF},${(argb shr 8) and 0xFF},${argb and 0xFF},${"%.3f".format(java.util.Locale.US, alpha)})"
    }

    private fun cssVars(colors: ClintColors): String =
        "--bg:${css(colors.background)};" +
            "--fg:${css(colors.onSurface)};" +
            "--fg2:${css(colors.secondaryText)};" +
            "--primary:${css(colors.primary)};" +
            "--on-primary:${css(colors.onPrimary)};" +
            "--tonal:${css(colors.surfaceVariant)};" +
            "--divider:${css(colors.divider)};" +
            "--error:${css(colors.colorError)};" +
            "--error-bg:${css(colors.colorErrorContainer)};" +
            "--on-error-bg:${css(colors.colorOnErrorContainer)};" +
            "--field:${css(colors.cardBackground)};" +
            "--gold:${if (colors.isLight) "#b7791f" else "#f0b84a"};"

    private fun script(uri: String): String {
        val target = JSONObject.quote(uri).replace("<", "\\u003c")
        return "var u=$target;" +
            "document.getElementById('retry').onclick=function(e){e.preventDefault();location.replace(u)};" +
            "var b=document.getElementById('back');" +
            "if(history.length>1){b.hidden=false;b.onclick=function(){history.back()}}" +
            "var p=document.getElementById('proceed');" +
            "if(p){p.hidden=false;p.onclick=function(){document.addCertException(false).then(function(){location.replace(u)})}}"
    }

    const val STYLE =
        "*{box-sizing:border-box}" +
            "html{-webkit-text-size-adjust:100%;text-size-adjust:100%}" +
            "html,body{margin:0;background:var(--bg);color:var(--fg)}" +
            "body{min-height:100vh;font-family:system-ui,Roboto,\"Noto Sans\",sans-serif;font-size:1rem;line-height:1.5;" +
            "padding:max(24px,env(safe-area-inset-top)) max(20px,env(safe-area-inset-right)) max(24px,env(safe-area-inset-bottom)) max(20px,env(safe-area-inset-left))}" +
            "main{width:100%;max-width:560px;margin:0 auto;padding-top:min(12vh,96px)}" +
            ".icon{width:72px;height:72px;border-radius:24px;background:var(--tonal);display:flex;align-items:center;justify-content:center;margin-bottom:24px}" +
            ".icon svg{width:40px;height:40px;fill:var(--fg2)}" +
            ".icon.danger{background:var(--error-bg)}" +
            ".icon.danger svg{fill:var(--on-error-bg)}" +
            "h1{font-size:1.5rem;line-height:1.3;font-weight:600;margin:0 0 12px;overflow-wrap:anywhere}" +
            ".msg{margin:0 0 20px;color:var(--fg2);overflow-wrap:anywhere}" +
            ".tips{margin:0 0 24px;color:var(--fg2)}" +
            ".tips p{margin:0 0 4px;color:var(--fg);font-weight:500}" +
            ".tips ul{margin:0;padding-inline-start:20px}" +
            ".tips li{margin:2px 0}" +
            ".actions{display:flex;flex-wrap:wrap;gap:12px;margin-bottom:12px}" +
            ".btn{font:inherit;font-weight:500;min-height:48px;padding:0 24px;border:0;border-radius:24px;cursor:pointer;-webkit-tap-highlight-color:transparent;display:inline-flex;align-items:center;justify-content:center;text-decoration:none}" +
            ".btn[hidden]{display:none}" +
            ".primary{background:var(--primary);color:var(--on-primary)}" +
            ".tonal{background:var(--tonal);color:var(--fg)}" +
            ".link{background:transparent;color:var(--error);padding:0 4px;margin-bottom:12px;justify-content:flex-start;text-align:start}" +
            ".btn:active{opacity:.85}" +
            ".btn:focus-visible,summary:focus-visible{outline:2px solid var(--primary);outline-offset:2px}" +
            "details{margin-top:12px;border-top:1px solid var(--divider);padding-top:8px;color:var(--fg2);font-size:.875rem}" +
            "summary{cursor:pointer;padding:12px 0;color:var(--fg);font-size:1rem}" +
            ".code{font-family:monospace;margin:4px 0}" +
            ".url{margin:4px 0;overflow-wrap:anywhere}" +
            "@media (max-width:420px){.actions .btn{flex:1 1 100%}}" +
            "@media (min-width:840px){main{max-width:640px;padding-top:min(16vh,160px)}h1{font-size:2rem}.icon{width:88px;height:88px;border-radius:28px}.icon svg{width:48px;height:48px}}" +
            "@media (max-height:480px){main{padding-top:0}.icon{width:56px;height:56px;border-radius:18px;margin-bottom:16px}.icon svg{width:32px;height:32px}}"
}
