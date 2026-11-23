package com.jhaiian.clint.browser.delegates
import com.jhaiian.clint.browser.webview.*
import com.jhaiian.clint.browser.sheets.*
import com.jhaiian.clint.browser.MainActivity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.jhaiian.clint.browser.webview.ClintWebView
import android.widget.Toast
import com.jhaiian.clint.R

private class LongPressHit {
    var gecko: ContextMenuInfo? = null
    var geckoSeen = false
    var scriptSeen = false
    var image = ""
    var alt = ""
    var link = ""
}

private val longPressHits = java.util.WeakHashMap<ClintWebView, LongPressHit>()
private var lastLongPressResolvedAt = 0L

class ImageHitBridge(activity: MainActivity, webView: ClintWebView) {
    private val activityRef = java.lang.ref.WeakReference(activity)
    private val webViewRef = java.lang.ref.WeakReference(webView)

    @android.webkit.JavascriptInterface
    fun report(image: String?, alt: String?, link: String?) {
        ClintGecko.main.post {
            val activity = activityRef.get() ?: return@post
            val webView = webViewRef.get() ?: return@post
            activity.collectLongPress(webView) {
                it.scriptSeen = true
                it.image = image.orEmpty()
                it.alt = alt.orEmpty()
                it.link = link.orEmpty()
            }
        }
    }
}

internal fun MainActivity.setupLinkLongPress(webView: ClintWebView) {
    webView.addJavascriptInterface(ImageHitBridge(this, webView), "ClintImageHit")
    webView.contextMenuListener = { info ->
        collectLongPress(webView) {
            it.gecko = info
            it.geckoSeen = true
        }
    }
}

private fun MainActivity.collectLongPress(webView: ClintWebView, update: (LongPressHit) -> Unit) {
    var hit = longPressHits[webView]
    if (hit == null) {
        if (System.currentTimeMillis() - lastLongPressResolvedAt < 600) return
        val created = LongPressHit()
        longPressHits[webView] = created
        ClintGecko.main.postDelayed({
            if (longPressHits[webView] === created) {
                longPressHits.remove(webView)
                resolveLongPress(webView, created)
            }
        }, 350)
        hit = created
    }
    update(hit)
    if (hit.geckoSeen && hit.scriptSeen) {
        longPressHits.remove(webView)
        resolveLongPress(webView, hit)
    }
}

private fun MainActivity.resolveLongPress(webView: ClintWebView, hit: LongPressHit) {
    lastLongPressResolvedAt = System.currentTimeMillis()
    if (tabManager.activeTab?.webView !== webView) return
    if (uiState.linkLongPressRequest != null || uiState.imageLongPressRequest != null) return
    val info = hit.gecko
    val geckoImage = if (info?.type == ContextMenuInfo.TYPE_IMAGE) info.srcUrl.orEmpty() else ""
    val image = hit.image.ifEmpty { geckoImage }
    val link = info?.linkUrl.orEmpty().ifEmpty { if (image.isNotEmpty()) hit.link else "" }
    val alt = (info?.altText ?: info?.title).orEmpty().trim().ifEmpty { hit.alt.trim() }
    when {
        link.isNotEmpty() -> showTrackedLinkLongPressSheet(webView, link, image, alt)
        image.isNotEmpty() -> showImageLongPressFor(webView, image, alt)
    }
}

private fun MainActivity.showImageLongPressFor(webView: ClintWebView, imageUrl: String, knownAlt: String) {
    val currentPageUrl = webView.url ?: ""
    val isStandalone = isStandaloneImagePage(currentPageUrl)
    if (knownAlt.isNotEmpty() || imageUrl.startsWith("data:")) {
        showImageLongPressSheet(imageUrl, knownAlt, isStandalone, currentPageUrl)
        return
    }
    val escapedUrl = imageUrl.replace("\\", "\\\\").replace("'", "\\'")
    val js = loadJsAsset("image_alt_text.js").replace("%URL%", escapedUrl)
    webView.evaluateJavascript(js) { raw ->
        val altText = raw?.removeSurrounding("\"")?.trim() ?: ""
        showImageLongPressSheet(imageUrl, altText, isStandalone, currentPageUrl)
    }
}

private fun MainActivity.showTrackedLinkLongPressSheet(webView: ClintWebView, linkUrl: String, imageUrl: String = "", imageAlt: String = "") {
    webView.queryLinkText { raw ->
        val result = raw?.let { runCatching { org.json.JSONObject(it) }.getOrNull() }
        val current = result?.optString("c").orEmpty()
        val original = result?.optString("h").orEmpty()
        val sameLink = current.isNotEmpty() && (current == linkUrl || original == linkUrl)
        val linkText = if (sameLink) result?.optString("t").orEmpty().trim().replace(Regex("\\s+"), " ") else ""
        val resolvedUrl = if (sameLink && original != linkUrl && (original.startsWith("https://") || original.startsWith("http://"))) original else linkUrl
        showLinkLongPressSheet(resolvedUrl, linkText, imageUrl, imageAlt.ifEmpty { linkText })
    }
}

internal fun MainActivity.showLinkLongPressSheet(url: String, linkText: String, imageUrl: String = "", imageAlt: String = "") {
    if (uiState.linkLongPressRequest != null) return
    uiState.linkLongPressRequest = LinkLongPressRequest(url, linkText, imageUrl, imageAlt)
}

internal fun MainActivity.handleLinkOpenInNewTab(url: String) {
    val sourceTab = tabManager.activeTab
    if (sourceTab?.isIncognito == true) {
        openNewTab(isIncognito = true, url = url, groupId = sourceTab.groupId)
        return
    }
    openNewTab(isIncognito = false, url = url, profileId = activeProfileId(), groupId = sourceTab?.groupId)
}

internal fun MainActivity.handleLinkOpenIncognito(url: String) {
    openNewTab(isIncognito = true, url = url)
}

internal fun MainActivity.handleLinkOpenNewTabBackground(url: String) {
    openNewTabInBackground(url)
}

internal fun MainActivity.handleLinkPreviewPage(url: String) {
    if (uiState.contentPreviewRequest != null) return
    uiState.contentPreviewRequest = ContentPreviewRequest.forPage(url, isDesktopMode)
}

internal fun MainActivity.handleLinkCopyAddress(url: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.link_copy_address), url))
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(this, getString(R.string.link_address_copied), Toast.LENGTH_SHORT).show()
    }
}

internal fun MainActivity.copyCurrentPageLink(fallback: String) {
    val url = tabManager.activeTab?.webView?.url
        ?.takeUnless { it.isBlank() || com.jhaiian.clint.browser.home.isClintHomeUrl(it) }
        ?: fallback.takeIf { it.isNotBlank() }
        ?: return
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.address_bar_copy_link), url))
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(this, getString(R.string.address_bar_link_copied), Toast.LENGTH_SHORT).show()
    }
}

internal fun MainActivity.handleLinkCopyText(text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.link_copy_text), text))
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(this, getString(R.string.link_text_copied), Toast.LENGTH_SHORT).show()
    }
}

internal fun MainActivity.handleLinkShare(url: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, url)
    }
    startActivity(Intent.createChooser(intent, getString(R.string.link_share)))
}
