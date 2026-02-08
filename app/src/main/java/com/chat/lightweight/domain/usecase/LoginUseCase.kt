package com.chat.lightweight.domain.usecase

import com.chat.lightweight.domain.model.AuthResponse
import com.chat.lightweight.domain.repository.ChatRepository

/**
 * 登录用例
 */
class LoginUseCase(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(username: String, password: String): Result<AuthResponse> {
        if (username.isBlank() || password.isBlank()) {
            return Result.failure(Exception("用户名和密码不能为空"))
        }
        return repository.login(username, password)
    }
}
