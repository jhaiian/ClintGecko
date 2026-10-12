package com.jhaiian.clint.blocker.engine

data class WebsiteBlockerCompileProgress(
    val currentLabel: String,
    val itemsProcessed: Int,
    val totalItems: Int
)

data class WebsiteBlockerCompileResult(
    val success: Boolean,
    val domainCount: Long,
    val sizeBytes: Long,
    val error: String? = null
)
