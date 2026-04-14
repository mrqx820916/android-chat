package com.chat.lightweight.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream

internal interface UpdateHttpTransport {
    suspend fun fetchString(url: String, timeoutMs: Int): String
    suspend fun downloadToFile(url: String, timeoutMs: Int, destination: File)
}

internal class UrlConnectionUpdateTransport : UpdateHttpTransport {

    override suspend fun fetchString(url: String, timeoutMs: Int): String {
        return withContext(Dispatchers.IO) {
            val connection = openConnection(url, timeoutMs)
            try {
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("Accept-Encoding", "gzip")
                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw IOException("HTTP $responseCode: ${readError(connection)}")
                }
                readText(connection.inputStream, connection.contentEncoding)
            } finally {
                connection.disconnect()
            }
        }
    }

    override suspend fun downloadToFile(url: String, timeoutMs: Int, destination: File) {
        return withContext(Dispatchers.IO) {
            val connection = openConnection(url, timeoutMs)
            try {
                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw IOException("HTTP $responseCode: ${readError(connection)}")
                }
                destination.outputStream().use { output ->
                    connection.inputStream.use { input ->
                        input.copyTo(output)
                    }
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun openConnection(url: String, timeoutMs: Int): HttpURLConnection {
        return (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            useCaches = false
            requestMethod = "GET"
            setRequestProperty("User-Agent", "LightweightChat/1.0")
        }
    }

    private fun readError(connection: HttpURLConnection): String {
        val errorStream = connection.errorStream ?: return ""
        return runCatching {
            readText(errorStream, connection.contentEncoding)
        }.getOrDefault("")
    }

    private fun readText(inputStream: InputStream, contentEncoding: String?): String {
        val source = when {
            contentEncoding?.contains("gzip", ignoreCase = true) == true ->
                GZIPInputStream(BufferedInputStream(inputStream))
            else -> inputStream
        }
        return BufferedReader(InputStreamReader(source, StandardCharsets.UTF_8)).use { reader ->
            reader.readText()
        }
    }
}
