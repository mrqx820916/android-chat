package com.chat.lightweight.network.api

import com.chat.lightweight.network.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * 设置相关API
 * 单一职责：处理系统设置相关接口
 */
interface SettingsApiService {

    /**
     * 获取自动删除设置
     * 需要管理员权限
     */
    @GET("settings/auto-delete")
    suspend fun getAutoDeleteSetting(
        @Query("userId") userId: String
    ): Response<AutoDeleteSetting>

    /**
     * 更新自动删除设置
     * 需要管理员权限
     */
    @PUT("settings/auto-delete")
    suspend fun updateAutoDeleteSetting(
        @Query("userId") userId: String,
        @Body request: UpdateAutoDeleteRequest
    ): Response<AutoDeleteSetting>
}
