package com.jhaiian.clint.extensions

import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.WebExtension

sealed class ExtensionPrompt(
    val extension: WebExtension,
    val permissions: List<String>,
    val origins: List<String>,
    val dataCollection: List<String>
) {
    val name: String get() = extension.metaData.name?.takeIf { it.isNotBlank() } ?: extension.id

    class Install(
        extension: WebExtension,
        permissions: List<String>,
        origins: List<String>,
        dataCollection: List<String>,
        val result: GeckoResult<WebExtension.PermissionPromptResponse>
    ) : ExtensionPrompt(extension, permissions, origins, dataCollection)

    class Update(
        extension: WebExtension,
        permissions: List<String>,
        origins: List<String>,
        dataCollection: List<String>,
        val result: GeckoResult<AllowOrDeny>
    ) : ExtensionPrompt(extension, permissions, origins, dataCollection)

    class Optional(
        extension: WebExtension,
        permissions: List<String>,
        origins: List<String>,
        dataCollection: List<String>,
        val result: GeckoResult<AllowOrDeny>
    ) : ExtensionPrompt(extension, permissions, origins, dataCollection)
}

class ExtensionPopup(
    val extensionId: String,
    val title: String,
    val session: GeckoSession,
    val fullScreen: Boolean
)
