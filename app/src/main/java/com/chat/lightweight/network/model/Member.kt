package com.chat.lightweight.network.model

import com.google.gson.annotations.SerializedName

/**
 * 成员信息（管理员视图）
 */
data class Member(
    @SerializedName("id")
    val id: String,

    @SerializedName("username")
    val username: String,

    @SerializedName("admin_note")
    val adminNote: String? = null,

    @SerializedName("is_online")
    val isOnline: Int? = null,

    @SerializedName("last_active")
    val lastActive: String? = null,

    @SerializedName("message_count")
    val messageCount: Int? = null,

    @SerializedName("last_message")
    val lastMessage: String? = null
)
