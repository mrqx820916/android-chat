package com.chat.lightweight.socket

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Socket.IO 连接管理器
 *
 * 功能：
 * - 连接/断开管理
 * - 自动重连（最多10次，指数退避）
 * - 心跳检测（30秒间隔）
 * - 连接状态管理
 */
class SocketManager(private val eventListener: SocketEventListener) {

    companion object {
        private const val TAG = "SocketManager"

        @Volatile
        private var instance: SocketManager? = null

        fun getInstance(context: android.content.Context): SocketManager {
            return instance ?: synchronized(this) {
                instance ?: SocketManager(SocketEventListener()).also { instance = it }
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var socket: Socket? = null
    private var currentUserId: String? = null

    // 连接状态
    private val isConnected = AtomicBoolean(false)
    private val shouldConnect = AtomicBoolean(false)
    private val reconnectAttempts = AtomicInteger(0)

    // 心跳相关
    private var heartbeatJob: kotlinx.coroutines.Job? = null
    private var waitingForPong = AtomicBoolean(false)

    // 连接状态流
    private val _connectionState = kotlinx.coroutines.flow.MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: kotlinx.coroutines.flow.StateFlow<ConnectionState> = _connectionState

    /**
     * 获取Socket事件流
     */
    fun getSocketEvents(): Flow<SocketEvent> = eventListener.socketEvents

    /**
     * 连接到服务器
     * @param userId 用户ID
     */
    fun connect(userId: String) {
        if (userId.isBlank()) {
            Log.e(TAG, "用户ID为空，无法连接")
            return
        }

        currentUserId = userId
        shouldConnect.set(true)

        // 如果已连接，先断开
        if (isConnected.get()) {
            disconnect()
        }

        scope.launch {
            performConnect()
        }
    }

    /**
     * 执行连接操作
     */
    private suspend fun performConnect() {
        if (!shouldConnect.get()) return

        try {
            _connectionState.value = ConnectionState.Connecting
            Log.d(TAG, "正在连接到 ${SocketConfig.SOCKET_URL}...")

            // 创建Socket实例
            val options = IO.Options().apply {
                path = SocketConfig.SOCKET_PATH
                transports = arrayOf("websocket", "polling")
                reconnection = false // 禁用内置重连，使用自定义逻辑
                timeout = SocketConfig.CONNECTION_TIMEOUT
                forceNew = true
            }

            socket = IO.socket(SocketConfig.SOCKET_URL, options)

            // 设置事件监听
            setupSocketListeners()

            // 连接
            socket?.connect()
        } catch (e: Exception) {
            Log.e(TAG, "连接失败", e)
            _connectionState.value = ConnectionState.Error(e.message ?: "连接失败")
            eventListener.emitConnectionError(e.message ?: "连接失败")

            // 尝试重连
            scheduleReconnect()
        }
    }

    /**
     * 设置Socket事件监听器
     */
    private fun setupSocketListeners() {
        socket?.apply {

            // 连接成功
            on(SocketConfig.Events.CONNECT) {
                Log.d(TAG, "连接成功: ${id()}")
                isConnected.set(true)
                reconnectAttempts.set(0)
                _connectionState.value = ConnectionState.Connected
                scope.launch {
                    eventListener.emitConnected(id().toString())
                }

                // 注册用户
                currentUserId?.let { userId ->
                    emit(SocketConfig.Events.REGISTER, userId)
                    Log.d(TAG, "已注册用户: $userId")
                }

                // 启动心跳
                startHeartbeat()
            }

            // 连接错误
            on(SocketConfig.Events.CONNECT_ERROR) { args ->
                val error = args.firstOrNull()?.toString() ?: "连接错误"
                Log.e(TAG, "连接错误: $error")
                _connectionState.value = ConnectionState.Error(error)
                scope.launch {
                    eventListener.emitConnectionError(error)
                }
            }

            // 断开连接
            on(SocketConfig.Events.DISCONNECT) { args ->
                val reason = args.firstOrNull()?.toString() ?: "未知原因"
                Log.d(TAG, "断开连接: $reason")
                handleDisconnect(reason)
            }

            // 新消息
            on(SocketConfig.Events.NEW_MESSAGE) { args ->
                try {
                    val data = args.firstOrNull() as? JSONObject
                    data?.let {
                        val message = parseNewMessage(it)
                        message?.let { msg ->
                            scope.launch {
                                eventListener.emitNewMessage(msg)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "处理新消息失败", e)
                }
            }

            // 消息删除
            on(SocketConfig.Events.MESSAGE_DELETED) { args ->
                try {
                    val data = args.firstOrNull() as? JSONObject
                    data?.let {
                        val event = MessageDeletedEvent(
                            messageId = it.optString("messageId"),
                            conversationId = it.optString("conversationId")
                        )
                        scope.launch {
                            eventListener.emitMessageDeleted(event)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "处理消息删除失败", e)
                }
            }

            // 消息发送失败
            on(SocketConfig.Events.MESSAGE_SEND_FAILED) { args ->
                try {
                    val data = args.firstOrNull() as? JSONObject
                    data?.let {
                        val event = MessageSendFailedEvent(
                            tempMessageId = it.optString("tempMessageId"),
                            conversationId = it.optString("conversationId"),
                            reason = it.optString("reason", "发送失败")
                        )
                        scope.launch {
                            eventListener.emitMessageSendFailed(event)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "处理消息发送失败", e)
                }
            }

            // 消息已读
            on(SocketConfig.Events.MESSAGES_READ) { args ->
                try {
                    val data = args.firstOrNull() as? JSONObject
                    data?.let {
                        val messageIds = it.optJSONArray("messageIds")?.let { arr ->
                            (0 until arr.length()).map { idx -> arr.getString(idx) }
                        } ?: emptyList()
                        val event = MessagesReadEvent(
                            conversationId = it.optString("conversationId"),
                            messageIds = messageIds,
                            readAt = it.optString("readAt")
                        )
                        scope.launch {
                            eventListener.emitMessagesRead(event)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "处理消息已读失败", e)
                }
            }

            // 用户上线
            on(SocketConfig.Events.USER_ONLINE) { args ->
                try {
                    val data = args.firstOrNull() as? JSONObject
                    data?.let {
                        val event = UserOnlineEvent(
                            userId = it.optString("userId"),
                            username = it.optString("username")
                        )
                        scope.launch {
                            eventListener.emitUserOnline(event)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "处理用户上线失败", e)
                }
            }

            // 用户下线
            on(SocketConfig.Events.USER_OFFLINE) { args ->
                try {
                    val data = args.firstOrNull() as? JSONObject
                    data?.let {
                        val event = UserOfflineEvent(
                            userId = it.optString("userId"),
                            username = it.optString("username")
                        )
                        scope.launch {
                            eventListener.emitUserOffline(event)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "处理用户下线失败", e)
                }
            }

            // 心跳响应
            on(SocketConfig.Events.PONG) {
                Log.d(TAG, "收到心跳响应")
                waitingForPong.set(false)
            }
        }
    }

    /**
     * 解析新消息
     */
    private fun parseNewMessage(json: JSONObject): NewMessageEvent? {
        return try {
            NewMessageEvent(
                id = json.optString("id"),
                conversation_id = json.optString("conversation_id"),
                sender_id = json.optString("sender_id"),
                sender_name = json.optString("username").ifBlank { json.optString("sender_name") },
                content = json.optString("content"),
                message_type = json.optString("message_type", "text"),
                file_url = json.optString("file_url").ifBlank { null },
                is_deleted = json.optBoolean("is_deleted", false),
                created_at = json.optString("created_at"),
                expires_at = json.optString("expires_at").ifBlank { null },
                temp_id = json.optString("temp_id").ifBlank { null }
            )
        } catch (e: Exception) {
            Log.e(TAG, "解析新消息失败", e)
            null
        }
    }

    /**
     * 处理断开连接
     */
    private fun handleDisconnect(reason: String) {
        isConnected.set(false)
        stopHeartbeat()
        _connectionState.value = ConnectionState.Disconnected
        scope.launch {
            eventListener.emitDisconnected(reason)
        }

        // 如果不是手动断开，尝试重连
        if (reason != "io client disconnect" && shouldConnect.get()) {
            scheduleReconnect()
        }
    }

    /**
     * 安排重连
     */
    private fun scheduleReconnect() {
        val attempt = reconnectAttempts.get()
        if (attempt >= SocketConfig.MAX_RECONNECT_ATTEMPTS) {
            Log.e(TAG, "达到最大重连次数: ${SocketConfig.MAX_RECONNECT_ATTEMPTS}")
            _connectionState.value = ConnectionState.Error("达到最大重连次数")
            return
        }

        val delay = SocketConfig.calculateReconnectDelay(attempt)
        Log.d(TAG, "将在 ${delay}ms 后重连 (${attempt + 1}/${SocketConfig.MAX_RECONNECT_ATTEMPTS})")

        scope.launch {
            delay(delay)
            if (shouldConnect.get()) {
                reconnectAttempts.incrementAndGet()
                performConnect()
            }
        }
    }

    /**
     * 启动心跳
     */
    private fun startHeartbeat() {
        stopHeartbeat()

        heartbeatJob = scope.launch {
            while (true) {
                if (!isConnected.get()) break

                delay(SocketConfig.HEARTBEAT_INTERVAL)

                if (isConnected.get()) {
                    waitingForPong.set(true)
                    socket?.emit(SocketConfig.Events.PING)
                    Log.d(TAG, "发送心跳")

                    // 等待响应超时检测
                    launch {
                        delay(SocketConfig.HEARTBEAT_TIMEOUT)
                        if (waitingForPong.get()) {
                            Log.w(TAG, "心跳超时，重新连接...")
                            socket?.disconnect()
                        }
                    }
                }
            }
        }
    }

    /**
     * 停止心跳
     */
    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        waitingForPong.set(false)
    }

    /**
     * 手动重连
     */
    fun reconnect() {
        reconnectAttempts.set(0)
        socket?.let {
            if (!isConnected.get()) {
                it.connect()
            }
        }
    }

    /**
     * 断开连接
     */
    fun disconnect() {
        Log.d(TAG, "手动断开连接")
        shouldConnect.set(false)
        stopHeartbeat()
        socket?.disconnect()
        socket = null
        isConnected.set(false)
        _connectionState.value = ConnectionState.Disconnected
    }

    /**
     * 发送事件
     * @param event 事件名称
     * @param args 参数
     * @return 是否发送成功
     */
    fun emit(event: String, vararg args: Any): Boolean {
        return if (isConnected.get()) {
            socket?.emit(event, *args)
            Log.d(TAG, "发送事件: $event")
            true
        } else {
            Log.w(TAG, "无法发送事件 $event: 未连接")
            false
        }
    }

    /**
     * 加入对话房间
     */
    fun joinConversation(conversationId: String) {
        emit(SocketConfig.Events.JOIN_CONVERSATION, conversationId)
    }

    /**
     * 发送消息
     */
    fun sendMessage(messageData: MessageData): Boolean {
        return emit(SocketConfig.Events.SEND_MESSAGE, JSONObject().apply {
            put("conversationId", messageData.conversationId)
            put("senderId", messageData.senderId)
            put("content", messageData.content)
            put("messageType", messageData.messageType)
            messageData.fileUrl?.let { put("fileUrl", it) }
        })
    }

    /**
     * 发送心跳
     */
    fun sendPing() {
        emit(SocketConfig.Events.PING)
    }

    /**
     * 获取连接状态
     */
    fun isSocketConnected(): Boolean = isConnected.get()

    /**
     * 获取重连次数
     */
    fun getReconnectAttempts(): Int = reconnectAttempts.get()

    /**
     * 是否可以重连
     */
    fun canReconnect(): Boolean = reconnectAttempts.get() < SocketConfig.MAX_RECONNECT_ATTEMPTS

    /**
     * 释放资源
     */
    fun release() {
        disconnect()
        eventListener.clear()
    }
}
