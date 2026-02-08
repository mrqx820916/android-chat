# Socket.IO 客户端使用说明

## 概述

本模块提供完整的Socket.IO客户端连接管理功能，包括：
- 自动重连（最多10次，指数退避）
- 心跳检测（30秒间隔）
- 连接状态管理
- 线程安全的事件处理

## 项目结构

```
com.chat.lightweight.socket/
├── SocketConfig.kt           # Socket配置常量
├── SocketEventModels.kt      # 事件数据模型
├── SocketEventListener.kt    # 事件监听器（Flow）
├── SocketManager.kt          # Socket连接管理器
├── SocketClient.kt           # Socket客户端单例
└── SocketExtensions.kt       # Kotlin扩展函数
```

## 快速开始

### 1. 初始化（在Application中）

```kotlin
class ChatApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SocketClient.init()
    }
}
```

### 2. 连接到服务器

```kotlin
val userId = "user_123"
SocketClient.connect(userId)
```

### 3. 监听连接状态

```kotlin
lifecycleScope.launch {
    SocketClient.getConnectionState().collect { state ->
        when (state) {
            is ConnectionState.Connected -> {
                // 连接成功
                Log.d("Socket", "已连接: ${state.socketId}")
            }
            is ConnectionState.Connecting -> {
                // 连接中
                showConnectingUI()
            }
            is ConnectionState.Disconnected -> {
                // 已断开
                showDisconnectedUI(state.reason)
            }
            is ConnectionState.Error -> {
                // 连接错误
                showError(state.message)
            }
        }
    }
}
```

### 4. 监听Socket事件

```kotlin
lifecycleScope.launch {
    SocketClient.getSocketEvents().collect { event ->
        when (event) {
            is SocketEvent.NewMessage -> {
                // 处理新消息
                onNewMessage(event.message)
            }
            is SocketEvent.MessageDeleted -> {
                // 处理消息删除
                onMessageDeleted(event.event)
            }
            is SocketEvent.UserOnline -> {
                // 处理用户上线
                onUserOnline(event.event)
            }
            // ... 其他事件
        }
    }
}
```

### 5. 发送消息

```kotlin
val messageData = MessageData(
    conversationId = "conv_123",
    senderId = "user_123",
    content = "Hello!",
    messageType = "text"
)
SocketClient.sendMessage(messageData)
```

## 在ViewModel中使用

```kotlin
class ChatViewModel : ViewModel() {

    init {
        // 监听连接状态
        observeConnectionState(
            onConnected = { socketId ->
                _uiState.update { it.copy(isConnected = true) }
            },
            onError = { error ->
                _uiState.update { it.copy(error = error) }
            }
        )

        // 监听Socket事件
        observeSocketEvents(
            onNewMessage = { message ->
                handleNewMessage(message)
            }
        )
    }

    fun connect(userId: String) {
        SocketClient.connect(userId)
    }

    fun sendMessage(conversationId: String, senderId: String, content: String) {
        SocketClient.sendMessage(MessageData(
            conversationId = conversationId,
            senderId = senderId,
            content = content
        ))
    }
}
```

## 配置参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| SOCKET_URL | https://chat.soft1688.vip | Socket服务器地址 |
| MAX_RECONNECT_ATTEMPTS | 10 | 最大重连次数 |
| BASE_RECONNECT_DELAY | 3000ms | 基础重连延迟 |
| HEARTBEAT_INTERVAL | 30000ms | 心跳间隔 |
| HEARTBEAT_TIMEOUT | 10000ms | 心跳超时 |

## 支持的事件

### 客户端发送事件
- `register(userId)` - 注册用户ID
- `join_conversation(conversationId)` - 加入对话房间
- `send_message(messageData)` - 发送消息
- `ping` - 心跳检测

### 服务器推送事件
- `new_message` - 新消息
- `message_deleted` - 消息删除
- `message_send_failed` - 消息发送失败
- `user_online` - 用户上线
- `user_offline` - 用户下线
- `pong` - 心跳响应

## 注意事项

1. **线程安全**: 所有Socket操作都是线程安全的
2. **自动重连**: 连接断开后会自动尝试重连，最多10次
3. **心跳检测**: 每30秒发送一次心跳，10秒未响应则重连
4. **生命周期**: 在Application中初始化，在应用退出时释放资源
5. **HTTPS**: 支持HTTPS连接，自动处理SSL证书

## 故障排查

### 连接失败
1. 检查网络连接
2. 确认服务器地址正确
3. 检查防火墙设置

### 重连失败
1. 检查重连次数是否达到上限
2. 确认服务器状态
3. 可以调用 `SocketClient.reconnect()` 手动重连

### 心跳超时
1. 检查网络延迟
2. 确认服务器负载
3. 可能需要调整心跳参数
