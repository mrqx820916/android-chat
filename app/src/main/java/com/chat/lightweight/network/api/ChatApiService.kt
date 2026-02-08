package com.chat.lightweight.network.api

import com.chat.lightweight.network.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * 聊天相关API
 * 单一职责：处理对话和消息相关接口
 */
interface ChatApiService {

    /**
     * 获取对话列表
     * 请求头需要 x-user-id
     */
    @GET("conversations")
    suspend fun getConversations(
        @Query("userId") userId: String
    ): Response<List<Conversation>>

    /**
     * 标记对话已读
     */
    @POST("conversations/{conversationId}/read")
    suspend fun markConversationRead(
        @Path("conversationId") conversationId: String
    ): Response<MarkReadResponse>

    /**
     * 获取对话消息
     */
    @GET("messages")
    suspend fun getMessages(
        @Query("conversationId") conversationId: String
    ): Response<List<Message>>

    /**
     * 发送消息
     */
    @POST("messages")
    suspend fun sendMessage(
        @Body request: SendMessageRequest
    ): Response<Message>

    /**
     * 删除消息
     * 需要管理员权限
     */
    @DELETE("messages/{messageId}")
    suspend fun deleteMessage(
        @Path("messageId") messageId: String,
        @Body request: DeleteMessageRequest
    ): Response<SuccessResponse>
}
