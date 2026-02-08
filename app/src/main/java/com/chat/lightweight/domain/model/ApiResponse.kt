package com.chat.lightweight.domain.model

/**
 * API响应基础类
 */
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val error: String? = null
)

/**
 * 认证请求
 */
data class AuthRequest(
    val username: String,
    val password: String,
    val isAdmin: Boolean = false
)

/**
 * 认证响应
 */
data class AuthResponse(
    val token: String? = null,
    val userId: String? = null,
    val username: String? = null,
    val isAdmin: Boolean = false
)

/**
 * 消息发送请求
 */
data class SendMessageRequest(
    val conversationId: String,
    val content: String,
    val messageType: String = "text",
    val fileUrl: String? = null,
    val tempId: String? = null
)

/**
 * 文件上传响应
 */
data class UploadResponse(
    val fileUrl: String,
    val fileName: String? = null
)
