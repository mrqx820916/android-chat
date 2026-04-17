package com.chat.lightweight.socket

import com.google.gson.annotations.SerializedName

/**
 * Socket.IO 事件数据模型
 */

/**
 * 消息数据
 */
data class MessageData(
    val conversationId: String,
    val senderId: String,
    val content: String,
    val messageType: String = "text",
    val fileUrl: String? = null
)

/**
 * 用户数据
 */
data class UserData(
    val id: String,
    val username: String,
    val isAdmin: Boolean = false
)

/**
 * 新消息事件
 * 使用@SerializedName注解适配后端字段名（后端使用驼峰username和tempId）
 */
data class NewMessageEvent(
    val id: String,
    @SerializedName("conversation_id")
    val conversation_id: String,
    @SerializedName("sender_id")
    val sender_id: String,
    @SerializedName("username")  // 后端发送username，映射到sender_name
    val sender_name: String?,
    val content: String,
    @SerializedName("message_type")
    val message_type: String,
    @SerializedName("file_url")
    val file_url: String?,
    @SerializedName("is_deleted")
    val is_deleted: Boolean,
    @SerializedName("created_at")
    val created_at: String,
    @SerializedName("expires_at")
    val expires_at: String?,
    @SerializedName("temp_id")  // 后端使用下划线temp_id
    val temp_id: String? = null // 用于匹配临时消息
)

/**
 * 消息删除事件
 */
data class MessageDeletedEvent(
    val messageId: String,
    val conversationId: String
)

/**
 * 消息发送失败事件
 */
data class MessageSendFailedEvent(
    val tempMessageId: String,
    val conversationId: String,
    val reason: String
)

/**
 * 消息已读事件
 */
data class MessagesReadEvent(
    val conversationId: String,
    val messageIds: List<String>,
    val readAt: String
)

/**
 * 用户上线事件
 */
data class UserOnlineEvent(
    val userId: String,
    val username: String
)

/**
 * 用户下线事件
 */
data class UserOfflineEvent(
    val userId: String,
    val username: String
)

/**
 * 连接状态
 */
sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data object Connecting : ConnectionState()
    data object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

/**
 * Socket事件
 */
sealed class SocketEvent {
    data class Connected(val socketId: String) : SocketEvent()
    data class Disconnected(val reason: String) : SocketEvent()
    data class ConnectionError(val error: String) : SocketEvent()
    data class NewMessage(val message: NewMessageEvent) : SocketEvent()
    data class MessageDeleted(val event: MessageDeletedEvent) : SocketEvent()
    data class MessageSendFailed(val event: MessageSendFailedEvent) : SocketEvent()
    data class MessagesRead(val event: MessagesReadEvent) : SocketEvent()
    data class UserOnline(val event: UserOnlineEvent) : SocketEvent()
    data class UserOffline(val event: UserOfflineEvent) : SocketEvent()
    data object Pong : SocketEvent()
}
