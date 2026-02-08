package com.chat.lightweight.data.remote.api

import com.chat.lightweight.domain.model.*
import okhttp3.MultipartBody
import retrofit2.http.*

/**
 * 聊天API服务接口
 */
interface ChatApiService {

    // 认证相关
    @POST("auth/register")
    suspend fun register(@Body request: AuthRequest): ApiResponse<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body request: AuthRequest): ApiResponse<AuthResponse>

    @GET("auth/admin-exists")
    suspend fun checkAdminExists(): ApiResponse<Boolean>

    // 用户管理
    @GET("users/members")
    suspend fun getMembers(@Query("userId") userId: String): ApiResponse<List<User>>

    @PUT("users/{userId}/note")
    suspend fun updateMemberNote(
        @Path("userId") userId: String,
        @Body note: Map<String, String>
    ): ApiResponse<Boolean>

    @DELETE("users/{userId}")
    suspend fun deleteMember(@Path("userId") userId: String): ApiResponse<Boolean>

    // 对话相关
    @GET("conversations")
    suspend fun getConversations(): ApiResponse<List<Conversation>>

    @POST("conversations/{conversationId}/read")
    suspend fun markConversationRead(@Path("conversationId") conversationId: String): ApiResponse<Boolean>

    // 消息相关
    @GET("messages")
    suspend fun getMessages(@Query("conversationId") conversationId: String): ApiResponse<List<Message>>

    @POST("messages")
    suspend fun sendMessage(@Body request: SendMessageRequest): ApiResponse<Message>

    @DELETE("messages/{messageId}")
    suspend fun deleteMessage(@Path("messageId") messageId: String): ApiResponse<Boolean>

    // 文件上传
    @Multipart
    @POST("upload")
    suspend fun uploadFile(@Part file: MultipartBody.Part): ApiResponse<UploadResponse>
}
