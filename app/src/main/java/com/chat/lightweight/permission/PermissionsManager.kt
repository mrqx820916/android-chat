package com.chat.lightweight.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * 权限管理器 - 负责检查、请求和管理运行时权限
 *
 * 权限分级:
 * - P0 (必需): 启动时必须请求的权限
 * - P1 (功能触发时): 使用特定功能时才请求的权限
 */
class PermissionsManager private constructor(){

    companion object {
        @Volatile private var instance: PermissionsManager? = null

        fun getInstance(): PermissionsManager {
            return instance ?: synchronized(this) {
                instance ?: PermissionsManager().also { instance = it }
            }
        }

        // 权限请求码
        const val RC_P0_PERMISSIONS = 1001
        const val RC_P1_AUDIO = 1002
        const val RC_P1_IMAGE = 1003
        const val RC_P1_CAMERA = 1004
        const val RC_SETTINGS = 1005

        // P0 权限 (必需)
        val P0_PERMISSIONS = mutableListOf<String>().apply {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray()

        // P1 权限 (功能触发时)
        val P1_AUDIO_PERMISSIONS = arrayOf(Manifest.permission.RECORD_AUDIO)

        @Suppress("DEPRECATION")
        val P1_IMAGE_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val P1_CAMERA_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    /**
     * 检查权限是否已授予
     */
    fun checkPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 检查多个权限是否都已授予
     */
    fun checkPermissions(context: Context, permissions: Array<String>): Boolean {
        return permissions.all { checkPermission(context, it) }
    }

    /**
     * 检查P0权限是否都已授予
     */
    fun checkP0Permissions(context: Context): Boolean {
        return checkPermissions(context, P0_PERMISSIONS)
    }

    /**
     * 请求P0权限
     */
    fun requestP0Permissions(activity: Activity) {
        val needRequestPermissions = P0_PERMISSIONS.filterNot {
            checkPermission(activity, it)
        }.toTypedArray()

        if (needRequestPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                needRequestPermissions,
                RC_P0_PERMISSIONS
            )
        }
    }

    /**
     * 请求录音权限
     */
    fun requestAudioPermission(activity: Activity) {
        if (!checkPermission(activity, Manifest.permission.RECORD_AUDIO)) {
            ActivityCompat.requestPermissions(
                activity,
                P1_AUDIO_PERMISSIONS,
                RC_P1_AUDIO
            )
        }
    }

    /**
     * 请求图片选择权限
     */
    fun requestImagePermission(activity: Activity) {
        val needRequest = P1_IMAGE_PERMISSIONS.filterNot {
            checkPermission(activity, it)
        }.toTypedArray()

        if (needRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                needRequest,
                RC_P1_IMAGE
            )
        }
    }

    /**
     * 请求相机权限
     */
    fun requestCameraPermission(activity: Activity) {
        if (!checkPermission(activity, Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(
                activity,
                P1_CAMERA_PERMISSIONS,
                RC_P1_CAMERA
            )
        }
    }

    /**
     * 处理权限请求结果
     *
     * @param activity 当前Activity
     * @param requestCode 请求码
     * @param permissions 权限数组
     * @param grantResults 授权结果
     * @return 是否所有权限都已授予
     */
    fun handlePermissionResult(
        activity: Activity,
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ): Boolean {
        val allGranted = grantResults.all {
            it == PackageManager.PERMISSION_GRANTED
        }

        when (requestCode) {
            RC_P0_PERMISSIONS -> {
                if (!allGranted) {
                    showP0PermissionDeniedDialog(activity)
                }
            }
            RC_P1_AUDIO -> {
                if (!allGranted && shouldShowPermissionRationale(activity, P1_AUDIO_PERMISSIONS)) {
                    showPermissionRationaleDialog(activity, "录音权限", "需要录音权限才能发送语音消息")
                }
            }
            RC_P1_IMAGE -> {
                if (!allGranted && shouldShowPermissionRationale(activity, P1_IMAGE_PERMISSIONS)) {
                    showPermissionRationaleDialog(activity, "存储权限", "需要存储权限才能选择图片")
                }
            }
            RC_P1_CAMERA -> {
                if (!allGranted && shouldShowPermissionRationale(activity, P1_CAMERA_PERMISSIONS)) {
                    showPermissionRationaleDialog(activity, "相机权限", "需要相机权限才能拍照")
                }
            }
        }

        return allGranted
    }

    /**
     * 检查是否应该显示权限说明
     */
    fun shouldShowPermissionRationale(activity: Activity, permissions: Array<String>): Boolean {
        return permissions.any {
            ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
        }
    }

    /**
     * 显示P0权限被拒绝的对话框
     */
    private fun showP0PermissionDeniedDialog(activity: Activity) {
        PermissionRequestDialog.showP0PermissionDeniedDialog(activity)
    }

    /**
     * 显示权限说明对话框
     */
    fun showPermissionRationaleDialog(
        activity: Activity,
        permissionName: String,
        message: String
    ) {
        PermissionRequestDialog.showPermissionRationaleDialog(
            activity,
            permissionName,
            message
        )
    }

    /**
     * 打开应用设置页面
     */
    fun openAppSettings(activity: Activity) {
        PermissionRequestDialog.openAppSettings(activity)
    }

    /**
     * 检查用户是否永久拒绝了权限
     */
    fun isPermissionPermanentlyDenied(activity: Activity, permission: String): Boolean {
        return !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission) &&
               !checkPermission(activity, permission)
    }

    /**
     * 获取未授予的P0权限列表
     */
    fun getDeniedP0Permissions(context: Context): List<String> {
        return P0_PERMISSIONS.filterNot { checkPermission(context, it) }
    }

    /**
     * 获取权限的友好名称
     */
    fun getPermissionFriendlyName(permission: String): String {
        return when (permission) {
            Manifest.permission.POST_NOTIFICATIONS -> "通知权限"
            Manifest.permission.RECORD_AUDIO -> "录音权限"
            Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_EXTERNAL_STORAGE -> "存储权限"
            Manifest.permission.CAMERA -> "相机权限"
            else -> "必需权限"
        }
    }

    /**
     * 获取权限的说明文本
     */
    fun getPermissionDescription(permission: String): String {
        return when (permission) {
            Manifest.permission.POST_NOTIFICATIONS -> "用于接收新消息通知"
            Manifest.permission.RECORD_AUDIO -> "用于发送语音消息"
            Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_EXTERNAL_STORAGE -> "用于选择和发送图片"
            Manifest.permission.CAMERA -> "用于拍照并发送"
            else -> "用于应用正常功能"
        }
    }

    /**
     * 初始化P0权限 (在MainActivity.onCreate中调用)
     *
     * @param activity 当前Activity
     * @return true 如果所有P0权限都已授予, false 否则
     */
    fun initializeP0Permissions(activity: Activity): Boolean {
        return if (checkP0Permissions(activity)) {
            true
        } else {
            requestP0Permissions(activity)
            false
        }
    }

    /**
     * 获取所有需要请求的权限状态 (用于调试)
     */
    fun getAllPermissionsStatus(context: Context): Map<String, Boolean> {
        val allPermissions = mutableListOf<String>().apply {
            addAll(P0_PERMISSIONS)
            addAll(P1_AUDIO_PERMISSIONS)
            addAll(P1_IMAGE_PERMISSIONS)
            addAll(P1_CAMERA_PERMISSIONS)
        }

        return allPermissions.associateWith { checkPermission(context, it) }
    }
}
