package com.chat.lightweight.data.repository

import android.content.Context
import com.chat.lightweight.data.local.PreferencesManager
import com.chat.lightweight.domain.model.*
import com.chat.lightweight.domain.repository.ChatRepository
import com.chat.lightweight.network.ApiClient
import com.chat.lightweight.network.ApiResponse
import com.chat.lightweight.network.NetworkRepository
import com.chat.lightweight.socket.SocketManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.emptyFlow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

/**
 * 聊天仓库实现
 */
class ChatRepositoryImpl(
    private val context: Context
) : ChatRepository {

    private val apiClient = ApiClient()
    private val networkRepository = NetworkRepository.getInstance()
    private val preferencesManager = PreferencesManager.getInstance(context)

    private lateinit var socketManager: SocketManager

    companion object {
        @Volatile
        private var instance: ChatRepositoryImpl? = null

        fun getInstance(context: Context): ChatRepositoryImpl {
            return instance ?: synchronized(this) {
                instance ?: ChatRepositoryImpl(context.applicationContext).also { instance = it }
            }
        }
    }

    // 认证相关
    override suspend fun register(
        username: String,
        password: String,
        isAdmin: Boolean
    ): Result<AuthResponse> {
        return when (val response = networkRepository.register(username, password, isAdmin)) {
            is ApiResponse.Success -> {
                val data = response.data
                // 保存用户信息
                if (data != null) {
                    preferencesManager.saveUserData(
                        com.chat.lightweight.data.model.UserData(
                            userId = data.userId,
                            username = data.username,
                            isAdmin = data.isAdmin,
                            token = data.userId
                        )
                    )
                }

                // 转换为 domain.model.AuthResponse
                val authResponse = AuthResponse(
                    token = data?.userId,
                    userId = data?.userId,
                    username = data?.username,
                    isAdmin = data?.isAdmin ?: false
                )
                Result.success(authResponse)
            }
            is ApiResponse.Error -> {
                Result.failure(Exception(response.message))
            }
            else -> Result.failure(Exception("注册失败"))
        }
    }

    override suspend fun login(username: String, password: String): Result<AuthResponse> {
        return when (val response = networkRepository.login(username, password)) {
            is ApiResponse.Success -> {
                val data = response.data
                // 保存用户信息
                if (data != null) {
                    preferencesManager.saveUserData(
                        com.chat.lightweight.data.model.UserData(
                            userId = data.userId,
                            username = data.username,
                            isAdmin = data.isAdmin,
                            token = data.userId
                        )
                    )
                }

                // 转换为 domain.model.AuthResponse
                val authResponse = AuthResponse(
                    token = data?.userId,
                    userId = data?.userId,
                    username = data?.username,
                    isAdmin = data?.isAdmin ?: false
                )
                Result.success(authResponse)
            }
            is ApiResponse.Error -> {
                Result.failure(Exception(response.message))
            }
            else -> Result.failure(Exception("登录失败"))
        }
    }

    override suspend fun checkAdminExists(): Result<Boolean> {
        return try {
            val response = apiClient.authApi.checkAdminExists()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.exists ?: false)
            } else {
                Result.failure(Exception("检查失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 用户相关
    override suspend fun getMembers(userId: String): Result<List<User>> {
        return try {
            val response = apiClient.userApi.getMembers(userId)
            if (response.isSuccessful && response.body() != null) {
                val members = response.body()!!
                // 转换为 domain.model.User
                val domainUsers = members.map { member ->
                    User(
                        id = member.id,
                        username = member.username,
                        isAdmin = false, // Member 类没有 isAdmin 属性
                        adminNote = member.adminNote,
                        isOnline = member.isOnline == 1,
                        createdAt = member.lastActive
                    )
                }
                Result.success(domainUsers)
            } else {
                Result.failure(Exception("获取成员列表失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateMemberNote(userId: String, note: String): Result<Boolean> {
        return try {
            val currentUserId = preferencesManager.getUserId() ?: ""
            val request = com.chat.lightweight.network.model.UpdateNoteRequest(note, currentUserId)
            val response = apiClient.userApi.updateMemberNote(userId, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.success ?: false)
            } else {
                Result.failure(Exception("更新备注失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteMember(userId: String): Result<Boolean> {
        return try {
            val currentUserId = preferencesManager.getUserId() ?: ""
            val request = com.chat.lightweight.network.model.DeleteMemberRequest(currentUserId)
            val response = apiClient.userApi.deleteMember(userId, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.success ?: false)
            } else {
                Result.failure(Exception("删除成员失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 对话相关
    override suspend fun getConversations(): Result<List<Conversation>> {
        val userId = preferencesManager.getUserId()
            ?: return Result.failure(Exception("用户未登录"))

        return try {
            val response = apiClient.chatApi.getConversations(userId)
            if (response.isSuccessful && response.body() != null) {
                val conversations = response.body()!!
                // 转换为 domain.model.Conversation
                val domainConversations = conversations.map { conv ->
                    Conversation(
                        id = conv.id,
                        userId = conv.userId,
                        unreadCount = conv.unreadCount,
                        lastMessage = null, // 简化处理
                        user = null, // 简化处理
                        createdAt = conv.createdAt
                    )
                }
                Result.success(domainConversations)
            } else {
                Result.failure(Exception("获取对话列表失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markConversationRead(conversationId: String): Result<Boolean> {
        return try {
            val userId = preferencesManager.getUserId() ?: return Result.failure(Exception("用户未登录"))
            val request = com.chat.lightweight.network.model.MarkReadRequest(userId)
            val response = apiClient.chatApi.markConversationRead(conversationId, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.success ?: false)
            } else {
                Result.failure(Exception("标记已读失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 消息相关
    override suspend fun getMessages(conversationId: String): Result<List<Message>> {
        return try {
            val response = apiClient.chatApi.getMessages(conversationId)
            if (response.isSuccessful && response.body() != null) {
                val messages = response.body()!!
                // 转换为 domain.model.Message
                val domainMessages = messages.map { msg ->
                    Message(
                        id = msg.id,
                        conversationId = msg.conversationId,
                        senderId = msg.senderId,
                        content = msg.content ?: "",
                        messageType = when (msg.messageType) {
                            "image" -> MessageType.IMAGE
                            "voice" -> MessageType.VOICE
                            "file" -> MessageType.FILE
                            else -> MessageType.TEXT
                        },
                        fileUrl = msg.fileUrl,
                        expiresAt = msg.expiresAt,
                        isDeleted = msg.isDeleted == 1,
                        isRead = msg.isRead == 1,
                        createdAt = msg.createdAt,
                        sender = null
                    )
                }
                Result.success(domainMessages)
            } else {
                Result.failure(Exception("获取消息失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendMessage(request: SendMessageRequest): Result<Message> {
        val currentUserId = preferencesManager.getUserId()
            ?: return Result.failure(Exception("用户未登录"))

        return try {
            val sendMessageRequest = com.chat.lightweight.network.model.SendMessageRequest(
                conversationId = request.conversationId,
                senderId = currentUserId,
                content = request.content,
                messageType = request.messageType,
                fileUrl = request.fileUrl,
                tempId = request.tempId
            )
            val response = apiClient.chatApi.sendMessage(sendMessageRequest)
            if (response.isSuccessful && response.body() != null) {
                val msg = response.body()!!
                // 转换为 domain.model.Message
                val domainMessage = Message(
                    id = msg.id,
                    conversationId = msg.conversationId,
                    senderId = msg.senderId,
                    content = msg.content ?: "",
                    messageType = when (msg.messageType) {
                        "image" -> MessageType.IMAGE
                        "voice" -> MessageType.VOICE
                        "file" -> MessageType.FILE
                        else -> MessageType.TEXT
                    },
                    fileUrl = msg.fileUrl,
                    expiresAt = msg.expiresAt,
                    isDeleted = msg.isDeleted == 1,
                    createdAt = msg.createdAt,
                    sender = null
                )
                Result.success(domainMessage)
            } else {
                Result.failure(Exception("发送消息失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteMessage(messageId: String): Result<Boolean> {
        return try {
            val userId = preferencesManager.getUserId() ?: ""
            val request = com.chat.lightweight.network.model.DeleteMessageRequest(userId)
            val response = apiClient.chatApi.deleteMessage(messageId, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.success ?: false)
            } else {
                Result.failure(Exception("删除消息失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 文件上传
    override suspend fun uploadFile(filePath: String): Result<UploadResponse> {
        val file = File(filePath)
        return when (val response = networkRepository.uploadFile(file)) {
            is ApiResponse.Success -> {
                val uploadResponse = UploadResponse(
                    fileUrl = response.data ?: "",
                    fileName = file.name
                )
                Result.success(uploadResponse)
            }
            is ApiResponse.Error -> {
                Result.failure(Exception(response.message))
            }
            else -> Result.failure(Exception("上传失败"))
        }
    }

    // Socket.IO连接
    override fun connectSocket(userId: String) {
        if (!::socketManager.isInitialized) {
            socketManager = SocketManager.getInstance(context)
        }
        socketManager.connect(userId)
    }

    override fun disconnectSocket() {
        if (::socketManager.isInitialized) {
            socketManager.disconnect()
        }
    }

    override fun subscribeToMessages(): Flow<Message> {
        return if (::socketManager.isInitialized) {
            socketManager.getSocketEvents().map { event ->
                when (event) {
                    is com.chat.lightweight.socket.SocketEvent.NewMessage -> {
                        // 转换为 domain.model.Message
                        val msg = event.message
                        Message(
                            id = msg.id,
                            conversationId = msg.conversation_id,
                            senderId = msg.sender_id,
                            content = msg.content,
                            messageType = MessageType.TEXT,
                            fileUrl = msg.file_url,
                            expiresAt = msg.expires_at,
                            isDeleted = msg.is_deleted,
                            createdAt = msg.created_at,
                            sender = null
                        )
                    }
                    else -> {
                        // 返回空消息或抛出异常
                        Message(
                            id = "",
                            conversationId = "",
                            senderId = "",
                            content = "",
                            messageType = MessageType.TEXT,
                            createdAt = System.currentTimeMillis().toString()
                        )
                    }
                }
            }
        } else {
            emptyFlow()
        }
    }

    override fun subscribeToMessageDeletions(): Flow<String> {
        return if (::socketManager.isInitialized) {
            socketManager.getSocketEvents().map { event ->
                when (event) {
                    is com.chat.lightweight.socket.SocketEvent.MessageDeleted -> event.event.messageId
                    else -> ""
                }
            }
        } else {
            emptyFlow()
        }
    }

    override fun subscribeToUserStatus(): Flow<Pair<String, Boolean>> {
        return if (::socketManager.isInitialized) {
            socketManager.getSocketEvents().map { event ->
                when (event) {
                    is com.chat.lightweight.socket.SocketEvent.UserOnline -> {
                        Pair(event.event.userId, true)
                    }
                    is com.chat.lightweight.socket.SocketEvent.UserOffline -> {
                        Pair(event.event.userId, false)
                    }
                    else -> {
                        Pair("", false)
                    }
                }
            }
        } else {
            emptyFlow()
        }
    }

    // 本地存储
    override suspend fun saveUserId(userId: String) {
        // PreferencesManager通过saveUserData保存，这里暂时忽略
    }

    override suspend fun getUserId(): String? {
        return preferencesManager.getUserId()
    }

    override suspend fun saveUsername(username: String) {
        // PreferencesManager通过saveUserData保存，这里暂时忽略
    }

    override suspend fun getUsername(): String? {
        return preferencesManager.getUserData()?.username
    }

    override suspend fun clearUserData() {
        preferencesManager.clearUserData()
    }
}
