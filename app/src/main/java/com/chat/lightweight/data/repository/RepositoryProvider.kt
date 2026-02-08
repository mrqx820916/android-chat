package com.chat.lightweight.data.repository

import android.content.Context

/**
 * 仓库提供者
 * 单例模式,全局访问各个Repository实例
 */
object RepositoryProvider {

    /**
     * 获取UserRepository实例
     */
    fun getUserRepository(context: Context): UserRepository {
        return UserRepository.getInstance(context.applicationContext)
    }

    /**
     * 获取CacheRepository实例
     */
    fun getCacheRepository(context: Context): CacheRepository {
        return cacheRepository ?: synchronized(this) {
            cacheRepository ?: CacheRepository(context.applicationContext).also {
                cacheRepository = it
            }
        }
    }

    /**
     * 获取SettingsRepository实例
     */
    fun getSettingsRepository(context: Context): SettingsRepository {
        return SettingsRepository.getInstance(context.applicationContext)
    }

    /**
     * 获取ConversationRepository实例
     */
    fun getConversationRepository(context: Context): ConversationRepository {
        return ConversationRepository.getInstance(context.applicationContext)
    }

    /**
     * 获取ChatRepository实例
     */
    fun getChatRepository(): ChatRepository {
        return ChatRepository.getInstance()
    }

    /**
     * 获取MemberRepository实例
     */
    fun getMemberRepository(context: Context): MemberRepository {
        return MemberRepository.getInstance(context.applicationContext)
    }

    @Volatile
    private var cacheRepository: CacheRepository? = null

    /**
     * 重置所有Repository (用于测试)
     */
    fun reset() {
        UserRepository.resetInstance()
        cacheRepository = null
        SettingsRepository.resetInstance()
        ConversationRepository.resetInstance()
        ChatRepository.resetInstance()
        MemberRepository.resetInstance()
    }
}
