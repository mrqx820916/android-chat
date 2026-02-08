package com.chat.lightweight.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore管理器
 * 负责本地数据持久化
 */
class DataStoreManager private constructor(private val context: Context) {

    companion object {
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USERNAME_KEY = stringPreferencesKey("username")
        private val IS_ADMIN_KEY = booleanPreferencesKey("is_admin")
        private val TOKEN_KEY = stringPreferencesKey("token")
        private val FCM_TOKEN_KEY = stringPreferencesKey("fcm_token")

        @Volatile
        private var instance: DataStoreManager? = null

        fun getInstance(context: Context): DataStoreManager {
            return instance ?: synchronized(this) {
                instance ?: DataStoreManager(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * 保存用户ID
     */
    suspend fun saveUserId(userId: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = userId
        }
    }

    /**
     * 获取用户ID
     */
    fun getUserId(): Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_ID_KEY]
    }

    /**
     * 保存用户名
     */
    suspend fun saveUsername(username: String) {
        context.dataStore.edit { preferences ->
            preferences[USERNAME_KEY] = username
        }
    }

    /**
     * 获取用户名
     */
    fun getUsername(): Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USERNAME_KEY]
    }

    /**
     * 保存管理员状态
     */
    suspend fun saveIsAdmin(isAdmin: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_ADMIN_KEY] = isAdmin
        }
    }

    /**
     * 获取管理员状态
     */
    fun getIsAdmin(): Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_ADMIN_KEY] ?: false
    }

    /**
     * 保存Token
     */
    suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
        }
    }

    /**
     * 获取Token
     */
    fun getToken(): Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[TOKEN_KEY]
    }

    /**
     * 清除用户数据
     */
    suspend fun clearUserData() {
        context.dataStore.edit { preferences ->
            preferences.remove(USER_ID_KEY)
            preferences.remove(USERNAME_KEY)
            preferences.remove(IS_ADMIN_KEY)
            preferences.remove(TOKEN_KEY)
        }
    }

    /**
     * 保存FCM Token
     */
    suspend fun saveFcmToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[FCM_TOKEN_KEY] = token
        }
    }

    /**
     * 获取FCM Token
     */
    fun getFcmToken(): Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[FCM_TOKEN_KEY]
    }

    /**
     * 同步获取用户ID（用于非协程环境）
     */
    suspend fun getUserIdSync(): String? {
        var result: String? = null
        getUserId().collect { result = it }
        return result
    }

    /**
     * 同步获取用户名（用于非协程环境）
     */
    suspend fun getUsernameSync(): String? {
        var result: String? = null
        getUsername().collect { result = it }
        return result
    }
}

// DataStore扩展属性
private val Context.dataStore: DataStore<Preferences> by androidx.datastore.preferences.preferencesDataStore(
    name = "user_preferences"
)
