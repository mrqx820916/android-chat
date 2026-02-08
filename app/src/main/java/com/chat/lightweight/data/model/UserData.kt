package com.chat.lightweight.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 用户数据模型
 * 用于持久化存储用户登录信息
 */
@Parcelize
data class UserData(
    val userId: String,
    val username: String,
    val isAdmin: Boolean,
    val token: String
) : Parcelable {
    companion object {
        const val UNKNOWN_USER_ID = ""
        const val UNKNOWN_TOKEN = ""
    }

    /**
     * 检查用户是否已登录
     */
    fun isLoggedIn(): Boolean = userId.isNotEmpty() && token.isNotEmpty()

    /**
     * 获取显示名称
     */
    fun getDisplayName(): String = username.ifEmpty { "未知用户" }

    /**
     * 是否为管理员用户
     */
    fun isAdministrator(): Boolean = isAdmin
}
