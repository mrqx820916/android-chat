package com.chat.lightweight.service.fcm

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.chat.lightweight.R
import com.chat.lightweight.presentation.ui.chat.ChatDetailActivity
import com.google.firebase.messaging.FirebaseMessaging

/**
 * 通知辅助类
 * 管理通知的显示和配置
 */
class NotificationHelper(private val context: Context) {

    companion object {
        // 通知ID
        private const val NOTIFICATION_ID_FOREGROUND = 2001
        private const val NOTIFICATION_ID_MESSAGE = 2002

        // 通知渠道
        const val CHANNEL_ID_HIGH = "high_priority_channel"
        const val CHANNEL_ID_DEFAULT = "default_channel"
        const val CHANNEL_ID_FOREGROUND = "foreground_service_channel"
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    /**
     * 获取FCM Token
     */
    fun getFcmToken(callback: (String?) -> Unit) {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            callback(token)
        }.addOnFailureListener { e ->
            callback(null)
        }
    }

    /**
     * 显示消息通知
     */
    fun showMessageNotification(
        title: String,
        content: String,
        conversationId: String? = null,
        messageId: String? = null,
        senderId: String? = null
    ) {
        val intent = createNotificationIntent(conversationId, messageId, senderId)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_HIGH)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID_MESSAGE, notification)
    }

    /**
     * 创建前台服务通知
     */
    fun createForegroundServiceNotification(): Notification {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_FOREGROUND)
            .setContentTitle("轻聊")
            .setContentText("保持连接中...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        return notification
    }

    /**
     * 更新前台服务通知
     */
    fun updateForegroundServiceNotification(text: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_FOREGROUND)
            .setContentTitle("轻聊")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_FOREGROUND, notification)
    }

    /**
     * 取消通知
     */
    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    /**
     * 取消所有通知
     */
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }

    /**
     * 创建通知Intent
     */
    private fun createNotificationIntent(
        conversationId: String?,
        messageId: String?,
        senderId: String?
    ): Intent {
        return Intent(context, ChatDetailActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            conversationId?.let { putExtra(FcmService.EXTRA_CONVERSATION_ID, it) }
            messageId?.let { putExtra(FcmService.EXTRA_MESSAGE_ID, it) }
            senderId?.let { putExtra(FcmService.EXTRA_SENDER_ID, it) }
        }
    }
}
