package com.chat.lightweight.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 消息类型枚举
 */
enum class MessageType(val value: String) {
    TEXT("text"),
    IMAGE("image"),
    VOICE("voice"),
    FILE("file");

    companion object {
        fun fromValue(value: String): MessageType = values().firstOrNull { it.value == value } ?: TEXT
    }
}

/**
 * 消息数据模型
 */
@Parcelize
data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val content: String,
    val messageType: MessageType = MessageType.TEXT,
    val fileUrl: String? = null,
    val expiresAt: String? = null,
    val isDeleted: Boolean = false,
    val isRead: Boolean = false,
    val createdAt: String,
    val sender: User? = null
) : Parcelable
