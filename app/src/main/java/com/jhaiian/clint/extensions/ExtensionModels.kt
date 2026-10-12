package com.jhaiian.clint.extensions

import org.mozilla.geckoview.WebExtension

enum class ExtensionRisk { LOW, MEDIUM, HIGH }

enum class ExtensionStatus { ON, OFF, BLOCKED, SOFT_BLOCKED, UNSIGNED, INCOMPATIBLE }

class ExtensionInfo(
    val raw: WebExtension,
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val author: String?,
    val homepageUrl: String?,
    val amoUrl: String?,
    val optionsUrl: String?,
    val updateDate: String?,
    val enabled: Boolean,
    val allowedInPrivate: Boolean,
    val status: ExtensionStatus,
    val signedState: Int,
    val temporary: Boolean,
    val permissions: List<String>,
    val origins: List<String>,
    val dataCollection: List<String>,
    val optionalPermissions: List<String>,
    val grantedOptionalPermissions: List<String>,
    val optionalOrigins: List<String>,
    val grantedOptionalOrigins: List<String>
) {
    val risk: ExtensionRisk get() = ExtensionPermissions.risk(permissions, origins)

    val grantedPermissions: List<String> get() = permissions + grantedOptionalPermissions

    val grantedOrigins: List<String> get() = origins + grantedOptionalOrigins

    val canToggle: Boolean
        get() = status != ExtensionStatus.BLOCKED && status != ExtensionStatus.INCOMPATIBLE

    companion object {
        fun from(ext: WebExtension): ExtensionInfo {
            val meta = ext.metaData
            val flags = meta.disabledFlags
            val status = when {
                meta.blocklistState == WebExtension.BlocklistStateFlags.BLOCKED ||
                    flags and WebExtension.DisabledFlags.BLOCKLIST != 0 -> ExtensionStatus.BLOCKED
                flags and WebExtension.DisabledFlags.SOFT_BLOCKLIST != 0 -> ExtensionStatus.SOFT_BLOCKED
                flags and WebExtension.DisabledFlags.APP_VERSION != 0 -> ExtensionStatus.INCOMPATIBLE
                flags and WebExtension.DisabledFlags.SIGNATURE != 0 -> ExtensionStatus.UNSIGNED
                meta.enabled -> ExtensionStatus.ON
                else -> ExtensionStatus.OFF
            }
            fun arr(a: Array<String>?): List<String> = a?.toList().orEmpty()
            return ExtensionInfo(
                raw = ext,
                id = ext.id,
                name = meta.name?.takeIf { it.isNotBlank() } ?: ext.id,
                description = meta.description.orEmpty(),
                version = meta.version,
                author = meta.creatorName?.takeIf { it.isNotBlank() },
                homepageUrl = meta.homepageUrl?.takeIf { it.startsWith("http") },
                amoUrl = meta.amoListingUrl?.takeIf { it.startsWith("http") },
                optionsUrl = meta.optionsPageUrl?.takeIf { it.isNotBlank() },
                updateDate = meta.updateDate?.takeIf { it.isNotBlank() },
                enabled = meta.enabled,
                allowedInPrivate = meta.allowedInPrivateBrowsing,
                status = status,
                signedState = meta.signedState,
                temporary = meta.temporary,
                permissions = arr(meta.requiredPermissions),
                origins = arr(meta.requiredOrigins),
                dataCollection = arr(meta.requiredDataCollectionPermissions).filter { it != "none" },
                optionalPermissions = arr(meta.optionalPermissions),
                grantedOptionalPermissions = arr(meta.grantedOptionalPermissions),
                optionalOrigins = arr(meta.optionalOrigins),
                grantedOptionalOrigins = arr(meta.grantedOptionalOrigins)
            )
        }
    }
}
