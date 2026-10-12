package com.jhaiian.clint.browser.suggestions

import android.os.Handler
import android.os.Looper
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import java.io.IOException
import java.util.concurrent.TimeUnit

internal class SuggestionFetcher {

    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    private val handler = Handler(Looper.getMainLooper())
    private var pendingCall: Call? = null

    fun fetch(query: String, provider: String = "duckduckgo", customUrl: String? = null, onResult: (List<String>) -> Unit) {
        pendingCall?.cancel()
        pendingCall = null

        if (query.isBlank()) {
            onResult(emptyList())
            return
        }

        val encodedQuery = android.net.Uri.encode(query)
        val url = when {
            !customUrl.isNullOrBlank() -> customUrl
            provider == "google" -> "https://www.google.com/complete/search?client=firefox&q=$encodedQuery"
            else -> "https://duckduckgo.com/ac/?q=$encodedQuery&type=list"
        }
        val request = Request.Builder()
            .url(url)
            .build()
        val call = client.newCall(request)
        pendingCall = call
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (call != pendingCall) return
                handler.post { onResult(emptyList()) }
            }

            override fun onResponse(call: Call, response: Response) {
                if (call != pendingCall) return
                val results = mutableListOf<String>()
                try {
                    val body = response.body.string()
                    val root = JSONArray(body)
                    if (root.length() >= 2) {
                        val suggestions = root.getJSONArray(1)
                        val limit = minOf(suggestions.length(), MAX_SUGGESTIONS)
                        for (i in 0 until limit) {
                            results.add(suggestions.getString(i))
                        }
                    }
                } catch (_: Exception) {}
                handler.post { onResult(results) }
            }
        })
    }

    fun cancel() {
        pendingCall?.cancel()
        pendingCall = null
    }

    companion object {
        private const val MAX_SUGGESTIONS = 10
    }
}
