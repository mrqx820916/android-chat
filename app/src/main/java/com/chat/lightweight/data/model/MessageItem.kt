package com.chat.lightweight.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 消息项数据模型
 * 用于RecyclerView显示和乐观更新
 */
@Parcelize
data class MessageItem(
    val id: String,
    val tempId: String? = null, // 临时ID，用于乐观更新
    val conversationId: String,
    val senderId: String,
    val senderName: String? = null,
    val content: String,
    val messageType: String = "text",
    val fileUrl: String? = null,
    val voiceDuration: String? = null, // 语音时长，如 "12""
    val timestamp: Long = System.currentTimeMillis(),
    val isFromSelf: Boolean = false,
    val isRead: Boolean = false,
    val expiresAt: Long? = null,
    val status: Status = Status.SENT,
    val isDeleted: Boolean = false,
    val createdAt: String = ""
) : Parcelable {
    /**
     * 消息状态
     */
    enum class Status {
        SENDING,  // 发送中
        SENT,     // 已发送
        FAILED,   // 发送失败
        DELETED   // 已删除
    }

    companion object {
        /**
         * 解析多种时间格式字符串
         * 支持：
         * 1. ISO 8601: "2024-02-09T08:30:00.000Z"
         * 2. SQLite DATETIME: "2024-02-09 08:30:00"
         *
         * 后端返回的是UTC时间，需要转换为本地时区（北京时间GMT+8）
         */
        private fun parseCreatedAt(dateString: String?): Long {
            if (dateString.isNullOrEmpty()) {
                android.util.Log.d("MessageItem", "parseCreatedAt: dateString is null or empty")
                return System.currentTimeMillis()
            }

            // 尝试多种格式
            val formats = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",  // ISO 8601 with milliseconds
                "yyyy-MM-dd'T'HH:mm:ss'Z'",       // ISO 8601 without milliseconds
                "yyyy-MM-dd HH:mm:ss.SSS",        // SQLite with milliseconds
                "yyyy-MM-dd HH:mm:ss",            // SQLite standard
                "yyyy-MM-dd'T'HH:mm:ss.SSS",      // ISO without Z
                "yyyy-MM-dd'T'HH:mm:ss"           // ISO without Z and milliseconds
            )

            for (format in formats) {
                try {
                    val sdf = java.text.SimpleDateFormat(format, java.util.Locale.getDefault())
                    // 后端返回的是UTC时间，先按UTC解析
                    sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                    val parsedTime = sdf.parse(dateString)?.time
                    if (parsedTime != null) {
                        android.util.Log.d("MessageItem", "parseCreatedAt: input='$dateString', format='$format', utc=$parsedTime, local=${parsedTime + getUtcOffsetMillis()}")
                        // 返回UTC时间戳，SimpleDateFormat会自动转换为本地时区显示
                        return parsedTime
                    }
                } catch (e: Exception) {
                    // 继续尝试下一种格式
                }
            }

            android.util.Log.e("MessageItem", "parseCreatedAt: failed to parse '$dateString' with any format")
            return System.currentTimeMillis()
        }

        /**
         * 获取UTC时区偏移量（毫秒）
         * 北京时间GMT+8 = 8 * 60 * 60 * 1000 = 28800000毫秒
         */
        private fun getUtcOffsetMillis(): Long {
            return java.util.TimeZone.getDefault().getOffset(System.currentTimeMillis()).toLong()
        }

        const val TYPE_TEXT = "text"
        const val TYPE_IMAGE = "image"
        const val TYPE_AUDIO = "audio"
        const val TYPE_FILE = "file"
        const val TYPE_VOICE = "voice"

        /**
         * 创建临时消息（用于乐观更新）
         */
        fun createTemp(
            conversationId: String,
            senderId: String,
            content: String,
            messageType: String = TYPE_TEXT
        ): MessageItem {
            return MessageItem(
                id = "",
                tempId = "temp_${System.currentTimeMillis()}_${(Math.random() * 10000).toInt()}",
                conversationId = conversationId,
                senderId = senderId,
                content = content,
                messageType = messageType,
                status = Status.SENDING,
                timestamp = System.currentTimeMillis(),
                createdAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault()).format(java.util.Date())
            )
        }

        /**
         * 从Domain Model转换为UI Model
         */
        fun fromDomain(message: com.chat.lightweight.domain.model.Message): MessageItem {
            return MessageItem(
                id = message.id,
                tempId = null,
                conversationId = message.conversationId,
                senderId = message.senderId,
                senderName = message.sender?.username,
                content = message.content,
                messageType = message.messageType.value,
                fileUrl = message.fileUrl,
                voiceDuration = null,
                timestamp = parseCreatedAt(message.createdAt),
                isFromSelf = false,
                isRead = false,
                expiresAt = null,
                status = if (message.isDeleted) Status.DELETED else Status.SENT,
                isDeleted = message.isDeleted,
                createdAt = message.createdAt
            )
        }

        /**
         * 从Domain Model转换为UI Model（带当前用户ID）
         */
        fun fromDomainMessage(
            message: com.chat.lightweight.domain.model.Message,
            currentUserId: String
        ): MessageItem {
            // 处理语音消息的时长显示
            val displayContent = if (message.messageType.value == TYPE_VOICE) {
                try {
                    val durationMs = message.content.toLongOrNull() ?: 0L
                    com.chat.lightweight.media.MediaUtils.formatDuration(durationMs)
                } catch (e: Exception) {
                    message.content
                }
            } else {
                message.content
            }

            return MessageItem(
                id = message.id,
                tempId = null,
                conversationId = message.conversationId,
                senderId = message.senderId,
                senderName = message.sender?.username,
                content = displayContent,
                messageType = message.messageType.value,
                fileUrl = message.fileUrl,
                voiceDuration = null,
                timestamp = parseCreatedAt(message.createdAt),
                isFromSelf = message.senderId == currentUserId,
                isRead = false,
                expiresAt = null,
                status = if (message.isDeleted) Status.DELETED else Status.SENT,
                isDeleted = message.isDeleted,
                createdAt = message.createdAt
            )
        }

        /**
         * 从Socket事件转换为UI Model
         */
        fun fromSocketEvent(event: com.chat.lightweight.socket.NewMessageEvent, currentUserId: String): MessageItem {
            // 处理语音消息的时长显示
            val displayContent = if (event.message_type == TYPE_VOICE) {
                // event.content 是毫秒数，需要格式化
                try {
                    val durationMs = event.content.toLongOrNull() ?: 0L
                    com.chat.lightweight.media.MediaUtils.formatDuration(durationMs)
                } catch (e: Exception) {
                    event.content
                }
            } else {
                event.content
            }

            return MessageItem(
                id = event.id,
                tempId = event.temp_id,
                conversationId = event.conversation_id,
                senderId = event.sender_id,
                senderName = event.sender_name,
                content = displayContent,
                messageType = event.message_type,
                fileUrl = event.file_url,
                voiceDuration = null,
                timestamp = parseCreatedAt(event.created_at),
                isFromSelf = event.sender_id == currentUserId,
                isRead = false,
                expiresAt = null,
                status = Status.SENT,
                isDeleted = event.is_deleted,
                createdAt = event.created_at
            )
        }
    }

    /**
     * 是否为文本消息
     */
    fun isTextMessage(): Boolean = messageType == TYPE_TEXT

    /**
     * 是否为图片消息
     */
    fun isImageMessage(): Boolean = messageType == TYPE_IMAGE

    /**
     * 是否为语音消息
     */
    fun isAudioMessage(): Boolean = messageType == TYPE_AUDIO

    /**
     * 是否为文件消息
     */
    fun isFileMessage(): Boolean = messageType == TYPE_FILE

    /**
     * 是否已过期
     */
    fun isExpired(): Boolean = expiresAt?.let { System.currentTimeMillis() > it } ?: false

    /**
     * 获取时间显示文本
     */
    fun getTimeDisplayText(): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60000 -> "刚刚"
            diff < 3600000 -> "${diff / 60000}分钟前"
            diff < 86400000 -> "${diff / 3600000}小时前"
            diff < 604800000 -> "${diff / 86400000}天前"
            else -> {
                val date = java.util.Date(timestamp)
                java.text.SimpleDateFormat("MM-dd", java.util.Locale.getDefault()).format(date)
            }
        }
    }
}
