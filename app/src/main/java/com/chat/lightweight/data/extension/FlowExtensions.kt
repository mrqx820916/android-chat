package com.chat.lightweight.data.extension

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.FlowCollector

/**
 * Flow扩展函数
 */

/**
 * 错误处理扩展
 */
fun <T> Flow<T>.catchError(
    onError: (Throwable) -> Unit = {}
): Flow<T> {
    return this.catch { e ->
        onError(e)
        throw e
    }
}

/**
 * 加载状态扩展
 */
sealed class FlowResult<out T> {
    data class Loading<T>(val data: T? = null) : FlowResult<T>()
    data class Success<T>(val data: T) : FlowResult<T>()
    data class Error<T>(val error: Throwable, val data: T? = null) : FlowResult<T>()
}

/**
 * 转换为带加载状态的Flow
 */
fun <T> Flow<T>.asResult(): Flow<FlowResult<T>> {
    return this
        .map<T, FlowResult<T>> { FlowResult.Success(it) }
        .onStart { emit(FlowResult.Loading(null)) }
        .catch { e -> emit(FlowResult.Error(e)) }
}

/**
 * 过滤null值
 */
fun <T : Any> Flow<T?>.notNull(): Flow<T> {
    return this.filterNotNull()
}

/**
 * 映射并过滤null值
 */
fun <T : Any, R> Flow<T>.mapNotNullNotNull(transform: (T) -> R?): Flow<R> {
    return this.map { transform(it) }.filterNotNull()
}

/**
 * 用于中断Flow收集的异常
 */
private class BreakException : Exception()
