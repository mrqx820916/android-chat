package com.chat.lightweight.network.model

import com.google.gson.annotations.SerializedName

/**
 * 用户数据模型
 */
data class User(
    @SerializedName("id")
    val id: String,

    @SerializedName("username")
    val username: String,

    @SerializedName("isAdmin")
    val isAdmin: Any? = null,  // 接受 Boolean 或 Int（后端SQLite返回0/1）

    @SerializedName("admin_note")
    val adminNote: String? = null,

    @SerializedName("is_online")
    val isOnline: Int? = null,

    @SerializedName("last_active")
    val lastActive: String? = null,

    @SerializedName("created_at")
    val createdAt: String? = null
) {
    /** 将 isAdmin 转换为布尔值 */
    val isAdminBoolean: Boolean
        get() = when (isAdmin) {
            is Boolean -> isAdmin
            is Number -> isAdmin.toInt() == 1
            else -> false
        }
}

/**
 * 登录请求
 */
data class LoginRequest(
    @SerializedName("username")
    val username: String,

    @SerializedName("password")
    val password: String
)

/**
 * 注册请求
 */
data class RegisterRequest(
    @SerializedName("username")
    val username: String,

    @SerializedName("password")
    val password: String,

    @SerializedName("isAdmin")
    val isAdmin: Boolean = false
)

/**
 * 登录/注册响应
 */
data class AuthResponse(
    @SerializedName("userId")
    val userId: String,

    @SerializedName("username")
    val username: String,

    @SerializedName("isAdmin")
    val isAdmin: Boolean
)

/**
 * 管理员存在检查响应
 */
data class AdminExistsResponse(
    @SerializedName("exists")
    val exists: Boolean
)
