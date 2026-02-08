package com.chat.lightweight.service.fcm

import android.content.Context
import android.util.Log
import com.chat.lightweight.data.local.DataStoreManager
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber

/**
 * FCM Token管理器
 * 负责Token的获取、缓存和上报
 */
class FcmTokenManager private constructor(context: Context) {

    companion object {
        private const val KEY_FCM_TOKEN = "fcm_token"
        private const val TAG = "FcmTokenManager"

        @Volatile
        private var instance: FcmTokenManager? = null

        fun getInstance(context: Context): FcmTokenManager {
            return instance ?: synchronized(this) {
                instance ?: FcmTokenManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val dataStoreManager = DataStoreManager.getInstance(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * 初始化FCM Token
     */
    fun initializeToken() {
        scope.launch {
            try {
                // 获取当前Token
                val currentToken = FirebaseMessaging.getInstance().token.await()
                Timber.d("FCM Token: $currentToken")

                // 检查是否需要上报
                val cachedToken = getCachedToken()
                if (cachedToken != currentToken && currentToken != null) {
                    // Token已更新，上报到服务器
                    cacheToken(currentToken)
                    sendTokenToServer(currentToken)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to get FCM token")
            }
        }
    }

    /**
     * 获取FCM Token
     */
    suspend fun getToken(): String? {
        return try {
            FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get FCM token")
            null
        }
    }

    /**
     * 刷新Token
     */
    fun refreshToken() {
        scope.launch {
            try {
                // 删除当前Token，Firebase会自动生成新Token
                FirebaseMessaging.getInstance().deleteToken()
                val newToken = FirebaseMessaging.getInstance().token.await()
                Timber.d("FCM Token refreshed: $newToken")

                if (newToken != null) {
                    cacheToken(newToken)
                    sendTokenToServer(newToken)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to refresh FCM token")
            }
        }
    }

    /**
     * 缓存Token到本地
     */
    private suspend fun cacheToken(token: String) {
        dataStoreManager.saveFcmToken(token)
        Timber.d("FCM Token cached: $token")
    }

    /**
     * 获取缓存的Token
     */
    private suspend fun getCachedToken(): String? {
        return dataStoreManager.getFcmToken().firstOrNull()
    }

    /**
     * 上报Token到服务器
     */
    private fun sendTokenToServer(token: String) {
        scope.launch {
            try {
                // TODO: 实现Token上报逻辑
                // 可以通过REST API上报
                Timber.d("Sending FCM token to server: $token")
            } catch (e: Exception) {
                Timber.e(e, "Failed to send FCM token to server")
            }
        }
    }

    /**
     * 清除Token
     */
    suspend fun clearToken() {
        dataStoreManager.saveFcmToken("")
        Timber.d("FCM Token cleared")
    }
}
