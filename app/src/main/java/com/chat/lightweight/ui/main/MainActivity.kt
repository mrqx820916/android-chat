package com.chat.lightweight.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.chat.lightweight.data.local.PreferencesManager
import com.chat.lightweight.ui.auth.LoginActivity
import com.chat.lightweight.ui.conversation.ConversationListActivity
import kotlinx.coroutines.launch

/**
 * 主页面
 * 用于登录后的导航
 */
class MainActivity : AppCompatActivity() {

    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferencesManager = PreferencesManager.getInstance(applicationContext)

        // 检查登录状态并导航
        checkLoginAndNavigate()
    }

    /**
     * 检查登录状态并导航
     */
    private fun checkLoginAndNavigate() {
        lifecycleScope.launch {
            val isLoggedIn = preferencesManager.isLoggedInFlow()
            isLoggedIn.collect { loggedIn ->
                if (loggedIn) {
                    // 已登录，跳转到对话列表
                    navigateToConversationList()
                } else {
                    // 未登录，跳转到登录页
                    navigateToLogin()
                }
            }
        }
    }

    /**
     * 跳转到对话列表
     */
    private fun navigateToConversationList() {
        val intent = Intent(this, ConversationListActivity::class.java)
        startActivity(intent)
        finish()
    }

    /**
     * 跳转到登录页
     */
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * 登出
     */
    fun logout() {
        lifecycleScope.launch {
            preferencesManager.clearUserData()
            navigateToLogin()
        }
    }
}
