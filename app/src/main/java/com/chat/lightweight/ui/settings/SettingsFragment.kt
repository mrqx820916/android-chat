package com.chat.lightweight.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.chat.lightweight.data.local.PreferencesManager
import com.chat.lightweight.databinding.FragmentSettingsBinding
import com.chat.lightweight.ui.update.UpdateDialogFragment
import com.chat.lightweight.update.UpdateCheckResult
import com.chat.lightweight.update.UpdateManager
import kotlinx.coroutines.launch
import timber.log.Timber

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var preferencesManager: PreferencesManager
    private var isCheckingUpdate = false

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

        binding.tvAppVersion.text = "轻聊 v${com.chat.lightweight.BuildConfig.VERSION_NAME}\n私密聊天平台"

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

        // 检查更新
        binding.checkUpdateCard.setOnClickListener {
            checkForUpdate()
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

    private fun checkForUpdate() {
        if (isCheckingUpdate) return
        isCheckingUpdate = true
        Toast.makeText(requireContext(), "正在检查更新...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                when (val result = UpdateManager.checkForUpdate(requireContext())) {
                    is UpdateCheckResult.UpdateAvailable -> {
                        Timber.d("发现更新: v${result.manifest.versionName}")
                        val dialog = UpdateDialogFragment.newInstance(result.manifest)
                        dialog.showAllowingStateLoss(childFragmentManager, "update_dialog")
                    }
                    is UpdateCheckResult.NoUpdate -> {
                        Toast.makeText(requireContext(), "已是最新版本", Toast.LENGTH_SHORT).show()
                    }
                    is UpdateCheckResult.Skipped -> {
                        Toast.makeText(requireContext(), "已是最新版本", Toast.LENGTH_SHORT).show()
                    }
                    is UpdateCheckResult.Failure -> {
                        Timber.w("检查更新失败: ${result.message}")
                        Toast.makeText(requireContext(), "检查更新失败: ${result.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "检查更新异常")
                Toast.makeText(requireContext(), "检查更新失败", Toast.LENGTH_SHORT).show()
            } finally {
                isCheckingUpdate = false
            }
        }
    }

    /**
     * 退出登录
     */
    private fun logout() {
        lifecycleScope.launch {
            preferencesManager.clearUserData()
            com.chat.lightweight.network.NetworkRepository.resetInstance()
            com.chat.lightweight.socket.SocketClient.disconnect()

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
        fun newInstance() = SettingsFragment()
    }
}
