# 媒体消息功能使用说明

## 概述

本模块提供完整的媒体消息功能，包括：
- 图片选择（相册/拍照）
- 语音录制（按住录音）
- 文件上传（进度显示）
- 语音播放（播放控制）
- 录音波形动画

## 项目结构

```
media/
├── ImagePickerHelper.kt       # 图片选择器
├── VoiceRecorderHelper.kt      # 语音录制器
├── VoicePlayerHelper.kt        # 语音播放器
├── FileUploadManager.kt        # 文件上传管理
├── VoiceWaveformView.kt        # 波形动画视图
├── VoiceRecordingDialog.kt     # 录音对话框
└── MediaUtils.kt              # 媒体工具类
```

## 快速开始

### 1. 图片选择

```kotlin
// 在Activity中使用
val imagePicker = ImagePickerHelper.with(this) {
    setOnImageSelected { uri ->
        // 处理选中的图片
        val file = MediaUtils.getFileFromUri(context, uri)
        // 上传文件
        lifecycleScope.launch {
            file?.let {
                uploadManager.uploadImage(it)
            }
        }
    }

    setOnPermissionDenied {
        showSnackbar("需要相机权限")
    }
}

// 打开相册
imagePicker.openGallery()

// 打开相机
imagePicker.openCamera()
```

### 2. 语音录制

```kotlin
// 创建录音器
val voiceRecorder = VoiceRecorderHelper(context)

// 监听录音状态
lifecycleScope.launch {
    voiceRecorder.recordingState.collect { state ->
        when (state) {
            is VoiceRecorderHelper.RecordingState.Recording -> {
                // 正在录音
            }
            is VoiceRecorderHelper.RecordingState.Completed -> {
                // 录音完成
                sendVoiceMessage(state.file, state.duration)
            }
            is VoiceRecorderHelper.RecordingState.TooShort -> {
                showSnackbar("说话时间太短")
            }
            is VoiceRecorderHelper.RecordingState.Error -> {
                showSnackbar("录音失败: ${state.message}")
            }
            else -> {}
        }
    }
}

// 开始录音
voiceRecorder.startRecording()

// 停止录音
voiceRecorder.stopRecording()

// 取消录音
voiceRecorder.cancelRecording()
```

### 3. 语音播放

```kotlin
// 创建播放器
val voicePlayer = VoicePlayerHelper(context)

// 监听播放状态
lifecycleScope.launch {
    voicePlayer.playingState.collect { state ->
        when (state) {
            is VoicePlayerHelper.PlayingState.Playing -> {
                // 正在播放
                updatePlayButton(true)
            }
            is VoicePlayerHelper.PlayingState.Paused -> {
                // 已暂停
                updatePlayButton(false)
            }
            is VoicePlayerHelper.PlayingState.Completed -> {
                // 播放完成
                updatePlayButton(false)
            }
            is VoicePlayerHelper.PlayingState.Error -> {
                showSnackbar("播放失败: ${state.message}")
            }
            else -> {}
        }
    }
}

// 播放语音
voicePlayer.play(file)

// 暂停播放
voicePlayer.pause()

// 继续播放
voicePlayer.resume()

// 停止播放
voicePlayer.stop()
```

### 4. 文件上传

```kotlin
// 上传图片
lifecycleScope.launch {
    uploadManager.uploadImage(file, userType = "member") { progress ->
        // 更新进度
        updateProgress(progress)
    }.fold(
        onSuccess = { fileUrl ->
            // 上传成功
            sendImageMessage(fileUrl)
        },
        onFailure = { error ->
            // 上传失败
            showSnackbar("上传失败: ${error.message}")
        }
    )
}

// 上传语音
lifecycleScope.launch {
    uploadManager.uploadVoice(file, userType = "member") { progress ->
        updateProgress(progress)
    }.fold(
        onSuccess = { fileUrl ->
            sendVoiceMessage(fileUrl)
        },
        onFailure = { error ->
            showSnackbar("上传失败: ${error.message}")
        }
    )
}
```

