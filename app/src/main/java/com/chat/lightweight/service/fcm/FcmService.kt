package com.chat.lightweight.service.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.chat.lightweight.R
import com.chat.lightweight.ui.chat.ChatDetailActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import timber.log.Timber

/**
 * FCM推送通知服务
 * 处理Firebase推送消息
 */
class FcmService : FirebaseMessagingService() {

    companion object {
        // 通知渠道ID
        private const val CHANNEL_ID_HIGH = "high_priority_channel"
        private const val CHANNEL_ID_DEFAULT = "default_channel"
        private const val CHANNEL_ID_FOREGROUND = "foreground_service_channel"

        // 通知ID
        private const val NOTIFICATION_ID = 1001

        // Intent extra keys
        const val EXTRA_CONVERSATION_ID = "conversation_id"
        const val EXTRA_MESSAGE_ID = "message_id"
        const val EXTRA_SENDER_ID = "sender_id"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("FCM Token refreshed: $token")

        // 上报Token到服务器
        sendTokenToServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Timber.d("FCM Message received from: ${remoteMessage.from}")

        // 处理消息
        remoteMessage.notification?.let { notification ->
            val title = notification.title ?: "新消息"
            val body = notification.body ?: ""
            val conversationId = remoteMessage.data["conversation_id"]
            val messageId = remoteMessage.data["message_id"]
            val senderId = remoteMessage.data["sender_id"]

            sendNotification(title, body, conversationId, messageId, senderId)
        }

        // 处理数据消息
        if (remoteMessage.data.isNotEmpty()) {
            Timber.d("Message data payload: ${remoteMessage.data}")

            // 可以在这里处理自定义数据
            val type = remoteMessage.data["type"]
            when (type) {
                "new_message" -> handleNewMessage(remoteMessage.data)
                "message_deleted" -> handleMessageDeleted(remoteMessage.data)
                "user_online" -> handleUserOnline(remoteMessage.data)
                "user_offline" -> handleUserOffline(remoteMessage.data)
            }
        }
    }

    /**
     * 发送通知
     */
    private fun sendNotification(
        title: String,
        messageBody: String,
        conversationId: String? = null,
        messageId: String? = null,
        senderId: String? = null
    ) {
        val notificationManager = NotificationManagerCompat.from(this)

        // 创建通知渠道 (Android 8.0+)
        createNotificationChannels()

        // 创建点击Intent
        val intent = Intent(this, ChatDetailActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            conversationId?.let { putExtra(EXTRA_CONVERSATION_ID, it) }
            messageId?.let { putExtra(EXTRA_MESSAGE_ID, it) }
            senderId?.let { putExtra(EXTRA_SENDER_ID, it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 构建通知
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID_HIGH)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setContentIntent(pendingIntent)

        // 显示通知
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    /**
     * 创建通知渠道
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 高优先级渠道 (紧急消息)
            val highPriorityChannel = NotificationChannel(
                CHANNEL_ID_HIGH,
                "紧急消息",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "用于显示紧急聊天消息"
                enableVibration(true)
                enableLights(true)
            }

            // 默认渠道 (普通消息)
            val defaultChannel = NotificationChannel(
                CHANNEL_ID_DEFAULT,
                "普通消息",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "用于显示普通聊天消息"
                enableVibration(true)
            }

            // 前台服务渠道
            val foregroundChannel = NotificationChannel(
                CHANNEL_ID_FOREGROUND,
                "前台服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "用于保持Socket连接的前台服务"
                setShowBadge(false)
            }

            notificationManager.createNotificationChannel(highPriorityChannel)
            notificationManager.createNotificationChannel(defaultChannel)
            notificationManager.createNotificationChannel(foregroundChannel)
        }
    }

    /**
     * 上报Token到服务器
     */
    private fun sendTokenToServer(token: String) {
        // TODO: 实现Token上报逻辑
        // 可以通过REST API或Socket.IO上报
        Timber.d("Sending FCM token to server: $token")
    }

    /**
     * 处理新消息
     */
    private fun handleNewMessage(data: Map<String, String>) {
        val conversationId = data["conversation_id"]
        val messageId = data["message_id"]
        val senderId = data["sender_id"]
        val content = data["content"]

        Timber.d("New message: conv=$conversationId, msg=$messageId, sender=$senderId")
        // 可以在这里更新本地缓存或发送本地广播
    }

    /**
     * 处理消息删除
     */
    private fun handleMessageDeleted(data: Map<String, String>) {
        val messageId = data["message_id"]
        Timber.d("Message deleted: $messageId")
        // 可以在这里更新UI
    }

    /**
     * 处理用户上线
     */
    private fun handleUserOnline(data: Map<String, String>) {
        val userId = data["user_id"]
        Timber.d("User online: $userId")
        // 可以在这里更新用户状态
    }

    /**
     * 处理用户下线
     */
    private fun handleUserOffline(data: Map<String, String>) {
        val userId = data["user_id"]
        Timber.d("User offline: $userId")
        // 可以在这里更新用户状态
    }
}
