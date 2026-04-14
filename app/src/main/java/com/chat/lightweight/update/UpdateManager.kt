package com.chat.lightweight.update

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import timber.log.Timber
import java.io.File

object UpdateManager {

    private lateinit var storeInstance: UpdateStore
    private lateinit var inspector: InstalledApkInspector
    private lateinit var checker: UpdateChecker
    private lateinit var downloader: UpdateDownloader
    private lateinit var installer: UpdateInstaller

    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        val appContext = context.applicationContext
        storeInstance = UpdateStore(appContext)
        inspector = InstalledApkInspector(appContext)
        checker = UpdateChecker(
            manifestUrl = UpdateConfig.MANIFEST_URL,
            channel = UpdateConfig.CHANNEL,
            store = storeInstance,
            installedApkSha256Provider = { inspector.getInstalledApkSha256() }
        )
        downloader = UpdateDownloader()
        installer = UpdateInstaller()
        initialized = true
    }

    suspend fun checkForUpdate(context: Context): UpdateCheckResult {
        init(context)
        val packageInfo = getPackageInfo(context)
        val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode
        }
        Timber.d("检查更新: currentVersionCode=$currentVersionCode")
        return checker.checkForUpdate(currentVersionCode, context.packageName)
    }

    suspend fun downloadUpdate(
        context: Context,
        manifest: AppUpdateManifest
    ): UpdateDownloadResult {
        init(context)
        Timber.d("下载更新: versionCode=${manifest.versionCode}, versionName=${manifest.versionName}")
        return downloader.download(manifest, context.cacheDir, UpdateConfig.CHANNEL)
    }

    fun installUpdate(context: Context, apkFile: File): UpdateInstallResult {
        init(context)
        Timber.d("安装更新: ${apkFile.absolutePath}")
        return installer.installUpdate(context, apkFile)
    }

    fun recordFailure(context: Context, versionCode: Int, message: String) {
        init(context)
        storeInstance.recordFailure(UpdateConfig.CHANNEL, versionCode, System.currentTimeMillis(), message)
    }

    fun clearFailure(context: Context) {
        init(context)
        storeInstance.clearFailure(UpdateConfig.CHANNEL)
    }

    private fun getPackageInfo(context: Context): PackageInfo {
        return context.packageManager.getPackageInfo(context.packageName, 0)
    }
}
