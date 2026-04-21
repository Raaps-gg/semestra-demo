package com.example.semestra.logic

import com.example.semestra.data.UserRepository

sealed interface LoginResult {
    data object Success : LoginResult
    data class Error(val message: String) : LoginResult
}

class UserLoginService(
    private val userRepository: UserRepository
) {
    suspend fun authenticate(userId: String, password: String): LoginResult {
        val normalizedUserId = userId.trim()
        val normalizedPassword = password.trim()

        if (normalizedUserId.isBlank() || normalizedPassword.isBlank()) {
            return LoginResult.Error(INVALID_CREDENTIALS_MESSAGE)
        }

        val user = userRepository.getUserByUserId(normalizedUserId)
            ?: return LoginResult.Error(INVALID_CREDENTIALS_MESSAGE)

        val passwordHash = PasswordHasher.hash(normalizedPassword)
        return if (user.passwordHash == passwordHash) {
            LoginResult.Success
        } else {
            LoginResult.Error(INVALID_CREDENTIALS_MESSAGE)
        }
    }

    companion object {
        const val INVALID_CREDENTIALS_MESSAGE = "Invalid credentials"
    }
}
