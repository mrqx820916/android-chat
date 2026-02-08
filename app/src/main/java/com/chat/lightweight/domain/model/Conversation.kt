package com.chat.lightweight.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 对话数据模型
 */
@Parcelize
data class Conversation(
    val id: String,
    val userId: String,
    val unreadCount: Int = 0,
    val lastMessage: Message? = null,
    val user: User? = null,
    val createdAt: String? = null
) : Parcelable
