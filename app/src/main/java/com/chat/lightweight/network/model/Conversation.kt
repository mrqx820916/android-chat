package com.chat.lightweight.network.model

import com.google.gson.annotations.SerializedName

/**
 * 对话数据模型
 */
data class Conversation(
    @SerializedName("id")
    val id: String,

    @SerializedName("user_id")
    val userId: String,

    @SerializedName("member_name")
    val memberName: String?,

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("message_count")
    val messageCount: Int = 0,

    @SerializedName("last_message")
    val lastMessage: String? = null,

    @SerializedName("unread_count")
    val unreadCount: Int = 0
)

/**
 * 标记已读响应
 */
data class MarkReadResponse(
    @SerializedName("success")
    val success: Boolean
)

/**
 * 标记已读请求
 */
data class MarkReadRequest(
    @SerializedName("userId")
    val userId: String
)
