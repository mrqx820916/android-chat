package com.chat.lightweight.service

import android.content.Context
import com.chat.lightweight.socket.SocketClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * 服务管理器
 * 统一管理前台服务、WorkManager和应用生命周期
 */
class ServiceManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var instance: ServiceManager? = null

        fun getInstance(context: Context): ServiceManager {
            return instance ?: synchronized(this) {
                instance ?: ServiceManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val workManagerScheduler = WorkManagerScheduler.getInstance(context)
    private val networkStateMonitor = NetworkStateMonitor.getInstance(context)
    private val powerManager = PowerManager.getInstance(context)
    private val appLifecycleObserver = AppLifecycleObserver.getInstance(context as android.app.Application)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isInitialized = false

    /**
     * 初始化服务管理器
     * 在Application.onCreate中调用
     */
    fun initialize() {
        if (isInitialized) return
        isInitialized = true

        Timber.d("ServiceManager initializing...")

        // 初始化应用生命周期监听
        appLifecycleObserver.init()

        // 启动网络监听
        startNetworkMonitoring()

        // 启动WorkManager心跳
        workManagerScheduler.startHeartbeatWork()

        Timber.d("ServiceManager initialized")
    }

    /**
     * 启动Socket服务（用户登录后调用）
     */
    fun startSocketService(userId: String) {
        Timber.d("Starting socket service for user: $userId")

        // 初始化Socket
        SocketClient.init()

        // 启动前台服务
        SocketForegroundService.startService(context, userId)

        // 启动WorkManager心跳
        workManagerScheduler.startHeartbeatWork()

        // 立即执行一次心跳检查
        workManagerScheduler.enqueueOneTimeHeartbeat()
    }

    /**
     * 停止Socket服务（用户登出时调用）
     */
    fun stopSocketService() {
        Timber.d("Stopping socket service")

        // 停止前台服务
        SocketForegroundService.stopService(context)

        // 断开Socket
        SocketClient.disconnect()

        // 停止WorkManager
        workManagerScheduler.stopHeartbeatWork()
    }

    /**
     * 启动网络监听
     */
    private fun startNetworkMonitoring() {
        networkStateMonitor.startMonitoring(
            onNetworkAvailable = {
                Timber.d("Network available, reconnecting...")
                handleNetworkAvailable()
            },
            onNetworkLost = {
                Timber.d("Network lost")
            }
        )
    }

    /**
     * 处理网络可用
     */
    private fun handleNetworkAvailable() {
        scope.launch {
            if (SocketClient.isConnected()) {
                Timber.d("Socket already connected")
            } else if (SocketClient.canReconnect()) {
                Timber.d("Reconnecting socket...")
                SocketClient.reconnect()
            } else {
                Timber.w("Cannot reconnect: max attempts reached")
            }
        }
    }

    /**
     * 获取连接状态报告
     */
    fun getConnectionStatusReport(): ConnectionStatusReport {
        return ConnectionStatusReport(
            isConnected = SocketClient.isConnected(),
            canReconnect = SocketClient.canReconnect(),
            reconnectAttempts = SocketClient.getReconnectAttempts(),
            isNetworkAvailable = networkStateMonitor.isNetworkAvailable(),
            networkType = networkStateMonitor.getNetworkType(),
            isPowerSaveMode = powerManager.isPowerSaveMode(),
            dozeState = powerManager.getDozeState()
        )
    }

    /**
     * 连接状态报告
     */
    data class ConnectionStatusReport(
        val isConnected: Boolean,
        val canReconnect: Boolean,
        val reconnectAttempts: Int,
        val isNetworkAvailable: Boolean,
        val networkType: NetworkStateMonitor.NetworkType,
        val isPowerSaveMode: Boolean,
        val dozeState: PowerManager.DozeState
    ) {
        fun getStatusDescription(): String {
            return """
                Socket连接: ${if (isConnected) "已连接" else "未连接"}
                可重连: ${if (canReconnect) "是" else "否"}
                重连次数: $reconnectAttempts
                网络状态: ${if (isNetworkAvailable) "可用" else "不可"} ($networkType)
                省电模式: ${if (isPowerSaveMode) "开启" else "关闭"}
                Doze状态: $dozeState
            """.trimIndent()
        }
    }
}
