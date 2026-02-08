package com.chat.lightweight.network.api

import com.chat.lightweight.network.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * 认证相关API
 * 单一职责：处理用户认证相关接口
 */
interface AuthApiService {

    /**
     * 用户登录
     */
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthResponse>

    /**
     * 用户注册
     */
    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<AuthResponse>

    /**
     * 检查管理员是否存在
     */
    @GET("auth/admin-exists")
    suspend fun checkAdminExists(): Response<AdminExistsResponse>
}
