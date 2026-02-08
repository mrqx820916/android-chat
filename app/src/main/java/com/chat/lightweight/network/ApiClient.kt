package com.chat.lightweight.network

import com.chat.lightweight.network.api.*
import retrofit2.Retrofit

/**
 * API客户端统一入口
 * 单一职责：提供各类API服务实例
 */
class ApiClient(private val userId: String? = null) {

    private val retrofit by lazy {
        if (userId != null) {
            Retrofit.Builder()
                .baseUrl("https://chat.soft1688.vip/api/")
                .client(NetworkModule.createHttpClientWithUserId(userId))
                .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                .build()
        } else {
            NetworkModule.provideRetrofit()
        }
    }

    /** 认证API */
    val authApi: AuthApiService by lazy {
        retrofit.create(AuthApiService::class.java)
    }

    /** 用户管理API */
    val userApi: UserApiService by lazy {
        retrofit.create(UserApiService::class.java)
    }

    /** 聊天API */
    val chatApi: ChatApiService by lazy {
        retrofit.create(ChatApiService::class.java)
    }

    /** 设置API */
    val settingsApi: SettingsApiService by lazy {
        retrofit.create(SettingsApiService::class.java)
    }

    /** 上传API */
    val uploadApi: UploadApiService by lazy {
        retrofit.create(UploadApiService::class.java)
    }
}
