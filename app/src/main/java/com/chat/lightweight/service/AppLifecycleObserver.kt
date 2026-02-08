package com.chat.lightweight.service

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.chat.lightweight.data.local.DataStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * 应用生命周期监听器
 * 监听应用前后台切换，处理自动重连
 */
class AppLifecycleObserver private constructor(private val context: Application) : DefaultLifecycleObserver {

    companion object {
        @Volatile
        private var instance: AppLifecycleObserver? = null

        fun getInstance(context: Application): AppLifecycleObserver {
            return instance ?: synchronized(this) {
                instance ?: AppLifecycleObserver(context).also { instance = it }
            }
        }
    }

    private val dataStoreManager = DataStoreManager.getInstance(context)
    private val workManagerScheduler = WorkManagerScheduler.getInstance(context)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var isAppInForeground = true

    /**
     * 初始化监听
     */
    fun init() {
        // ProcessLifecycleOwner 需要额外的依赖，暂时禁用
        Timber.d("AppLifecycleObserver initialized")
    }

    /**
     * 应用进入前台
     */
    override fun onResume(owner: LifecycleOwner) {
        // 功能暂时禁用
    }

    /**
     * 应用进入后台
     */
    override fun onPause(owner: LifecycleOwner) {
        // 功能暂时禁用
    }

    /**
     * 应用前台处理
     */
    private fun onAppForeground() {
        scope.launch {
            try {
                // 检查用户是否已登录
                val userId = dataStoreManager.getUserId().firstOrNull()
                if (userId != null) {
                    Timber.d("User logged in, ensuring socket connection...")
                    // 确保Socket连接
                    ensureSocketConnection(userId)
                } else {
                    Timber.d("No user logged in")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error checking login status")
            }
        }
    }

    /**
     * 应用后台处理
     */
    private fun onAppBackground() {
        // 启动WorkManager心跳任务
        // 确保应用在后台也能保持连接
        workManagerScheduler.startHeartbeatWork()
    }

    /**
     * 确保Socket连接
     */
    private fun ensureSocketConnection(userId: String) {
        // 如果Socket未连接，触发重连
        if (!com.chat.lightweight.socket.SocketClient.isConnected()) {
            Timber.d("Socket not connected, reconnecting...")
            com.chat.lightweight.socket.SocketClient.connect(userId)
        }

        // 启动前台服务
        SocketForegroundService.startService(context, userId)
    }

    /**
     * 处理应用重启
     * 在Application.onCreate中调用
     */
    fun handleAppRestart() {
        Timber.d("Handling app restart")

        scope.launch {
            try {
                // 检查用户是否已登录
                val userId = dataStoreManager.getUserId().firstOrNull()
                if (userId != null) {
                    Timber.d("User logged in after restart, starting service...")

                    // 初始化Socket
                    com.chat.lightweight.socket.SocketClient.init()

                    // 启动前台服务
                    SocketForegroundService.startService(context, userId)

                    // 启动心跳任务
                    workManagerScheduler.startHeartbeatWork()
                }
            } catch (e: Exception) {
                Timber.e(e, "Error handling app restart")
            }
        }
    }
}
