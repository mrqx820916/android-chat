# 权限管理模块使用指南

## 概述

权限管理模块提供了完整的运行时权限请求功能，支持Android 13-16，包含权限说明UI和降级方案。

## 权限清单

### P0 权限（必需）
- `INTERNET` - 自动授予
- `ACCESS_NETWORK_STATE` - 自动授予
- `POST_NOTIFICATIONS` - 启动时请求 (Android 13+)
- `FOREGROUND_SERVICE` - 声明即可
- `FOREGROUND_SERVICE_DATA_SYNC` - 声明即可 (Android 14+)

### P1 权限（功能触发时请求）
- `RECORD_AUDIO` - 语音录制
- `READ_MEDIA_IMAGES` - 选择图片 (Android 13+)
- `READ_EXTERNAL_STORAGE` - 选择图片 (Android 12及以下)
- `CAMERA` - 拍照

## 使用方法

### 1. 在Activity中请求权限

```kotlin
class MainActivity : BasePermissionActivity() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // 请求相机权限
    private fun requestCameraPermission() {
        scope.launch {
            val result = requestPermissionWithRationale(PermissionType.CAMERA)

            handlePermissionResult(
                permissionType = PermissionType.CAMERA,
                result = result,
                granted = {
                    // 权限已授予，打开相机
                    openCamera()
                },
                denied = { shouldShowRationale ->
                    // 权限被拒绝
                    if (shouldShowRationale) {
                        // 用户拒绝但可以再次请求
                        showToast("需要相机权限才能拍照")
                    } else {
                        // 用户永久拒绝，引导到设置页面
                        showPermanentlyDeniedDialog(PermissionType.CAMERA)
                    }
                },
                permanentlyDenied = {
                    // 永久拒绝
                    showPermanentlyDeniedDialog(PermissionType.CAMERA)
                }
            )
        }
    }

    // 请求多个权限
    private fun requestStorageAndCamera() {
        scope.launch {
            val result = permissionsManager.requestPermissions(
                activity = this@MainActivity,
                permissions = listOf(
                    PermissionType.CAMERA.permission,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        PermissionType.READ_MEDIA_IMAGES.permission
                    } else {
                        PermissionType.READ_EXTERNAL_STORAGE.permission
                    }
                )
            )

            result.forEach { (permission, permissionResult) ->
                when (permissionResult) {
                    is PermissionResult.Granted -> {
                        // 权限已授予
                    }
                    is PermissionResult.Denied -> {
                        // 权限被拒绝
                    }
                    else -> {}
                }
            }
        }
    }

    private fun showPermanentlyDeniedDialog(permissionType: PermissionType) {
        PermissionPermanentlyDeniedDialog(
            context = this,
            permissionName = permissionType.description,
            onOpenSettings = {
                openAppSettings(this)
            }
        ).show()
    }
}
```

### 2. 在Fragment中请求权限

```kotlin
class ChatFragment : BasePermissionFragment() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // 请求录音权限
    private fun requestRecordPermission() {
        scope.launch {
            val result = requestPermissionWithRationale(PermissionType.RECORD_AUDIO)

            handlePermissionResult(
                permissionType = PermissionType.RECORD_AUDIO,
                result = result,
                granted = {
                    // 权限已授予，开始录音
                    startRecording()
                },
                permanentlyDenied = {
                    // 永久拒绝
                    showToast(getString(R.string.permission_microphone_denied))
                }
            )
        }
    }

    // 使用扩展函数简化
    private fun requestCameraPermissionSimple() {
        scope.launch {
            when (val result = requestPermissionWithRationale(PermissionType.CAMERA)) {
                is PermissionResult.Granted -> {
                    openCamera()
                }
                is PermissionResult.Denied -> {
                    if (!result.shouldShowRationale) {
                        openAppSettings(requireContext())
                    } else {
                        showToast("需要相机权限")
                    }
                }
                else -> {}
            }
        }
    }
}
```

### 3. 检查权限状态

```kotlin
// 检查单个权限
if (isPermissionGranted(Manifest.permission.CAMERA)) {
    openCamera()
}

// 检查权限类型
if (permissionsManager.isPermissionGranted(PermissionType.RECORD_AUDIO)) {
    startRecording()
}

// 检查多个权限
if (arePermissionsGranted(listOf(
    Manifest.permission.CAMERA,
    Manifest.permission.RECORD_AUDIO
))) {
    // 所有权限都已授予
}
```

### 4. 在ViewModel中使用

