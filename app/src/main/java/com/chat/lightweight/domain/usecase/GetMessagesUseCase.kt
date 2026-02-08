package com.chat.lightweight.domain.usecase

import com.chat.lightweight.domain.model.Message
import com.chat.lightweight.domain.repository.ChatRepository

/**
 * 获取消息列表用例
 */
class GetMessagesUseCase(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(conversationId: String): Result<List<Message>> {
        if (conversationId.isBlank()) {
            return Result.failure(Exception("对话ID不能为空"))
        }
        return repository.getMessages(conversationId)
    }
}
