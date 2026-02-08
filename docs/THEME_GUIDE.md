# 轻聊应用主题使用指南

## Material Design 3 主题系统

### 主题切换

#### 在代码中切换主题

```kotlin
// 切换到浅色主题
AppCompat.setDefaultNightMode(AppCompat.MODE_NIGHT_NO)

// 切换到深色主题
AppCompat.setDefaultNightMode(AppCompat.MODE_NIGHT_YES)

// 跟随系统
AppCompat.setDefaultNightMode(AppCompat.MODE_NIGHT_FOLLOW_SYSTEM)
```

#### 在AndroidManifest.xml中指定主题

```xml
<application
    android:theme="@style/Theme.LightChat" />

<!-- 或使用深色主题 -->
<application
    android:theme="@style/Theme.LightChat.Dark" />
```

## 组件使用示例

### 按钮

#### 基础按钮
```xml
<com.google.android.material.button.MaterialButton
    android:id="@+id/btn_primary"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="@string/common_confirm"
    style="@style/Widget.LightChat.Button" />
```

#### 轮廓按钮
```xml
<com.google.android.material.button.MaterialButton
    android:id="@+id/btn_outline"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="@string/common_cancel"
    style="@style/Widget.LightChat.Button.OutlinedButton" />
```

#### 文本按钮
```xml
<com.google.android.material.button.MaterialButton
    android:id="@+id/btn_text"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="@string/common_retry"
    style="@style/Widget.LightChat.Button.TextButton" />
```

#### 图标按钮
```xml
<com.google.android.material.button.MaterialButton
    android:id="@+id/btn_icon"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    style="@style/Widget.LightChat.Button.IconButton"
    app:icon="@drawable/ic_send" />
```

#### 浮动操作按钮
```xml
<com.google.android.material.floatingactionbutton.FloatingActionButton
    android:id="@+id/fab"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:contentDescription="@string/chat_send"
    app:srcCompat="@drawable/ic_send"
    style="@style/Widget.LightChat.FloatingActionButton" />
```

### 输入框

#### 轮廓输入框
```xml
<com.google.android.material.textfield.TextInputLayout
    android:id="@+id/til_username"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:hint="@string/auth_username_hint"
    style="@style/Widget.LightChat.TextInputLayout.OutlinedBox">

    <com.google.android.material.textfield.TextInputEditText
        android:id="@+id/et_username"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:inputType="text"
        style="@style/Widget.LightChat.EditText.Outlined" />
</com.google.android.material.textfield.TextInputLayout>
```

#### 密码输入框
```xml
<com.google.android.material.textfield.TextInputLayout
    android:id="@+id/til_password"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:hint="@string/auth_password_hint"
    app:endIconMode="password_toggle"
    style="@style/Widget.LightChat.TextInputLayout.OutlinedBox">

    <com.google.android.material.textfield.TextInputEditText
        android:id="@+id/et_password"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:inputType="textPassword"
        style="@style/Widget.LightChat.EditText.Outlined" />
</com.google.android.material.textfield.TextInputLayout>
```

#### 搜索输入框
```xml
<com.google.android.material.textfield.TextInputLayout
    android:id="@+id/til_search"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:hint="@string/search_hint"
    style="@style/Widget.LightChat.TextInputLayout.Search">

    <com.google.android.material.textfield.TextInputEditText
        android:id="@+id/et_search"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:inputType="text"
        android:imeOptions="actionSearch" />
</com.google.android.material.textfield.TextInputLayout>
```

### 对话框

#### 基础对话框
```kotlin
MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_LightChat_AlertDialog)
    .setTitle(R.string.common_confirm)
    .setMessage(R.string.message_delete_confirm)
    .setPositiveButton(R.string.common_confirm) { dialog, which ->
        // 确认操作
    }
    .setNegativeButton(R.string.common_cancel) { dialog, which ->
        // 取消操作
    }
    .show()
```

#### 底部对话框
```kotlin
val bottomSheetDialog = BottomSheetDialog(context, R.style.Theme_LightChat_BottomSheetDialog)
val view = layoutInflater.inflate(R.layout.bottom_sheet_content, null)
bottomSheetDialog.setContentView(view)
bottomSheetDialog.show()
```

### 进度指示器

