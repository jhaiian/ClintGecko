package com.jhaiian.clint.customtab

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.base.ClintActivity
import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.browser.webview.ClintGecko
import com.jhaiian.clint.browser.webview.ClintWebChromeClient
import com.jhaiian.clint.browser.webview.ClintWebView
import com.jhaiian.clint.browser.webview.ClintWebViewClient
import com.jhaiian.clint.ui.listscreen.ListMenuItem
import com.jhaiian.clint.ui.listscreen.PopupShape
import com.jhaiian.clint.ui.theme.ClintComposeTheme
import com.jhaiian.clint.ui.theme.LocalClintColors

class CustomTabActivity : ClintActivity() {

    private var webView: ClintWebView? = null
    private var pageTitle by mutableStateOf("")
    private var pageUrl by mutableStateOf("")
    private var pageProgress by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val startUrl = intent.data?.toString()
        if (startUrl == null) {
            finish()
            return
        }
        pageUrl = startUrl
        ClintGecko.init(this)
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val theme = prefs.getString("app_theme", "system") ?: "system"

        val view = ClintWebView(this)
        view.settings.javaScriptEnabled = prefs.getBoolean("javascript_enabled", true)
        view.settings.userAgentString = ClintGecko.defaultUserAgent()
        view.webViewClient = ClintWebViewClient(
            prefs = prefs,
            onTabUrlUpdatedCallback = { _, url -> pageUrl = url },
            getTabId = { "custom_tab" }
        )
        view.webChromeClient = ClintWebChromeClient(
            onTitleChanged = { pageTitle = it },
            onProgressChanged = { pageProgress = it },
            onUrlChanged = { if (it.isNotEmpty()) pageUrl = it },
            onNewWindowRequest = { view.loadUrl(it) }
        )
        webView = view
        view.loadUrl(startUrl)

        setContent {
            ClintComposeTheme(theme = theme) {
                val colors = LocalClintColors.current
                BackHandler { if (view.canGoBack()) view.goBack() else finish() }
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(colors.surface)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                ) {
                    CustomTabBar(
                        title = pageTitle,
                        url = pageUrl,
                        onClose = { finish() },
                        onShare = { shareCurrent() },
                        onCopy = { copyCurrent() },
                        onOpenInClint = { openInClint() }
                    )
                    if (pageProgress in 1..99) {
                        LinearProgressIndicator(
                            progress = { pageProgress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = colors.primary,
                            trackColor = colors.divider
                        )
                    }
                    AndroidView(
                        factory = { view },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }
        }
    }

    private fun currentUrl(): String = pageUrl.ifEmpty { webView?.url ?: intent.data?.toString().orEmpty() }

    private fun shareCurrent() {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, currentUrl())
        runCatching { startActivity(Intent.createChooser(send, null)) }
    }

    private fun copyCurrent() {
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("", currentUrl()))
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(this, R.string.download_menu_link_copied, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openInClint() {
        val open = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl()))
            .setClass(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(open) }
        finish()
    }

    override fun onResume() {
        super.onResume()
        webView?.onResume()
    }

    override fun onPause() {
        webView?.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        webView?.destroy()
        webView = null
        super.onDestroy()
    }
}

@Composable
private fun CustomTabBar(
    title: String,
    url: String,
    onClose: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onOpenInClint: () -> Unit
) {
    val colors = LocalClintColors.current
    var menuOpen by remember { mutableStateOf(false) }
    val host = remember(url) { runCatching { Uri.parse(url).host }.getOrNull().orEmpty() }
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_close), tint = colors.iconTint)
        }
        Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
            Text(
                text = title.ifEmpty { host },
                color = colors.onSurface,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = host,
                color = colors.secondaryText,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.shortcut_manager_more_options), tint = colors.iconTint)
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                shape = PopupShape,
                containerColor = colors.popupBackground,
                border = BorderStroke(1.dp, colors.popupStroke)
            ) {
                ListMenuItem(Icons.Filled.Share, stringResource(R.string.download_menu_share), checked = false) {
                    menuOpen = false; onShare()
                }
                ListMenuItem(Icons.Filled.ContentCopy, stringResource(R.string.downloads_sheet_copy_link), checked = false) {
                    menuOpen = false; onCopy()
                }
                ListMenuItem(Icons.AutoMirrored.Filled.OpenInNew, stringResource(R.string.custom_tab_open_in_clint), checked = false) {
                    menuOpen = false; onOpenInClint()
                }
            }
        }
    }
}
