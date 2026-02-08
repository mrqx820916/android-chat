# Android项目架构升级总结

## 项目信息
- **项目名称**: 轻聊 (Lightweight Chat)
- **包名**: com.chat.lightweight
- **语言**: Kotlin 1.9.20
- **架构模式**: MVVM + Clean Architecture
- **minSdk**: 33 (Android 13)
- **targetSdk**: 34 (Android 14)

## 项目结构

```
app/src/main/java/com/chat/lightweight/
├── LightweightChatApp.kt          # Application类
├── presentation/                  # 表现层
│   ├── ui/
│   │   ├── base/                 # 基础UI类
│   │   │   ├── BaseActivity.kt
│   │   │   ├── BaseFragment.kt
│   │   │   └── BaseAdapter.kt
│   │   └── MainActivity.kt       # 主Activity (WebView容器)
│   └── viewmodel/                # ViewModel
│       ├── AuthViewModel.kt
│       └── ChatViewModel.kt
├── domain/                       # 领域层
│   ├── model/                    # 数据模型
│   │   ├── User.kt
│   │   ├── Message.kt
│   │   ├── Conversation.kt
│   │   └── ApiResponse.kt
│   ├── repository/               # 仓库接口
│   │   └── ChatRepository.kt
│   └── usecase/                  # 用例
│       ├── LoginUseCase.kt
│       ├── RegisterUseCase.kt
│       ├── SendMessageUseCase.kt
│       ├── GetConversationsUseCase.kt
│       └── GetMessagesUseCase.kt
├── data/                         # 数据层
│   ├── remote/                   # 远程数据源
│   │   └── api/
│   │       ├── ChatApiService.kt     # Retrofit API
│   │       └── SocketManager.kt      # Socket.IO管理
│   ├── local/                    # 本地数据源
│   │   └── DataStoreManager.kt
│   └── repository/               # 仓库实现
│       └── ChatRepositoryImpl.kt
└── di/                           # 依赖注入
    └── AppModule.kt              # 手动DI模块
```

## 技术栈

### 核心依赖
- **Kotlin**: 1.9.20
- **Coroutines**: 1.7.3
- **Material Design 3**: 1.11.0

### 生命周期组件
- **Lifecycle ViewModel**: 2.7.0
- **Lifecycle LiveData**: 2.7.0
- **Lifecycle Runtime**: 2.7.0
- **Activity KTX**: 1.8.2
- **Fragment KTX**: 1.6.2

### 网络通信
- **Retrofit**: 2.9.0
- **OkHttp**: 4.12.0
- **Socket.IO Client**: 2.1.0

### 数据处理
- **Gson**: 2.10.1
- **DataStore Preferences**: 1.0.0

### UI组件
- **RecyclerView**: 1.3.2
- **SwipeRefreshLayout**: 1.1.0
- **Coil**: 2.5.0 (图片加载)
- **WebView**: androidx.webkit 1.10.0

## BuildConfig配置

支持调试/生产环境切换：

```kotlin
// 在代码中使用
BuildConfig.BASE_URL        // https://chat.soft1688.vip
BuildConfig.SOCKET_URL      // https://chat.soft1688.vip
BuildConfig.DEBUG_MODE      // true/false
```

## 编译项目

### Windows环境
```bash
cd K:\AICODE\chat\android-chat
gradlew.bat clean assembleDebug
```

### 构建输出
- **Debug APK**: `app/build/outputs/apk/debug/app-debug.apk`
- **Release APK**: `app/build/outputs/apk/release/app-release.apk`

## 架构特点

### 1. Clean Architecture分层
- **Presentation层**: UI + ViewModel
- **Domain层**: 业务逻辑 + 用例
- **Data层**: 数据获取 + 存储

### 2. MVVM模式
- **Model**: 数据模型和仓库
- **View**: Activity/Fragment
- **ViewModel**: 业务逻辑处理

### 3. 依赖注入
采用手动DI方式,避免引入Hilt/Dagger的复杂性:
- `AppModule` 单例管理所有依赖
- 构造函数注入
- 编译时安全

### 4. 响应式编程
- **Kotlin Flow**: 数据流处理
- **StateFlow**: UI状态管理
- **SharedFlow**: 事件分发

### 5. 协程支持
- **ViewModelScope**: ViewModel生命周期
- **生命周期感知**: 自动取消

## 网络通信

### REST API (Retrofit)
```kotlin
interface ChatApiService {
    @POST("auth/login")
    suspend fun login(@Body request: AuthRequest): ApiResponse<AuthResponse>

    @GET("messages")
    suspend fun getMessages(@Query("conversationId") id: String): ApiResponse<List<Message>>
}
```

### WebSocket (Socket.IO)
```kotlin
class SocketManager {
    fun connect(userId: String)
    fun disconnect()
    fun subscribeToMessages(): Flow<Message>
}
```

## 本地存储

使用DataStore Preferences:
```kotlin
class DataStoreManager(context: Context) {
    suspend fun saveUserId(userId: String)
    fun getUserId(): Flow<String?>
}
```

## 状态管理

### UiState模式
```kotlin
data class AuthUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val user: AuthResponse? = null
)
```

## 项目亮点

1. **现代Android开发**: 使用最新的Android Jetpack组件
2. **Clean Architecture**: 清晰的分层架构
3. **类型安全**: Kotlin空安全和类型推断
4. **响应式**: Flow协程实现异步编程
5. **可测试**: 用例模式便于单元测试
6. **可维护**: 模块化设计,职责单一
7. **可扩展**: 接口抽象,易于扩展功能

## 下一步工作

1. 完善UI界面实现
2. 添加单元测试
3. 实现图片选择和上传功能
4. 实现语音录制和发送功能
5. 添加推送通知支持
6. 优化性能和内存管理
7. 实现离线消息缓存

## 验收标准

✅ 项目成功编译
✅ 所有依赖正确解析
✅ 目录结构符合Clean Architecture规范
✅ MVVM架构完整实现
✅ BuildConfig环境配置完成
✅ Application类注册完成
