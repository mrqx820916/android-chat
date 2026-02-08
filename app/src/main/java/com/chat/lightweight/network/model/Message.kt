package com.chat.lightweight.network.model

import com.google.gson.annotations.SerializedName

/**
 * 消息数据模型
 */
data class Message(
    @SerializedName("id")
    val id: String,

    @SerializedName("conversation_id")
    val conversationId: String,

    @SerializedName("sender_id")
    val senderId: String,

    @SerializedName("content")
    val content: String?,

    @SerializedName("message_type")
    val messageType: String = "text",

    @SerializedName("file_url")
    val fileUrl: String? = null,

    @SerializedName("username")
    val username: String? = null,

    @SerializedName("is_admin")
    val isAdmin: Any? = null,  // 接受 Boolean 或 Int（后端SQLite返回0/1）

    @SerializedName("temp_id")
    val tempId: String? = null,

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("expires_at")
    val expiresAt: String? = null,

    @SerializedName("is_deleted")
    val isDeleted: Int? = null
) {
    /** 将 isAdmin 转换为布尔值 */
    val isAdminBoolean: Boolean
        get() = when (isAdmin) {
            is Boolean -> isAdmin
            is Number -> isAdmin.toInt() == 1
            else -> false
        }
}

/**
 * 发送消息请求
 */
data class SendMessageRequest(
    @SerializedName("conversation_id")
    val conversationId: String,

    @SerializedName("sender_id")
    val senderId: String,

    @SerializedName("content")
    val content: String?,

    @SerializedName("message_type")
    val messageType: String = "text",

    @SerializedName("file_url")
    val fileUrl: String? = null,

    @SerializedName("temp_id")
    val tempId: String? = null
)
