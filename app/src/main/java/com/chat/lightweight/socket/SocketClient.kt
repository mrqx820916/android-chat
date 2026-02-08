package com.chat.lightweight.socket

import kotlinx.coroutines.flow.StateFlow

/**
 * Socket.IO 客户端单例
 * 提供全局唯一的Socket连接实例
 */
object SocketClient {

    private var socketManager: SocketManager? = null
    private var eventListener: SocketEventListener? = null

    /**
     * 初始化Socket客户端
     */
    fun init() {
        if (eventListener == null) {
            eventListener = SocketEventListener()
        }
        if (socketManager == null) {
            socketManager = SocketManager(eventListener!!)
        }
    }

    /**
     * 获取SocketManager实例
     */
    fun getManager(): SocketManager {
        ensureInitialized()
        return socketManager!!
    }

    /**
     * 获取事件流
     */
    fun getSocketEvents() = getManager().getSocketEvents()

    /**
     * 获取连接状态流
     */
    fun getConnectionState(): StateFlow<ConnectionState> = getManager().connectionState

    /**
     * 连接到服务器
     */
    fun connect(userId: String) {
        init()
        getManager().connect(userId)
    }

    /**
     * 断开连接
     */
    fun disconnect() {
        socketManager?.disconnect()
    }

    /**
     * 重新连接
     */
    fun reconnect() {
        socketManager?.reconnect()
    }

    /**
     * 加入对话房间
     */
    fun joinConversation(conversationId: String) {
        socketManager?.joinConversation(conversationId)
    }

    /**
     * 发送消息
     */
    fun sendMessage(messageData: MessageData): Boolean {
        return socketManager?.sendMessage(messageData) ?: false
    }

    /**
     * 发送心跳
     */
    fun sendPing() {
        socketManager?.sendPing()
    }

    /**
     * 是否已连接
     */
    fun isConnected(): Boolean = socketManager?.isSocketConnected() ?: false

    /**
     * 获取重连次数
     */
    fun getReconnectAttempts(): Int = socketManager?.getReconnectAttempts() ?: 0

    /**
     * 是否可以重连
     */
    fun canReconnect(): Boolean = socketManager?.canReconnect() ?: false

    /**
     * 释放资源
     */
    fun release() {
        socketManager?.release()
        socketManager = null
        eventListener = null
    }

    /**
     * 确保已初始化
     */
    private fun ensureInitialized() {
        if (socketManager == null) {
            throw IllegalStateException("SocketClient未初始化，请先调用init()")
        }
    }
}
