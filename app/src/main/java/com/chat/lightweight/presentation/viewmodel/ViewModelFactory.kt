package com.chat.lightweight.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.chat.lightweight.data.repository.ChatRepository
import com.chat.lightweight.data.repository.ConversationRepository
import com.chat.lightweight.data.repository.MemberRepository
import com.chat.lightweight.data.repository.RepositoryProvider
import com.chat.lightweight.ui.chat.viewmodel.ChatDetailViewModel
import com.chat.lightweight.ui.conversation.ConversationViewModel
import com.chat.lightweight.ui.member.MemberViewModel
import com.chat.lightweight.ui.settings.AutoDeleteViewModel
import com.chat.lightweight.ui.viewmodel.AuthViewModel as UiAuthViewModel
import com.chat.lightweight.viewmodel.AuthViewModel

/**
 * ViewModel工厂
 * 用于创建带有依赖注入的ViewModel实例
 */
class ViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    private val conversationRepository: ConversationRepository by lazy {
        RepositoryProvider.getConversationRepository(application)
    }

    private val memberRepository: MemberRepository by lazy {
        RepositoryProvider.getMemberRepository(application)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass) {
            AuthViewModel::class.java -> {
                AuthViewModel() as T
            }
            UiAuthViewModel::class.java -> {
                UiAuthViewModel(application) as T
            }
            ConversationViewModel::class.java -> {
                ConversationViewModel(conversationRepository) as T
            }
            ChatDetailViewModel::class.java -> {
                ChatDetailViewModel(application) as T
            }
            AutoDeleteViewModel::class.java -> {
                AutoDeleteViewModel(application) as T
            }
            MemberViewModel::class.java -> {
                MemberViewModel(memberRepository) as T
            }
            else -> {
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }

    companion object {
        @Volatile
        private var instance: ViewModelFactory? = null

        /**
         * 获取ViewModelFactory单例
         */
        fun getInstance(application: Application): ViewModelFactory {
            return instance ?: synchronized(this) {
                instance ?: ViewModelFactory(application).also { instance = it }
            }
        }

        /**
         * 重置实例 (用于测试)
         */
        fun reset() {
            instance = null
        }
    }
}

/**
 * Application扩展函数
 * 用于快速获取ViewModelFactory
 */
fun Application.viewModelFactory(): ViewModelFactory {
    return ViewModelFactory.getInstance(this)
}
