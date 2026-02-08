package com.chat.lightweight.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.chat.lightweight.R
import com.chat.lightweight.data.local.PreferencesManager
import com.chat.lightweight.data.model.UserData
import com.chat.lightweight.databinding.ActivityLoginBinding
import com.chat.lightweight.viewmodel.AuthViewModel
import com.chat.lightweight.ui.conversation.ConversationListActivity
import com.chat.lightweight.network.NetworkRepository
import com.chat.lightweight.socket.SocketClient
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * 登录页面
 * 功能：
 * - 用户登录
 * - 表单验证
 * - 错误提示
 * - 跳转注册
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: AuthViewModel
    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]
        preferencesManager = PreferencesManager.getInstance(applicationContext)

        // 检查是否已登录
        checkLoginStatus()

        // 初始化UI
        setupUI()

        // 观察登录状态
        observeLoginState()
    }

    /**
     * 检查登录状态
     */
    private fun checkLoginStatus() {
        lifecycleScope.launch {
            val isLoggedIn = preferencesManager.isLoggedInFlow()
            isLoggedIn.collect { loggedIn ->
                if (loggedIn) {
                    // 已登录，获取用户ID并初始化
                    val userId = preferencesManager.getUserId() ?: ""
                    if (userId.isNotEmpty()) {
                        NetworkRepository.init(userId)
                        // 初始化并连接 Socket
                        SocketClient.connect(userId)
                    }
                    // 直接进入主页
                    navigateToMain()
                }
            }
        }
    }

    /**
     * 初始化UI
     */
    private fun setupUI() {
        // 登录按钮点击
        binding.loginButton.setOnClickListener {
            handleLogin()
        }

        // 注册链接点击
        binding.registerLinkTextView.setOnClickListener {
            navigateToRegister()
        }

        // 用户名输入监听
        binding.usernameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.usernameInputLayout.error = null
                binding.errorTextView.visibility = View.GONE
            }
        })

        // 密码输入监听
        binding.passwordEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.passwordInputLayout.error = null
                binding.errorTextView.visibility = View.GONE
            }
        })
    }

    /**
     * 处理登录
     */
    private fun handleLogin() {
        val username = binding.usernameEditText.text?.toString()?.trim() ?: ""
        val password = binding.passwordEditText.text?.toString()?.trim() ?: ""

        // 表单验证
        if (!validateForm(username, password)) {
            return
        }

        // 调用登录
        viewModel.login(username, password)
    }

    /**
     * 表单验证
     */
    private fun validateForm(username: String, password: String): Boolean {
        var isValid = true

        if (username.isEmpty()) {
            binding.usernameInputLayout.error = getString(R.string.auth_username_required)
            isValid = false
        }

        if (password.isEmpty()) {
            binding.passwordInputLayout.error = getString(R.string.auth_password_required)
            isValid = false
        }

        return isValid
    }

    /**
     * 观察登录状态
     */
    private fun observeLoginState() {
        viewModel.loginState.observe(this) { state ->
            renderLoginState(state)
        }
    }

    /**
     * 渲染登录状态
     */
    private fun renderLoginState(state: com.chat.lightweight.network.ApiResponse<com.chat.lightweight.network.model.AuthResponse>) {
        when (state) {
            is com.chat.lightweight.network.ApiResponse.Loading -> {
                // 显示加载进度
                binding.progressIndicator.visibility = View.VISIBLE
                binding.loginButton.isEnabled = false
            }
            is com.chat.lightweight.network.ApiResponse.Success -> {
                // 隐藏加载进度
                binding.progressIndicator.visibility = View.GONE
                binding.loginButton.isEnabled = true
                // 登录成功，保存用户信息
                state.data?.let { saveUserDataAndNavigate(it) }
            }
            is com.chat.lightweight.network.ApiResponse.Error -> {
                // 隐藏加载进度
                binding.progressIndicator.visibility = View.GONE
                binding.loginButton.isEnabled = true
                // 显示错误提示
                showError(state.message)
            }
        }
    }

    /**
     * 保存用户数据并跳转
     */
    private fun saveUserDataAndNavigate(user: com.chat.lightweight.network.model.AuthResponse) {
        lifecycleScope.launch {
            val userId = user.userId ?: ""
            val userData = UserData(
                userId = userId,
                username = user.username ?: "",
                isAdmin = user.isAdmin ?: false,  // AuthResponse.isAdmin 仍是 Boolean 类型
                token = userId // 使用userId作为token
            )
            preferencesManager.saveUserData(userData)

            // 初始化 NetworkRepository
            NetworkRepository.init(userId)

            // 初始化并连接 Socket
            SocketClient.connect(userId)

            navigateToMain()
        }
    }

    /**
     * 显示错误
     */
    private fun showError(message: String) {
        binding.errorTextView.text = message
        binding.errorTextView.visibility = View.VISIBLE
    }

    /**
     * 显示SnackBar
     */
    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    /**
     * 跳转到注册页面
     */
    private fun navigateToRegister() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }

    /**
     * 跳转到对话列表
     */
    private fun navigateToMain() {
        val intent = Intent(this@LoginActivity, ConversationListActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        // ViewModel状态会在销毁时自动清除
    }
}
