# 后台保活服务使用说明

## 概述

本模块实现完整的Socket.IO后台保活方案，包括：
- 前台服务保持连接
- WorkManager定时心跳
- 网络状态监听和自动重连
- Doze模式兼容
- 应用重启自动恢复

## 项目结构

```
service/
├── SocketForegroundService.kt      # 前台服务
├── HeartbeatWorker.kt              # WorkManager心跳任务
├── NetworkStateMonitor.kt          # 网络状态监听
├── AppLifecycleObserver.kt         # 应用生命周期监听
├── PowerManager.kt                 # 电源管理
└── ServiceManager.kt               # 服务统一管理
```

## 快速开始

### 1. Application配置

```kotlin
@HiltAndroidApp
class LightweightChatApp : Application() {

    @Inject
    lateinit var serviceManager: ServiceManager

    @Inject
    lateinit var appLifecycleObserver: AppLifecycleObserver

    override fun onCreate() {
        super.onCreate()

        // 初始化服务管理器
        serviceManager.initialize()

        // 处理应用重启
        appLifecycleObserver.handleAppRestart()
    }
}
```

### 2. 用户登录后启动服务

```kotlin
// 在登录成功后
serviceManager.startSocketService(userId)
```

### 3. 用户登出时停止服务

```kotlin
// 在登出时
serviceManager.stopSocketService()
```

## 功能特性

### 前台服务

**SocketForegroundService** 保持Socket.IO连接活跃：

- 显示持续通知 "轻聊正在运行"
- 处理通知栏点击（打开应用）
- 30秒心跳监控
- 自动重连机制
- START_STICKY（被杀后自动重启）

### WorkManager心跳

**HeartbeatWorker** 定期检查连接：

- 最小15分钟间隔（Android限制）
- 只在有网络时执行
- 电量不低时优先执行
- 省电模式自动降频

### 网络监听

**NetworkStateMonitor** 实时监听网络变化：

- 网络恢复时自动重连
- 网络断开时记录日志
- 区分WiFi/移动网络
- 提供网络状态Flow

### 应用生命周期

**AppLifecycleObserver** 处理前后台切换：

- 前台：确保Socket连接
- 后台：启动WorkManager心跳
- 重启：恢复服务

### 电源管理

**PowerManager** 处理Doze模式：

- 检测省电模式
- 检测Doze状态
- 智能心跳调整
- 电池优化建议

## 使用示例

### 获取连接状态报告

```kotlin
val report = serviceManager.getConnectionStatusReport()
Log.d("Service", report.getStatusDescription())

/*
输出示例：
Socket连接: 已连接
可重连: 是
重连次数: 0
网络状态: 可用 (WIFI)
省电模式: 关闭
Doze状态: WHITELISTED
*/
```

### 手动重连

```kotlin
// 检查网络状态
if (networkStateMonitor.isNetworkAvailable()) {
    // 触发重连
    SocketClient.reconnect()
}
```

### 监听网络状态

```kotlin
lifecycleScope.launch {
    networkStateMonitor.observeNetworkState().collect { isConnected ->
        if (isConnected) {
            // 网络可用
        } else {
            // 网络不可用
        }
    }
}
```

## 权限配置

### AndroidManifest.xml

```xml
<!-- 前台服务权限 -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />

<!-- 网络状态权限 -->
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- 电池优化白名单建议 -->
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />

<!-- 注册服务 -->
<service
    android:name=".service.SocketForegroundService"
    android:enabled="true"
    android:exported="false"
    android:foregroundServiceType="dataSync" />
```

## 电池优化建议

### 引导用户加入白名单

```kotlin
if (!powerManager.isIgnoringBatteryOptimizations()) {
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    startActivity(intent)
}
```

### Doze模式说明

| 状态 | 说明 | 心跳间隔 |
|------|------|----------|
| WHITELISTED | 已在白名单 | 正常 |
| NOT_WHITELISTED | 未在白名单 | 受限 |
| NOT_SUPPORTED | 系统不支持 | 正常 |

## 配置参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| 前台服务心跳 | 30秒 | Socket.IO ping间隔 |
| WorkManager间隔 | 15分钟 | 最小周期任务间隔 |
| 省电模式倍数 | 3x | 心跳间隔倍数 |
| 空闲模式倍数 | 2x | 心跳间隔倍数 |
| 最大重连次数 | 10次 | Socket.IO重连限制 |

## 故障排查

### 服务频繁重启

1. 检查是否在省电模式
2. 检查Doze白名单状态
3. 查看Battery Historian

### 连接不稳定

1. 检查网络状态变化
2. 调整心跳频率
3. 检查服务器负载

### 通知不显示

1. 检查通知权限
2. 检查通知渠道设置
3. 检查应用通知设置

## 最佳实践

1. **登录后立即启动服务**
2. **登出时停止服务**
3. **引导用户加入电池白名单**
4. **根据使用场景调整心跳频率**
5. **监控电池消耗**
6. **记录连接状态日志**

## 注意事项

- Android 8.0+ 后台服务限制
- WorkManager最小15分钟间隔
- Doze模式限制网络访问
- 部分厂商有额外省电策略
- 前台服务需要通知栏显示
