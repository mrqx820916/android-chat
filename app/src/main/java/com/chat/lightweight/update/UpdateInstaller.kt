package com.chat.lightweight.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File

internal class UpdateInstaller {

    fun installUpdate(context: Context, apkFile: File): UpdateInstallResult {
        return try {
            val intent = createInstallIntent(context, apkFile)
            context.startActivity(intent)
            UpdateInstallResult(
                success = true,
                detail = "已拉起系统安装器",
                launchedInteractiveInstaller = true
            )
        } catch (throwable: Throwable) {
            UpdateInstallResult(
                success = false,
                detail = throwable.message ?: "拉起系统安装器失败"
            )
        }
    }

    internal fun createInstallIntent(context: Context, apkFile: File): Intent {
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Android 8+ 需要请求安装未知来源应用权限
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            }
        }
    }
}
