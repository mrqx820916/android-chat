package com.chat.lightweight.update

import java.io.File

data class AppUpdateManifest(
    val channel: String,
    val packageName: String,
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val sha256: String,
    val sizeBytes: Long,
    val releasedAt: String,
    val releaseNotes: String,
    val force: Boolean
)

sealed class UpdateCheckResult {
    data class UpdateAvailable(
        val manifest: AppUpdateManifest,
        val sameVersionHotfix: Boolean = false
    ) : UpdateCheckResult()

    data class NoUpdate(
        val reason: String
    ) : UpdateCheckResult()

    data class Skipped(
        val manifest: AppUpdateManifest,
        val reason: String
    ) : UpdateCheckResult()

    data class Failure(
        val message: String,
        val cause: Throwable? = null
    ) : UpdateCheckResult()
}

sealed class UpdateDownloadResult {
    data class Success(
        val file: File,
        val reusedCachedFile: Boolean
    ) : UpdateDownloadResult()

    data class Failure(
        val message: String,
        val cause: Throwable? = null
    ) : UpdateDownloadResult()
}

data class UpdateInstallResult(
    val success: Boolean,
    val detail: String,
    val launchedInteractiveInstaller: Boolean = false
)

data class UpdateFailureRecord(
    val versionCode: Int,
    val failedAtMs: Long,
    val message: String?
)
