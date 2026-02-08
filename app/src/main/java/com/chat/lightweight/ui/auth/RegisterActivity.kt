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
import com.chat.lightweight.databinding.ActivityRegisterBinding
import com.chat.lightweight.viewmodel.AuthViewModel
import com.chat.lightweight.ui.conversation.ConversationListActivity
import kotlinx.coroutines.launch

/**
 * 注册页面
 * 功能：
 * - 用户注册
 * - 管理员注册选项（仅首次）
 * - 表单验证
 * - 密码确认
 * - 错误提示
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var viewModel: AuthViewModel
    private lateinit var preferencesManager: PreferencesManager
    private var adminExists = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]
        preferencesManager = PreferencesManager.getInstance(applicationContext)

        // 初始化UI
        setupUI()

        // 检查管理员是否存在
        checkAdminExists()

        // 观察注册状态
        observeRegisterState()
    }

    /**
     * 初始化UI
     */
    private fun setupUI() {
        // 返回按钮
        binding.backButton.setOnClickListener {
            finish()
        }

        // 注册按钮
        binding.registerButton.setOnClickListener {
            handleRegister()
        }

        // 管理员开关
        binding.adminSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && adminExists) {
                // 管理员已存在，禁止选择
                binding.adminSwitch.isChecked = false
                showAdminExistsHint()
            }
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
                binding.confirmPasswordInputLayout.error = null
                binding.errorTextView.visibility = View.GONE
            }
        })

        // 确认密码输入监听
        binding.confirmPasswordEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.confirmPasswordInputLayout.error = null
                binding.errorTextView.visibility = View.GONE
            }
        })
    }

    /**
     * 检查管理员是否存在
     */
    private fun checkAdminExists() {
        lifecycleScope.launch {
            // TODO: 调用API检查管理员是否存在
            // 这里先设置为false，实际应该从API获取
            adminExists = false

            // 如果管理员已存在，禁用管理员选项
            if (adminExists) {
                binding.adminSwitch.isEnabled = false
                binding.adminOptionCard.alpha = 0.5f
            }
        }
    }

    /**
     * 显示管理员已存在提示
     */
    private fun showAdminExistsHint() {
        binding.errorTextView.text = "管理员已存在"
        binding.errorTextView.visibility = View.VISIBLE
    }

    /**
     * 处理注册
     */
    private fun handleRegister() {
        val username = binding.usernameEditText.text?.toString()?.trim() ?: ""
        val password = binding.passwordEditText.text?.toString()?.trim() ?: ""
        val confirmPassword = binding.confirmPasswordEditText.text?.toString()?.trim() ?: ""
        val isAdmin = binding.adminSwitch.isChecked

        // 表单验证
        if (!validateForm(username, password, confirmPassword)) {
            return
        }

        // 调用注册
        viewModel.register(username, password, isAdmin)
    }

    /**
     * 表单验证
     */
    private fun validateForm(username: String, password: String, confirmPassword: String): Boolean {
        var isValid = true

        // 验证用户名
        when {
            username.isEmpty() -> {
                binding.usernameInputLayout.error = getString(R.string.auth_username_required)
                isValid = false
            }
            username.length < 2 -> {
                binding.usernameInputLayout.error = "用户名至少2个字符"
                isValid = false
            }
            username.length > 20 -> {
                binding.usernameInputLayout.error = "用户名最多20个字符"
                isValid = false
            }
        }

        // 验证密码
        when {
            password.isEmpty() -> {
                binding.passwordInputLayout.error = getString(R.string.auth_password_required)
                isValid = false
            }
            password.length < 6 -> {
                binding.passwordInputLayout.error = "密码至少6个字符"
                isValid = false
            }
        }

        // 验证确认密码
        if (confirmPassword.isEmpty()) {
            binding.confirmPasswordInputLayout.error = "请确认密码"
            isValid = false
        } else if (password != confirmPassword) {
            binding.confirmPasswordInputLayout.error = "两次输入的密码不一致"
            isValid = false
        }

        return isValid
    }

    /**
     * 观察注册状态
     */
    private fun observeRegisterState() {
        viewModel.registerState.observe(this) { state ->
            renderRegisterState(state)
        }
    }

    /**
     * 渲染注册状态
     */
    private fun renderRegisterState(state: com.chat.lightweight.network.ApiResponse<com.chat.lightweight.network.model.AuthResponse>) {
        when (state) {
            is com.chat.lightweight.network.ApiResponse.Loading -> {
                // 显示加载进度
                binding.progressIndicator.visibility = View.VISIBLE
                binding.registerButton.isEnabled = false
            }
            is com.chat.lightweight.network.ApiResponse.Success -> {
                // 隐藏加载进度
                binding.progressIndicator.visibility = View.GONE
                binding.registerButton.isEnabled = true
                // 注册成功，保存用户信息并跳转
                state.data?.let { saveUserDataAndNavigate(it) }
            }
            is com.chat.lightweight.network.ApiResponse.Error -> {
                // 隐藏加载进度
                binding.progressIndicator.visibility = View.GONE
                binding.registerButton.isEnabled = true
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
            val userData = UserData(
                userId = user.userId ?: "",
                username = user.username ?: "",
                isAdmin = user.isAdmin ?: false,
                token = user.userId ?: "" // 使用userId作为token
            )
            preferencesManager.saveUserData(userData)
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
     * 跳转到对话列表
     */
    private fun navigateToMain() {
        val intent = Intent(this@RegisterActivity, ConversationListActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        // ViewModel状态会在销毁时自动清除
    }
}
