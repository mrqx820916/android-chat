# 权限管理模块使用说明

## 概述

本权限管理模块提供了完整的Android运行时权限解决方案,支持Android 13-16。

## 权限分级

### P0 权限 (必需)
- **INTERNET**: 自动授予
- **ACCESS_NETWORK_STATE**: 自动授予
- **POST_NOTIFICATIONS**: Android 13+ 需要在启动时请求
- **FOREGROUND_SERVICE**: 声明即可
- **FOREGROUND_SERVICE_DATA_SYNC**: 声明即可 (Android 14+)

### P1 权限 (功能触发时请求)
- **RECORD_AUDIO**: 录音功能触发时请求
- **READ_MEDIA_IMAGES**: 选择图片时请求 (Android 13+)
- **READ_EXTERNAL_STORAGE**: 选择图片时请求 (Android 12及以下)
- **CAMERA**: 拍照功能触发时请求

## 模块结构

```
permission/
├── PermissionsManager.kt        # 权限管理器 (单例)
├── PermissionRequestDialog.kt   # 权限请求对话框
└── PermissionHelper.kt          # 权限辅助工具类
```

## 使用示例

### 1. 在MainActivity中初始化P0权限

```kotlin
class MainActivity : AppCompatActivity() {
    private val permissionsManager = PermissionsManager.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化P0权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationPermission = Manifest.permission.POST_NOTIFICATIONS
            if (!permissionsManager.checkPermission(this, notificationPermission)) {
                p0PermissionLauncher.launch(arrayOf(notificationPermission))
            }
        }
    }
}
```

### 2. 请求录音权限

```kotlin
fun requestAudioPermission() {
    if (!permissionsManager.checkPermission(this, Manifest.permission.RECORD_AUDIO)) {
        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
}
```

### 3. 请求图片选择权限

```kotlin
fun requestImagePermission() {
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    val needRequest = permissions.filterNot {
        permissionsManager.checkPermission(this, it)
    }.toTypedArray()

    if (needRequest.isNotEmpty()) {
        imagePermissionLauncher.launch(needRequest)
    }
}
```

### 4. 请求相机权限

```kotlin
fun requestCameraPermission() {
    if (!permissionsManager.checkPermission(this, Manifest.permission.CAMERA)) {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }
}
```

## API参考

### PermissionsManager

主要方法:

- `checkPermission(context, permission)`: 检查单个权限
- `checkPermissions(context, permissions)`: 检查多个权限
- `checkP0Permissions(context)`: 检查所有P0权限
- `requestP0Permissions(activity)`: 请求P0权限
- `requestAudioPermission(activity)`: 请求录音权限
- `requestImagePermission(activity)`: 请求图片权限
- `requestCameraPermission(activity)`: 请求相机权限
- `handlePermissionResult()`: 处理权限请求结果
- `openAppSettings(activity)`: 打开应用设置页面

### PermissionRequestDialog

对话框方法:

- `showP0PermissionDeniedDialog()`: 显示P0权限被拒绝对话框
- `showPermissionRationaleDialog()`: 显示权限说明对话框
- `showPermissionsSummaryDialog()`: 显示权限总结对话框
- `openAppSettings()`: 打开设置页面

### PermissionHelper

辅助方法:

- `hasNotificationPermission()`: 检查通知权限
- `hasRecordAudioPermission()`: 检查录音权限
- `hasImagePermission()`: 检查图片权限
- `hasCameraPermission()`: 检查相机权限
- `formatPermissionStatus()`: 格式化权限状态为字符串

## 权限说明文本

权限说明会在以下情况显示:

1. **首次请求**: 显示友好的权限说明
2. **权限被拒绝**: 显示为什么需要该权限
3. **永久拒绝**: 引导用户去设置页面手动开启

## 兼容性

- **最低版本**: Android 7.0 (API 24)
- **目标版本**: Android 14 (API 34)
- **测试版本**: Android 13-16

## 最佳实践

1. **按需请求**: 只在需要使用某功能时才请求相应权限
2. **说明清晰**: 向用户说明为什么需要该权限
3. **降级方案**: 权限被拒绝后提供替代方案
4. **友好提示**: 使用对话框而不是Toast来提示用户

## 测试清单

- [ ] Android 13以下版本 (无需通知权限)
- [ ] Android 13版本 (需要通知权限)
- [ ] Android 14版本 (需要前台服务类型)
- [ ] 首次安装后请求权限
- [ ] 权限被拒绝后的处理
- [ ] 永久拒绝后引导到设置
- [ ] WebView调用权限请求

## 调试

查看权限状态:

```kotlin
val status = PermissionHelper.formatPermissionStatus(context)
Log.d("Permissions", status)
```

输出示例:
```
=== 权限状态 ===
通知权限: 已授予
录音权限: 未授予
图片权限: 已授予
相机权限: 未授予
```