## 功能特性

### 图片选择

**ImagePickerHelper** - 统一的图片选择入口：
- 从相册选择图片
- 打开相机拍照
- 权限自动处理
- 支持Activity和Fragment

### 语音录制

**VoiceRecorderHelper** - 完整的录音功能：
- 按住录音
- 自动停止（60秒限制）
- 最短时长检测（1秒）
- 文件自动保存

**VoiceWaveformView** - 波形动画：
- 实时波形显示
- 平滑动画效果
- 音量等级响应

### 语音播放

**VoicePlayerHelper** - 语音播放器：
- 播放/暂停控制
- 播放进度显示
- 自动播放下一个
- 播放状态监听

### 文件上传

**FileUploadManager** - 统一上传管理：
- 图片/语音上传
- 实时进度显示
- 错误处理
- 类型验证

## 配置参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| 图片最大大小 | 100MB | 可配置 |
| 语音最短时长 | 1秒 | 最短录音时间 |
| 语音最长时长 | 60秒 | 自动停止录音 |
| 上传超时 | 30秒 | 网络超时 |
| 波形条数 | 8条 | 波形动画 |

## 权限配置

### AndroidManifest.xml

```xml
<!-- 图片选择 -->
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />

<!-- 相机 -->
<uses-permission android:name="android.permission.CAMERA" />

<!-- 录音 -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />

<!-- 存储 -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28" />
```

## 使用示例

### 完整的图片发送流程

```kotlin
// 1. 选择图片
imagePicker.openGallery()

// 2. 处理选中
setOnImageSelected { uri ->
    val file = MediaUtils.getFileFromUri(context, uri)

    // 3. 验证文件
    if (!MediaUtils.isValidImageFile(context, uri)) {
        showSnackbar("请选择图片文件")
        return
    }

    if (!MediaUtils.isFileSizeValid(context, uri, 100)) {
        showSnackbar("图片大小不能超过100MB")
        return
    }

    // 4. 上传文件
    lifecycleScope.launch {
        showLoading()
        file?.let {
            uploadManager.uploadImage(it, userType = getUserType()).fold(
                onSuccess = { fileUrl ->
                    // 5. 发送消息
                    viewModel.sendImageMessage(fileUrl)
                    hideLoading()
                },
                onFailure = { error ->
                    hideLoading()
                    showSnackbar("上传失败: ${error.message}")
                }
            )
        }
    }
}
```

### 完整的语音发送流程

```kotlin
// 1. 显示录音对话框
val dialog = VoiceRecordingDialog(
    context,
    onRecordingComplete = { file, duration ->
        // 2. 上传语音
        lifecycleScope.launch {
            uploadManager.uploadVoice(file).fold(
                onSuccess = { fileUrl ->
                    // 3. 发送消息
                    viewModel.sendVoiceMessage(fileUrl, duration)
                },
                onFailure = { error ->
                    showSnackbar("上传失败: ${error.message}")
                }
            )
        }
    },
    onRecordingCancel = {
        // 取消录音，删除文件
        MediaUtils.deleteFile(file)
    }
)

dialog.show()

// 2. 开始录音
dialog.startRecording()

// 3. 停止录音并自动触发上传
dialog.stopRecording()
```

## 注意事项

1. **权限请求** - 确保在使用前请求必要权限
2. **文件验证** - 上传前验证文件类型和大小
3. **生命周期** - Activity销毁时释放资源
4. **线程安全** - 使用协程处理异步操作
5. **错误处理** - 捕获并处理所有异常

## 故障排查

### 图片选择失败

1. 检查READ_MEDIA_IMAGES权限
2. 检查文件路径是否有效
3. 验证文件MIME类型

### 录音失败

1. 检查RECORD_AUDIO权限
2. 检查存储权限
3. 确认麦克风未被占用

### 上传失败

1. 检查网络连接
2. 验证服务器地址
3. 查看服务器日志

### 播放失败

1. 检查文件是否存在
2. 验证文件格式
3. 确认MediaPlayer未冲突
