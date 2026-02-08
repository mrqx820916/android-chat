package com.chat.lightweight.domain.repository

import com.chat.lightweight.domain.model.*
import kotlinx.coroutines.flow.Flow

/**
 * 聊天仓库接口
 */
interface ChatRepository {

    // 认证相关
    suspend fun register(username: String, password: String, isAdmin: Boolean): Result<AuthResponse>
    suspend fun login(username: String, password: String): Result<AuthResponse>
    suspend fun checkAdminExists(): Result<Boolean>

    // 用户相关
    suspend fun getMembers(userId: String): Result<List<User>>
    suspend fun updateMemberNote(userId: String, note: String): Result<Boolean>
    suspend fun deleteMember(userId: String): Result<Boolean>

    // 对话相关
    suspend fun getConversations(): Result<List<Conversation>>
    suspend fun markConversationRead(conversationId: String): Result<Boolean>

    // 消息相关
    suspend fun getMessages(conversationId: String): Result<List<Message>>
    suspend fun sendMessage(request: SendMessageRequest): Result<Message>
    suspend fun deleteMessage(messageId: String): Result<Boolean>

    // 文件上传
    suspend fun uploadFile(filePath: String): Result<UploadResponse>

    // Socket.IO连接
    fun connectSocket(userId: String)
    fun disconnectSocket()
    fun subscribeToMessages(): Flow<Message>
    fun subscribeToMessageDeletions(): Flow<String>
    fun subscribeToUserStatus(): Flow<Pair<String, Boolean>> // userId to isOnline

    // 本地存储
    suspend fun saveUserId(userId: String)
    suspend fun getUserId(): String?
    suspend fun saveUsername(username: String)
    suspend fun getUsername(): String?
    suspend fun clearUserData()
}
