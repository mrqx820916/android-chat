package com.chat.lightweight.ui.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chat.lightweight.data.model.ConversationItem
import com.chat.lightweight.data.repository.ConversationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 对话列表UI状态
 */
data class ConversationListUiState(
    val isLoading: Boolean = false,
    val conversations: List<ConversationItem> = emptyList(),
    val error: String? = null,
    val isRefreshing: Boolean = false
)

/**
 * 对话列表ViewModel
 * 遵循单一职责原则，仅负责对话列表的业务逻辑
 */
class ConversationViewModel(
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationListUiState())
    val uiState: StateFlow<ConversationListUiState> = _uiState.asStateFlow()

    /**
     * 加载对话列表
     */
    fun loadConversations() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            conversationRepository.getConversations().collect { result ->
                result.onSuccess { conversations ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        conversations = conversations,
                        error = null
                    )
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "加载失败"
                    )
                }
            }
        }
    }

    /**
     * 刷新对话列表
     */
    fun refreshConversations() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)

            val result = conversationRepository.refreshConversations()
            result.onSuccess { conversations ->
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    conversations = conversations,
                    error = null
                )
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    error = exception.message ?: "刷新失败"
                )
            }
        }
    }

    /**
     * 标记对话已读
     */
    fun markConversationRead(conversationId: String) {
        viewModelScope.launch {
            conversationRepository.markConversationRead(conversationId)

            // 更新本地状态
            val updatedConversations = _uiState.value.conversations.map { conversation ->
                if (conversation.id == conversationId) {
                    conversation.copy(unreadCount = 0)
                } else {
                    conversation
                }
            }
            _uiState.value = _uiState.value.copy(conversations = updatedConversations)
        }
    }

    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
