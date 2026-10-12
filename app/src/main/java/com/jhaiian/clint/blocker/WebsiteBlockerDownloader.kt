package com.jhaiian.clint.blocker

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

object WebsiteBlockerDownloader {
    private val client = OkHttpClient()

    sealed class Result {
        data class Success(val etag: String?, val lastModified: String?, val bytesWritten: Long) : Result()
        object NotModified : Result()
        data class Failure(val message: String) : Result()
    }

    suspend fun download(
        url: String,
        destination: File,
        etag: String?,
        lastModified: String?,
        onProgress: (bytesRead: Long, contentLength: Long) -> Unit
    ): Result = withContext(Dispatchers.IO) {
        try {
            val requestBuilder = Request.Builder().url(url)
            if (!etag.isNullOrBlank()) requestBuilder.header("If-None-Match", etag)
            if (!lastModified.isNullOrBlank()) requestBuilder.header("If-Modified-Since", lastModified)

            client.newCall(requestBuilder.build()).execute().use { response ->
                if (response.code == 304) return@withContext Result.NotModified
                if (!response.isSuccessful) return@withContext Result.Failure("HTTP ${response.code}")

                val body = response.body
                val contentLength = body.contentLength()
                val tempFile = File(destination.parentFile, "${destination.name}.tmp")

                var bytesRead = 0L
                body.byteStream().use { input ->
                    tempFile.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            bytesRead += read
                            onProgress(bytesRead, contentLength)
                        }
                    }
                }

                if (destination.exists()) destination.delete()
                if (!tempFile.renameTo(destination)) {
                    return@withContext Result.Failure("Failed to save file")
                }

                Result.Success(
                    etag = response.header("ETag"),
                    lastModified = response.header("Last-Modified"),
                    bytesWritten = bytesRead
                )
            }
        } catch (e: Exception) {
            Result.Failure(e.message ?: "Download failed")
        }
    }
}
