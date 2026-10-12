package com.jhaiian.clint.blocker

data class WebsiteBlockerCategory(
    val id: String,
    val downloadUrl: String,
    val isEnabled: Boolean = false,
    val isDownloaded: Boolean = false,
    val downloadedAt: Long = 0L,
    val domainCount: Long = 0L,
    val fileSizeBytes: Long = 0L,
    val etag: String? = null,
    val lastModified: String? = null
)

object WebsiteBlockerCategoryIds {
    const val ABUSE = "abuse"
    const val CRYPTO = "crypto"
    const val DRUGS = "drugs"
    const val FRAUD = "fraud"
    const val GAMBLING = "gambling"
    const val MALWARE = "malware"
    const val PHISHING = "phishing"
    const val PIRACY = "piracy"
    const val PORN = "porn"
    const val RANSOMWARE = "ransomware"
    const val REDIRECT = "redirect"
    const val SCAM = "scam"
    const val SOCIAL = "social"
    const val TORRENT = "torrent"
    const val TRACKING = "tracking"
}

object WebsiteBlockerDefaults {
    val CATEGORIES = listOf(
        WebsiteBlockerCategory(
            id = WebsiteBlockerCategoryIds.ABUSE,
            downloadUrl = "https://blocklistproject.github.io/Lists/alt-version/abuse-nl.txt"
        ),
        WebsiteBlockerCategory(
            id = WebsiteBlockerCategoryIds.CRYPTO,
            downloadUrl = "https://blocklistproject.github.io/Lists/alt-version/crypto-nl.txt"
        ),
        WebsiteBlockerCategory(
            id = WebsiteBlockerCategoryIds.DRUGS,
            downloadUrl = "https://blocklistproject.github.io/Lists/alt-version/drugs-nl.txt"
        ),
        WebsiteBlockerCategory(
            id = WebsiteBlockerCategoryIds.FRAUD,
            downloadUrl = "https://blocklistproject.github.io/Lists/alt-version/fraud-nl.txt"
        ),
        WebsiteBlockerCategory(
            id = WebsiteBlockerCategoryIds.GAMBLING,
            downloadUrl = "https://blocklistproject.github.io/Lists/alt-version/gambling-nl.txt"
        ),
        WebsiteBlockerCategory(
            id = WebsiteBlockerCategoryIds.MALWARE,
            downloadUrl = "https://blocklistproject.github.io/Lists/alt-version/malware-nl.txt"
        ),
        WebsiteBlockerCategory(
            id = WebsiteBlockerCategoryIds.PHISHING,
            downloadUrl = "https://blocklistproject.github.io/Lists/alt-version/phishing-nl.txt"
        ),
        WebsiteBlockerCategory(
            id = WebsiteBlockerCategoryIds.PIRACY,
            downloadUrl = "https://blocklistproject.github.io/Lists/alt-version/piracy-nl.txt"
        ),
        WebsiteBlockerCategory(
            id = WebsiteBlockerCategoryIds.PORN,
            downloadUrl = "https://blocklistproject.github.io/Lists/alt-version/porn-nl.txt"
        ),
        WebsiteBlockerCategory(
            id = WebsiteBlockerCategoryIds.RANSOMWARE,
            downloadUrl = "https://blocklistproject.github.io/Lists/alt-version/ransomware-nl.txt"
        ),
        WebsiteBlockerCategory(
            id = WebsiteBlockerCategoryIds.REDIRECT,
            downloadUrl = "https://blocklistproject.github.io/Lists/alt-version/redirect-nl.txt"
        ),
        WebsiteBlockerCategory(
            id = WebsiteBlockerCategoryIds.SCAM,
            downloadUrl = "https://blocklistproject.github.io/Lists/alt-version/scam-nl.txt"
        ),
        WebsiteBlockerCategory(
            id = WebsiteBlockerCategoryIds.SOCIAL,
            downloadUrl = "https://raw.githubusercontent.com/StevenBlack/hosts/master/alternates/social-only/hosts"
        ),
        WebsiteBlockerCategory(
            id = WebsiteBlockerCategoryIds.TORRENT,
            downloadUrl = "https://blocklistproject.github.io/Lists/alt-version/torrent-nl.txt"
        ),
        WebsiteBlockerCategory(
            id = WebsiteBlockerCategoryIds.TRACKING,
            downloadUrl = "https://blocklistproject.github.io/Lists/alt-version/tracking-nl.txt"
        )
    )
}
