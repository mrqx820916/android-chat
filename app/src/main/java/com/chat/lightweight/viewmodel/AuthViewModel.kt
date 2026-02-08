package com.chat.lightweight.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chat.lightweight.network.ApiResponse
import com.chat.lightweight.network.NetworkRepository
import com.chat.lightweight.network.model.AuthResponse
import kotlinx.coroutines.launch

/**
 * 认证ViewModel示例
 * 演示如何使用NetworkRepository进行API调用
 */
class AuthViewModel : ViewModel() {

    private var repository: NetworkRepository? = null

    // 登录状态LiveData
    private val _loginState = MutableLiveData<ApiResponse<AuthResponse>>()
    val loginState: LiveData<ApiResponse<AuthResponse>> = _loginState

    // 注册状态LiveData
    private val _registerState = MutableLiveData<ApiResponse<AuthResponse>>()
    val registerState: LiveData<ApiResponse<AuthResponse>> = _registerState

    // 加载状态
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    /**
     * 用户登录
     */
    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _loginState.value = ApiResponse.Error(400, "用户名和密码不能为空")
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            // 使用临时userId进行登录（登录时userId为空）
            val tempRepo = NetworkRepository.init("")
            val result = tempRepo.login(username, password)

            _loginState.value = result
            _isLoading.value = false

            // 登录成功后初始化repository
            if (result is ApiResponse.Success) {
                repository = NetworkRepository.init(result.data.userId)
            }
        }
    }

    /**
     * 用户注册
     */
    fun register(username: String, password: String, isAdmin: Boolean) {
        if (username.isBlank() || password.isBlank()) {
            _registerState.value = ApiResponse.Error(400, "用户名和密码不能为空")
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            // 使用临时userId进行注册（注册时userId为空）
            val tempRepo = NetworkRepository.init("")
            val result = tempRepo.register(username, password, isAdmin)

            _registerState.value = result
            _isLoading.value = false

            // 注册成功后初始化repository
            if (result is ApiResponse.Success) {
                repository = NetworkRepository.init(result.data.userId)
            }
        }
    }

    /**
     * 获取已初始化的repository
     */
    fun getRepository(): NetworkRepository? {
        return repository
    }

    /**
     * 初始化repository（用于从本地存储恢复用户状态）
     */
    fun initRepository(userId: String) {
        if (repository == null) {
            repository = NetworkRepository.init(userId)
        }
    }
}
