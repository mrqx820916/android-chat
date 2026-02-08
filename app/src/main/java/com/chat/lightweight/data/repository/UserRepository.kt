package com.chat.lightweight.data.repository

import android.content.Context
import com.chat.lightweight.data.local.PreferencesManager
import com.chat.lightweight.data.model.UserData
import kotlinx.coroutines.flow.Flow

/**
 * 用户数据仓库
 * 负责管理用户数据的持久化和读取
 */
class UserRepository(context: Context) {

    private val preferencesManager = PreferencesManager.getInstance(context.applicationContext)

    companion object {
        @Volatile
        private var instance: UserRepository? = null

        fun getInstance(context: Context): UserRepository {
            return instance ?: synchronized(this) {
                instance ?: UserRepository(context.applicationContext).also { instance = it }
            }
        }

        fun resetInstance() {
            instance = null
        }
    }

    /**
     * 保存用户数据
     */
    suspend fun saveUserData(userData: UserData) {
        preferencesManager.saveUserData(userData)
    }

    /**
     * 获取用户数据Flow
     */
    fun getUserDataFlow(): Flow<UserData?> {
        return preferencesManager.getUserDataFlow()
    }

    /**
     * 获取用户数据 (同步)
     */
    suspend fun getUserData(): UserData? {
        return preferencesManager.getUserData()
    }

    /**
     * 获取Token Flow
     */
    fun getTokenFlow(): Flow<String?> {
        return preferencesManager.getTokenFlow()
    }

    /**
     * 获取用户ID Flow
     */
    fun getUserIdFlow(): Flow<String?> {
        return preferencesManager.getUserIdFlow()
    }

    /**
     * 检查是否已登录
     */
    fun isLoggedInFlow(): Flow<Boolean> {
        return preferencesManager.isLoggedInFlow()
    }

    /**
     * 检查是否为管理员
     */
    fun isAdminFlow(): Flow<Boolean> {
        return preferencesManager.isAdminFlow()
    }

    /**
     * 登出 (清除用户数据)
     */
    suspend fun logout() {
        preferencesManager.clearUserData()
    }

    /**
     * 更新Token
     */
    suspend fun updateToken(token: String) {
        val currentUserData = getUserData()
        if (currentUserData != null) {
            saveUserData(currentUserData.copy(token = token))
        }
    }
}
