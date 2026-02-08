package com.chat.lightweight.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chat.lightweight.data.repository.SettingsRepository
import com.chat.lightweight.network.NetworkRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 自动删除设置UI状态
 */
data class AutoDeleteUiState(
    val isLoading: Boolean = false,
    val enabled: Boolean = false,
    val unit: String = "permanent",
    val value: Int = 0,
    val error: String? = null,
    val saveSuccess: Boolean = false
)

/**
 * 自动删除设置ViewModel
 * 遵循单一职责原则，仅负责自动删除设置的业务逻辑
 */
class AutoDeleteViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository.getInstance(application)
    private val networkRepository = NetworkRepository.getInstance()

    private val _uiState = MutableStateFlow(AutoDeleteUiState())
    val uiState: StateFlow<AutoDeleteUiState> = _uiState.asStateFlow()

    /**
     * 加载设置
     */
    fun loadSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // 从网络获取设置
                when (val result = networkRepository.getAutoDeleteSetting()) {
                    is com.chat.lightweight.network.ApiResponse.Success -> {
                        val setting = result.data
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            enabled = setting.enabled,
                            unit = setting.unit,
                            value = setting.value,
                            error = null
                        )
                    }
                    is com.chat.lightweight.network.ApiResponse.Error -> {
                        // 从本地缓存加载
                        val cachedValue = settingsRepository.getAutoDeleteSettingFlow().first()
                        parseCachedValue(cachedValue)
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "加载失败"
                )
            }
        }
    }

    /**
     * 保存设置
     */
    fun saveSettings(enabled: Boolean, unit: String, value: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val result = networkRepository.updateAutoDeleteSetting(enabled, unit, value)
                when (result) {
                    is com.chat.lightweight.network.ApiResponse.Success -> {
                        // 保存到本地缓存
                        val cachedValue = if (enabled) "$unit:$value" else "off"
                        settingsRepository.saveAutoDeleteSetting(cachedValue)

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            enabled = enabled,
                            unit = unit,
                            value = value,
                            saveSuccess = true,
                            error = null
                        )
                    }
                    is com.chat.lightweight.network.ApiResponse.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.message ?: "保存失败"
                        )
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "保存失败"
                )
            }
        }
    }

    /**
     * 解析缓存的设置值
     */
    private fun parseCachedValue(value: String) {
        if (value == "off") {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                enabled = false,
                unit = "permanent",
                value = 0
            )
        } else {
            val parts = value.split(":")
            if (parts.size == 2) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    enabled = true,
                    unit = parts[0],
                    value = parts[1].toIntOrNull() ?: 0
                )
            }
        }
    }

    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
