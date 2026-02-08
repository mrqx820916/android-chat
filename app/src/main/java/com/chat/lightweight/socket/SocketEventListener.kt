package com.chat.lightweight.socket

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Socket.IO 事件监听器
 * 使用Flow分发Socket事件，线程安全
 */
class SocketEventListener {

    private val _socketEvents = MutableSharedFlow<SocketEvent>(
        replay = 0,
        extraBufferCapacity = 100,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /**
     * Socket事件流
     */
    val socketEvents: Flow<SocketEvent> = _socketEvents.asSharedFlow()

    /**
     * 发出连接成功事件
     */
    suspend fun emitConnected(socketId: String) {
        _socketEvents.emit(SocketEvent.Connected(socketId))
    }

    /**
     * 发出断开连接事件
     */
    suspend fun emitDisconnected(reason: String) {
        _socketEvents.emit(SocketEvent.Disconnected(reason))
    }

    /**
     * 发出连接错误事件
     */
    suspend fun emitConnectionError(error: String) {
        _socketEvents.emit(SocketEvent.ConnectionError(error))
    }

    /**
     * 发出新消息事件
     */
    suspend fun emitNewMessage(message: NewMessageEvent) {
        _socketEvents.emit(SocketEvent.NewMessage(message))
    }

    /**
     * 发出消息删除事件
     */
    suspend fun emitMessageDeleted(event: MessageDeletedEvent) {
        _socketEvents.emit(SocketEvent.MessageDeleted(event))
    }

    /**
     * 发出消息发送失败事件
     */
    suspend fun emitMessageSendFailed(event: MessageSendFailedEvent) {
        _socketEvents.emit(SocketEvent.MessageSendFailed(event))
    }

    /**
     * 发出用户上线事件
     */
    suspend fun emitUserOnline(event: UserOnlineEvent) {
        _socketEvents.emit(SocketEvent.UserOnline(event))
    }

    /**
     * 发出用户下线事件
     */
    suspend fun emitUserOffline(event: UserOfflineEvent) {
        _socketEvents.emit(SocketEvent.UserOffline(event))
    }

    /**
     * 发出心跳响应事件
     */
    suspend fun emitPong() {
        _socketEvents.emit(SocketEvent.Pong)
    }

    /**
     * 清空事件缓冲
     * 注意: MutableSharedFlow不支持reset，此方法保留为接口兼容
     */
    fun clear() {
        // MutableSharedFlow 不支持 reset，可以发出清除事件或保留为空操作
        // 实际使用中，由于我们设置了 replay=0，缓冲会自动清理
    }
}
