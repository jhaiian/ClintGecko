package com.jhaiian.clint.privacy

import android.net.Uri

object TrackingParams {

    private val exactNames = setOf(
        "fbclid", "gclid", "gclsrc", "dclid", "gbraid", "wbraid", "msclkid", "yclid", "twclid", "ttclid",
        "li_fat_id", "igshid", "mc_eid", "mc_cid", "_ga", "_gl", "_hsenc", "_hsmi", "hsctatracking",
        "mkt_tok", "vero_id", "oly_enc_id", "oly_anon_id", "srsltid", "ref_src", "ref_url", "wickedid",
        "rb_clickid", "s_cid", "pk_campaign", "pk_kwd", "pk_source", "pk_medium"
    )

    fun strip(url: String): String {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return url
        if (!uri.isHierarchical) return url
        val query = uri.encodedQuery ?: return url
        val kept = ArrayList<String>()
        var removed = false
        for (part in query.split('&')) {
            if (part.isEmpty()) continue
            val name = Uri.decode(part.substringBefore('=')).lowercase()
            if (name in exactNames || name.startsWith("utm_")) removed = true else kept.add(part)
        }
        if (!removed) return url
        val builder = uri.buildUpon().clearQuery()
        if (kept.isNotEmpty()) builder.encodedQuery(kept.joinToString("&"))
        return builder.build().toString()
    }
}
