package com.chat.lightweight.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chat.lightweight.data.model.UserData
import com.chat.lightweight.data.repository.RepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 认证ViewModel
 * 示例: 如何使用数据持久化
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository = RepositoryProvider.getUserRepository(application)

    // 登录状态
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // 用户数据
    private val _userData = MutableStateFlow<UserData?>(null)
    val userData: StateFlow<UserData?> = _userData.asStateFlow()

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 错误消息
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        // 检查登录状态
        checkLoginStatus()
    }

    /**
     * 检查登录状态 (从持久化存储中读取)
     */
    private fun checkLoginStatus() {
        viewModelScope.launch {
            _isLoading.value = true

            // 监听登录状态
            userRepository.isLoggedInFlow().collect { isLoggedIn ->
                _isLoggedIn.value = isLoggedIn

                if (isLoggedIn) {
                    // 已登录,加载用户数据
                    loadUserData()
                } else {
                    _userData.value = null
                }

                _isLoading.value = false
            }
        }
    }

    /**
     * 加载用户数据
     */
    private fun loadUserData() {
        viewModelScope.launch {
            userRepository.getUserDataFlow().collect { userData ->
                _userData.value = userData
            }
        }
    }

    /**
     * 保存登录信息 (持久化)
     */
    fun saveLoginData(userData: UserData) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                // 保存到持久化存储
                userRepository.saveUserData(userData)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "保存登录信息失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 登出 (清除持久化数据)
     */
    fun logout() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                // 清除持久化数据
                userRepository.logout()
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "登出失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 清除错误消息
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * 示例: 登录函数
     */
    fun login(username: String, password: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // TODO: 这里应该调用API进行登录验证
                // 示例代码: 模拟登录成功
                val mockUserData = UserData(
                    userId = "user_${System.currentTimeMillis()}",
                    username = username,
                    isAdmin = false,
                    token = "mock_token_${System.currentTimeMillis()}"
                )

                // 保存登录信息到持久化存储
                saveLoginData(mockUserData)

                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "登录失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 示例: 注册函数
     */
    fun register(username: String, password: String, isAdmin: Boolean) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // TODO: 这里应该调用API进行注册
                // 示例代码: 模拟注册成功
                val mockUserData = UserData(
                    userId = "user_${System.currentTimeMillis()}",
                    username = username,
                    isAdmin = isAdmin,
                    token = "mock_token_${System.currentTimeMillis()}"
                )

                // 保存登录信息到持久化存储
                saveLoginData(mockUserData)

                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "注册失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
