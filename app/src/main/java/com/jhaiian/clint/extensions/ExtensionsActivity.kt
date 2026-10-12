package com.jhaiian.clint.extensions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.preference.PreferenceManager
import com.jhaiian.clint.base.ClintActivity
import com.jhaiian.clint.browser.webview.ClintGecko
import com.jhaiian.clint.ui.rememberMaxContentWidth
import com.jhaiian.clint.ui.theme.ClintComposeTheme

class ExtensionsActivity : ClintActivity() {

    private val pickFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) ClintExtensions.installFromFile(this, uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ClintGecko.init(this)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val theme = prefs.getString("app_theme", "system") ?: "system"
        val hideStatusBar = prefs.getBoolean("hide_status_bar", false)
        val hideSystemNavigation = prefs.getBoolean("hide_system_navigation", false)
        val initialId = intent.getStringExtra(EXTRA_OPEN_ID)

        setContent {
            ClintComposeTheme(theme = theme) {
                ExtensionsRoot(
                    activity = this@ExtensionsActivity,
                    initialId = initialId,
                    hideStatusBar = hideStatusBar,
                    hideSystemNavigation = hideSystemNavigation,
                    onPickFile = { pickFile.launch(arrayOf("*/*")) },
                    onGetAddons = {
                        ClintExtensions.openInTab(ClintExtensions.STORE_URL)
                        finish()
                    },
                    onExit = { finish() }
                )
                ExtensionHost()
            }
        }
    }

    companion object {
        const val EXTRA_OPEN_ID = "extension_id"

        fun launch(context: Context, extensionId: String? = null) {
            val intent = Intent(context, ExtensionsActivity::class.java)
            if (extensionId != null) intent.putExtra(EXTRA_OPEN_ID, extensionId)
            if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}

@Composable
private fun ExtensionsRoot(
    activity: Activity,
    initialId: String?,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onPickFile: () -> Unit,
    onGetAddons: () -> Unit,
    onExit: () -> Unit
) {
    var selectedId by rememberSaveable { mutableStateOf(initialId) }
    var openedDirectly by rememberSaveable { mutableStateOf(initialId != null) }
    val goBack = {
        if (openedDirectly) {
            onExit()
        } else {
            selectedId = null
        }
    }
    val showList = {
        openedDirectly = false
        selectedId = null
    }
    val maxContentWidth = rememberMaxContentWidth(activity)
    val selected = ClintExtensions.extensions.firstOrNull { it.id == selectedId }

    BackHandler(enabled = selectedId != null) { goBack() }
    LaunchedEffect(selected, ClintExtensions.loaded) {
        if (selectedId != null && selected == null && ClintExtensions.loaded) showList()
    }

    Crossfade(targetState = if (selected != null) selectedId else null, label = "extensions") { id ->
        val info = ClintExtensions.extensions.firstOrNull { it.id == id }
        if (id != null && info != null) {
            ExtensionDetailScreen(
                info = info,
                maxContentWidth = maxContentWidth,
                hideStatusBar = hideStatusBar,
                hideSystemNavigation = hideSystemNavigation,
                onBack = { goBack() },
                onRemoved = { showList() }
            )
        } else {
            ExtensionsScreen(
                hideStatusBar = hideStatusBar,
                hideSystemNavigation = hideSystemNavigation,
                onExit = onExit,
                onOpenDetail = { openedDirectly = false; selectedId = it },
                onPickFile = onPickFile,
                onGetAddons = onGetAddons
            )
        }
    }
}
