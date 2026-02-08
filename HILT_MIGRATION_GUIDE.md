# Hilt 依赖注入迁移指南

## 一、当前状态分析

### 1.1 问题诊断

**当前依赖管理方式**:
```kotlin
// ❌ 问题代码示例
private val viewModel: ChatDetailViewModel by viewModels {
    ViewModelFactory {
        ChatDetailViewModel(
            getMessagesUseCase = GetMessagesUseCase(AppModule.chatRepository),
            sendMessageUseCase = SendMessageUseCase(AppModule.chatRepository),
            markConversationReadUseCase = MarkConversationReadUseCase(AppModule.chatRepository),
            repository = AppModule.chatRepository  // 单例依赖
        )
    }
}
```

**存在的问题**:
1. **违反依赖注入原则**: Activity 手动创建依赖
2. **测试困难**: 无法 mock 依赖进行单元测试
3. **生命周期混乱**: Repository 单例可能导致内存泄漏
4. **代码重复**: 每个 Activity 都要创建 ViewModelFactory
5. **编译时检查缺失**: 运行时才发现依赖错误

### 1.2 影响范围

**需要重构的文件**:
- `ChatDetailActivity.kt`
- `ConversationListActivity.kt`
- `LoginActivity.kt`
- `RegisterActivity.kt`
- `MainActivity.kt`
- `ViewModelFactory.kt` (可删除)
- `AppModule.kt` (需要改造)

---

## 二、Hilt 优势

### 2.1 为什么选择 Hilt?

1. **编译时验证**: 编译时检查依赖图
2. **Android 集成**: 自动管理 Android 组件生命周期
3. **ViewModel 支持**: 原生支持 ViewModel 创建
4. **类型安全**: 编译时类型检查
5. **减少样板代码**: 自动生成依赖注入代码
6. **测试友好**: 轻松替换依赖进行测试

### 2.2 Hilt 架构

```
@HiltAndroidApp (Application)
    ↓
@AndroidEntryPoint (Activity/Fragment)
    ↓
@Inject (ViewModel/Repository/UseCase)
    ↓
@Module (提供依赖的模块)
```

---

## 三、迁移步骤

### 步骤 1: 添加依赖

**build.gradle (Project)**:
```gradle
buildscript {
    ext {
        hilt_version = '2.48'
    }
    dependencies {
        classpath "com.google.dagger:hilt-android-gradle-plugin:$hilt_version"
    }
}
```

**build.gradle (Module)**:
```gradle
plugins {
    id 'kotlin-kapt'
    id 'dagger.hilt.android.plugin'
}

dependencies {
    implementation "com.google.dagger:hilt-android:$hilt_version"
    kapt "com.google.dagger:hilt-compiler:$hilt_version"

    // 对于 ViewModel
    implementation "androidx.hilt:hilt-lifecycle-viewmodel:1.0.0-alpha03"
    kapt "androidx.hilt:hilt-compiler:1.0.0-alpha03"
}
```

### 步骤 2: 配置 Application

**修改 `LightweightChatApp.kt`**:
```kotlin
@HiltAndroidApp
class LightweightChatApp : Application() {

    companion object {
        lateinit var instance: LightweightChatApp
            private set

        lateinit var imageLoader: ImageLoader
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 初始化 Coil 图片加载器 (Hilt 会注入)
        imageLoader = CoilImageLoadConfigOptimized.createImageLoader(this)
    }
}
```

### 步骤 3: 创建 Hilt 模块

**创建 `di/AppModule.kt`**:
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // 提供 Application Context
    @Provides
    @Singleton
    fun provideApplicationContext(@ApplicationContext context: Context): Context {
        return context
    }

    // 提供 OkHttpClient
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            })
            .build()
    }

    // 提供 Retrofit
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // 提供 ImageLoader
    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient
    ): ImageLoader {
        return CoilImageLoadConfigOptimized.createImageLoader(
            context = context,
            isDebug = BuildConfig.DEBUG,
            okHttpClient = okHttpClient
        )
    }

    // 提供 DataStore
    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.createDataStore(name = "chat_preferences")
    }

    // 提供 WorkManager
    @Provides
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }
}
```

**创建 `di/NetworkModule.kt`**:
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // 提供 API 服务
    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideChatApiService(retrofit: Retrofit): ChatApiService {
        return retrofit.create(ChatApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideUploadApiService(retrofit: Retrofit): UploadApiService {
        return retrofit.create(UploadApiService::class.java)
    }
}
```

