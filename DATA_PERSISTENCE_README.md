# Android数据持久化模块使用指南

## 概述

本模块使用Android DataStore实现轻量级数据持久化,支持用户数据、对话列表、消息缓存和应用设置的存储。

## 架构

```
数据层架构:
├── data/
│   ├── model/          # 数据模型
│   │   ├── UserData.kt
│   │   ├── ConversationItem.kt
│   │   ├── MessageItem.kt
│   │   └── CachePolicy.kt
│   ├── local/          # 本地存储
│   │   ├── PreferencesManager.kt  # DataStore封装
│   │   └── FakeDataGenerator.kt   # 测试数据生成
│   ├── repository/     # 数据仓库
│   │   ├── UserRepository.kt
│   │   ├── CacheRepository.kt
│   │   ├── SettingsRepository.kt
│   │   └── RepositoryProvider.kt
│   └── extension/      # 扩展函数
│       ├── ModelExtensions.kt     # JSON转换
│       └── FlowExtensions.kt      # Flow扩展
```

## 使用方法

### 1. 用户数据管理

#### 保存用户数据 (登录后)
```kotlin
val userRepository = RepositoryProvider.getUserRepository(context)

// 方式1: 在ViewModel中使用
viewModelScope.launch {
    val userData = UserData(
        userId = "user_123",
        username = "张三",
        isAdmin = false,
        token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    )
    userRepository.saveUserData(userData)
}
```

#### 读取用户数据
```kotlin
// 方式1: 使用Flow监听 (推荐)
userRepository.getUserDataFlow().collect { userData ->
    if (userData != null) {
        // 用户已登录
        println("用户名: ${userData.username}")
        println("是否管理员: ${userData.isAdministrator()}")
    } else {
        // 用户未登录
    }
}

// 方式2: 检查登录状态
userRepository.isLoggedInFlow().collect { isLoggedIn ->
    if (isLoggedIn) {
        // 已登录
    } else {
        // 未登录,跳转到登录页
    }
}

// 方式3: 同步获取 (仅在协程中)
val userData = userRepository.getUserData()
```

#### 登出 (清除用户数据)
```kotlin
viewModelScope.launch {
    userRepository.logout()
    // 用户数据已清除,下次启动应用需要重新登录
}
```

### 2. 对话列表缓存

#### 保存对话列表
```kotlin
val cacheRepository = RepositoryProvider.getCacheRepository(context)

viewModelScope.launch {
    val conversations = listOf(
        ConversationItem(
            id = "conv_1",
            userId = "user_1",
            username = "张三",
            unreadCount = 2,
            lastMessage = "你好",
            lastMessageTime = System.currentTimeMillis()
        ),
        // 更多对话...
    )
    cacheRepository.saveConversationsToCache(conversations)
}
```

#### 读取对话缓存
```kotlin
cacheRepository.getCachedConversations().collect { conversations ->
    if (conversations != null) {
        // 缓存有效,显示对话列表
        updateUI(conversations)
    } else {
        // 缓存无效或不存在,从服务器加载
        loadFromServer()
    }
}
```

#### 清除对话缓存
```kotlin
viewModelScope.launch {
    cacheRepository.clearConversationsCache()
}
```

### 3. 消息缓存

#### 保存消息
```kotlin
viewModelScope.launch {
    val messages = listOf(
        MessageItem(
            id = "msg_1",
            conversationId = "conv_1",
            senderId = "user_1",
            senderName = "张三",
            content = "你好",
            timestamp = System.currentTimeMillis(),
            isFromSelf = false
        ),
        // 更多消息...
    )
    cacheRepository.saveMessagesToCache("conv_1", messages)
}
```

#### 读取消息缓存
```kotlin
cacheRepository.getCachedMessages("conv_1").collect { messages ->
    if (messages != null) {
        // 缓存有效,显示消息列表
        updateUI(messages)
    } else {
        // 缓存无效或不存在,从服务器加载
        loadFromServer("conv_1")
    }
}
```

### 4. 应用设置

#### 自动删除设置
```kotlin
val settingsRepository = RepositoryProvider.getSettingsRepository(context)

// 保存设置
viewModelScope.launch {
    settingsRepository.saveAutoDeleteSetting("5min")  // 5分钟后自动删除
}

// 读取设置
settingsRepository.getAutoDeleteSettingFlow().collect { value ->
    // value: "off", "1min", "5min", "30min", "1hour", "24hours"
    updateUI(value)
}
```

#### 通知设置
```kotlin
// 保存设置
viewModelScope.launch {
    settingsRepository.saveNotificationSetting(true)
}

// 读取设置
settingsRepository.getNotificationSettingFlow().collect { enabled ->
    if (enabled) {
        // 启用通知
    } else {
        // 禁用通知
    }
}
```

### 5. JSON数据转换

#### JSON字符串转对象
```kotlin
import com.chat.lightweight.data.extension.*

// JSON转UserData
val json = """{"id":"user_123","username":"张三","is_admin":false,"token":"token_xxx"}"""
val userData = json.toUserData()

// JSON转ConversationItem列表
val conversationsJson = """[{"id":"conv_1","username":"张三",...}]"""
val conversations = conversationsJson.toConversationList()

// JSON转MessageItem列表
val messagesJson = """[{"id":"msg_1","content":"你好",...}]"""
val messages = messagesJson.toMessageList()
```

