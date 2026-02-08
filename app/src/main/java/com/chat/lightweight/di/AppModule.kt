package com.chat.lightweight.di

import android.content.Context
import com.chat.lightweight.data.repository.ChatRepositoryImpl
import com.chat.lightweight.domain.repository.ChatRepository
import com.chat.lightweight.network.ApiClient

/**
 * 应用程序依赖注入模块
 * 采用手动DI方式，避免引入Hilt/Dagger的复杂性
 */
object AppModule {

    /**
     * 获取仓库实例
     */
    fun getChatRepository(context: Context): ChatRepository {
        return ChatRepositoryImpl.getInstance(context)
    }
}
