package com.chat.lightweight.data.model

import android.Manifest
import android.os.Build

/**
 * 权限类型枚举
 */
enum class PermissionType(
    val permission: String,
    val minSdk: Int = Build.VERSION_CODES.BASE,
    val description: String,
    val rationale: String
) {
    /**
     * 通知权限 (Android 13+)
     */
    POST_NOTIFICATIONS(
        permission = Manifest.permission.POST_NOTIFICATIONS,
        minSdk = Build.VERSION_CODES.TIRAMISU,
        description = "通知权限",
        rationale = "需要通知权限以便在收到新消息时向您推送通知"
    ),

    /**
     * 录音权限
     */
    RECORD_AUDIO(
        permission = Manifest.permission.RECORD_AUDIO,
        description = "录音权限",
        rationale = "需要录音权限以便发送语音消息"
    ),

    /**
     * 相机权限
     */
    CAMERA(
        permission = Manifest.permission.CAMERA,
        description = "相机权限",
        rationale = "需要相机权限以便拍照发送图片"
    ),

    /**
     * 读取媒体图片权限 (Android 13+)
     */
    READ_MEDIA_IMAGES(
        permission = Manifest.permission.READ_MEDIA_IMAGES,
        minSdk = Build.VERSION_CODES.TIRAMISU,
        description = "读取图片权限",
        rationale = "需要读取图片权限以便从相册选择图片发送"
    ),

    /**
     * 读取外部存储权限 (Android 12及以下)
     */
    READ_EXTERNAL_STORAGE(
        permission = Manifest.permission.READ_EXTERNAL_STORAGE,
        description = "读取存储权限",
        rationale = "需要读取存储权限以便从相册选择图片发送"
    ),

    /**
     * 写入外部存储权限 (Android 10及以下)
     */
    WRITE_EXTERNAL_STORAGE(
        permission = Manifest.permission.WRITE_EXTERNAL_STORAGE,
        description = "写入存储权限",
        rationale = "需要写入存储权限以便保存图片和语音文件"
    );

    companion object {
        /**
         * 获取当前SDK版本支持的权限列表
         */
        fun getSupportedPermissions(): List<PermissionType> {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                listOf(
                    POST_NOTIFICATIONS,
                    RECORD_AUDIO,
                    CAMERA,
                    READ_MEDIA_IMAGES
                )
            } else {
                listOf(
                    RECORD_AUDIO,
                    CAMERA,
                    READ_EXTERNAL_STORAGE,
                    WRITE_EXTERNAL_STORAGE
                )
            }
        }

        /**
         * 根据权限字符串获取权限类型
         */
        fun fromPermission(permission: String): PermissionType? {
            return values().find { it.permission == permission }
        }
    }
}
