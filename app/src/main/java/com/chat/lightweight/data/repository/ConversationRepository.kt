package com.chat.lightweight.data.repository

import android.content.Context
import com.chat.lightweight.data.local.PreferencesManager
import com.chat.lightweight.data.model.ConversationItem
import com.chat.lightweight.network.NetworkRepository
import com.chat.lightweight.network.model.Conversation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 对话仓库
 * 负责对话数据的获取和缓存
 * 遵循单一职责原则，仅负责数据层操作
 */
class ConversationRepository(
    private val networkRepository: NetworkRepository,
    preferencesManager: PreferencesManager
) {

    private val preferencesManager = preferencesManager

    companion object {
        @Volatile
        private var instance: ConversationRepository? = null

        fun getInstance(context: Context): ConversationRepository {
            return instance ?: synchronized(this) {
                val prefsManager = PreferencesManager.getInstance(context)
                instance ?: ConversationRepository(
                    NetworkRepository.getInstance(),
                    prefsManager
                ).also { instance = it }
            }
        }

        fun resetInstance() {
            instance = null
        }
    }

    /**
     * 获取对话列表
     * 先从缓存获取，再从网络更新
     */
    fun getConversations(): Flow<Result<List<ConversationItem>>> = flow {
        try {
            // 获取当前用户ID
            val userId = getCurrentUserId() ?: throw Exception("用户未登录")

            // 调用网络API
            when (val result = networkRepository.getConversations()) {
                is com.chat.lightweight.network.ApiResponse.Success -> {
                    val conversations = result.data
                    val conversationItems = conversations.map { it.toConversationItem() }

                    // 缓存到本地
                    cacheConversations(conversationItems)

                    emit(Result.success(conversationItems))
                }
                is com.chat.lightweight.network.ApiResponse.Error -> {
                    // 网络请求失败，尝试从缓存获取
                    val cached = getCachedConversations()
                    if (cached.isNotEmpty()) {
                        emit(Result.success(cached))
                    } else {
                        emit(Result.failure(Exception(result.message)))
                    }
                }
                else -> {}
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * 刷新对话列表
     * 强制从网络获取最新数据
     */
    suspend fun refreshConversations(): Result<List<ConversationItem>> {
        return try {
            val userId = getCurrentUserId() ?: throw Exception("用户未登录")

            when (val result = networkRepository.getConversations()) {
                is com.chat.lightweight.network.ApiResponse.Success -> {
                    val conversations = result.data
                    val conversationItems = conversations.map { it.toConversationItem() }

                    // 更新缓存
                    cacheConversations(conversationItems)

                    Result.success(conversationItems)
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
     */
    private suspend fun getCurrentUserId(): String? {
        return preferencesManager.getUserId()
    }

    /**
     * 缓存对话列表
     */
    private suspend fun cacheConversations(conversations: List<ConversationItem>) {
        // TODO: 实现缓存逻辑
        // preferencesManager.saveConversationsCache(conversations)
    }

    /**
     * 获取缓存的对话列表
     */
    private suspend fun getCachedConversations(): List<ConversationItem> {
        // TODO: 从缓存读取
        return emptyList()
    }
}

/**
 * Conversation转ConversationItem扩展函数
 */
private fun Conversation.toConversationItem(): ConversationItem {
    // 解析ISO 8601格式日期字符串
    val timestamp = try {
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
            .parse(createdAt)?.time ?: System.currentTimeMillis()
    } catch (e: Exception) {
        try {
            // 尝试不带毫秒的格式
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                .parse(createdAt)?.time ?: System.currentTimeMillis()
        } catch (e2: Exception) {
            System.currentTimeMillis()
        }
    }

    return ConversationItem(
        id = id,
        userId = userId,
        username = memberName ?: "未知用户",
        unreadCount = unreadCount,
        lastMessage = lastMessage ?: "",
        lastMessageTime = timestamp,
        avatarUrl = ""
    )
}
