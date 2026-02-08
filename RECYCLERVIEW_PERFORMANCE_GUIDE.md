# RecyclerView 性能优化指南

## 一、基础优化

### 1.1 使用 ViewHolder 模式 ✅

**现状**: 项目已使用 ViewBinding + ViewHolder 模式

**优势**:
- 避免重复 findViewById
- 提升 onBindViewHolder 性能
- 代码更简洁

### 1.2 使用 DiffUtil ✅

**现状**: 所有 Adapter 都使用 ListAdapter + DiffUtil

**优化点**:
```kotlin
class MessageDiffCallback : DiffUtil.ItemCallback<MessageItem>() {
    override fun areItemsTheSame(oldItem: MessageItem, newItem: MessageItem): Boolean {
        return oldItem.id == newItem.id || oldItem.tempId == newItem.tempId
    }

    override fun areContentsTheSame(oldItem: MessageItem, newItem: MessageItem): Boolean {
        return oldItem == newItem
    }

    // 新增: 实现部分更新
    override fun getChangePayload(oldItem: MessageItem, newItem: MessageItem): Any? {
        return if (oldItem.status != newItem.status) {
            PAYLOAD_STATUS_CHANGED
        } else null
    }
}
```

### 1.3 启用 StableIds ✅ (新增)

**问题**: 当前未启用

**解决方案**:
```kotlin
class MessageAdapter : ListAdapter<MessageItem, RecyclerView.ViewHolder>(...) {
    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id.hashCode().toLong()
    }
}
```

**性能提升**: 减少 item 动画开销，提升刷新性能

---

## 二、布局优化

### 2.1 减少布局层级

**问题**: 发现最深8层的布局

**解决方案**:
```xml
<!-- 使用 ConstraintLayout 替代嵌套 LinearLayout -->
<androidx.constraintlayout.widget.ConstraintLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content">

    <TextView
        android:id="@+id/textViewMessage"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        ... />

    <ImageView
        android:id="@+id/imageViewStatus"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        ... />

</androidx.constraintlayout.widget.ConstraintLayout>
```

### 2.2 使用 merge 标签

**适用场景**: include 布局时减少层级

```xml
<!-- item_message.xml -->
<merge xmlns:android="http://schemas.android.com/apk/res/android">
    <TextView android:id="@+id/textView" ... />
    <ImageView android:id="@+id/imageView" ... />
</merge>
```

### 2.3 避免过度绘制

**检查方法**: 开发者选项 > 调试 GPU 过度绘制

**优化**:
- 移除不必要的背景
- 使用 ViewStub 延迟加载
- 使用 ConstraintLayout 的约束避免重叠

---

## 三、数据绑定优化

### 3.1 避免在 bind 中创建对象

**问题**: SimpleDateFormat 每次创建

**解决方案**:
```kotlin
class MessageAdapter : ... {
    companion object {
        // 使用对象池
        private val TIME_FORMAT = SimpleDateFormat("HH:mm", Locale.getDefault())
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding.textViewTime.text = TIME_FORMAT.format(Date(item.timestamp))
    }
}
```

### 3.2 延迟加载昂贵资源

```kotlin
class MessageViewHolder : ... {
    private var cachedDrawable: Drawable? = null

    fun bind(item: MessageItem) {
        // 只在需要时加载
        if (cachedDrawable == null) {
            cachedDrawable = ContextCompat.getDrawable(context, R.drawable.ic_placeholder)
        }
        imageView.setImageDrawable(cachedDrawable)
    }
}
```

---

## 四、图片加载优化

### 4.1 使用 Coil 优化配置 ✅

**已创建**: `CoilImageLoadConfigOptimized.kt`

**优化特性**:
```kotlin
ImageLoader.Builder(context)
    .memoryCache {
        MemoryCache.Builder(context)
            .maxSizePercent(0.25) // 25%可用内存
            .build()
    }
    .diskCache {
        DiskCache.Builder()
            .directory(context.cacheDir.resolve("image_cache"))
            .maxSizeBytes(200 * 1024 * 1024) // 200MB
            .build()
    }
    .crossfade(true)
    .build()
```

### 4.2 图片尺寸优化

```kotlin
imageView.load(url) {
    // 减小图片尺寸以节省内存
    sizeMultiplier(0.5f)
    // 或指定精确尺寸
    size(width, height)
}
```

### 4.3 占位图优化

```kotlin
imageView.load(url) {
    // 使用矢量图占位符
    placeholder(R.drawable.ic_image_placeholder_vector)
    // 使用矢量图错误图
    error(R.drawable.ic_image_error_vector)
}
```

---

## 五、缓存策略

### 5.1 RecyclerView 缓存机制

**理解三级缓存**:
1. **Scrap Cache**: 屏幕内的 View (快速复用)
2. **Cache Cache**: 刚滚出屏幕的 View (中速复用)
3. **RecycledViewPool**: 完全释放的 View (慢速复用)

### 5.2 自定义缓存大小

```kotlin
recyclerView.apply {
    // 增加缓存容量
    setItemViewCacheSize(20)
    setDrawingCacheEnabled(true)

    // 共享 RecycledViewPool (多个 RecyclerView 相同类型时)
    setRecycledViewPool(sharedPool)
}
```

