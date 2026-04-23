package com.example.semestra.logic

import android.util.Patterns
import com.example.semestra.data.User
import com.example.semestra.data.UserRepository

data class RegistrationInput(
    val userId: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String,
    val password: String,
    val securityQuestion: String,
    val securityAnswer: String
)

data class RegistrationErrors(
    val userId: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val phoneNumber: String? = null,
    val password: String? = null,
    val securityQuestion: String? = null,
    val securityAnswer: String? = null
) {
    fun hasErrors(): Boolean {
        return listOf(
            userId,
            firstName,
            lastName,
            email,
            phoneNumber,
            password,
            securityQuestion,
            securityAnswer
        ).any { it != null }
    }
}

sealed interface RegistrationResult {
    data object Success : RegistrationResult
    data class ValidationFailed(val errors: RegistrationErrors) : RegistrationResult
}

class UserRegistrationService(
    private val userRepository: UserRepository
) {
    suspend fun register(input: RegistrationInput): RegistrationResult {
        val trimmedInput = input.normalized()
        val validationErrors = validate(trimmedInput)
        if (validationErrors.hasErrors()) {
            return RegistrationResult.ValidationFailed(validationErrors)
        }

        val existingUserById = userRepository.getUserByUserId(trimmedInput.userId)
        val existingUserByEmail = userRepository.getUserByEmail(trimmedInput.email)

        val conflictErrors = RegistrationErrors(
            userId = if (existingUserById != null) "User ID already exists." else null,
            email = if (existingUserByEmail != null) "Email already exists." else null
        )
        if (conflictErrors.hasErrors()) {
            return RegistrationResult.ValidationFailed(conflictErrors)
        }

        val user = User(
            userId = trimmedInput.userId,
            firstName = trimmedInput.firstName,
            lastName = trimmedInput.lastName,
            email = trimmedInput.email,
            phoneNumber = trimmedInput.phoneNumber,
            passwordHash = PasswordHasher.hash(trimmedInput.password),
            securityQuestion = trimmedInput.securityQuestion,
            securityAnswer = trimmedInput.securityAnswer
        )
        userRepository.insertUser(user)
        return RegistrationResult.Success
    }

    private fun validate(input: RegistrationInput): RegistrationErrors {
        return RegistrationErrors(
            userId = when {
                input.userId.isBlank() -> "User ID is required."
                input.userId.length < 8 -> "User ID must be at least 8 characters."
                !input.userId.matches(USER_ID_REGEX) -> "User ID must be alphanumeric."
                else -> null
            },
            firstName = if (input.firstName.isBlank()) "First name is required." else null,
            lastName = if (input.lastName.isBlank()) "Last name is required." else null,
            email = when {
                input.email.isBlank() -> "Email is required."
                !Patterns.EMAIL_ADDRESS.matcher(input.email).matches() -> "Enter a valid email address."
                else -> null
            },
            phoneNumber = when {
                input.phoneNumber.isBlank() -> "Phone number is required."
                !input.phoneNumber.matches(PHONE_REGEX) -> "Enter a valid phone number."
                else -> null
            },
            password = when {
                input.password.isBlank() -> "Password is required."
                input.password.length < 8 -> "Password must be at least 8 characters."
                !input.password.contains(Regex("[A-Za-z]")) -> "Password must include letters."
                !input.password.contains(Regex("[A-Z]")) -> "Password must include a capital letter."
                !input.password.contains(Regex("[0-9]")) -> "Password must include a number."
                else -> null
            },
            securityQuestion = if (input.securityQuestion.isBlank()) "Security question is required." else null,
            securityAnswer = if (input.securityAnswer.isBlank()) "Security answer is required." else null
        )
    }

    private fun RegistrationInput.normalized(): RegistrationInput {
        return copy(
            userId = userId.trim(),
            firstName = firstName.trim(),
            lastName = lastName.trim(),
            email = email.trim().lowercase(),
            phoneNumber = phoneNumber.trim(),
            password = password.trim(),
            securityQuestion = securityQuestion.trim(),
            securityAnswer = securityAnswer.trim()
        )
    }

    private companion object {
        val USER_ID_REGEX = Regex("^[A-Za-z0-9]{8,}$")
        val PHONE_REGEX = Regex("^[0-9+()\\-\\s]{7,20}$")
    }
}