**创建 `di/RepositoryModule.kt`**:
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    // 提供 ChatRepository
    @Provides
    @Singleton
    fun provideChatRepository(
        authApiService: AuthApiService,
        chatApiService: ChatApiService,
        dataStore: DataStore<Preferences>
    ): ChatRepository {
        return ChatRepositoryImpl(
            authApiService = authApiService,
            chatApiService = chatApiService,
            dataStore = dataStore
        )
    }

    // 提供 UserRepository
    @Provides
    @Singleton
    fun provideUserRepository(
        authApiService: AuthApiService,
        dataStore: DataStore<Preferences>
    ): UserRepository {
        return UserRepositoryImpl(
            authApiService = authApiService,
            dataStore = dataStore
        )
    }

    // 提供 SettingsRepository
    @Provides
    @Singleton
    fun provideSettingsRepository(
        settingsApiService: SettingsApiService,
        dataStore: DataStore<Preferences>
    ): SettingsRepository {
        return SettingsRepositoryImpl(
            settingsApiService = settingsApiService,
            dataStore = dataStore
        )
    }
}
```

**创建 `di/UseCaseModule.kt`**:
```kotlin
@Module
@InstallIn(ViewModelComponent::class) // UseCase 绑定到 ViewModel 作用域
object UseCaseModule {

    // 认证相关 UseCase
    @Provides
    fun provideLoginUseCase(repository: UserRepository): LoginUseCase {
        return LoginUseCase(repository)
    }

    @Provides
    fun provideRegisterUseCase(repository: UserRepository): RegisterUseCase {
        return RegisterUseCase(repository)
    }

    // 聊天相关 UseCase
    @Provides
    fun provideGetMessagesUseCase(repository: ChatRepository): GetMessagesUseCase {
        return GetMessagesUseCase(repository)
    }

    @Provides
    fun provideSendMessageUseCase(repository: ChatRepository): SendMessageUseCase {
        return SendMessageUseCase(repository)
    }

    @Provides
    fun provideMarkConversationReadUseCase(repository: ChatRepository): MarkConversationReadUseCase {
        return MarkConversationReadUseCase(repository)
    }
}
```

**创建 `di/SocketModule.kt`**:
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object SocketModule {

    @Provides
    @Singleton
    fun provideSocketEventListener(): SocketEventListener {
        return SocketEventListener()
    }

    @Provides
    @Singleton
    fun provideSocketManager(eventListener: SocketEventListener): SocketManager {
        return SocketManager(eventListener)
    }
}
```

**创建 `di/ViewModelModule.kt`**:
```kotlin
@Module
@InstallIn(ViewModelComponent::class)
object ViewModelModule {

    // Hilt 会自动生成 ViewModelFactory
    // 不需要手动创建 ViewModelFactory
}
```

### 步骤 4: 重构 ViewModel

**重构 `ChatDetailViewModel.kt`**:
```kotlin
@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    private val getMessagesUseCase: GetMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val markConversationReadUseCase: MarkConversationReadUseCase,
    private val chatRepository: ChatRepository
) : ViewModel() {

    // ViewModel 代码保持不变
    // ...
}
```

### 步骤 5: 重构 Activity

**重构 `ChatDetailActivity.kt`**:
```kotlin
@AndroidEntryPoint
class ChatDetailActivity : BaseActivity<ActivityChatDetailBinding>() {

    // ✅ Hilt 自动注入 ViewModel，无需手动创建
    private val viewModel: ChatDetailViewModel by viewModels()

    private lateinit var adapter: MessageAdapterRefactored

    override fun inflateBinding(): ActivityChatDetailBinding {
        return ActivityChatDetailBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        conversationId = intent.getStringExtra("conversation_id") ?: return

        setupRecyclerView()
        setupUI()
        observeViewModel()
        loadMessages()

        // 加入 Socket.IO 房间
        // socketManager 需要从 Application 或 ViewModel 注入
    }

    // ... 其余代码保持不变
}
```

### 步骤 6: 注入非 ViewModel 依赖

**示例**: 在 Activity 中注入 Repository
```kotlin
@AndroidEntryPoint
class ChatDetailActivity : BaseActivity<ActivityChatDetailBinding>() {

    // ✅ 直接注入 Repository
    @Inject
    lateinit var chatRepository: ChatRepository

    // ✅ 或注入 UseCase
    @Inject
    lateinit var sendMessageUseCase: SendMessageUseCase
}
```

### 步骤 7: 配置 ProGuard

**proguard-rules.pro**:
```proguard
# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Keep generated Hilt classes
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$ActivityContextWrapper

# Keep @Inject constructors
-keepclassmembers,allowobfuscation class * {
    @inject <init>(...);
}

# Keep Hilt generated classes
-keep class dagger.hilt.android.internal.** { *; }
-keep class dagger.hilt.android.internal.lifecycle.** { *; }
```

