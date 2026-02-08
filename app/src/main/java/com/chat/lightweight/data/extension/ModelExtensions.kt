package com.chat.lightweight.data.extension

import com.chat.lightweight.data.model.ConversationItem
import com.chat.lightweight.data.model.MessageItem
import com.chat.lightweight.data.model.UserData
import org.json.JSONArray
import org.json.JSONObject

/**
 * JSON到UserData转换扩展
 */
fun JSONObject.toUserData(): UserData? {
    return try {
        UserData(
            userId = optString("id", ""),
            username = optString("username", ""),
            isAdmin = optBoolean("is_admin", false),
            token = optString("token", "")
        )
    } catch (e: Exception) {
        null
    }
}

/**
 * JSON字符串到UserData转换
 */
fun String.toUserData(): UserData? {
    return try {
        JSONObject(this).toUserData()
    } catch (e: Exception) {
        null
    }
}

/**
 * UserData到JSON转换
 */
fun UserData.toJson(): String {
    return JSONObject().apply {
        put("id", userId)
        put("username", username)
        put("is_admin", isAdmin)
        put("token", token)
    }.toString()
}

/**
 * JSON到ConversationItem转换扩展
 */
fun JSONObject.toConversationItem(): ConversationItem? {
    return try {
        ConversationItem(
            id = optString("id", ""),
            userId = optString("user_id", ""),
            username = optString("username", ""),
            unreadCount = optInt("unread_count", 0),
            lastMessage = optString("last_message", ""),
            lastMessageTime = optLong("last_message_time", System.currentTimeMillis()),
            avatarUrl = optString("avatar_url", "")
        )
    } catch (e: Exception) {
        null
    }
}

/**
 * JSON字符串列表到ConversationItem列表转换
 */
fun String.toConversationList(): List<ConversationItem> {
    return try {
        val jsonArray = JSONArray(this)
        (0 until jsonArray.length()).mapNotNull { index ->
            jsonArray.optJSONObject(index)?.toConversationItem()
        }
    } catch (e: Exception) {
        emptyList()
    }
}

/**
 * ConversationItem到JSON转换
 */
fun ConversationItem.toJson(): String {
    return JSONObject().apply {
        put("id", id)
        put("user_id", userId)
        put("username", username)
        put("unread_count", unreadCount)
        put("last_message", lastMessage)
        put("last_message_time", lastMessageTime)
        put("avatar_url", avatarUrl)
    }.toString()
}

/**
 * ConversationItem列表到JSON数组转换
 */
@JvmName("conversationListToJson")
fun List<ConversationItem>.toJson(): String {
    return JSONArray().apply {
        forEach { item ->
            put(JSONObject(item.toJson()))
        }
    }.toString()
}

/**
 * JSON到MessageItem转换扩展
 */
fun JSONObject.toMessageItem(): MessageItem? {
    return try {
        MessageItem(
            id = optString("id", ""),
            conversationId = optString("conversation_id", ""),
            senderId = optString("sender_id", ""),
            senderName = optString("sender_name", ""),
            content = optString("content", ""),
            messageType = optString("message_type", MessageItem.TYPE_TEXT),
            fileUrl = optString("file_url", ""),
            timestamp = optLong("timestamp", System.currentTimeMillis()),
            isFromSelf = optBoolean("is_from_self", false),
            isRead = optBoolean("is_read", false),
            expiresAt = if (has("expires_at") && !isNull("expires_at")) {
                optLong("expires_at")
            } else {
                null
            }
        )
    } catch (e: Exception) {
        null
    }
}

/**
 * JSON字符串列表到MessageItem列表转换
 */
fun String.toMessageList(): List<MessageItem> {
    return try {
        val jsonArray = JSONArray(this)
        (0 until jsonArray.length()).mapNotNull { index ->
            jsonArray.optJSONObject(index)?.toMessageItem()
        }
    } catch (e: Exception) {
        emptyList()
    }
}

/**
 * MessageItem到JSON转换
 */
fun MessageItem.toJson(): String {
    return JSONObject().apply {
        put("id", id)
        put("conversation_id", conversationId)
        put("sender_id", senderId)
        put("sender_name", senderName)
        put("content", content)
        put("message_type", messageType)
        put("file_url", fileUrl)
        put("timestamp", timestamp)
        put("is_from_self", isFromSelf)
        put("is_read", isRead)
        expiresAt?.let { put("expires_at", it) }
    }.toString()
}

/**
 * MessageItem列表到JSON数组转换
 */
@JvmName("messageListToJson")
fun List<MessageItem>.toJson(): String {
    return JSONArray().apply {
        forEach { item ->
            put(JSONObject(item.toJson()))
        }
    }.toString()
}

/**
 * 列表分页扩展
 */
fun <T> List<T>.paginate(page: Int, pageSize: Int): List<T> {
    val startIndex = page * pageSize
    if (startIndex >= size) return emptyList()

    val endIndex = minOf(startIndex + pageSize, size)
    return subList(startIndex, endIndex)
}

/**
 * 列表去重扩展 (基于指定键)
 */
fun <T, K> List<T>.distinctBy(keySelector: (T) -> K): List<T> {
    val set = HashSet<K>()
    val result = ArrayList<T>()

    for (item in this) {
        val key = keySelector(item)
        if (set.add(key)) {
            result.add(item)
        }
    }

    return result
}
