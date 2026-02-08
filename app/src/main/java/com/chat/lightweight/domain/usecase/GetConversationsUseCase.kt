package com.chat.lightweight.domain.usecase

import com.chat.lightweight.domain.model.Conversation
import com.chat.lightweight.domain.repository.ChatRepository

/**
 * 获取对话列表用例
 */
class GetConversationsUseCase(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(): Result<List<Conversation>> {
        return repository.getConversations()
    }
}
