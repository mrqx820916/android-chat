package com.chat.lightweight.service

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * WorkManager 调度器
 * 管理周期性心跳任务和一次性任务
 */
class WorkManagerScheduler private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var instance: WorkManagerScheduler? = null

        fun getInstance(context: Context): WorkManagerScheduler {
            return instance ?: synchronized(this) {
                instance ?: WorkManagerScheduler(context.applicationContext).also { instance = it }
            }
        }
    }

    private val workManager = WorkManager.getInstance(context)

    /**
     * 启动心跳任务
     * 定期检查Socket连接状态
     */
    fun startHeartbeatWork() {
        // 约束：只在有网络时执行
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true) // 电量不低时执行
            .build()

        // 周期性任务：最小间隔15分钟
        val heartbeatRequest = PeriodicWorkRequestBuilder<HeartbeatWorker>(
            15, TimeUnit.MINUTES // 重复间隔（最小15分钟）
        ).setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            HeartbeatWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // 保留现有任务
            heartbeatRequest
        )

        Timber.d("HeartbeatWork scheduled")
    }

    /**
     * 停止心跳任务
     */
    fun stopHeartbeatWork() {
        workManager.cancelUniqueWork(HeartbeatWorker.WORK_NAME)
        Timber.d("HeartbeatWork cancelled")
    }

    /**
     * 立即执行一次心跳任务
     */
    fun enqueueOneTimeHeartbeat() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = androidx.work.OneTimeWorkRequestBuilder<HeartbeatWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueue(request)
        Timber.d("One-time heartbeat work enqueued")
    }

    /**
     * 取消所有工作
     */
    fun cancelAllWork() {
        workManager.cancelAllWork()
        Timber.d("All work cancelled")
    }

    /**
     * 清理已完成的工作
     */
    fun pruneWork() {
        workManager.pruneWork()
        Timber.d("Work pruned")
    }
}
