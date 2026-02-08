package com.chat.lightweight.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.chat.lightweight.socket.SocketClient
import timber.log.Timber

/**
 * WorkManager 心跳Worker
 * 定期检查Socket连接状态，必要时重连
 * 用于在应用被杀后恢复连接
 */
class HeartbeatWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Timber.d("HeartbeatWorker: Checking socket connection...")

        return try {
            // 检查Socket连接状态
            if (!SocketClient.isConnected()) {
                Timber.w("HeartbeatWorker: Socket not connected, attempting reconnect...")

                // 尝试重连
                if (SocketClient.canReconnect()) {
                    SocketClient.reconnect()

                    // 等待重连结果
                    // 注意：这里只是触发重连，实际连接是异步的
                    Timber.d("HeartbeatWorker: Reconnect triggered")
                } else {
                    Timber.w("HeartbeatWorker: Cannot reconnect, max attempts reached")
                }

                Result.success()
            } else {
                Timber.d("HeartbeatWorker: Socket connected, no action needed")
                Result.success()
            }
        } catch (e: Exception) {
            Timber.e(e, "HeartbeatWorker: Error checking socket connection")
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "heartbeat_worker"
    }
}
