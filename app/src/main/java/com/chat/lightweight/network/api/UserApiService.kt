package com.chat.lightweight.network.api

import com.chat.lightweight.network.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * 用户管理API
 * 单一职责：处理用户管理相关接口（管理员专用）
 */
interface UserApiService {

    /**
     * 获取成员列表
     * 需要管理员权限
     */
    @GET("users/members")
    suspend fun getMembers(
        @Query("userId") userId: String
    ): Response<List<Member>>

    /**
     * 更新成员备注
     * 需要管理员权限
     */
    @PUT("users/{userId}/note")
    suspend fun updateMemberNote(
        @Path("userId") userId: String,
        @Body request: UpdateNoteRequest
    ): Response<SuccessResponse>

    /**
     * 删除成员
     * 需要管理员权限
     */
    @DELETE("users/{userId}")
    suspend fun deleteMember(
        @Path("userId") userId: String,
        @Body request: DeleteMemberRequest
    ): Response<SuccessResponse>
}
