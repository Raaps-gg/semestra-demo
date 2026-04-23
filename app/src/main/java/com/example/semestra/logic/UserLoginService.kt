package com.example.semestra.logic

import com.example.semestra.data.UserRepository

sealed interface LoginResult {
    data object Success : LoginResult
    data class Error(
        val userIdMessage: String? = null,
        val passwordMessage: String? = null
    ) : LoginResult {
        fun primaryMessage(): String =
            passwordMessage ?: userIdMessage ?: "Invalid credentials"
    }
}

class UserLoginService(
    private val userRepository: UserRepository
) {
    suspend fun authenticate(userId: String, password: String): LoginResult {
        val normalizedUserId = userId.trim()
        val normalizedPassword = password.trim()

        if (normalizedUserId.isBlank() && normalizedPassword.isBlank()) {
            return LoginResult.Error(
                userIdMessage = EMPTY_USER_ID_MESSAGE,
                passwordMessage = EMPTY_PASSWORD_MESSAGE
            )
        }
        if (normalizedUserId.isBlank()) {
            return LoginResult.Error(userIdMessage = EMPTY_USER_ID_MESSAGE, passwordMessage = null)
        }
        if (normalizedPassword.isBlank()) {
            return LoginResult.Error(userIdMessage = null, passwordMessage = EMPTY_PASSWORD_MESSAGE)
        }

        val user = userRepository.getUserByUserId(normalizedUserId)
            ?: return LoginResult.Error(
                userIdMessage = null,
                passwordMessage = INVALID_CREDENTIALS_MESSAGE
            )

        val passwordHash = PasswordHasher.hash(normalizedPassword)
        return if (user.passwordHash == passwordHash) {
            LoginResult.Success
        } else {
            LoginResult.Error(
                userIdMessage = null,
                passwordMessage = INVALID_CREDENTIALS_MESSAGE
            )
        }
    }

    companion object {
        const val INVALID_CREDENTIALS_MESSAGE = "Invalid credentials"
        const val EMPTY_USER_ID_MESSAGE = "User ID is required."
        const val EMPTY_PASSWORD_MESSAGE = "Password is required."
    }
}
