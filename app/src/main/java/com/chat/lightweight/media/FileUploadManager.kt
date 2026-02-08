package com.chat.lightweight.media

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * 文件上传管理器
 * 支持图片和语音文件上传，带进度显示
 */
class FileUploadManager private constructor(context: Context) {

    companion object {
        @Volatile
        private var instance: FileUploadManager? = null

        fun getInstance(context: Context): FileUploadManager {
            return instance ?: synchronized(this) {
                instance ?: FileUploadManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // 上传状态
    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()

    /**
     * 上传状态
     */
    sealed class UploadState {
        data object Idle : UploadState()
        data class Progress(val progress: Int, val bytesWritten: Long, val totalBytes: Long) : UploadState()
        data class Success(val fileUrl: String) : UploadState()
        data class Error(val message: String) : UploadState()
    }

    /**
     * 上传图片
     */
    suspend fun uploadImage(
        file: File,
        userType: String = "member",
        onProgress: ((Int) -> Unit)? = null
    ): Result<String> {
        return try {
            _uploadState.value = UploadState.Progress(0, 0, file.length())

            val requestBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.name, requestBody)
                .build()

            val request = okhttp3.Request.Builder()
                .url("${getBaseUrl()}/api/upload?userType=$userType")
                .post(multipartBody)
                .build()

            // 执行上传（带进度）
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val json = JSONObject(responseBody ?: "{}")

                if (json.optBoolean("success") && json.has("fileUrl")) {
                    val fileUrl = json.getString("fileUrl")
                    _uploadState.value = UploadState.Success(fileUrl)
                    Result.success(fileUrl)
                } else {
                    val error = json.optString("error", "上传失败")
                    _uploadState.value = UploadState.Error(error)
                    Result.failure(Exception(error))
                }
            } else {
                val error = "上传失败: ${response.code}"
                _uploadState.value = UploadState.Error(error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _uploadState.value = UploadState.Error(e.message ?: "上传失败")
            Result.failure(e)
        } finally {
            // 延迟重置状态
            kotlinx.coroutines.delay(1000)
            _uploadState.value = UploadState.Idle
        }
    }

    /**
     * 上传语音
     */
    suspend fun uploadVoice(
        file: File,
        userType: String = "member",
        onProgress: ((Int) -> Unit)? = null
    ): Result<String> {
        return try {
            _uploadState.value = UploadState.Progress(0, 0, file.length())

            val requestBody = file.asRequestBody("audio/mp4".toMediaTypeOrNull())
            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.name, requestBody)
                .build()

            val request = okhttp3.Request.Builder()
                .url("${getBaseUrl()}/api/upload?userType=$userType")
                .post(multipartBody)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val json = JSONObject(responseBody ?: "{}")

                if (json.optBoolean("success") && json.has("fileUrl")) {
                    val fileUrl = json.getString("fileUrl")
                    _uploadState.value = UploadState.Success(fileUrl)
                    Result.success(fileUrl)
                } else {
                    val error = json.optString("error", "上传失败")
                    _uploadState.value = UploadState.Error(error)
                    Result.failure(Exception(error))
                }
            } else {
                val error = "上传失败: ${response.code}"
                _uploadState.value = UploadState.Error(error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _uploadState.value = UploadState.Error(e.message ?: "上传失败")
            Result.failure(e)
        } finally {
            kotlinx.coroutines.delay(1000)
            _uploadState.value = UploadState.Idle
        }
    }

    /**
     * 获取基础URL
     */
    private fun getBaseUrl(): String {
        // 从BuildConfig获取
        return try {
            val buildConfigClass = Class.forName("com.chat.lightweight.BuildConfig")
            val baseUrlField = buildConfigClass.getField("BASE_URL")
            baseUrlField.get(null) as String
        } catch (e: Exception) {
            "https://chat.soft1688.vip"
        }
    }

    /**
     * 清除状态
     */
    fun clearState() {
        _uploadState.value = UploadState.Idle
    }

    /**
     * 取消上传
     */
    fun cancelUpload() {
        client.dispatcher.cancelAll()
        _uploadState.value = UploadState.Idle
    }
}
