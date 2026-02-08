package com.chat.lightweight.media

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

/**
 * 图片选择器助手
 * 支持从相册选择和拍照
 */
class ImagePickerHelper private constructor() {

    private var onImageSelected: ((Uri) -> Unit)? = null
    private var onPermissionDenied: (() -> Unit)? = null
    private var galleryLauncher: ActivityResultLauncher<String>? = null
    private var cameraLauncher: ActivityResultLauncher<Uri>? = null
    private var permissionLauncher: ActivityResultLauncher<String>? = null

    /**
     * 初始化Activity
     */
    fun initWithActivity(activity: AppCompatActivity): ImagePickerHelper {
        // 相册选择器
        galleryLauncher = activity.registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let { onImageSelected?.invoke(it) }
        }

        // 权限请求
        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                openCamera()
            } else {
                onPermissionDenied?.invoke()
            }
        }

        return this
    }

    /**
     * 初始化Fragment
     */
    fun initWithFragment(fragment: Fragment): ImagePickerHelper {
        // 相册选择器
        galleryLauncher = fragment.registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let { onImageSelected?.invoke(it) }
        }

        // 权限请求
        permissionLauncher = fragment.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                openCamera()
            } else {
                onPermissionDenied?.invoke()
            }
        }

        return this
    }

    /**
     * 设置图片选择回调
     */
    fun setOnImageSelected(callback: (Uri) -> Unit): ImagePickerHelper {
        this.onImageSelected = callback
        return this
    }

    /**
     * 设置权限拒绝回调
     */
    fun setOnPermissionDenied(callback: () -> Unit): ImagePickerHelper {
        this.onPermissionDenied = callback
        return this
    }

    /**
     * 打开相册选择图片
     */
    fun openGallery() {
        galleryLauncher?.launch("image/*")
    }

    /**
     * 打开相机拍照
     */
    fun openCamera() {
        // 检查相机权限
        if (permissionLauncher == null) {
            throw IllegalStateException("必须先调用 initWithActivity 或 initWithFragment")
        }

        when (ContextCompat.checkSelfPermission(
            context ?: return,
            Manifest.permission.CAMERA
        )) {
            PackageManager.PERMISSION_GRANTED -> {
                // 权限已授予，创建临时文件并启动相机
                createImageUri()?.let { uri ->
                    cameraLauncher?.launch(uri)
                }
            }
            else -> {
                // 请求权限
                permissionLauncher?.launch(Manifest.permission.CAMERA)
            }
        }
    }

    /**
     * 创建图片URI
     */
    private fun createImageUri(): Uri? {
        val fileName = "IMG_${System.currentTimeMillis()}.jpg"
        val ctx = context ?: return null
        return ctx.contentResolver?.let { contentResolver ->
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            }
            contentResolver.insert(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
        }
    }

    /**
     * 释放资源
     */
    fun release() {
        onImageSelected = null
        onPermissionDenied = null
        galleryLauncher = null
        cameraLauncher = null
        permissionLauncher = null
        context = null
    }

    private var context: Context? = null

    companion object {
        /**
         * 创建实例
         */
        fun create(): ImagePickerHelper {
            return ImagePickerHelper()
        }

        /**
         * 便捷方法：在Activity中使用
         */
        fun with(activity: AppCompatActivity, init: ImagePickerHelper.() -> Unit): ImagePickerHelper {
            val helper = ImagePickerHelper().apply {
                context = activity
                initWithActivity(activity)
                init()
            }
            return helper
        }

        /**
         * 便捷方法：在Fragment中使用
         */
        fun with(fragment: Fragment, init: ImagePickerHelper.() -> Unit): ImagePickerHelper {
            val helper = ImagePickerHelper().apply {
                context = fragment.requireContext()
                initWithFragment(fragment)
                init()
            }
            return helper
        }
    }
}
