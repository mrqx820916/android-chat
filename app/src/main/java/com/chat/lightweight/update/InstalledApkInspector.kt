package com.chat.lightweight.update

import android.content.Context
import java.io.File
import java.security.MessageDigest
import java.util.Locale

internal class InstalledApkInspector(
    private val context: Context
) {
    fun getInstalledApkSha256(): String {
        return computeSha256(File(context.applicationInfo.sourceDir))
    }

    internal fun computeSha256(file: File): String {
        require(file.exists()) { "当前安装包不存在: ${file.absolutePath}" }
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString(separator = "") { byte ->
            String.format(Locale.US, "%02x", byte)
        }
    }
}
