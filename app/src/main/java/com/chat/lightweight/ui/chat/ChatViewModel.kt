package com.chat.lightweight.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chat.lightweight.data.model.MessageItem
import com.chat.lightweight.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 聊天ViewModel
 * 遵循单一职责原则，仅负责聊天页面的业务逻辑
 */
class ChatViewModel(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<MessageItem>>(emptyList())
    val messages: StateFlow<List<MessageItem>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private var currentConversationId: String? = null
    private var currentMemberId: String? = null

    /**
     * 初始化对话
     */
    fun initConversation(conversationId: String, memberId: String) {
        currentConversationId = conversationId
        currentMemberId = memberId
    }

    /**
     * 加载消息列表
     */
    fun loadMessages() {
        val conversationId = currentConversationId ?: return

        viewModelScope.launch {
            _isLoading.value = true

            chatRepository.getMessages(conversationId).collect { result ->
                result.onSuccess { messages ->
                    _messages.value = messages
                }.onFailure { exception ->
                    // 处理错误
                }
            }

            _isLoading.value = false
        }
    }

    /**
     * 发送消息
     */
    fun sendMessage(content: String) {
        val conversationId = currentConversationId ?: return
        val memberId = currentMemberId ?: return

        viewModelScope.launch {
            _isSending.value = true

            val result = chatRepository.sendMessage(
                conversationId = conversationId,
                content = content,
                messageType = "text"
            )

            result.onSuccess { message ->
                // 添加到消息列表
                _messages.value = _messages.value + message
            }.onFailure { exception ->
                // 处理错误
            }

            _isSending.value = false
        }
    }

    /**
     * 发送图片
     */
    fun sendImage(imageUrl: String) {
        val conversationId = currentConversationId ?: return

        viewModelScope.launch {
            _isSending.value = true

            val result = chatRepository.sendMessage(
                conversationId = conversationId,
                content = null,
                messageType = "image",
                fileUrl = imageUrl
            )

            result.onSuccess { message ->
                _messages.value = _messages.value + message
            }

            _isSending.value = false
        }
    }

    /**
     * 发送语音
     */
    fun sendVoice(voiceUrl: String, duration: Int) {
        val conversationId = currentConversationId ?: return

        viewModelScope.launch {
            _isSending.value = true

            val result = chatRepository.sendMessage(
                conversationId = conversationId,
                content = null,
                messageType = "voice",
                fileUrl = voiceUrl
            )

            result.onSuccess { message ->
                _messages.value = _messages.value + message
            }

            _isSending.value = false
        }
    }

    /**
     * 删除消息
     */
    fun deleteMessage(message: MessageItem) {
        viewModelScope.launch {
            val result = chatRepository.deleteMessage(message.id)

            result.onSuccess {
                // 从列表中移除
                _messages.value = _messages.value.filter { it.id != message.id }
            }.onFailure { exception ->
                // 处理错误
            }
        }
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        currentConversationId = null
        currentMemberId = null
    }
}