---

## 四、迁移前后对比

### 4.1 迁移前

```kotlin
// ❌ 手动创建依赖
class ChatDetailActivity : BaseActivity<ActivityChatDetailBinding>() {

    private val viewModel: ChatDetailViewModel by viewModels {
        ViewModelFactory {
            ChatDetailViewModel(
                getMessagesUseCase = GetMessagesUseCase(AppModule.chatRepository),
                sendMessageUseCase = SendMessageUseCase(AppModule.chatRepository),
                markConversationReadUseCase = MarkConversationReadUseCase(AppModule.chatRepository),
                repository = AppModule.chatRepository
            )
        }
    }
}
```

### 4.2 迁移后

```kotlin
// ✅ Hilt 自动注入
@AndroidEntryPoint
class ChatDetailActivity : BaseActivity<ActivityChatDetailBinding>() {

    private val viewModel: ChatDetailViewModel by viewModels()
}
```

**代码减少**: 9行 → 1行

---

## 五、测试支持

### 5.1 单元测试示例

**测试 ViewModel**:
```kotlin
@HiltAndroidTest
class ChatDetailViewModelTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var repository: FakeChatRepository // 测试用的假实现

    @Inject
    lateinit var getMessagesUseCase: GetMessagesUseCase

    private lateinit var viewModel: ChatDetailViewModel

    @Before
    fun setup() {
        hiltRule.inject()
        viewModel = ChatDetailViewModel(
            getMessagesUseCase = getMessagesUseCase,
            sendMessageUseCase = SendMessageUseCase(repository),
            markConversationReadUseCase = MarkConversationReadUseCase(repository),
            chatRepository = repository
        )
    }

    @Test
    fun `loadMessages should update uiState with messages`() = runTest {
        // Given
        val conversationId = "test_conversation"
        val messages = listOf(/* 测试数据 */)
        coEvery { repository.getMessages(conversationId) } returns messages

        // When
        viewModel.loadMessages(conversationId)

        // Then
        assertEquals(messages, viewModel.uiState.value.messages)
    }
}
```

**替换依赖**:
```kotlin
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [RepositoryModule::class])
object TestRepositoryModule {

    @Provides
    @Singleton
    fun provideTestChatRepository(): ChatRepository {
        return FakeChatRepository() // 测试实现
    }
}
```

---

## 六、常见问题

### 6.1 循环依赖

**错误**: `Dependency cycle detected`

**解决**: 重新设计依赖关系，使用中间层

### 6.2 多个构造函数

**问题**: 类有多个构造函数

**解决**: 使用 `@Inject` 标记主构造函数

```kotlin
class ChatRepository @Inject constructor(
    private val apiService: ChatApiService
) {
    // 其他辅助构造函数
    constructor(apiService: ChatApiService, cache: Cache) : this(apiService) {
        // ...
    }
}
```

### 6.3 Context 注入

**正确方式**:
```kotlin
class MyClass @Inject constructor(
    @ApplicationContext private val context: Context,  // Application Context
    @ActivityContext private val activityContext: Context  // Activity Context
)
```

---

## 七、迁移检查清单

### ✅ 准备阶段
- [ ] 备份当前代码
- [ ] 创建 feature 分支
- [ ] 添加 Hilt 依赖
- [ ] 同步 Gradle

### ✅ 实施阶段
- [ ] 修改 Application 类
- [ ] 创建 Hilt 模块
- [ ] 重构 ViewModel
- [ ] 重构 Activity/Fragment
- [ ] 删除 ViewModelFactory

### ✅ 测试阶段
- [ ] 编译通过
- [ ] 运行时无崩溃
- [ ] 功能正常
- [ ] 性能无退化

### ✅ 清理阶段
- [ ] 删除旧的依赖创建代码
- [ ] 删除 ViewModelFactory
- [ ] 更新文档
- [ ] 提交代码

---

## 八、参考资源

- [Hilt 官方文档](https://dagger.dev/hilt/)
- [Android Hilt 指南](https://developer.android.com/training/dependency-injection/hilt-android)
- [Hilt ViewModel 注入](https://developer.android.com/training/dependency-injection/hilt-jetpack)
- [Hilt 测试指南](https://developer.android.com/training/dependency-injection/hilt-testing)

---

**文档版本**: v1.0
**更新日期**: 2026-02-08
**预计工作量**: 2-3天
**风险等级**: 中等 (需要充分测试)
