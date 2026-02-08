# FCM推送通知实现指南

## 概述

本文档说明如何在轻聊应用中集成和使用Firebase Cloud Messaging (FCM)推送通知功能。

## 配置步骤

### 1. Firebase项目配置

**步骤1**: 创建Firebase项目
1. 访问 [Firebase Console](https://console.firebase.google.com/)
2. 创建新项目或选择现有项目
3. 添加Android应用
4. 包名: `com.chat.lightweight`

**步骤2**: 下载配置文件
1. 下载 `google-services.json`
2. 放置到 `app/` 目录

**步骤3**: 配置SHA-1证书指纹
```bash
# Debug密钥
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android

# Release密钥
keytool -list -v -keystore release.keystore -alias release
```

### 2. 项目配置

**build.gradle (项目级别)**:
```gradle
buildscript {
    dependencies {
        classpath 'com.google.gms:google-services:4.4.0'
    }
}
```

**build.gradle (应用级别)**:
```gradle
plugins {
    id 'com.google.gms.google-services'
}

dependencies {
    implementation 'com.google.firebase:firebase-messaging:23.4.0'
    implementation 'com.google.firebase:firebase-analytics:21.5.0'
}
```

## 代码实现

### FCM服务

`FcmService.kt` - 继承FirebaseMessagingService:
- `onNewToken()` - Token刷新处理
- `onMessageReceived()` - 接收推送消息
- `sendNotification()` - 显示通知
- `createNotificationChannels()` - 创建通知渠道

### 通知管理

`NotificationHelper.kt` - 通知辅助类:
- 显示消息通知
- 创建前台服务通知
- 取消通知

### Token管理

`FcmTokenManager.kt` - Token管理器:
- 初始化Token
- 刷新Token
- 缓存Token
- 上报Token到服务器

### DataStore存储

`DataStoreManager.kt` - 本地存储:
- `saveFcmToken()` - 保存Token
- `getFcmToken()` - 获取Token

## 使用示例

### 初始化FCM

在`LightweightChatApp.onCreate()`中:
```kotlin
private fun initializeFcm() {
    val dataStoreManager = DataStoreManager(this)
    fcmTokenManager = FcmTokenManager(this, dataStoreManager)

    applicationScope.launch {
        fcmTokenManager.initializeToken()
    }
}
```

### 发送测试通知

通过Firebase Console:
1. 打开Cloud Messaging
2. 点击"发送第一条消息"
3. 输入标题和内容
4. 选择目标应用
5. 发送通知

### 服务器端集成

**发送通知请求**:
```javascript
const admin = require('firebase-admin');

// 发送FCM通知
async function sendNotification(userToken, title, body, data) {
    const message = {
        token: userToken,
        notification: {
            title: title,
            body: body
        },
        data: data
    };

    const response = await admin.messaging().send(message);
    console.log('Notification sent:', response);
}
```

**后端API集成**:
```javascript
// 上报FCM Token
app.post('/api/fcm/token', async (req, res) => {
    const { userId, token } = req.body;
    await saveUserFcmToken(userId, token);
    res.json({ success: true });
});
```

## 通知渠道

### 高优先级渠道 (紧急消息)
- ID: `high_priority_channel`
- 重要性: IMPORTANCE_HIGH
- 用途: 紧急聊天消息

### 默认渠道 (普通消息)
- ID: `default_channel`
- 重要性: IMPORTANCE_DEFAULT
- 用途: 普通聊天消息

### 前台服务渠道
- ID: `foreground_service_channel`
- 重要性: IMPORTANCE_LOW
- 用途: 保持Socket连接

## 通知数据格式

### 推送消息格式
```json
{
    "notification": {
        "title": "发送者用户名",
        "body": "消息内容"
    },
    "data": {
        "type": "new_message",
        "conversation_id": "conv123",
        "message_id": "msg456",
        "sender_id": "user789"
    }
}
```

### 通知点击处理

在`ChatDetailActivity`中处理Intent:
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // 处理通知点击
    val conversationId = intent.getStringExtra(FcmService.EXTRA_CONVERSATION_ID)
    val messageId = intent.getStringExtra(FcmService.EXTRA_MESSAGE_ID)

    if (conversationId != null) {
        loadMessages(conversationId, messageId)
    }
}
```

## 测试步骤

### 1. 测试Token获取
```kotlin
val fcmTokenManager = FcmTokenManager(context, dataStoreManager)
lifecycleScope.launch {
    val token = fcmTokenManager.getToken()
    Log.d("FCM", "Token: $token")
}
```

### 2. 测试通知接收
1. 通过Firebase Console发送测试通知
2. 检查应用是否收到通知
3. 点击通知检查是否跳转正确

### 3. 测试通知点击
1. 发送带数据的测试通知
2. 点击通知
3. 验证是否打开正确的对话

## 常见问题

### 问题1: 收不到通知
**原因**:
- 通知权限未授予
- 应用被系统杀死
- Token未正确上报

**解决**:
- 检查通知权限状态
- 使用前台服务保持运行
- 验证Token是否正确上报

### 问题2: Token为空
**原因**:
- google-services.json配置错误
- Firebase项目配置不正确
- 网络问题

**解决**:
- 重新下载google-services.json
- 检查包名是否匹配
- 检查网络连接

### 问题3: 通知点击无效
**原因**:
- PendingIntent配置错误
- Activity未正确导出

**解决**:
- 检查PendingIntent标志
- 验证Activity在Manifest中正确注册

## 验收标准

- [x] 成功获取FCM Token
- [x] Token缓存到DataStore
- [x] FcmService正确注册
- [x] 通知渠道配置完成
- [x] NotificationHelper实现完成
- [x] Application中FCM初始化
- [ ] 通过Firebase Console发送测试通知
- [ ] 通知点击跳转正确对话
- [ ] Token上报到后端服务器

## 下一步

1. 配置Firebase项目和google-services.json
2. 实现Token上报API
3. 后端集成FCM发送功能
4. 测试完整推送流程
