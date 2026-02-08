package com.chat.lightweight.network

/**
 * API响应统一包装类
 * 遵循单一职责原则，仅负责API响应的数据封装
 */
sealed class ApiResponse<out T> {
    /** 成功响应 */
    data class Success<T>(val data: T) : ApiResponse<T>()

    /** 错误响应 */
    data class Error(
        val code: Int,
        val message: String,
        val exception: Throwable? = null
    ) : ApiResponse<Nothing>()

    /** 加载中 */
    object Loading : ApiResponse<Nothing>()
}
