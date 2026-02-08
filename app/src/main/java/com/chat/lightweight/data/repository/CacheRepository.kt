package com.chat.lightweight.data.repository

import android.content.Context
import com.chat.lightweight.data.local.PreferencesManager
import com.chat.lightweight.data.model.CachePolicy
import com.chat.lightweight.data.model.ConversationItem
import com.chat.lightweight.data.model.MessageItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

/**
 * 缓存仓库
 * 负责管理对话和消息的缓存
 */
class CacheRepository(context: Context) {

    private val preferencesManager = PreferencesManager(context.applicationContext)
    private val gson = Gson()

    private val cachePolicy = CachePolicy.default()

    // ========== 对话缓存 ==========

    /**
     * 保存对话列表到缓存
     */
    suspend fun saveConversationsToCache(conversations: List<ConversationItem>) {
        val json = gson.toJson(conversations)
        preferencesManager.saveConversationsCache(json)
    }

    /**
     * 获取缓存的对话列表
     * @return 如果缓存有效则返回对话列表,否则返回null
     */
    fun getCachedConversations(): Flow<List<ConversationItem>?> {
        return preferencesManager.getConversationsCacheFlow().map { cacheData ->
            if (cacheData != null) {
                val (json, cacheTime) = cacheData
                val isCacheValid = (System.currentTimeMillis() - cacheTime) < cachePolicy.conversationsCacheDuration

                if (isCacheValid) {
                    try {
                        val type = object : TypeToken<List<ConversationItem>>() {}.type
                        gson.fromJson<List<ConversationItem>>(json, type)
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }
            } else {
                null
            }
        }
    }

    /**
     * 清除对话缓存
     */
    suspend fun clearConversationsCache() {
        preferencesManager.clearConversationsCache()
    }

    // ========== 消息缓存 ==========

    /**
     * 保存消息到缓存
     */
    suspend fun saveMessagesToCache(conversationId: String, messages: List<MessageItem>) {
        val json = gson.toJson(messages)
        preferencesManager.saveMessagesCache(conversationId, json)
    }

    /**
     * 获取缓存的消息
     * @return 如果缓存有效则返回消息列表,否则返回null
     */
    fun getCachedMessages(conversationId: String): Flow<List<MessageItem>?> {
        return preferencesManager.getMessagesCacheFlow(conversationId).map { cacheData ->
            if (cacheData != null) {
                val (json, cacheTime) = cacheData
                val isCacheValid = (System.currentTimeMillis() - cacheTime) < cachePolicy.messagesCacheDuration

                if (isCacheValid) {
                    try {
                        val type = object : TypeToken<List<MessageItem>>() {}.type
                        gson.fromJson<List<MessageItem>>(json, type)
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }
            } else {
                null
            }
        }
    }

    /**
     * 清除指定对话的消息缓存
     */
    suspend fun clearMessagesCache(conversationId: String) {
        preferencesManager.clearMessagesCache(conversationId)
    }

    /**
     * 清除所有消息缓存
     */
    suspend fun clearAllMessagesCache() {
        preferencesManager.clearAllMessagesCache()
    }

    // ========== 全局缓存操作 ==========

    /**
     * 清除所有缓存
     */
    suspend fun clearAllCache() {
        preferencesManager.clearAllCache()
    }

    /**
     * 检查对话缓存是否有效
     */
    suspend fun isConversationsCacheValid(): Boolean {
        val cacheData = preferencesManager.getConversationsCacheFlow().firstOrNull()
        if (cacheData != null) {
            val (_, cacheTime) = cacheData
            return (System.currentTimeMillis() - cacheTime) < cachePolicy.conversationsCacheDuration
        }
        return false
    }

    /**
     * 检查消息缓存是否有效
     */
    suspend fun isMessagesCacheValid(conversationId: String): Boolean {
        val cacheData = preferencesManager.getMessagesCacheFlow(conversationId).firstOrNull()
        if (cacheData != null) {
            val (_, cacheTime) = cacheData
            return (System.currentTimeMillis() - cacheTime) < cachePolicy.messagesCacheDuration
        }
        return false
    }
}
