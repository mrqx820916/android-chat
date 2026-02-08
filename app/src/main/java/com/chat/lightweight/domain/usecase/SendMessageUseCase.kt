package com.chat.lightweight.domain.usecase

import com.chat.lightweight.domain.model.Message
import com.chat.lightweight.domain.model.SendMessageRequest
import com.chat.lightweight.domain.repository.ChatRepository

/**
 * 发送消息用例
 */
class SendMessageUseCase(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(request: SendMessageRequest): Result<Message> {
        if (request.content.isBlank() && request.fileUrl == null) {
            return Result.failure(Exception("消息内容不能为空"))
        }
        return repository.sendMessage(request)
    }
}
