package com.example.semestra.logic

import com.example.semestra.data.UserRepository

sealed interface ForgotPasswordResult {
    data class Success(val temporaryPassword: String) : ForgotPasswordResult
    data class Error(val message: String) : ForgotPasswordResult
}

class ForgotPasswordService(
    private val userRepository: UserRepository
) {
    suspend fun resetPassword(userId: String, securityAnswer: String): ForgotPasswordResult {
        val uid = userId.trim()
        val answer = securityAnswer.trim()
        if (uid.isBlank()) {
            return ForgotPasswordResult.Error("User ID is required.")
        }
        if (answer.isBlank()) {
            return ForgotPasswordResult.Error("Security answer is required.")
        }

        val user = userRepository.getUserByUserId(uid)
            ?: return ForgotPasswordResult.Error("User not found.")

        if (!user.securityAnswer.equals(answer, ignoreCase = true)) {
            return ForgotPasswordResult.Error("Incorrect security answer.")
        }

        val temp = TempPasswordGenerator.generate()
        val hash = PasswordHasher.hash(temp)
        userRepository.updatePasswordHash(uid, hash)
        return ForgotPasswordResult.Success(temp)
    }
}
