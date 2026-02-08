package com.chat.lightweight.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * 权限辅助类 - 提供便捷的权限检查和请求方法
 */
object PermissionHelper {

    /**
     * 检查是否有通知权限
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * 检查是否有录音权限
     */
    fun hasRecordAudioPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 检查是否有图片访问权限
     */
    fun hasImagePermission(context: Context): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            @Suppress("DEPRECATION")
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 检查是否有相机权限
     */
    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 检查是否应该显示权限说明
     */
    fun shouldShowRationale(activity: Activity, permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    /**
     * 获取所有权限的状态 (用于调试)
     */
    fun getPermissionStatusMap(context: Context): Map<String, Boolean> {
        val permissions = mutableMapOf<String, Boolean>()

        // 通知权限
        permissions["通知权限"] = hasNotificationPermission(context)

        // 录音权限
        permissions["录音权限"] = hasRecordAudioPermission(context)

        // 图片权限
        permissions["图片权限"] = hasImagePermission(context)

        // 相机权限
        permissions["相机权限"] = hasCameraPermission(context)

        return permissions
    }

    /**
     * 格式化权限状态为字符串 (用于调试日志)
     */
    fun formatPermissionStatus(context: Context): String {
        val statusMap = getPermissionStatusMap(context)
        val sb = StringBuilder()
        sb.append("=== 权限状态 ===\n")
        statusMap.forEach { (name, granted) ->
            sb.append("$name: ${if (granted) "已授予" else "未授予"}\n")
        }
        return sb.toString()
    }

    /**
     * 检查是否所有必需权限都已授予
     */
    fun hasAllRequiredPermissions(context: Context): Boolean {
        return hasNotificationPermission(context)
    }

    /**
     * 获取缺失的权限列表
     */
    fun getMissingPermissions(context: Context): List<String> {
        val missing = mutableListOf<String>()

        if (!hasNotificationPermission(context)) {
            missing.add("通知权限")
        }

        return missing
    }

    /**
     * 检查权限是否被永久拒绝
     */
    fun isPermanentlyDenied(activity: Activity, permission: String): Boolean {
        return !shouldShowRationale(activity, permission) &&
               !ContextCompat.checkSelfPermission(
                   activity,
                   permission
               ).let { it == PackageManager.PERMISSION_GRANTED }
    }
}
