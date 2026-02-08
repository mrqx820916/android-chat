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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConversationListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferencesManager = PreferencesManager(applicationContext)

        setupToolbar()

        // 首先检查用户是否已登录（阻塞式，确保完成后再继续）
        checkLoginAndInit()

        setupBottomNavigation()

        // 默认显示对话列表
        if (savedInstanceState == null) {
            switchToConversations()
        }

        // 处理通知点击
        handleNotificationIntent()
    }

    /**
     * 检查登录状态并初始化
     */
    private fun checkLoginAndInit() {
        // 使用 runBlocking 确保在继续前完成
        val userId = kotlinx.coroutines.runBlocking {
            val isLoggedIn = preferencesManager.isLoggedInFlow().first()
            if (!isLoggedIn) {
                // 未登录，跳转到登录页
                navigateToLogin()
                return@runBlocking null
            }
            // 已登录，获取 userId 并初始化 NetworkRepository
            val uid = preferencesManager.getUserId()
            if (uid != null && uid.isNotEmpty()) {
                com.chat.lightweight.network.NetworkRepository.init(uid)
                // 初始化 Socket 连接
                com.chat.lightweight.socket.SocketClient.connect(uid)
                uid
            } else {
                null
            }
        }

        // 如果 userId 为空，跳转到登录页
        if (userId == null) {
            navigateToLogin()
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
            // TODO: 导航到指定对话
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

    companion object {
        const val EXTRA_FROM_NOTIFICATION = "from_notification"
        const val EXTRA_CONVERSATION_ID = "conversation_id"
    }
}
