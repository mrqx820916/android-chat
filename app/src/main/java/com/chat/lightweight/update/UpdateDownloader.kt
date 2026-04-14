package com.chat.lightweight.update

import java.io.File
import java.security.MessageDigest
import java.util.Locale

internal class UpdateDownloader(
    private val transport: UpdateHttpTransport = UrlConnectionUpdateTransport()
) {

    suspend fun download(
        manifest: AppUpdateManifest,
        cacheDir: File,
        channel: String
    ): UpdateDownloadResult {
        val updatesDir = File(cacheDir, "${UpdateConfig.updatesDirName}/$channel")
        if (!updatesDir.exists() && !updatesDir.mkdirs()) {
            return UpdateDownloadResult.Failure("创建更新目录失败: ${updatesDir.absolutePath}")
        }

        val targetFile = File(updatesDir, "${manifest.versionCode}.apk")
        if (targetFile.exists()) {
            when (val cachedResult = verifyFile(targetFile, manifest)) {
                is UpdateDownloadResult.Success -> return cachedResult.copy(reusedCachedFile = true)
                is UpdateDownloadResult.Failure -> targetFile.delete()
            }
        }

        val tempFile = File(targetFile.absolutePath + ".part")
        tempFile.delete()

        return try {
            transport.downloadToFile(manifest.apkUrl, UpdateConfig.downloadTimeoutMs, tempFile)
            if (targetFile.exists()) {
                targetFile.delete()
            }
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }
            verifyFile(targetFile, manifest)
        } catch (throwable: Throwable) {
            tempFile.delete()
            UpdateDownloadResult.Failure(
                message = throwable.message ?: "下载更新 APK 失败",
                cause = throwable
            )
        }
    }

    internal fun verifyFile(file: File, manifest: AppUpdateManifest): UpdateDownloadResult {
        if (!file.exists()) {
            return UpdateDownloadResult.Failure("更新文件不存在: ${file.absolutePath}")
        }
        if (file.length() != manifest.sizeBytes) {
            file.delete()
            return UpdateDownloadResult.Failure("APK 大小校验失败")
        }
        val sha256 = computeSha256(file)
        if (!sha256.equals(manifest.sha256, ignoreCase = true)) {
            file.delete()
            return UpdateDownloadResult.Failure("APK SHA256 校验失败")
        }
        return UpdateDownloadResult.Success(
            file = file,
            reusedCachedFile = false
        )
    }

    internal fun computeSha256(file: File): String {
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
