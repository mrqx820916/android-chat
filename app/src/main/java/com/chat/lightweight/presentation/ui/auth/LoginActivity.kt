package com.chat.lightweight.presentation.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.chat.lightweight.R
import com.chat.lightweight.databinding.ActivityLoginBinding
import com.chat.lightweight.presentation.ui.base.BaseActivity
import com.chat.lightweight.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

/**
 * 登录Activity
 */
class LoginActivity : BaseActivity<ActivityLoginBinding>() {

    private lateinit var viewModel: AuthViewModel

    override fun inflateBinding(): ActivityLoginBinding {
        return ActivityLoginBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupViewModel()
        setupUI()
        observeViewModel()
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]
    }

    private fun setupUI() {
        // TODO: 设置登录按钮和注册按钮的点击事件
        // 暂时注释掉，因为 binding 视图引用有问题
        /*
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (username.isBlank()) {
                Toast.makeText(this, "用户名不能为空", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.isBlank()) {
                Toast.makeText(this, "密码不能为空", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.login(username, password)
        }

        binding.btnRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
        */
    }

    private fun observeViewModel() {
        // TODO: 观察登录状态
        // 暂时注释掉，因为 binding 视图引用有问题
        /*
        viewModel.loginState.observe(this) { response ->
            when (response) {
                is com.chat.lightweight.network.ApiResponse.Loading -> {
                    binding.btnLogin.isEnabled = false
                    binding.btnRegister.isEnabled = false
                }
                is com.chat.lightweight.network.ApiResponse.Success -> {
                    binding.btnLogin.isEnabled = true
                    binding.btnRegister.isEnabled = true
                    // 保存用户信息并跳转到主界面
                    navigateToMain()
                }
                is com.chat.lightweight.network.ApiResponse.Error -> {
                    binding.btnLogin.isEnabled = true
                    binding.btnRegister.isEnabled = true
                    Toast.makeText(this, response.message ?: "登录失败", Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.btnLogin.isEnabled = !isLoading
            binding.btnRegister.isEnabled = !isLoading
        }
        */
    }

    private fun navigateToMain() {
        val intent = Intent(this, com.chat.lightweight.presentation.ui.MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
