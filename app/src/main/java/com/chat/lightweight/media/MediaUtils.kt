package com.chat.lightweight.media

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * 媒体工具类
 * 处理文件转换、验证等
 */
object MediaUtils {

    /**
     * 格式化时长
     */
    fun formatDuration(milliseconds: Long): String {
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return "${minutes}:${remainingSeconds.toString().padStart(2, '0')}"
    }

    /**
     * 从URI获取文件
     */
    fun getFileFromUri(context: Context, uri: Uri): File? {
        val fileName = getFileName(context, uri) ?: "media_${System.currentTimeMillis()}"
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null

        val tempFile = File(context.externalCacheDir, fileName)
        FileOutputStream(tempFile).use { output ->
            inputStream.copyTo(output)
        }

        return tempFile
    }

    /**
     * 获取文件名
     */
    fun getFileName(context: Context, uri: Uri): String? {
        var fileName: String? = null

        // 从URI获取文件名
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex)
                }
            }
        }

        return fileName
    }

    /**
     * 获取文件大小
     */
    fun getFileSize(context: Context, uri: Uri): Long {
        var size: Long = 0

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1) {
                    size = cursor.getLong(sizeIndex)
                }
            }
        }

        return size
    }

    /**
     * 格式化文件大小
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        if (bytes < 1024 * 1024) return "${bytes / 1024} KB"
        if (bytes < 1024 * 1024 * 1024) return "${bytes / (1024 * 1024)} MB"
        return "${bytes / (1024 * 1024 * 1024)} GB"
    }

    /**
     * 验证图片文件
     */
    fun isValidImageFile(context: Context, uri: Uri): Boolean {
        val type = context.contentResolver.getType(uri)
        return type?.startsWith("image/") == true
    }

    /**
     * 验证音频文件
     */
    fun isValidAudioFile(context: Context, uri: Uri): Boolean {
        val type = context.contentResolver.getType(uri)
        return type?.startsWith("audio/") == true
    }

    /**
     * 检查文件大小
     */
    fun isFileSizeValid(context: Context, uri: Uri, maxSizeMB: Int = 100): Boolean {
        val fileSize = getFileSize(context, uri)
        val maxSizeBytes = maxSizeMB * 1024 * 1024L
        return fileSize <= maxSizeBytes
    }

    /**
     * 获取MIME类型
     */
    fun getMimeType(context: Context, uri: Uri): String? {
        return context.contentResolver.getType(uri)
    }

    /**
     * 创建图片文件
     */
    fun createImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "IMG_$timeStamp.jpg"

        val imageDir = File(context.getExternalFilesDir(null), "images")
        if (!imageDir.exists()) {
            imageDir.mkdirs()
        }

        return File(imageDir, fileName)
    }

    /**
     * 创建语音文件
     */
    fun createVoiceFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "VOICE_$timeStamp.m4a"

        val voiceDir = File(context.getExternalFilesDir(null), "voices")
        if (!voiceDir.exists()) {
            voiceDir.mkdirs()
        }

        return File(voiceDir, fileName)
    }

    /**
     * 删除文件
     */
    fun deleteFile(file: File?): Boolean {
        return file?.delete() ?: false
    }

    /**
     * 清理临时文件
     */
    fun clearTempFiles(context: Context) {
        context.externalCacheDir?.listFiles()?.forEach { file ->
            if (file.exists()) {
                file.delete()
            }
        }
    }

    /**
     * 判断是否为视频文件
     */
    fun isVideoFile(context: Context, uri: Uri): Boolean {
        val type = context.contentResolver.getType(uri)
        return type?.startsWith("video/") == true
    }

    /**
     * 判断是否为图片文件
     */
    fun isImageFile(context: Context, uri: Uri): Boolean {
        val type = context.contentResolver.getType(uri)
        return type?.startsWith("image/") == true
    }

    /**
     * 判断是否为音频文件
     */
    fun isAudioFile(context: Context, uri: Uri): Boolean {
        val type = context.contentResolver.getType(uri)
        return type?.startsWith("audio/") == true
    }

    /**
     * 获取文件扩展名
     */
    fun getFileExtension(fileName: String): String {
        return fileName.substringAfterLast('.', "")
    }

    /**
     * 检查是否支持此文件类型
     */
    fun isSupportedFileType(context: Context, uri: Uri): Boolean {
        return isImageFile(context, uri) || isAudioFile(context, uri)
    }
}