### 5.3 预加载

```kotlin
// 预加载即将显示的 item
recyclerView.viewTreeObserver.addOnPreDrawListener {
    val firstVisible = layoutManager.findFirstVisibleItemPosition()
    val lastVisible = layoutManager.findLastVisibleItemPosition()

    // 预加载前后各10条
    for (i in (firstVisible - 10)..(lastVisible + 10)) {
        if (i >= 0 && i < adapter.itemCount) {
            preloadItem(adapter.getItemId(i))
        }
    }
    true
}
```

---

## 六、分页加载

### 6.1 使用 Paging 3 库

**推荐**: Jetpack Paging 3

```kotlin
// ViewModel
val messages: Flow<PagingData<Message>> = Pager(
    config = PagingConfig(
        pageSize = 20,
        prefetchDistance = 10,
        enablePlaceholders = false
    )
) {
    MessagePagingSource(repository)
}.flow.cachedIn(viewModelScope)

// Adapter
class MessagePagingAdapter : PagingDataAdapter<Message, ViewHolder>(DiffCallback()) {
    // 与普通 Adapter 相同
}
```

### 6.2 手动分页 (当前实现)

**现状**: 使用 ViewModel + LiveData

**优化点**:
```kotlin
// 增加预加载
private fun loadMessages(conversationId: String) {
    viewModel.loadMessages(conversationId, page = 1, pageSize = 30)

    // 监听滚动，接近底部时加载更多
    recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
            val lastVisible = layoutManager.findLastVisibleItemPosition()
            val total = adapter.itemCount

            if (lastVisible >= total - 10) {
                // 加载下一页
                viewModel.loadMore()
            }
        }
    })
}
```

---

## 七、性能监控

### 7.1 使用 StrictMode

```kotlin
// Application.onCreate()
if (BuildConfig.DEBUG) {
    StrictMode.setThreadPolicy(
        StrictMode.ThreadPolicy.Builder()
            .detectDiskReads()
            .detectDiskWrites()
            .detectNetwork()
            .penaltyLog()
            .build()
    )

    StrictMode.setVmPolicy(
        StrictMode.VmPolicy.Builder()
            .detectLeakedSqlLiteObjects()
            .detectLeakedClosableObjects()
            .penaltyLog()
            .build()
    )
}
```

### 7.2 使用 Profiler

**Android Studio Profiler**:
- CPU: 检查 onBindViewHolder 是否耗时
- Memory: 检查是否有内存泄漏
- Network: 检查图片加载性能

### 7.3 自定义性能追踪

```kotlin
class PerformanceTracker {
    companion object {
        private const val TAG = "Performance"

        fun trackBindStart(holder: RecyclerView.ViewHolder) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Bind start: ${holder::class.java.simpleName}")
            }
        }

        fun trackBindEnd(holder: RecyclerView.ViewHolder, duration: Long) {
            if (BuildConfig.DEBUG && duration > 16) { // 超过16ms警告
                Log.w(TAG, "Slow bind: ${holder::class.java.simpleName} - ${duration}ms")
            }
        }
    }
}
```

---

## 八、常见问题与解决方案

### 8.1 卡顿问题

**症状**: 滚动不流畅

**排查步骤**:
1. 检查 onBindViewHolder 耗时
2. 检查是否有内存泄漏
3. 检查图片加载是否在主线程
4. 检查布局层级是否过深

### 8.2 内存泄漏

**症状**: 滚动后内存不释放

**排查步骤**:
1. 使用 LeakCanary
2. 检查 ViewHolder 是否持有 Context
3. 检查是否有未取消的协程
4. 检查监听器是否正确移除

### 8.3 图片闪烁

**症状**: 滚动时图片闪烁

**解决方案**:
```kotlin
imageView.load(url) {
    // 禁用内存缓存中的淡入淡出
    crossfade(false)
    // 或使用内存缓存
    memoryCachePolicy(CachePolicy.ENABLED)
}
```

---

## 九、最佳实践总结

### ✅ 已实施
- ViewBinding + ViewHolder
- DiffUtil
- Coil 图片加载
- 基础缓存配置

### 🔄 需要优化
- 启用 StableIds
- 减少布局层级
- 优化对象创建 (SimpleDateFormat)
- 实现分页加载
- 添加性能监控

### 📋 性能检查清单
- [ ] onBindViewHolder 耗时 < 16ms
- [ ] 滚动帧率 > 55fps
- [ ] 内存使用稳定
- [ ] 无内存泄漏
- [ ] 图片加载不阻塞主线程
- [ ] 布局层级 < 6层
- [ ] 过度绘制 < 50%

---

## 十、参考资源

- [Android RecyclerView 官方文档](https://developer.android.com/guide/topics/ui/layout/recyclerview)
- [Coil 图片加载库](https://coil-kt.github.io/coil/)
- [Paging 3 库](https://developer.android.com/topic/libraries/architecture/paging/v3-overview)
- [Android 性能优化最佳实践](https://developer.android.com/topic/performance/best-practices)

---

**文档版本**: v1.0
**更新日期**: 2026-02-08
**维护者**: Android 开发团队
