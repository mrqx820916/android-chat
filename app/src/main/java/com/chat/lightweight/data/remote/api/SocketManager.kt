package com.chat.lightweight.data.remote.api

import com.chat.lightweight.domain.model.Message
import com.chat.lightweight.domain.model.MessageType
import com.chat.lightweight.BuildConfig
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * Socket.IO管理器
 * 负责管理WebSocket连接和实时消息推送
 */
class SocketManager {

    private var socket: Socket? = null
    private val listeners = ConcurrentHashMap<String, Emitter.Listener>()

    /**
     * 连接Socket.IO服务器
     */
    fun connect(userId: String) {
        if (socket?.connected() == true) return

        try {
            val options = IO.Options().apply {
                reconnection = true
                reconnectionAttempts = Int.MAX_VALUE
                reconnectionDelay = 1000
                reconnectionDelayMax = 5000
                timeout = 20000
                forceNew = true
            }

            socket = IO.socket(BuildConfig.SOCKET_URL, options).apply {
                on(Socket.EVENT_CONNECT) {
                    emit("register", JSONObject().apply {
                        put("userId", userId)
                    })
                }
                on(Socket.EVENT_DISCONNECT) {
                    // 处理断开连接
                }
                on(Socket.EVENT_CONNECT_ERROR) {
                    // 处理连接错误
                }
                connect()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 断开Socket.IO连接
     */
    fun disconnect() {
        socket?.disconnect()
        socket = null
        listeners.clear()
    }

    /**
     * 加入对话房间
     */
    fun joinConversation(conversationId: String) {
        socket?.emit("join_conversation", JSONObject().apply {
            put("conversationId", conversationId)
        })
    }

    /**
     * 发送消息
     */
    fun sendMessage(message: Message) {
        socket?.emit("send_message", JSONObject().apply {
            put("conversationId", message.conversationId)
            put("senderId", message.senderId)
            put("content", message.content)
            put("messageType", message.messageType.value)
            message.fileUrl?.let { put("fileUrl", it) }
        })
    }

    /**
     * 订阅新消息
     */
    fun subscribeToMessages(): Flow<Message> = callbackFlow {
        val listener = Emitter.Listener { args ->
            try {
                val data = args[0] as? JSONObject ?: return@Listener
                val message = parseMessage(data)
                trySend(message)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        socket?.on("new_message", listener)
        listeners["new_message"] = listener

        awaitClose {
            socket?.off("new_message", listener)
            listeners.remove("new_message")
        }
    }

    /**
     * 订阅消息删除事件
     */
    fun subscribeToMessageDeletions(): Flow<String> = callbackFlow {
        val listener = Emitter.Listener { args ->
            try {
                val data = args[0] as? JSONObject ?: return@Listener
                val messageId = data.optString("messageId")
                if (messageId.isNotEmpty()) {
                    trySend(messageId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        socket?.on("message_deleted", listener)
        listeners["message_deleted"] = listener

        awaitClose {
            socket?.off("message_deleted", listener)
            listeners.remove("message_deleted")
        }
    }

    /**
     * 订阅用户状态变化
     */
    fun subscribeToUserStatus(): Flow<Pair<String, Boolean>> = callbackFlow {
        val listener = Emitter.Listener { args ->
            try {
                val data = args[0] as? JSONObject ?: return@Listener
                val userId = data.optString("userId")
                val isOnline = data.optBoolean("isOnline", false)
                if (userId.isNotEmpty()) {
                    trySend(userId to isOnline)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        socket?.on("user_online", listener)
        socket?.on("user_offline", listener)

        awaitClose {
            socket?.off("user_online", listener)
            socket?.off("user_offline", listener)
        }
    }

    /**
     * 解析消息JSON
     */
    private fun parseMessage(json: JSONObject): Message {
        return Message(
            id = json.getString("id"),
            conversationId = json.getString("conversationId"),
            senderId = json.getString("senderId"),
            content = json.getString("content"),
            messageType = MessageType.fromValue(json.optString("messageType", "text")),
            fileUrl = json.optString("fileUrl", null),
            expiresAt = json.optString("expiresAt", null),
            isDeleted = json.optBoolean("isDeleted", false),
            createdAt = json.getString("createdAt")
        )
    }

    /**
     * 发送心跳
     */
    fun sendPing() {
        socket?.emit("ping")
    }

    /**
     * 检查连接状态
     */
    fun isConnected(): Boolean = socket?.connected() == true
}