```kotlin
class PermissionViewModel(
    private val permissionsManager: PermissionsManager
) : ViewModel() {

    // 检查权限状态
    fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsManager.isPermissionGranted(PermissionType.POST_NOTIFICATIONS)
        } else {
            true
        }
    }

    // 获取权限说明
    fun getPermissionRationale(permissionType: PermissionType): String {
        return permissionType.rationale
    }
}
```

## 权限说明弹窗

### 使用默认权限说明

```kotlin
PermissionRequestDialog(
    context = this,
    permissionName = PermissionType.CAMERA.description,
    rationale = PermissionType.CAMERA.rationale,
    onConfirm = {
        // 用户点击确定，继续请求权限
    },
    onDismiss = {
        // 用户点击取消
    }
).show()
```

### 使用自定义权限说明

```kotlin
requestPermissionWithRationale(
    permissionType = PermissionType.CAMERA,
    customRationale = "需要相机权限来拍摄照片并发送给好友"
) { result ->
    // 处理结果
}
```

### 永久拒绝弹窗

```kotlin
PermissionPermanentlyDeniedDialog(
    context = this,
    permissionName = PermissionType.CAMERA.description,
    onOpenSettings = {
        openAppSettings(this)
    },
    onDismiss = {
        // 用户取消
    }
).show()
```

## 降级方案

### 相机权限被拒绝
```kotlin
when (result) {
    is PermissionResult.Granted -> openCamera()
    is PermissionResult.Denied -> {
        // 降级：使用图片选择
        if (result.shouldShowRationale) {
            showImagePicker()
        } else {
            showToast(getString(R.string.permission_camera_denied))
        }
    }
}
```

### 录音权限被拒绝
```kotlin
when (result) {
    is PermissionResult.Granted -> startRecording()
    is PermissionResult.Denied -> {
        // 降级：只能发送文字和图片
        showToast(getString(R.string.permission_microphone_denied))
        hideVoiceButton()
    }
}
```

### 存储权限被拒绝
```kotlin
when (result) {
    is PermissionResult.Granted -> openImagePicker()
    is PermissionResult.Denied -> {
        // 降级：只能拍照
        if (isPermissionGranted(Manifest.permission.CAMERA)) {
            openCamera()
        } else {
            showToast(getString(R.string.permission_storage_denied))
        }
    }
}
```

## 通知权限（Android 13+）

### 在启动时请求

```kotlin
class MainActivity : BasePermissionActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission()
        }
    }

    private fun requestNotificationPermission() {
        lifecycleScope.launch {
            val result = requestPermissionWithRationale(
                PermissionType.POST_NOTIFICATIONS,
                customRationale = "需要通知权限以便在收到新消息时向您推送通知"
            )

            when (result) {
                is PermissionResult.Granted -> {
                    // 可以发送通知
                }
                else -> {
                    // 不发送通知，但不影响应用使用
                }
            }
        }
    }
}
```

## 最佳实践

1. **在需要时才请求权限** - 不要在启动时一次性请求所有权限
2. **提供清晰的权限说明** - 让用户明白为什么需要这个权限
3. **提供降级方案** - 权限被拒绝时，提供替代功能
4. **不要频繁请求** - 用户拒绝后，不要立即再次请求
5. **引导到设置页面** - 永久拒绝时，引导用户手动开启
6. **测试各种场景** - 测试首次请求、拒绝后请求、永久拒绝等场景

## 兼容性

- **最低SDK**: 33 (Android 13)
- **目标SDK**: 34 (Android 14)
- **支持范围**: Android 13-16

## 注意事项

1. Android 13+需要单独请求通知权限
2. Android 14+需要声明`FOREGROUND_SERVICE_DATA_SYNC`
3. 使用`READ_MEDIA_IMAGES`替代`READ_EXTERNAL_STORAGE` (Android 13+)
4. 检查权限前先检查SDK版本

## 相关文件

```
app/src/main/java/com/chat/lightweight/
├── data/model/
│   ├── PermissionType.kt          # 权限类型定义
│   └── PermissionResult.kt        # 权限结果封装
├── domain/permission/
│   └── PermissionsManager.kt      # 权限管理器
├── presentation/
│   ├── base/
│   │   ├── BasePermissionActivity.kt
│   │   └── BasePermissionFragment.kt
│   ├── dialog/
│   │   └── PermissionRequestDialog.kt
│   └── extension/
│       └── PermissionExtensions.kt
```
