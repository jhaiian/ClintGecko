package com.jhaiian.clint.blocker.additional

data class AdditionalWebsiteRule(
    val id: Long = 0,
    val host: String,
    val createdAt: Long = System.currentTimeMillis()
)
