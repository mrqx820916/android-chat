# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

轻聊 (Lightweight Chat) Android 客户端 - 聊天平台的 Android 应用。使用 WebView 加载 https://chat.soft1688.vip，同时提供原生功能（语音录制、文件上传、推送通知等）。

- **包名**: `com.chat.lightweight`
- **语言**: Kotlin 1.9.20
- **架构**: MVVM + Clean Architecture
- **minSdk**: 33 (Android 13)
- **targetSdk**: 34 (Android 14)

## 构建命令

### Windows 环境
```bash
# 构建 Debug APK
gradlew.bat assembleDebug

# 构建 Release APK
gradlew.bat assembleRelease

# 清理构建
gradlew.bat clean

# 安装到设备
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 构建输出
- **Debug APK**: `app/build/outputs/apk/debug/app-debug.apk`
- **Release APK**: `app/build/outputs/apk/release/app-release.apk`

## 服务器配置

```kotlin
// 在代码中使用 BuildConfig
BuildConfig.BASE_URL        // https://chat.soft1688.vip
BuildConfig.SOCKET_URL      // https://chat.soft1688.vip
BuildConfig.DEBUG_MODE      // true (debug) / false (release)
```

## 架构概览

### 分层结构

```
app/src/main/java/com/chat/lightweight/
├── presentation/           # 表现层
│   ├── ui/                # Activity/Fragment
│   └── viewmodel/         # ViewModel
├── domain/                 # 领域层
│   ├── model/             # 领域模型
│   ├── repository/        # 仓库接口
│   └── usecase/           # 用例
├── data/                   # 数据层
│   ├── remote/            # 远程数据源 (API/Socket)
│   ├── local/             # 本地数据源 (DataStore)
│   └── repository/        # 仓库实现
├── socket/                 # Socket.IO 客户端
├── network/                # Retrofit API 客户端
├── media/                  # 媒体处理 (录音/播放/上传)
├── service/                # 后台服务 (保活/心跳)
└── di/                     # 手动依赖注入
```

### Socket.IO 事件

**客户端 → 服务器**:
- `register(userId)` - 注册用户
- `join_conversation(conversationId)` - 加入对话房间
- `send_message(messageData)` - 发送消息
- `ping` - 心跳

**服务器 → 客户端**:
- `new_message` - 新消息
- `message_deleted` - 消息删除
- `message_send_failed` - 发送失败
- `user_online` - 用户上线
- `user_offline` - 用户下线

### 关键组件

**SocketClient** (`socket/SocketClient.kt`):
- 单例 Socket.IO 客户端
- 自动重连（最多10次，指数退避）
- 心跳检测（30秒间隔）
- 提供 StateFlow 监听连接状态和事件

```kotlin
SocketClient.init()                              // 在 Application 中初始化
SocketClient.connect(userId)                     // 连接服务器
SocketClient.getSocketEvents().collect { event -> }  // 监听事件
SocketClient.getConnectionState().collect { state -> }  // 监听连接状态
```

**MessageItem** (`data/model/MessageItem.kt`):
- UI 层消息模型
- `fromSocketEvent(event, currentUserId)` - 从 Socket 事件转换
- `fromDomainMessage(message, currentUserId)` - 从领域模型转换

**ChatDetailViewModel** (`ui/chat/viewmodel/ChatDetailViewModel.kt`):
- 聊天详情 ViewModel
- 乐观更新：发送时立即创建临时消息，Socket 回调后更新状态
- `currentUserId` 从 DataStore 获取，用于区分发送者

## 语音消息处理

语音时长存储格式：
- **服务器存储**: 毫秒数（如 `65000`）
- **UI 显示**: 格式化时长（如 `1:05`）

转换逻辑在 `MessageItem.fromSocketEvent()` 和 `fromDomainMessage()` 中：
```kotlin
val displayContent = if (messageType == TYPE_VOICE) {
    val durationMs = content.toLongOrNull() ?: 0L
    MediaUtils.formatDuration(durationMs)  // "分:秒" 格式
} else {
    content
}
```

## 后台保活

**SocketForegroundService** - 前台服务保持 Socket 连接
**HeartbeatWorker** - WorkManager 定期心跳（15分钟间隔）
**NetworkStateMonitor** - 网络状态监听，自动重连

## 媒体功能

**VoiceRecorderHelper** - 语音录制
- 最短1秒，最长60秒
- 自动保存到外部存储

**VoicePlayerHelper** - 语音播放
- 播放/暂停控制
- 播放状态 Flow

**FileUploadManager** - 文件上传
- 支持图片/语音
- 实时进度显示

## Git 仓库

这是聊天平台项目的 Android 客户端，作为 Git 子模块存在：
- **仓库**: `git@github.com:mrqx820916/android-chat.git`
- **父仓库**: `git@github.com:mrqx820916/lightweight-chat.git`

使用 SSH 推送：
```bash
git remote set-url origin git@github.com:mrqx820916/android-chat.git
git push origin master
```

## 常见问题

**语音消息显示毫秒数而非格式化时长**
- 检查 `fromSocketEvent()` 是否使用 `MediaUtils.formatDuration()`

**点击自己发送的语音无法播放**
- 检查 `fromSocketEvent(event, currentUserId)` 第二个参数是否传递
- `isFromSelf = event.sender_id == currentUserId` 必须正确判断

**Socket 连接失败**
- 检查 `BuildConfig.SOCKET_URL`
- 检查网络权限 `ACCESS_NETWORK_STATE`
- 查看连接状态 Flow 中的错误信息
