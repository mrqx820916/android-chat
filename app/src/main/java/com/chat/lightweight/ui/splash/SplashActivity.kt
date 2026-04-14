package com.chat.lightweight.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.chat.lightweight.data.local.PreferencesManager
import com.chat.lightweight.databinding.ActivitySplashBinding
import com.chat.lightweight.ui.auth.LoginActivity
import com.chat.lightweight.ui.conversation.ConversationListActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferencesManager = PreferencesManager(applicationContext)

        // 延迟一小段时间后检查登录状态并跳转
        binding.root.postDelayed({ checkLoginAndNavigate() }, 800)
    }

    private fun checkLoginAndNavigate() {
        lifecycleScope.launch {
            val isLoggedIn = preferencesManager.isLoggedInFlow().first()
            val intent = if (isLoggedIn) {
                // 已登录，初始化网络和 Socket
                val userId = preferencesManager.getUserId()
                if (userId != null) {
                    com.chat.lightweight.network.NetworkRepository.init(userId)
                    com.chat.lightweight.socket.SocketClient.connect(userId)
                }
                Intent(this@SplashActivity, ConversationListActivity::class.java)
            } else {
                Intent(this@SplashActivity, LoginActivity::class.java)
            }
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }
}
