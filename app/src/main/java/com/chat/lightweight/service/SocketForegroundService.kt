package com.chat.lightweight.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.chat.lightweight.R
import com.chat.lightweight.socket.SocketClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Socket.IO 前台服务
 * 保持Socket.IO连接活跃，处理后台消息接收
 */
class SocketForegroundService : Service() {

    companion object {
        private const val TAG = "SocketForegroundService"
        private const val CHANNEL_ID = "socket_service_channel"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.chat.lightweight.START_SOCKET_SERVICE"
        const val ACTION_STOP = "com.chat.lightweight.STOP_SOCKET_SERVICE"
        const val EXTRA_USER_ID = "user_id"

        /**
         * 启动服务
         */
        fun startService(context: Context, userId: String) {
            val intent = Intent(context, SocketForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_USER_ID, userId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * 停止服务
         */
        fun stopService(context: Context) {
            val intent = Intent(context, SocketForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private lateinit var networkStateMonitor: NetworkStateMonitor
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var userId: String? = null
    private var isServiceRunning = false

    override fun onCreate() {
        super.onCreate()
        Timber.d("SocketForegroundService: onCreate")
        networkStateMonitor = NetworkStateMonitor.getInstance(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("SocketForegroundService: onStartCommand, action=${intent?.action}")

        when (intent?.action) {
            ACTION_START -> {
                val newUserId = intent.getStringExtra(EXTRA_USER_ID)
                if (newUserId != null) {
                    startSocketService(newUserId)
                }
            }
            ACTION_STOP -> {
                stopSocketService()
            }
        }

        // 如果服务被杀，自动重启
        return START_STICKY
    }

    /**
     * 启动Socket服务
     */
    private fun startSocketService(newUserId: String) {
        if (isServiceRunning && userId == newUserId) {
            Timber.d("Service already running for user: $newUserId")
            return
        }

        userId = newUserId
        isServiceRunning = true

        // 启动前台通知
        startForeground(NOTIFICATION_ID, createNotification())

        // 初始化Socket
        SocketClient.init()

        // 连接Socket
        userId?.let { SocketClient.connect(it) }

        // 启动心跳监控
        startHeartbeatMonitor()

        // 启动网络监听
        networkStateMonitor.startMonitoring(
            onNetworkAvailable = {
                Timber.d("Network available, reconnecting socket...")
                reconnectSocket()
            },
            onNetworkLost = {
                Timber.d("Network lost")
            }
        )

        Timber.d("SocketForegroundService started for user: $newUserId")
    }

    /**
     * 停止Socket服务
     */
    private fun stopSocketService() {
        Timber.d("Stopping SocketForegroundService")

        isServiceRunning = false

        // 停止网络监听
        networkStateMonitor.stopMonitoring()

        // 断开Socket
        SocketClient.disconnect()

        // 停止前台服务
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        Timber.d("SocketForegroundService stopped")
    }

    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Socket服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保持Socket.IO连接，接收实时消息"
                setShowBadge(false)
                setSound(null, null)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 创建持续通知
     */
    private fun createNotification(): Notification {
        // 点击通知打开对话列表
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, com.chat.lightweight.ui.conversation.ConversationListActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("轻聊正在运行")
            .setContentText("保持连接，实时接收消息")
            .setSmallIcon(R.drawable.ic_chat)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    /**
     * 启动心跳监控
     * 定期检查Socket连接状态，必要时重连
     */
    private fun startHeartbeatMonitor() {
        serviceScope.launch {
            while (isServiceRunning) {
                delay(30000) // 每30秒检查一次

                if (!SocketClient.isConnected() && SocketClient.canReconnect()) {
                    Timber.d("Heartbeat check: Socket not connected, reconnecting...")
                    reconnectSocket()
                } else {
                    Timber.d("Heartbeat check: Socket connected")
                }
            }
        }
    }

    /**
     * 重连Socket
     */
    private fun reconnectSocket() {
        userId?.let {
            SocketClient.reconnect()
            // 如果重连失败，延迟后再次尝试
            serviceScope.launch {
                delay(5000)
                if (!SocketClient.isConnected() && SocketClient.canReconnect()) {
                    Timber.d("Retry reconnect...")
                    SocketClient.reconnect()
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // 不支持绑定
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("SocketForegroundService: onDestroy")
        stopSocketService()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Timber.d("SocketForegroundService: onTaskRemoved")
        // 应用被移除时，保持服务运行
        // 不调用stopSelf
    }
}
