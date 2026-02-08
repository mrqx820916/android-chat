package com.chat.lightweight.presentation.ui.auth

import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.chat.lightweight.databinding.ActivityRegisterBinding
import com.chat.lightweight.presentation.ui.base.BaseActivity
import com.chat.lightweight.viewmodel.AuthViewModel

/**
 * 注册Activity
 */
class RegisterActivity : BaseActivity<ActivityRegisterBinding>() {

    private lateinit var viewModel: AuthViewModel

    override fun inflateBinding(): ActivityRegisterBinding {
        return ActivityRegisterBinding.inflate(layoutInflater)
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
        // TODO: 设置返回按钮和注册按钮的点击事件
        // 暂时注释掉，因为 binding 视图引用有问题
        /*
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnRegister.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()
            val isAdmin = binding.cbAdmin.isChecked

            if (username.isBlank()) {
                Toast.makeText(this, "用户名不能为空", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.isBlank()) {
                Toast.makeText(this, "密码不能为空", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "两次密码不一致", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.register(username, password, isAdmin)
        }
        */
    }

    private fun observeViewModel() {
        // TODO: 观察注册状态
        // 暂时注释掉，因为 binding 视图引用有问题
        /*
        viewModel.registerState.observe(this) { response ->
            when (response) {
                is com.chat.lightweight.network.ApiResponse.Loading -> {
                    binding.btnRegister.isEnabled = false
                    binding.btnBack.isEnabled = false
                }
                is com.chat.lightweight.network.ApiResponse.Success -> {
                    binding.btnRegister.isEnabled = true
                    binding.btnBack.isEnabled = true
                    Toast.makeText(this, "注册成功，请登录", Toast.LENGTH_SHORT).show()
                    finish()
                }
                is com.chat.lightweight.network.ApiResponse.Error -> {
                    binding.btnRegister.isEnabled = true
                    binding.btnBack.isEnabled = true
                    Toast.makeText(this, response.message ?: "注册失败", Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.btnRegister.isEnabled = !isLoading
            binding.btnBack.isEnabled = !isLoading
        }
        */
    }
}
