package com.chat.lightweight.data.model

/**
 * 成员数据模型（UI层）
 */
data class MemberItem(
    val id: String,
    val username: String,
    val note: String = "",
    val isOnline: Boolean = false,
    val messageCount: Int = 0,
    val lastMessage: String = "",
    val lastActive: String = ""
)
