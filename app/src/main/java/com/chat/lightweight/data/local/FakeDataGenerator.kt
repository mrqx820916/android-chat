package com.chat.lightweight.data.local

import com.chat.lightweight.data.model.ConversationItem
import com.chat.lightweight.data.model.MessageItem
import java.util.UUID

/**
 * 假数据生成器
 * 用于开发和测试
 */
object FakeDataGenerator {

    /**
     * 生成假对话列表
     */
    fun generateFakeConversations(count: Int = 10): List<ConversationItem> {
        val names = listOf(
            "张三", "李四", "王五", "赵六", "钱七",
            "孙八", "周九", "吴十", "郑十一", "陈十二"
        )

        val lastMessages = listOf(
            "你好,在吗?", "今天天气不错", "收到", "好的,没问题",
            "明天见", "谢谢", "哈哈", "这个不错", "了解", "OK"
        )

        return (1..count).map { index ->
            ConversationItem(
                id = UUID.randomUUID().toString(),
                userId = "user_$index",
                username = names.getOrElse(index - 1) { "用户$index" },
                unreadCount = if (index % 3 == 0) (index % 5) + 1 else 0,
                lastMessage = lastMessages.getOrElse(index - 1) { "消息$index" },
                lastMessageTime = System.currentTimeMillis() - (index * 3600000L),
                avatarUrl = ""
            )
        }
    }

    /**
     * 生成假消息列表
     */
    fun generateFakeMessages(conversationId: String, count: Int = 20): List<MessageItem> {
        val contents = listOf(
            "你好", "在吗?", "今天有什么安排?",
            "收到,知道了", "好的,没问题", "明天见",
            "这个不错", "了解一下", "OK", "谢谢",
            "哈哈", "有意思", "真的吗?", "我觉得可以",
            "稍等一下", "马上就好", "没问题", "收到消息",
            "好的好的", "了解情况"
        )

        val messageTypes = listOf(
            MessageItem.TYPE_TEXT,
            MessageItem.TYPE_IMAGE,
            MessageItem.TYPE_AUDIO,
            MessageItem.TYPE_TEXT,
            MessageItem.TYPE_TEXT
        )

        return (1..count).map { index ->
            val isFromSelf = index % 2 == 0
            MessageItem(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                senderId = if (isFromSelf) "current_user" else "other_user",
                senderName = if (isFromSelf) "我" else "对方",
                content = contents.getOrElse(index - 1) { "消息$index" },
                messageType = messageTypes.getOrElse((index - 1) % messageTypes.size) { MessageItem.TYPE_TEXT },
                fileUrl = if (messageTypes.getOrElse((index - 1) % messageTypes.size) { MessageItem.TYPE_TEXT } != MessageItem.TYPE_TEXT) {
                    "https://via.placeholder.com/150"
                } else "",
                timestamp = System.currentTimeMillis() - ((count - index) * 600000L),
                isFromSelf = isFromSelf,
                isRead = true,
                expiresAt = null
            )
        }
    }

    /**
     * 生成单个假对话
     */
    fun generateFakeConversation(
        userId: String = "user_1",
        username: String = "张三",
        unreadCount: Int = 0
    ): ConversationItem {
        return ConversationItem(
            id = UUID.randomUUID().toString(),
            userId = userId,
            username = username,
            unreadCount = unreadCount,
            lastMessage = "最近的消息",
            lastMessageTime = System.currentTimeMillis(),
            avatarUrl = ""
        )
    }

    /**
     * 生成单条假消息
     */
    fun generateFakeMessage(
        conversationId: String,
        isFromSelf: Boolean = false,
        content: String = "测试消息"
    ): MessageItem {
        return MessageItem(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            senderId = if (isFromSelf) "current_user" else "other_user",
            senderName = if (isFromSelf) "我" else "对方",
            content = content,
            messageType = MessageItem.TYPE_TEXT,
            fileUrl = "",
            timestamp = System.currentTimeMillis(),
            isFromSelf = isFromSelf,
            isRead = true,
            expiresAt = null
        )
    }
}
