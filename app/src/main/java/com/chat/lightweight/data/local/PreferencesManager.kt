package com.chat.lightweight.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.chat.lightweight.data.model.UserData
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

/**
 * DataStore扩展属性
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "chat_preferences")

/**
 * PreferencesManager - 数据持久化管理器
 * 使用DataStore实现轻量级数据持久化
 *
 * 功能:
 * - 用户数据持久化 (登录信息)
 * - 对话列表缓存
 * - 消息缓存
 * - 用户设置存储
 */
class PreferencesManager(private val context: Context) {

    private val gson = Gson()

    companion object {
        @Volatile
        private var instance: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: PreferencesManager(context.applicationContext).also { instance = it }
            }
        }
    }

    // Preferences Keys
    private object Keys {
        val USER_ID = stringPreferencesKey("user_id")
        val USERNAME = stringPreferencesKey("username")
        val IS_ADMIN = booleanPreferencesKey("is_admin")
        val TOKEN = stringPreferencesKey("token")
        val USER_DATA_JSON = stringPreferencesKey("user_data_json")

        val CONVERSATIONS_CACHE = stringPreferencesKey("conversations_cache")
        val CONVERSATIONS_CACHE_TIME = stringPreferencesKey("conversations_cache_time")

        val MESSAGES_CACHE_PREFIX = "messages_cache_"
        val MESSAGES_CACHE_TIME_PREFIX = "messages_cache_time_"

        val SETTINGS_AUTO_DELETE = stringPreferencesKey("settings_auto_delete")
        val SETTINGS_NOTIFICATION_ENABLED = booleanPreferencesKey("settings_notification_enabled")
        val SETTINGS_SOUND_ENABLED = booleanPreferencesKey("settings_sound_enabled")
    }

    // ========== 用户数据管理 ==========

    /**
     * 保存用户数据
     */
    suspend fun saveUserData(userData: UserData) {
        context.dataStore.edit { preferences ->
            preferences[Keys.USER_ID] = userData.userId
            preferences[Keys.USERNAME] = userData.username
            preferences[Keys.IS_ADMIN] = userData.isAdmin
            preferences[Keys.TOKEN] = userData.token
            preferences[Keys.USER_DATA_JSON] = gson.toJson(userData)
        }
    }

    /**
     * 获取用户数据Flow
     */
    fun getUserDataFlow(): Flow<UserData?> {
        return context.dataStore.data.map { preferences ->
            val json = preferences[Keys.USER_DATA_JSON]
            if (json != null) {
                try {
                    gson.fromJson(json, UserData::class.java)
                } catch (e: Exception) {
                    // JSON解析失败,尝试使用单独的字段构建
                    val userId = preferences[Keys.USER_ID] ?: ""
                    val username = preferences[Keys.USERNAME] ?: ""
                    val isAdmin = preferences[Keys.IS_ADMIN] ?: false
                    val token = preferences[Keys.TOKEN] ?: ""
                    if (userId.isNotEmpty()) {
                        UserData(userId, username, isAdmin, token)
                    } else {
                        null
                    }
                }
            } else {
                null
            }
        }
    }

    /**
     * 获取用户数据 (同步方法,用于非协程环境)
     * 注意: 这个方法会阻塞当前线程,建议使用getUserDataFlow()
     */
    suspend fun getUserData(): UserData? {
        return getUserDataFlow().firstOrNull()
    }

    /**
     * 获取Token
     */
    fun getTokenFlow(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[Keys.TOKEN]
        }
    }

    /**
     * 获取用户ID
     */
    fun getUserIdFlow(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[Keys.USER_ID]
        }
    }

    /**
     * 获取用户ID (同步方法，返回缓存值)
     */
    suspend fun getUserId(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[Keys.USER_ID]
        }.first()
    }

    /**
     * 检查是否已登录
     */
    fun isLoggedInFlow(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            val userId = preferences[Keys.USER_ID]
            val token = preferences[Keys.TOKEN]
            !userId.isNullOrBlank() && !token.isNullOrBlank()
        }
    }

    /**
     * 检查是否为管理员
     */
    fun isAdminFlow(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[Keys.IS_ADMIN] ?: false
        }
    }

    /**
     * 清除用户数据 (登出)
     */
    suspend fun clearUserData() {
        context.dataStore.edit { preferences ->
            preferences.remove(Keys.USER_ID)
            preferences.remove(Keys.USERNAME)
            preferences.remove(Keys.IS_ADMIN)
            preferences.remove(Keys.TOKEN)
            preferences.remove(Keys.USER_DATA_JSON)
        }
    }

    // ========== 对话列表缓存 ==========

    /**
     * 保存对话列表缓存
     */
    suspend fun saveConversationsCache(conversationsJson: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.CONVERSATIONS_CACHE] = conversationsJson
            preferences[Keys.CONVERSATIONS_CACHE_TIME] = System.currentTimeMillis().toString()
        }
    }

    /**
     * 获取对话列表缓存
     */
    fun getConversationsCacheFlow(): Flow<Pair<String, Long>?> {
        return context.dataStore.data.map { preferences ->
            val cache = preferences[Keys.CONVERSATIONS_CACHE]
            val cacheTime = preferences[Keys.CONVERSATIONS_CACHE_TIME]?.toLongOrNull() ?: 0L
            if (cache != null) {
                Pair(cache, cacheTime)
            } else {
                null
            }
        }
    }

    /**
     * 清除对话列表缓存
     */
    suspend fun clearConversationsCache() {
        context.dataStore.edit { preferences ->
            preferences.remove(Keys.CONVERSATIONS_CACHE)
            preferences.remove(Keys.CONVERSATIONS_CACHE_TIME)
        }
    }

    // ========== 消息缓存 ==========

    /**
     * 保存消息缓存
     */
    suspend fun saveMessagesCache(conversationId: String, messagesJson: String) {
        context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey(Keys.MESSAGES_CACHE_PREFIX + conversationId)] = messagesJson
            preferences[stringPreferencesKey(Keys.MESSAGES_CACHE_TIME_PREFIX + conversationId)] =
                System.currentTimeMillis().toString()
        }
    }

    /**
     * 获取消息缓存
     */
    fun getMessagesCacheFlow(conversationId: String): Flow<Pair<String, Long>?> {
        return context.dataStore.data.map { preferences ->
            val cacheKey = stringPreferencesKey(Keys.MESSAGES_CACHE_PREFIX + conversationId)
            val timeKey = stringPreferencesKey(Keys.MESSAGES_CACHE_TIME_PREFIX + conversationId)

            val cache = preferences[cacheKey]
            val cacheTime = preferences[timeKey]?.toLongOrNull() ?: 0L

            if (cache != null) {
                Pair(cache, cacheTime)
            } else {
                null
            }
        }
    }

    /**
     * 清除指定对话的消息缓存
     */
    suspend fun clearMessagesCache(conversationId: String) {
        context.dataStore.edit { preferences ->
            val cacheKey = stringPreferencesKey(Keys.MESSAGES_CACHE_PREFIX + conversationId)
            val timeKey = stringPreferencesKey(Keys.MESSAGES_CACHE_TIME_PREFIX + conversationId)
            preferences.remove(cacheKey)
            preferences.remove(timeKey)
        }
    }

    /**
     * 清除所有消息缓存
     */
    suspend fun clearAllMessagesCache() {
        context.dataStore.edit { preferences ->
            preferences.asMap().keys.forEach { key ->
                if (key.name.startsWith(Keys.MESSAGES_CACHE_PREFIX) ||
                    key.name.startsWith(Keys.MESSAGES_CACHE_TIME_PREFIX)) {
                    preferences.remove(key)
                }
            }
        }
    }

    // ========== 用户设置 ==========

    /**
     * 保存自动删除设置
     */
    suspend fun saveAutoDeleteSetting(value: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.SETTINGS_AUTO_DELETE] = value
        }
    }

    /**
     * 获取自动删除设置
     */
    fun getAutoDeleteSettingFlow(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[Keys.SETTINGS_AUTO_DELETE] ?: "off"
        }
    }

    /**
     * 保存通知设置
     */
    suspend fun saveNotificationSetting(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.SETTINGS_NOTIFICATION_ENABLED] = enabled
        }
    }

    /**
     * 获取通知设置
     */
    fun getNotificationSettingFlow(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[Keys.SETTINGS_NOTIFICATION_ENABLED] ?: true
        }
    }

    /**
     * 保存声音设置
     */
    suspend fun saveSoundSetting(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.SETTINGS_SOUND_ENABLED] = enabled
        }
    }

    /**
     * 获取声音设置
     */
    fun getSoundSettingFlow(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[Keys.SETTINGS_SOUND_ENABLED] ?: true
        }
    }

    // ========== 全局操作 ==========

    /**
     * 清除所有数据 (包括缓存和设置)
     */
    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    /**
     * 清除所有缓存 (保留用户数据和设置)
     */
    suspend fun clearAllCache() {
        clearConversationsCache()
        clearAllMessagesCache()
    }
}
