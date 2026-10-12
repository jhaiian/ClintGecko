package com.jhaiian.clint.util

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

fun extractHost(input: String): String {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return trimmed
    val candidate = if (trimmed.contains("://")) trimmed else "https://$trimmed"
    candidate.toHttpUrlOrNull()?.host?.let { return it.lowercase() }
    return trimmed
        .substringAfter("://")
        .substringBefore('/')
        .substringBefore('?')
        .substringBefore('#')
        .substringAfterLast('@')
        .substringBefore(':')
        .lowercase()
}

fun registeredDomain(host: String): String {
    val cleanHost = extractHost(host)
    return "https://$cleanHost".toHttpUrlOrNull()?.topPrivateDomain() ?: cleanHost
}
