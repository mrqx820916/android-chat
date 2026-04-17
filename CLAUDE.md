# CLAUDE.md

> 详细项目文档参见上级目录 `K:\AICODE\chat\CLAUDE.md`

## 项目概述

轻聊 (Lightweight Chat) Android 客户端 - 原生 Kotlin 实现的聊天应用。

- **包名**: `com.chat.lightweight`
- **语言**: Kotlin 1.9.20
- **架构**: MVVM + Clean Architecture
- **minSdk**: 26 (Android 8.0)
- **targetSdk**: 34 (Android 14)
- **启动入口**: `ui.auth.LoginActivity`

## 构建与发布

```bash
# 构建 Release APK
.\gradlew.bat assembleRelease

# 生成更新包（APK + latest.json）
.\prepare-update-package.ps1

# 上传到雨云对象存储（按版本替换脚本名）
.\upload-v{版本}.ps1
```

- **Debug APK**: `app/build/outputs/apk/debug/app-debug.apk`
- **Release APK**: `app/build/outputs/apk/release/app-release.apk`

### 版本发布流程

1. 修改 `app/build.gradle` 中 `versionCode` 和 `versionName`
2. `.\gradlew.bat assembleRelease`
3. `.\prepare-update-package.ps1`
4. 编写并执行 `.\upload-v{版本}.ps1`
5. `git add . && git commit -m "..."`

## 服务器配置

```kotlin
BuildConfig.BASE_URL        // https://chat.soft1688.vip
BuildConfig.SOCKET_URL      // https://chat.soft1688.vip
BuildConfig.DEBUG_MODE      // true (debug) / false (release)
```

## 关键代码路径

| 功能 | 文件 |
|------|------|
| 对话列表 | `ui/conversation/ConversationListFragment.kt` |
| 聊天详情 | `ui/chat/ChatDetailActivity.kt` |
| 设置页面 | `ui/settings/SettingsFragment.kt` |
| Socket 客户端 | `socket/SocketClient.kt` |
| 网络层 | `network/NetworkRepository.kt` |
| 对话仓库 | `data/repository/ConversationRepository.kt` |
| 消息模型 | `data/model/MessageItem.kt` |
| 更新管理 | `update/UpdateManager.kt` |

## 注意事项

- **日期解析**: 后端返回 SQLite datetime 格式 (`yyyy-MM-dd HH:mm:ss`)，解析时需兼容多种格式
- **关于页面版本号**: 从 `BuildConfig.VERSION_NAME` 动态读取，不要硬编码
- **子模块**: `lightweight-chat/android-chat/` 是旧的子模块引用，实际开发使用本项目
