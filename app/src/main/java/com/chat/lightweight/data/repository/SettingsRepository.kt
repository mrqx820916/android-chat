package com.chat.lightweight.data.repository

import android.content.Context
import com.chat.lightweight.data.local.PreferencesManager
import kotlinx.coroutines.flow.Flow

/**
 * 设置仓库
 * 负责管理应用设置
 */
class SettingsRepository(context: Context) {

    private val preferencesManager = PreferencesManager.getInstance(context.applicationContext)

    companion object {
        @Volatile
        private var instance: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: SettingsRepository(context.applicationContext).also { instance = it }
            }
        }

        fun resetInstance() {
            instance = null
        }
    }

    // ========== 自动删除设置 ==========

    /**
     * 保存自动删除设置
     * @param value 可选值: "off", "1min", "5min", "30min", "1hour", "24hours"
     */
    suspend fun saveAutoDeleteSetting(value: String) {
        preferencesManager.saveAutoDeleteSetting(value)
    }

    /**
     * 获取自动删除设置
     */
    fun getAutoDeleteSettingFlow(): Flow<String> {
        return preferencesManager.getAutoDeleteSettingFlow()
    }

    // ========== 通知设置 ==========

    /**
     * 保存通知设置
     */
    suspend fun saveNotificationSetting(enabled: Boolean) {
        preferencesManager.saveNotificationSetting(enabled)
    }

    /**
     * 获取通知设置
     */
    fun getNotificationSettingFlow(): Flow<Boolean> {
        return preferencesManager.getNotificationSettingFlow()
    }

    // ========== 声音设置 ==========

    /**
     * 保存声音设置
     */
    suspend fun saveSoundSetting(enabled: Boolean) {
        preferencesManager.saveSoundSetting(enabled)
    }

    /**
     * 获取声音设置
     */
    fun getSoundSettingFlow(): Flow<Boolean> {
        return preferencesManager.getSoundSettingFlow()
    }
}
