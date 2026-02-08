package com.chat.lightweight.data.repository

import com.chat.lightweight.data.model.MessageItem
import com.chat.lightweight.domain.model.SendMessageRequest
import com.chat.lightweight.network.NetworkRepository
import com.chat.lightweight.network.model.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 聊天仓库
 * 负责消息数据的获取和发送
 * 遵循单一职责原则，仅负责数据层操作
 */
class ChatRepository(private val networkRepository: NetworkRepository) {

    companion object {
        @Volatile
        private var instance: ChatRepository? = null

        fun getInstance(): ChatRepository {
            return instance ?: synchronized(this) {
                instance ?: ChatRepository(NetworkRepository.getInstance()).also { instance = it }
            }
        }

        fun resetInstance() {
            instance = null
        }
    }

    /**
     * 获取对话消息列表
     */
    fun getMessages(conversationId: String): Flow<Result<List<MessageItem>>> = flow {
        try {
            when (val result = networkRepository.getMessages(conversationId)) {
                is com.chat.lightweight.network.ApiResponse.Success -> {
                    val messages = result.data.map { it.toMessageItem() }
                    emit(Result.success(messages))
                }
                is com.chat.lightweight.network.ApiResponse.Error -> {
                    emit(Result.failure(Exception(result.message)))
                }
                else -> {}
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * 发送消息
     */
    suspend fun sendMessage(
        conversationId: String,
        content: String?,
        messageType: String = "text",
        fileUrl: String? = null
    ): Result<MessageItem> {
        return try {
            when (val result = networkRepository.sendMessage(
                conversationId = conversationId,
                content = content,
                messageType = messageType,
                fileUrl = fileUrl
            )) {
                is com.chat.lightweight.network.ApiResponse.Success -> {
                    val message = result.data.toMessageItem()
                    Result.success(message)
                }
                is com.chat.lightweight.network.ApiResponse.Error -> {
                    Result.failure(Exception(result.message))
                }
                else -> Result.failure(Exception("未知错误"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 发送消息 (使用Domain层Request)
     */
    suspend fun sendMessage(request: SendMessageRequest): Result<MessageItem> {
        return try {
            when (val result = networkRepository.sendMessage(
                conversationId = request.conversationId,
                content = request.content,
                messageType = request.messageType,
                fileUrl = request.fileUrl
            )) {
                is com.chat.lightweight.network.ApiResponse.Success -> {
                    val message = result.data.toMessageItem()
                    Result.success(message)
                }
                is com.chat.lightweight.network.ApiResponse.Error -> {
                    Result.failure(Exception(result.message))
                }
                else -> Result.failure(Exception("未知错误"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 删除消息
     */
    suspend fun deleteMessage(messageId: String): Result<Unit> {
        return try {
            when (networkRepository.deleteMessage(messageId)) {
                is com.chat.lightweight.network.ApiResponse.Success -> {
                    Result.success(Unit)
                }
                is com.chat.lightweight.network.ApiResponse.Error -> {
                    Result.failure(Exception("删除失败"))
                }
                else -> Result.failure(Exception("未知错误"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 标记对话已读
     */
    suspend fun markConversationRead(conversationId: String): Result<Unit> {
        return try {
            when (networkRepository.markConversationRead(conversationId)) {
                is com.chat.lightweight.network.ApiResponse.Success -> {
                    Result.success(Unit)
                }
                is com.chat.lightweight.network.ApiResponse.Error -> {
                    Result.failure(Exception("标记失败"))
                }
                else -> Result.failure(Exception("未知错误"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取当前用户ID
     * TODO: 从PreferencesManager获取
     */
    suspend fun getUserId(): String? {
        // 暂时返回null，需要从PreferencesManager获取
        return null
    }
}

/**
 * Message转MessageItem扩展函数
 */
private fun Message.toMessageItem(): MessageItem {
    return MessageItem(
        id = id,
        conversationId = conversationId,
        senderId = senderId,
        senderName = "用户",
        content = content ?: "",
        messageType = "text",
        fileUrl = fileUrl,
        timestamp = System.currentTimeMillis(),
        isFromSelf = false,
        isRead = true
    )
}
