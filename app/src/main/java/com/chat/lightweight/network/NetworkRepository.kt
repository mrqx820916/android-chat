package com.chat.lightweight.network

import com.chat.lightweight.network.api.*
import com.chat.lightweight.network.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.File

/**
 * 网络仓库 - API调用的统一入口
 * 单一职责：提供业务层网络请求方法
 */
class NetworkRepository private constructor(private val userId: String) {

    private val apiClient = ApiClient(userId)

    // 获取当前userId
    fun getUserId(): String = userId

    companion object {
        @Volatile
        private var instance: NetworkRepository? = null

        fun getInstance(): NetworkRepository {
            return instance ?: throw IllegalStateException("NetworkRepository not initialized. Call init() first.")
        }

        fun init(userId: String): NetworkRepository {
            return synchronized(this) {
                // 如果已有实例但userId不同，则重新创建
                if (instance != null) {
                    val currentUserId = instance!!.getUserId()
                    if (currentUserId != userId) {
                        instance = NetworkRepository(userId)
                    }
                } else {
                    instance = NetworkRepository(userId)
                }
                instance!!
            }
        }

        fun resetInstance() {
            instance = null
        }
    }

    /**
     * 用户登录
     */
    suspend fun login(username: String, password: String): ApiResponse<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiClient.authApi.login(LoginRequest(username, password))
                if (response.isSuccessful && response.body() != null) {
                    ApiResponse.Success(response.body()!!)
                } else {
                    ApiResponse.Error(
                        code = response.code(),
                        message = NetworkErrorHandler.extractErrorMessage(response.errorBody()?.string())
                    )
                }
            } catch (e: Exception) {
                NetworkErrorHandler.handleError(e)
            }
        }
    }

    /**
     * 用户注册
     */
    suspend fun register(username: String, password: String, isAdmin: Boolean): ApiResponse<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiClient.authApi.register(RegisterRequest(username, password, isAdmin))
                if (response.isSuccessful && response.body() != null) {
                    ApiResponse.Success(response.body()!!)
                } else {
                    ApiResponse.Error(
                        code = response.code(),
                        message = NetworkErrorHandler.extractErrorMessage(response.errorBody()?.string())
                    )
                }
            } catch (e: Exception) {
                NetworkErrorHandler.handleError(e)
            }
        }
    }

    /**
     * 检查管理员是否存在
     */
    suspend fun checkAdminExists(): ApiResponse<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiClient.authApi.checkAdminExists()
                if (response.isSuccessful && response.body() != null) {
                    ApiResponse.Success(response.body()!!.exists)
                } else {
                    ApiResponse.Error(
                        code = response.code(),
                        message = "检查失败"
                    )
                }
            } catch (e: Exception) {
                NetworkErrorHandler.handleError(e)
            }
        }
    }

    /**
     * 获取成员列表（管理员）
     */
    suspend fun getMembers(): ApiResponse<List<Member>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiClient.userApi.getMembers(userId)
                if (response.isSuccessful && response.body() != null) {
                    ApiResponse.Success(response.body()!!)
                } else {
                    ApiResponse.Error(
                        code = response.code(),
                        message = NetworkErrorHandler.extractErrorMessage(response.errorBody()?.string())
                    )
                }
            } catch (e: Exception) {
                NetworkErrorHandler.handleError(e)
            }
        }
    }

    /**
     * 更新成员备注（管理员）
     */
    suspend fun updateMemberNote(memberId: String, note: String?): ApiResponse<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiClient.userApi.updateMemberNote(
                    memberId,
                    UpdateNoteRequest(note, userId)
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    ApiResponse.Success(true)
                } else {
                    ApiResponse.Error(
                        code = response.code(),
                        message = NetworkErrorHandler.extractErrorMessage(response.errorBody()?.string())
                    )
                }
            } catch (e: Exception) {
                NetworkErrorHandler.handleError(e)
            }
        }
    }

    /**
     * 删除成员（管理员）
     */
    suspend fun deleteMember(memberId: String): ApiResponse<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiClient.userApi.deleteMember(
                    memberId,
                    DeleteMemberRequest(userId)
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    ApiResponse.Success(true)
                } else {
                    ApiResponse.Error(
                        code = response.code(),
                        message = NetworkErrorHandler.extractErrorMessage(response.errorBody()?.string())
                    )
                }
            } catch (e: Exception) {
                NetworkErrorHandler.handleError(e)
            }
        }
    }

    /**
     * 获取对话列表
     */
    suspend fun getConversations(): ApiResponse<List<Conversation>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiClient.chatApi.getConversations(userId)
                if (response.isSuccessful && response.body() != null) {
                    ApiResponse.Success(response.body()!!)
                } else {
                    ApiResponse.Error(
                        code = response.code(),
                        message = NetworkErrorHandler.extractErrorMessage(response.errorBody()?.string())
                    )
                }
            } catch (e: Exception) {
                NetworkErrorHandler.handleError(e)
            }
        }
    }

    /**
     * 标记对话已读
     */
    suspend fun markConversationRead(conversationId: String, userId: String = ""): ApiResponse<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiClient.chatApi.markConversationRead(
                    conversationId,
                    com.chat.lightweight.network.model.MarkReadRequest(userId)
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    ApiResponse.Success(true)
                } else {
                    ApiResponse.Error(
                        code = response.code(),
                        message = "标记失败"
                    )
                }
            } catch (e: Exception) {
                NetworkErrorHandler.handleError(e)
            }
        }
    }

    /**
     * 获取对话消息
     */
    suspend fun getMessages(conversationId: String): ApiResponse<List<Message>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiClient.chatApi.getMessages(conversationId)
                if (response.isSuccessful && response.body() != null) {
                    ApiResponse.Success(response.body()!!)
                } else {
                    ApiResponse.Error(
                        code = response.code(),
                        message = NetworkErrorHandler.extractErrorMessage(response.errorBody()?.string())
                    )
                }
            } catch (e: Exception) {
                NetworkErrorHandler.handleError(e)
            }
        }
    }

    /**
     * 发送消息
     */
    suspend fun sendMessage(
        conversationId: String,
        content: String?,
        messageType: String = "text",
        fileUrl: String? = null,
        tempId: String? = null
    ): ApiResponse<Message> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiClient.chatApi.sendMessage(
                    SendMessageRequest(
                        conversationId = conversationId,
                        senderId = userId,
                        content = content,
                        messageType = messageType,
                        fileUrl = fileUrl,
                        tempId = tempId
                    )
                )
                if (response.isSuccessful && response.body() != null) {
                    ApiResponse.Success(response.body()!!)
                } else {
                    ApiResponse.Error(
                        code = response.code(),
                        message = NetworkErrorHandler.extractErrorMessage(response.errorBody()?.string())
                    )
                }
            } catch (e: Exception) {
                NetworkErrorHandler.handleError(e)
            }
        }
    }

    /**
     * 删除消息（管理员）
     */
    suspend fun deleteMessage(messageId: String): ApiResponse<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiClient.chatApi.deleteMessage(
                    messageId,
                    DeleteMessageRequest(userId)
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    ApiResponse.Success(true)
                } else {
                    ApiResponse.Error(
                        code = response.code(),
                        message = NetworkErrorHandler.extractErrorMessage(response.errorBody()?.string())
                    )
                }
            } catch (e: Exception) {
                NetworkErrorHandler.handleError(e)
            }
        }
    }

    /**
     * 获取自动删除设置（管理员）
     */
    suspend fun getAutoDeleteSetting(): ApiResponse<AutoDeleteSetting> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiClient.settingsApi.getAutoDeleteSetting(userId)
                if (response.isSuccessful && response.body() != null) {
                    ApiResponse.Success(response.body()!!)
                } else {
                    ApiResponse.Error(
                        code = response.code(),
                        message = NetworkErrorHandler.extractErrorMessage(response.errorBody()?.string())
                    )
                }
            } catch (e: Exception) {
                NetworkErrorHandler.handleError(e)
            }
        }
    }

    /**
     * 更新自动删除设置（管理员）
     */
    suspend fun updateAutoDeleteSetting(
        enabled: Boolean,
        unit: String,
        value: Int
    ): ApiResponse<AutoDeleteSetting> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiClient.settingsApi.updateAutoDeleteSetting(
                    userId,
                    UpdateAutoDeleteRequest(enabled, unit, value)
                )
                if (response.isSuccessful && response.body() != null) {
                    ApiResponse.Success(response.body()!!)
                } else {
                    ApiResponse.Error(
                        code = response.code(),
                        message = NetworkErrorHandler.extractErrorMessage(response.errorBody()?.string())
                    )
                }
            } catch (e: Exception) {
                NetworkErrorHandler.handleError(e)
            }
        }
    }

    /**
     * 上传文件
     * @param file 要上传的文件
     * @param userType 用户类型 ("admin" 或 "member")
     */
    suspend fun uploadFile(file: File, userType: String = "member"): ApiResponse<String> {
        return withContext(Dispatchers.IO) {
            try {
                val requestFile = okhttp3.RequestBody.create(
                    getMimeType(file).toMediaTypeOrNull(),
                    file
                )
                val body = okhttp3.MultipartBody.Part.createFormData("file", file.name, requestFile)

                val response = apiClient.uploadApi.uploadFile(body, userType)
                if (response.isSuccessful && response.body() != null) {
                    ApiResponse.Success(response.body()!!.fileUrl)
                } else {
                    ApiResponse.Error(
                        code = response.code(),
                        message = NetworkErrorHandler.extractErrorMessage(response.errorBody()?.string())
                    )
                }
            } catch (e: Exception) {
                NetworkErrorHandler.handleError(e)
            }
        }
    }

    /**
     * 根据文件扩展名获取MIME类型
     */
    private fun getMimeType(file: File): String {
        val extension = file.extension.lowercase()
        return when (extension) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "ogg" -> "audio/ogg"
            "m4a" -> "audio/mp4"
            "webm" -> "audio/webm"
            else -> "application/octet-stream"
        }
    }
}
