package com.chat.lightweight.network.model

import com.google.gson.annotations.SerializedName

/**
 * 自动删除设置
 */
data class AutoDeleteSetting(
    @SerializedName("enabled")
    val enabled: Boolean,

    @SerializedName("unit")
    val unit: String,

    @SerializedName("value")
    val value: Int
)

/**
 * 更新自动删除设置请求
 */
data class UpdateAutoDeleteRequest(
    @SerializedName("enabled")
    val enabled: Boolean,

    @SerializedName("unit")
    val unit: String,

    @SerializedName("value")
    val value: Int
)

/**
 * 更新备注请求
 */
data class UpdateNoteRequest(
    @SerializedName("note")
    val note: String?,

    @SerializedName("currentUserId")
    val currentUserId: String
)

/**
 * 删除成员请求
 */
data class DeleteMemberRequest(
    @SerializedName("currentUserId")
    val currentUserId: String
)

/**
 * 删除消息请求
 */
data class DeleteMessageRequest(
    @SerializedName("userId")
    val userId: String
)

/**
 * 通用成功响应
 */
data class SuccessResponse(
    @SerializedName("success")
    val success: Boolean
)
