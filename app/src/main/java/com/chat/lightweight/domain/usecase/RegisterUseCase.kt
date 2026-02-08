package com.chat.lightweight.domain.usecase

import com.chat.lightweight.domain.model.AuthResponse
import com.chat.lightweight.domain.repository.ChatRepository

/**
 * 注册用例
 */
class RegisterUseCase(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(
        username: String,
        password: String,
        isAdmin: Boolean
    ): Result<AuthResponse> {
        if (username.isBlank() || password.isBlank()) {
            return Result.failure(Exception("用户名和密码不能为空"))
        }
        if (password.length < 6) {
            return Result.failure(Exception("密码长度不能少于6位"))
        }
        return repository.register(username, password, isAdmin)
    }
}
