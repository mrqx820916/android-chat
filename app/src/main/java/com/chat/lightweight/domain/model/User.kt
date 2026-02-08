package com.chat.lightweight.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 用户数据模型
 */
@Parcelize
data class User(
    val id: String,
    val username: String,
    val isAdmin: Boolean = false,
    val adminNote: String? = null,
    val isOnline: Boolean = false,
    val lastActive: String? = null,
    val createdAt: String? = null
) : Parcelable
