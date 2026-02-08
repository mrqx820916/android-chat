# REST API客户端使用指南

## 架构说明

本API客户端严格遵循以下原则：
- **DRY原则**: 所有API端点统一配置，避免重复定义
- **单一职责原则**: 每个类只负责一项核心功能
- **模块化原则**: API按业务领域划分为独立模块

## 目录结构

```
network/
├── ApiResponse.kt              # 统一响应包装类
├── NetworkModule.kt            # Retrofit配置
├── NetworkErrorHandler.kt      # 错误处理器
├── ApiClient.kt                # API客户端统一入口
├── NetworkRepository.kt        # 仓库层（业务调用）
├── api/                        # API接口定义
│   ├── AuthApiService.kt      # 认证API
│   ├── UserApiService.kt      # 用户管理API
│   ├── ChatApiService.kt      # 聊天API
│   ├── SettingsApiService.kt  # 设置API
│   └── UploadApiService.kt    # 上传API
└── model/                      # 数据模型
    ├── User.kt                # 用户相关模型
    ├── Message.kt             # 消息模型
    ├── Conversation.kt        # 对话模型
    ├── Setting.kt             # 设置模型
    ├── Member.kt              # 成员模型
    └── FileUpload.kt          # 上传模型
```

## 快速开始

### 1. 初始化仓库

```kotlin
class AuthViewModel : ViewModel() {
    private var repository: NetworkRepository? = null

    fun initRepository(userId: String) {
        repository = NetworkRepository(userId)
    }
}
```

### 2. 登录示例

```kotlin
viewModelScope.launch {
    val repository = NetworkRepository("") // 登录前userId为空
    when (val result = repository.login(username, password)) {
        is ApiResponse.Success -> {
            val authResponse = result.data
            // 保存用户信息
            saveUserData(authResponse.userId, authResponse.username, authResponse.isAdmin)
            // 初始化带userId的仓库
            initRepository(authResponse.userId)
        }
        is ApiResponse.Error -> {
            // 显示错误信息
            showError(result.message)
        }
        else -> {}
    }
}
```

### 3. 获取对话列表

```kotlin
viewModelScope.launch {
    repository?.let { repo ->
        when (val result = repo.getConversations()) {
            is ApiResponse.Success -> {
                _conversations.value = result.data
            }
            is ApiResponse.Error -> {
                showError(result.message)
            }
            else -> {}
        }
    }
}
```

### 4. 发送消息

```kotlin
viewModelScope.launch {
    repository?.let { repo ->
        when (val result = repo.sendMessage(
            conversationId = conversationId,
            content = "Hello",
            messageType = "text"
        )) {
            is ApiResponse.Success -> {
                // 消息发送成功
            }
            is ApiResponse.Error -> {
                showError(result.message)
            }
            else -> {}
        }
    }
}
```

### 5. 上传文件

```kotlin
viewModelScope.launch {
    repository?.let { repo ->
        val file = File(filePath)
        when (val result = repo.uploadFile(file, userType = "member")) {
            is ApiResponse.Success -> {
                val fileUrl = result.data
                // 使用fileUrl发送消息
                sendMessage(conversationId, null, "image", fileUrl)
            }
            is ApiResponse.Error -> {
                showError(result.message)
            }
            else -> {}
        }
    }
}
```

### 6. 获取成员列表（管理员）

```kotlin
viewModelScope.launch {
    repository?.let { repo ->
        when (val result = repo.getMembers()) {
            is ApiResponse.Success -> {
                _members.value = result.data
            }
            is ApiResponse.Error -> {
                showError(result.message)
            }
            else -> {}
        }
    }
}
```

## API端点列表

### 认证API (AuthApiService)
- `POST /auth/login` - 用户登录
- `POST /auth/register` - 用户注册
- `GET /auth/admin-exists` - 检查管理员是否存在

### 用户管理API (UserApiService)
- `GET /users/members?userId={userId}` - 获取成员列表
- `PUT /users/{userId}/note` - 更新成员备注
- `DELETE /users/{userId}` - 删除成员

### 聊天API (ChatApiService)
- `GET /conversations?userId={userId}` - 获取对话列表
- `POST /conversations/{id}/read` - 标记对话已读
- `GET /messages?conversationId={id}` - 获取对话消息
- `POST /messages` - 发送消息
- `DELETE /messages/{id}` - 删除消息

### 设置API (SettingsApiService)
- `GET /settings/auto-delete?userId={userId}` - 获取自动删除设置
- `PUT /settings/auto-delete?userId={userId}` - 更新自动删除设置

### 上传API (UploadApiService)
- `POST /upload` - 上传文件

## 错误处理

所有API调用都返回 `ApiResponse<T>` 类型：

```kotlin
sealed class ApiResponse<out T> {
    data class Success<T>(val data: T) : ApiResponse<T>()
    data class Error(val code: Int, val message: String, val exception: Throwable? = null) : ApiResponse<Nothing>()
    object Loading : ApiResponse<Nothing>()
}
```

### 错误码说明
- `-1`: 网络连接失败
- `-2`: 连接超时
- `-3`: 网络IO错误
- `400`: 请求参数错误
- `401`: 认证失败
- `403`: 无权限访问
- `404`: 资源不存在
- `500`: 服务器错误

## 重要说明

1. **请求头**: 所有需要身份验证的API请求会自动添加 `x-user-id` 请求头（小写）
2. **HTTPS**: 默认使用HTTPS连接到 `https://chat.soft1688.vip`
3. **超时设置**: 连接、读取、写入超时均为30秒
4. **日志拦截**: Debug模式下会打印完整的请求和响应日志
5. **线程切换**: Repository方法已使用 `withContext(Dispatchers.IO)` 自动切换到IO线程

## 测试建议

1. 使用协程测试框架进行单元测试
2. Mock WebService来模拟API响应
3. 测试各种错误场景（网络异常、超时、服务器错误等）
