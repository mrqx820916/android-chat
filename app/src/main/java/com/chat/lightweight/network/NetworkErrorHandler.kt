package com.chat.lightweight.network

import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * 网络错误处理器
 * 单一职责：统一处理网络请求错误
 */
object NetworkErrorHandler {

    /**
     * 处理异常，返回用户友好的错误信息
     */
    fun handleError(throwable: Throwable): ApiResponse.Error {
        return when (throwable) {
            // 网络连接错误
            is UnknownHostException -> {
                ApiResponse.Error(
                    code = -1,
                    message = "网络连接失败，请检查网络设置"
                )
            }

            // 连接超时
            is SocketTimeoutException -> {
                ApiResponse.Error(
                    code = -2,
                    message = "连接超时，请稍后重试"
                )
            }

            // IO异常
            is IOException -> {
                ApiResponse.Error(
                    code = -3,
                    message = "网络错误，请稍后重试"
                )
            }

            // HTTP错误
            is HttpException -> {
                val code = throwable.code()
                val message = when (code) {
                    400 -> "请求参数错误"
                    401 -> "用户名或密码错误"
                    403 -> "无权限访问"
                    404 -> "请求的资源不存在"
                    500 -> "服务器错误，请稍后重试"
                    else -> "请求失败 ($code)"
                }
                ApiResponse.Error(code = code, message = message)
            }

            // 其他错误
            else -> {
                ApiResponse.Error(
                    code = -999,
                    message = throwable.message ?: "未知错误",
                    exception = throwable
                )
            }
        }
    }

    /**
     * 从错误响应体中提取错误信息
     */
    fun extractErrorMessage(errorBody: String?): String {
        if (errorBody.isNullOrBlank()) return "请求失败"

        return try {
            // 尝试解析JSON格式的错误响应
            // {"error": "错误信息"}
            val regex = """"error"\s*:\s*"([^"]+)"""".toRegex()
            regex.find(errorBody)?.groupValues?.get(1) ?: "请求失败"
        } catch (e: Exception) {
            "请求失败"
        }
    }
}