#### 对象转JSON字符串
```kotlin
// UserData转JSON
val userData = UserData("user_123", "张三", false, "token_xxx")
val json = userData.toJson()

// ConversationItem列表转JSON
val conversations = listOf(ConversationItem(...))
val json = conversations.toJson()

// MessageItem列表转JSON
val messages = listOf(MessageItem(...))
val json = messages.toJson()
```

## 缓存策略

默认缓存策略:
- 对话列表: 5分钟
- 消息缓存: 10分钟
- 最大缓存条目: 100

自定义缓存策略:
```kotlin
// 修改CacheRepository中的cachePolicy
private val cachePolicy = CachePolicy(
    conversationsCacheDuration = 10 * 60 * 1000,  // 10分钟
    messagesCacheDuration = 15 * 60 * 1000,       // 15分钟
    maxCacheSize = 200,                             // 最多200条
    enableConversationCache = true,
    enableMessageCache = true
)
```

## 数据验证

### 检查缓存是否有效
```kotlin
viewModelScope.launch {
    val isValid = cacheRepository.isConversationsCacheValid()
    if (isValid) {
        // 缓存有效
    } else {
        // 缓存已过期
    }
}
```

## 完整示例

### ViewModel中使用
```kotlin
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository = RepositoryProvider.getUserRepository(application)
    private val cacheRepository = RepositoryProvider.getCacheRepository(application)

    private val _userData = MutableStateFlow<UserData?>(null)
    val userData: StateFlow<UserData?> = _userData.asStateFlow()

    init {
        // 自动加载用户数据
        viewModelScope.launch {
            userRepository.getUserDataFlow().collect { userData ->
                _userData.value = userData
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            // 清除用户数据
            userRepository.logout()
            // 清除所有缓存
            cacheRepository.clearAllCache()
        }
    }
}
```

### Activity/Fragment中使用
```kotlin
class MainActivity : AppCompatActivity() {

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 监听登录状态
        lifecycleScope.launch {
            viewModel.isLoggedIn.collect { isLoggedIn ->
                if (isLoggedIn) {
                    // 已登录,显示主页
                } else {
                    // 未登录,显示登录页
                }
            }
        }

        // 监听用户数据
        lifecycleScope.launch {
            viewModel.userData.collect { userData ->
                userData?.let {
                    // 更新UI
                    updateUI(it)
                }
            }
        }
    }
}
```

## 测试数据生成

使用FakeDataGenerator生成测试数据:
```kotlin
import com.chat.lightweight.data.local.FakeDataGenerator

// 生成假对话列表
val fakeConversations = FakeDataGenerator.generateFakeConversations(10)

// 生成假消息列表
val fakeMessages = FakeDataGenerator.generateFakeMessages("conv_1", 20)

// 生成单个假对话
val fakeConversation = FakeDataGenerator.generateFakeConversation(
    userId = "user_1",
    username = "张三",
    unreadCount = 5
)
```

## 注意事项

1. **线程安全**: 所有数据操作都是线程安全的,可以在任何线程调用
2. **协程作用域**: suspend函数需要在协程作用域中调用
3. **Flow收集**: Flow数据需要使用collect()方法收集
4. **内存泄漏**: 在Activity/Fragment中收集Flow时,注意使用lifecycleScope或viewModelScope
5. **数据清除**: 登出时记得清除用户数据和缓存

## 自动登录实现

在Application或启动Activity中检查登录状态:
```kotlin
class MainActivity : AppCompatActivity() {

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 检查登录状态
        viewModel.isLoggedIn.collect { isLoggedIn ->
            if (isLoggedIn) {
                // 已登录,直接进入主页
                navigateToHome()
            } else {
                // 未登录,显示登录页
                showLoginScreen()
            }
        }
    }
}
```

## 依赖项

已在build.gradle中添加:
```gradle
// DataStore
implementation 'androidx.datastore:datastore-preferences:1.0.0'

// Gson用于JSON解析
implementation 'com.google.code.gson:gson:2.10.1'

// Kotlin协程
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
```

## 文件清单

- `data/model/UserData.kt` - 用户数据模型
- `data/model/ConversationItem.kt` - 对话项模型
- `data/model/MessageItem.kt` - 消息项模型
- `data/model/CachePolicy.kt` - 缓存策略配置
- `data/local/PreferencesManager.kt` - DataStore封装
- `data/local/FakeDataGenerator.kt` - 测试数据生成器
- `data/repository/UserRepository.kt` - 用户数据仓库
- `data/repository/CacheRepository.kt` - 缓存仓库
- `data/repository/SettingsRepository.kt` - 设置仓库
- `data/repository/RepositoryProvider.kt` - 仓库提供者
- `data/extension/ModelExtensions.kt` - 数据模型扩展函数
- `data/extension/FlowExtensions.kt` - Flow扩展函数
- `ChatApplication.kt` - Application类
- `ui/viewmodel/AuthViewModel.kt` - 认证ViewModel示例

## 验收标准

✅ 用户数据正确持久化
✅ 重启应用后自动登录
✅ 数据转换正确
✅ 缓存策略工作正常
✅ 设置正确保存和读取
✅ 登出功能正常
✅ 错误处理完善
