package com.chat.lightweight.data.model

/**
 * 对话项数据模型
 */
data class ConversationItem(
    val id: String,
    val userId: String,
    val username: String,
    val unreadCount: Int = 0,
    val lastMessage: String = "",
    val lastMessageTime: Long = System.currentTimeMillis(),
    val avatarUrl: String = ""
) {
    /**
     * 是否有未读消息
     */
    fun hasUnreadMessages(): Boolean = unreadCount > 0

    /**
     * 获取未读消息数量显示文本
     */
    fun getUnreadCountText(): String = if (unreadCount > 99) "99+" else unreadCount.toString()
}
