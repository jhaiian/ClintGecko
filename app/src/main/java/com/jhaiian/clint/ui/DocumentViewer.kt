package com.jhaiian.clint.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Handler
import android.os.Looper
import androidx.preference.PreferenceManager
import com.jhaiian.clint.ui.theme.ClintComposeTheme
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.Executors

object DocumentViewer {

    private val client = OkHttpClient()
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    const val PRIVACY_POLICY_URL =
        "https://raw.githubusercontent.com/jhaiian/ClintGecko/main/PRIVACY_POLICY.md"
    const val TERMS_URL =
        "https://raw.githubusercontent.com/jhaiian/ClintGecko/main/TERMS_OF_SERVICE.md"
    const val CHANGELOG_URL =
        "https://raw.githubusercontent.com/jhaiian/ClintGecko/main/CHANGELOG.md"
    const val ATTRIBUTION_URL =
        "https://raw.githubusercontent.com/jhaiian/ClintGecko/main/Attribution.md"
    const val SUPPORTERS_URL =
        "https://raw.githubusercontent.com/jhaiian/ClintGecko/main/Supporters.md"

    private fun Context.findActivity(): Activity? {
        var ctx = this
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    fun show(context: Context, title: String, url: String) {
        val host = context.findActivity() as? OverlayHostActivity ?: return
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val theme = prefs.getString("app_theme", "system") ?: "system"
        val hideStatusBar = prefs.getBoolean("hide_status_bar", false)
        val hideSystemNavigation = prefs.getBoolean("hide_system_navigation", false)
        val state = DocumentViewerUiState()

        host.overlayContent = {
            ClintComposeTheme(theme = theme) {
                DocumentViewerDialog(
                    title = title,
                    state = state,
                    hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                    onDismiss = { host.overlayContent = null }
                )
            }
        }

        executor.submit {
            try {
                val request = Request.Builder().url(url).build()
                val markdown = client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
                    response.body.string()
                }
                mainHandler.post {
                    state.markdown = markdown
                    state.isLoading = false
                }
            } catch (_: Exception) {
                mainHandler.post {
                    state.isError = true
                    state.isLoading = false
                }
            }
        }
    }
}
