package com.chat.lightweight.socket

/**
 * Socket.IO 连接配置
 */
object SocketConfig {
    // 服务器地址
    const val SOCKET_URL = "https://chat.soft1688.vip"

    // Socket.IO 路径
    const val SOCKET_PATH = "/socket.io"

    // 最大重连次数
    const val MAX_RECONNECT_ATTEMPTS = 10

    // 基础重连间隔（毫秒）
    const val BASE_RECONNECT_DELAY = 3000L

    // 最大重连间隔（毫秒）
    const val MAX_RECONNECT_DELAY = 30000L

    // 心跳间隔（毫秒）
    const val HEARTBEAT_INTERVAL = 30000L

    // 心跳超时（毫秒）
    const val HEARTBEAT_TIMEOUT = 10000L

    // 连接超时（毫秒）
    const val CONNECTION_TIMEOUT = 10000L

    /**
     * 计算指数退避的重连延迟
     * @param attempt 当前重连次数
     * @return 延迟时间（毫秒）
     */
    fun calculateReconnectDelay(attempt: Int): Long {
        val delay = BASE_RECONNECT_DELAY * (1 shl attempt.coerceAtMost(8)) // 2^attempt，最大2^8
        return delay.coerceAtMost(MAX_RECONNECT_DELAY)
    }

    /**
     * Socket.IO 事件名称
     */
    object Events {
        // 客户端发送事件
        const val REGISTER = "register"
        const val JOIN_CONVERSATION = "join_conversation"
        const val SEND_MESSAGE = "send_message"
        const val PING = "ping"

        // 服务器推送事件
        const val NEW_MESSAGE = "new_message"
        const val MESSAGE_DELETED = "message_deleted"
        const val MESSAGE_SEND_FAILED = "message_send_failed"
        const val USER_ONLINE = "user_online"
        const val USER_OFFLINE = "user_offline"
        const val PONG = "pong"

        // 连接事件
        const val CONNECT = "connect"
        const val DISCONNECT = "disconnect"
        const val CONNECT_ERROR = "connect_error"
    }
}
