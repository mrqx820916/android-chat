# RecyclerView 性能优化指南

## 消息列表性能优化

### 1. 基础优化

#### 设置固定大小
```kotlin
// 在Activity/Fragment中设置
binding.rvMessages.apply {
    layoutManager = LinearLayoutManager(this@ChatDetailActivity).apply {
        stackFromEnd = true
    }
    adapter = messageAdapter

    // 性能优化：固定大小
    setHasFixedSize(true)

    // 性能优化：禁用动画
    itemAnimator = null

    // 性能优化：启用缓存
    setItemViewCacheSize(20)
}
```

#### 优化Adapter
```kotlin
class MessageAdapter : ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    // 使用DiffUtil自动计算差异
    class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }

    // 优化的添加方法
    fun addMessage(message: Message) {
        val currentList = currentList.toMutableList()
        currentList.add(message)
        submitList(currentList) // DiffUtil自动计算差异
    }

    // 批量添加
    fun addMessages(messages: List<Message>) {
        val currentList = currentList.toMutableList()
        currentList.addAll(messages)
        submitList(currentList)
    }
}
```

### 2. ViewHolder复用

#### 正确的ViewHolder实现
```kotlin
class SentViewHolder(
    private val binding: ItemMessageSentBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(message: Message) {
        binding.apply {
            // 设置数据
            tvMessageContent.text = message.content

            // 使用Coil加载图片（自动缓存）
            ivMessageImage.load(message.fileUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_image_placeholder)
                error(R.drawable.ic_image_error)
                transformations(RoundedCornersTransformation(12f))
            }

            // 避免创建新对象
            root.setOnClickListener { onMessageClick(message) }
        }
    }
}
```

### 3. 分页加载

#### 使用Paging3实现分页
```kotlin
// 1. 创建PagingSource
class MessagePagingSource(
    private val conversationId: String,
    private val repository: ChatRepository
) : PagingSource<Int, Message>() {

    override suspend fun load(
        params: LoadParams<Int>
    ): LoadResult<Int, Message> {
        val page = params.key ?: 0
        val pageSize = params.loadSize

        val messages = repository.getMessages(
            conversationId = conversationId,
            page = page,
            pageSize = pageSize
        )

        return LoadResult.Page(
            data = messages,
            prevKey = if (page == 0) null else page - 1,
            nextKey = if (messages.size < pageSize) null else page + 1
        )
    }

    override fun getRefreshKey(state: PagingState<Int, Message>): Int? {
        return state.anchorPosition
    }
}

// 2. 创建PagingDataAdapter
val messageAdapter = PagingDataAdapter { parent ->
    SentViewHolder(
        ItemMessageSentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )
}

// 3. 在Activity中使用
lifecycleScope.launch {
    val pager = Pager(
        config = PagingConfig(
            pageSize = 50,
            enablePlaceholders = false,
            initialLoadSize = 50
        )
    ) {
        MessagePagingSource(conversationId, repository)
    }.flow.collectLatest { pagingData ->
        messageAdapter.submitData(lifecycle, pagingData)
    }
}
```

### 4. 图片加载优化

#### Coil配置（已在Application中初始化）
```kotlin
// 内存缓存：25%可用内存，最大50MB
// 磁盘缓存：200MB
// 支持：GIF、SVG、渐进式加载
```

#### 使用Coil加载图片
```kotlin
// 基础用法
imageView.load(url) {
    crossfade(true)
    placeholder(R.drawable.placeholder)
    error(R.drawable.error)
}

// 高级用法
imageView.load(url) {
    crossfade(300) // 淡入时长
    placeholder(R.drawable.placeholder)
    error(R.drawable.error)
    transformations(RoundedCornersTransformation(12f))
    size(200, 200) // 压缩尺寸
    memoryCacheKeyPolicy(CacheKeyPolicy.MemoryCacheKeyPolicy)
    diskCacheKeyPolicy(CacheKeyPolicy.DiskCacheKeyPolicy)
}

// 预加载图片
val imageLoader = context.getImageLoader()
val request = ImageRequest.Builder(context)
    .data(url)
    .target { drawable ->
        // 加载完成
    }
    .build()
imageLoader.enqueue(request)
```

### 5. 滑动性能优化

#### RecyclerView配置
```xml
<androidx.recyclerview.widget.RecyclerView
    android:id="@+id/rv_messages"
    android:layout_width="match_parent"
    android:layout_height="0dp"
    android:clipToPadding="false"
    android:overScrollMode="never"
    app:layoutManager="androidx.recyclerview.widget.LinearLayoutManager"
    app:setHasFixedSize="true" />

<!-- 在Activity/Fragment中 -->
<androidx.recyclerview.widget.RecyclerView
    android:id="@+id/rv_messages"
    android:layout_width="match_parent"
    android:layout_height="0dp"
    android:clipToPadding="false"
    android:overScrollMode="never" />
```

#### Kotlin配置
```kotlin
binding.rvMessages.apply {
    // 固定大小优化
    setHasFixedSize(true)

    // 禁用动画
    itemAnimator = null

    // 缓存ViewHolder
    setItemViewCacheSize(20)

    // 预加载
    setInitialItemPrefetchCount(10)
}
```

### 6. 内存优化

#### 对象池模式
```kotlin
// 复用相同的Drawable对象
private val placeholderDrawable by lazy {
    ContextCompat.getDrawable(context, R.drawable.ic_image_placeholder)
}

// 在ViewHolder中使用
imageView.load(url) {
    placeholder(placeholderDrawable) // 复用同一对象
}
```

#### 及时清理
```kotlin
override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
    // 清理大对象
    System.gc()
}
```

### 7. 预加载策略

#### 提前加载下一页
```kotlin
// 在Paging3中配置
val pager = Pager(
    config = PagingConfig(
        pageSize = 50,
        enablePlaceholders = false,
        initialLoadSize = 50,
        prefetchDistance = 10 // 预加载距离
    )
) {
    MessagePagingSource(conversationId, repository)
}.flow
```

#### 预加载相邻图片
```kotlin
// 预加载消息中的图片
fun preloadMessageImages(context: Context, messages: List<Message>) {
    val urls = messages
        .filter { it.messageType == MessageType.IMAGE }
        .mapNotNull { it.fileUrl }
        .distinct()

    CoilImageLoadConfig.preloadImages(context, urls)
}
```

## 性能指标

### 目标
- **帧率**: 60fps（16.67ms/帧）
- **内存占用**: < 150MB
- **滑动流畅**: 无卡顿
- **图片加载**: < 500ms

### 测试方法
```kotlin
// 启用严格模式
if (BuildConfig.DEBUG) {
    StrictMode.setThreadPolicy(
        ThreadPolicy.Builder()
            .detectAll()
            .penaltyLog()
            .build()
    )
}
```

## 最佳实践

1. ✅ **使用DiffUtil** - 差量更新，避免刷新整个列表
2. ✅ **setHasFixedSize(true)** - 固定大小优化布局计算
3. ✅ **禁用itemAnimator** - 移除默认动画
4. ✅ **setItemViewCacheSize** - 缓存ViewHolder
5. ✅ **Coil图片缓存** - 内存和磁盘缓存
6. ✅ **预加载** - 提前加载下一页
7. ✅ **避免嵌套RecyclerView** - 性能杀手
8. ✅ **及时清理** - onDestroyView中清理引用