#### 圆形进度条
```xml
<com.google.android.material.progressindicator.CircularProgressIndicator
    android:id="@+id/progress_circular"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:indeterminate="true"
    style="@style/Widget.LightChat.ProgressBar.Circular" />
```

#### 线性进度条
```xml
<com.google.android.material.progressindicator.LinearProgressIndicator
    android:id="@+id/progress_linear"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:progress="50"
    style="@style/Widget.LightChat.ProgressBar.Linear" />
```

#### 下拉刷新
```xml
<androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    android:id="@+id/swipe_refresh"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    style="@style/Widget.LightChat.SwipeRefreshLayout">

    <!-- 内容视图 -->
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/recycler_view"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

</androidx.swiperefreshlayout.widget.SwipeRefreshLayout>
```

## 颜色使用

### 在XML中使用颜色
```xml
<!-- 直接引用颜色 -->
<item name="android:textColor">@color/brand_primary</item>
<item name="android:background">@color/brand_primary_container</item>

<!-- 使用主题属性 -->
<item name="android:textColor">?attr/colorPrimary</item>
<item name="android:background">?attr/colorSurface</item>
```

### 在代码中使用颜色
```kotlin
// 直接获取颜色
val primaryColor = ContextCompat.getColor(context, R.color.brand_primary)

// 使用主题属性
val typedValue = TypedValue()
context.theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
val primaryColor = typedValue.data

// 使用MaterialColors工具
val primaryColor = MaterialColors.getColor(context, R.attr.colorPrimary, Color.RED)
```

## 圆角使用

### 在XML中使用圆角
```xml
<!-- 使用预定义圆角大小 -->
<item name="cornerRadius">?attr/cornerSizeMedium</item>

<!-- 自定义圆角大小 -->
<item name="cardCornerRadius">12dp</item>
```

### 在代码中设置圆角
```kotlin
// 使用ShapeAppearanceModel
val shapeAppearanceModel = ShapeAppearanceModel.builder()
    .setAllCorners(CornerFamily.ROUNDED, 12f)
    .build()

materialShapeAppearance.setShapeAppearanceModel(shapeAppearanceModel)
```

## 字体样式

### 文字外观样式
```xml
<TextView
    android:id="@+id/title"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="标题"
    style="@style/TextAppearance.LightChat.TitleLarge" />
```

### 在代码中设置文字样式
```kotlin
// 使用TextAppearance
TextViewCompat.setTextAppearance(textView, R.style.TextAppearance_LightChat_BodyLarge)

// 设置文字颜色
textView.setTextColor(MaterialColors.getColor(context, R.attr.colorOnSurface, Color.BLACK))
```

## 状态处理

### 禁用状态
```xml
<com.google.android.material.button.MaterialButton
    android:id="@+id/btn_disabled"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:enabled="false"
    android:text="@string/common_save"
    style="@style/Widget.LightChat.Button" />
```

### 错误状态
```kotlin
// 设置输入框错误状态
textInputLayout.error = getString(R.string.auth_username_required)
textInputLayout.setErrorIconDrawable(R.drawable.ic_error)
textInputLayout.setErrorIconTintList(ContextCompat.getColorStateList(context, R.color.error))

// 清除错误状态
textInputLayout.error = null
```

## 卡片样式

### Material CardView
```xml
<com.google.android.material.card.MaterialCardView
    android:id="@+id/card"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:cardCornerRadius="12dp"
    app:cardElevation="2dp"
    app:cardBackgroundColor="@color/md_theme_light_surface">

    <!-- 卡片内容 -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">

        <!-- 内容 -->
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

## 最佳实践

1. **始终使用主题属性**而非硬编码颜色，以便主题切换
2. **使用预定义样式**保持UI一致性
3. **遵循Material Design 3指南**设置间距和圆角
4. **为所有交互元素提供反馈**（如Ripple效果）
5. **确保可访问性**，提供足够的对比度

## 主题资源文件

```
res/values/
├── colors.xml          # 颜色定义
├── themes.xml          # 主题定义
├── button_styles.xml   # 按钮样式
├── input_styles.xml    # 输入框样式
├── dialog_styles.xml   # 对话框样式
└── progress_styles.xml # 进度指示器样式
```

## 更新历史

- 2025-02-08: 创建主题使用指南
