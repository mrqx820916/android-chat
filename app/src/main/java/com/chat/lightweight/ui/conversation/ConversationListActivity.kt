package com.chat.lightweight.ui.conversation

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.app.Application
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.chat.lightweight.R
import com.chat.lightweight.data.local.PreferencesManager
import com.chat.lightweight.databinding.ActivityConversationListBinding
import com.chat.lightweight.ui.auth.LoginActivity
import com.chat.lightweight.ui.member.MemberManagementFragment
import com.chat.lightweight.presentation.viewmodel.ViewModelFactory
import com.chat.lightweight.ui.member.MemberViewModel
import com.chat.lightweight.ui.settings.SettingsFragment
import com.chat.lightweight.ui.update.UpdateDialogFragment
import com.chat.lightweight.service.NetworkStateMonitor
import com.chat.lightweight.update.UpdateCheckResult
import com.chat.lightweight.update.UpdateManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * 对话列表Activity
 *
 * 功能:
 * - 作为主容器，管理Fragment切换
 * - 实现底部导航功能
 * - 管理对话列表、成员管理、设置三个页面
 */
class ConversationListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConversationListBinding
    private val conversationViewModel: ConversationViewModel by viewModels {
        ViewModelFactory.getInstance(application)
    }
    private val memberViewModel: MemberViewModel by viewModels {
        ViewModelFactory.getInstance(application)
    }
    private lateinit var preferencesManager: PreferencesManager

    private var currentFragment: Fragment? = null
    private var networkSnackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConversationListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferencesManager = PreferencesManager(applicationContext)

        setupToolbar()

        // SplashActivity 已完成登录检查，此处仅做防御性校验
        ensureLoggedIn()

        setupBottomNavigation()

        // 默认显示对话列表
        if (savedInstanceState == null) {
            switchToConversations()
        }

        // 处理通知点击
        handleNotificationIntent()

        // 监听网络状态
        setupNetworkMonitoring()

        // 启动时后台检查更新
        checkForUpdateInBackground()
    }

    /**
     * 防御性校验登录状态
     * 正常情况下 SplashActivity 已完成登录检查，此处防止直接打开此页面时异常
     */
    private fun ensureLoggedIn() {
        lifecycleScope.launch {
            val isLoggedIn = preferencesManager.isLoggedInFlow().first()
            if (!isLoggedIn) {
                navigateToLogin()
            }
        }
    }

    /**
     * 设置Toolbar
     */
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.app_name)
    }

    /**
     * 设置底部导航
     */
    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_conversations -> {
                    switchToConversations()
                    true
                }
                R.id.navigation_members -> {
                    switchToMembers()
                    true
                }
                R.id.navigation_settings -> {
                    switchToSettings()
                    true
                }
                else -> false
            }
        }

        // 根据权限隐藏"成员"标签
        lifecycleScope.launch {
            val isAdmin = preferencesManager.isAdminFlow().first()
            if (!isAdmin) {
                binding.bottomNavigation.menu.findItem(R.id.navigation_members)?.isVisible = false
            }
        }
    }

    /**
     * 切换到对话列表
     */
    private fun switchToConversations() {
        val fragment = ConversationListFragment()
        replaceFragment(fragment)
        supportActionBar?.title = getString(R.string.conversation_list_title)
    }

    /**
     * 切换到成员管理
     */
    private fun switchToMembers() {
        lifecycleScope.launch {
            // 检查管理员权限
            val isAdmin = preferencesManager.isAdminFlow().first()

            if (isAdmin) {
                val fragment = MemberManagementFragment()
                // 传递用户ID给ViewModel
                val userId = preferencesManager.getUserId() ?: return@launch
                memberViewModel.init(userId, true)
                replaceFragment(fragment)
                supportActionBar?.title = "成员管理"
            } else {
                // 非管理员，显示提示并切换回对话列表
                android.widget.Toast.makeText(
                    this@ConversationListActivity,
                    "仅管理员可访问",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                binding.bottomNavigation.selectedItemId = R.id.navigation_conversations
            }
        }
    }

    /**
     * 切换到设置
     */
    private fun switchToSettings() {
        val fragment = SettingsFragment()
        replaceFragment(fragment)
        supportActionBar?.title = "设置"
    }

    /**
     * 替换Fragment
     */
    private fun replaceFragment(fragment: Fragment) {
        if (currentFragment != fragment) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
            currentFragment = fragment
        }
    }

    /**
     * 处理通知点击
     */
    private fun handleNotificationIntent() {
        val fromNotification = intent.getBooleanExtra(EXTRA_FROM_NOTIFICATION, false)
        val conversationId = intent.getStringExtra(EXTRA_CONVERSATION_ID)

        if (fromNotification && conversationId != null) {
            // 延迟导航，等待 Fragment 创建完成
            binding.root.post {
                lifecycleScope.launch {
                    val conversation = conversationViewModel.getConversationById(conversationId)
                    if (conversation != null) {
                        val intent = Intent(
                            this@ConversationListActivity,
                            com.chat.lightweight.ui.chat.ChatDetailActivity::class.java
                        ).apply {
                            putExtra(com.chat.lightweight.ui.chat.ChatDetailActivity.EXTRA_CONVERSATION_ID, conversation.id)
                            putExtra(com.chat.lightweight.ui.chat.ChatDetailActivity.EXTRA_MEMBER_ID, conversation.userId)
                            putExtra(com.chat.lightweight.ui.chat.ChatDetailActivity.EXTRA_MEMBER_NAME, conversation.username)
                        }
                        startActivity(intent)
                    }
                }
            }
        }
    }

    /**
     * 跳转到登录页
     */
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.menu_conversation_list, menu)
        return true
    }

    /**
     * 监听网络状态变化
     */
    private fun setupNetworkMonitoring() {
        val monitor = NetworkStateMonitor.getInstance(applicationContext)
        lifecycleScope.launch {
            monitor.observeNetworkState().collect { isConnected ->
                if (isConnected) {
                    networkSnackbar?.dismiss()
                    networkSnackbar = null
                } else {
                    if (networkSnackbar == null || networkSnackbar?.isShown != true) {
                        networkSnackbar = Snackbar.make(
                            binding.root,
                            "网络已断开，请检查网络连接",
                            Snackbar.LENGTH_INDEFINITE
                        ).also { it.show() }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        networkSnackbar?.dismiss()
    }

    private var lastBackPressTime = 0L

    override fun onBackPressed() {
        val now = System.currentTimeMillis()
        if (now - lastBackPressTime < 2000) {
            super.onBackPressed()
        } else {
            lastBackPressTime = now
            Snackbar.make(binding.root, "再按一次退出轻聊", Snackbar.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val EXTRA_FROM_NOTIFICATION = "from_notification"
        const val EXTRA_CONVERSATION_ID = "conversation_id"
    }

    private fun checkForUpdateInBackground() {
        lifecycleScope.launch {
            try {
                when (val result = UpdateManager.checkForUpdate(this@ConversationListActivity)) {
                    is UpdateCheckResult.UpdateAvailable -> {
                        Timber.d("发现更新: v${result.manifest.versionName}")
                        val dialog = UpdateDialogFragment.newInstance(result.manifest)
                        dialog.showAllowingStateLoss(supportFragmentManager, "update_dialog")
                    }
                    is UpdateCheckResult.NoUpdate -> {
                        Timber.d("已是最新版本: ${result.reason}")
                    }
                    is UpdateCheckResult.Skipped -> {
                        Timber.d("更新已跳过: ${result.reason}")
                    }
                    is UpdateCheckResult.Failure -> {
                        Timber.w("检查更新失败: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "检查更新异常")
            }
        }
    }
}
