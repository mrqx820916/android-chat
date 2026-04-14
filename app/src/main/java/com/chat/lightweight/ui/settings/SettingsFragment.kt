package com.chat.lightweight.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.chat.lightweight.data.local.PreferencesManager
import com.chat.lightweight.databinding.FragmentSettingsBinding
import kotlinx.coroutines.launch

/**
 * 设置Fragment
 *
 * 功能:
 * - 管理员专属：自动删除设置入口
 * - 通用功能：关于应用、退出登录
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var preferencesManager: PreferencesManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferencesManager = PreferencesManager.getInstance(requireContext())

        setupClickListeners()
        observePermissions()
    }

    /**
     * 设置点击事件
     */
    private fun setupClickListeners() {
        // 自动删除设置（管理员专属）
        binding.autoDeleteCard.setOnClickListener {
            startActivity(Intent(requireContext(), AutoDeleteSettingsActivity::class.java))
        }

        // 退出登录
        binding.logoutButton.setOnClickListener {
            logout()
        }
    }

    /**
     * 观察用户权限，动态显示/隐藏管理员功能
     */
    private fun observePermissions() {
        lifecycleScope.launch {
            preferencesManager.isAdminFlow().collect { isAdmin ->
                binding.autoDeleteCard.visibility = if (isAdmin) View.VISIBLE else View.GONE
            }
        }
    }

    /**
     * 退出登录
     */
    private fun logout() {
        lifecycleScope.launch {
            // 清除用户数据
            preferencesManager.clearUserData()

            // 重置网络实例和断开Socket连接
            com.chat.lightweight.network.NetworkRepository.resetInstance()
            com.chat.lightweight.socket.SocketClient.disconnect()

            // 跳转到登录页
            val intent = Intent(requireContext(),
                com.chat.lightweight.ui.auth.LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        /**
         * 创建实例的工厂方法
         */
        fun newInstance() = SettingsFragment()
    }
}
