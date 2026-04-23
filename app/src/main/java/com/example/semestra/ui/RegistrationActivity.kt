package com.example.semestra.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.semestra.R
import com.example.semestra.data.AppDatabase
import com.example.semestra.data.UserRepository
import com.example.semestra.logic.RegistrationInput
import com.example.semestra.logic.RegistrationResult
import com.example.semestra.logic.UserRegistrationService
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class RegistrationActivity : AppCompatActivity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var registrationService: UserRegistrationService

    private lateinit var firstName: TextInputEditText
    private lateinit var lastName: TextInputEditText
    private lateinit var email: TextInputEditText
    private lateinit var phone: TextInputEditText
    private lateinit var userId: TextInputEditText
    private lateinit var password: TextInputEditText
    private lateinit var securityQuestion: TextInputEditText
    private lateinit var securityAnswer: TextInputEditText

    private lateinit var firstNameError: TextView
    private lateinit var lastNameError: TextView
    private lateinit var emailError: TextView
    private lateinit var phoneError: TextView
    private lateinit var userIdError: TextView
    private lateinit var passwordError: TextView
    private lateinit var securityQuestionError: TextView
    private lateinit var securityAnswerError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)
        title = getString(R.string.registration_title)

        val db = AppDatabase.getInstance(applicationContext)
        registrationService = UserRegistrationService(UserRepository(db.userDao()))

        firstName = findViewById(R.id.editTextFirstName)
        lastName = findViewById(R.id.editTextLastName)
        email = findViewById(R.id.editTextEmail)
        phone = findViewById(R.id.editTextPhone)
        userId = findViewById(R.id.editTextRegistrationUserId)
        password = findViewById(R.id.editTextRegistrationPassword)
        securityQuestion = findViewById(R.id.editTextSecurityQuestion)
        securityAnswer = findViewById(R.id.editTextSecurityAnswer)

        firstNameError = findViewById(R.id.textFirstNameError)
        lastNameError = findViewById(R.id.textLastNameError)
        emailError = findViewById(R.id.textEmailError)
        phoneError = findViewById(R.id.textPhoneError)
        userIdError = findViewById(R.id.textRegistrationUserIdError)
        passwordError = findViewById(R.id.textRegistrationPasswordError)
        securityQuestionError = findViewById(R.id.textSecurityQuestionError)
        securityAnswerError = findViewById(R.id.textSecurityAnswerError)

        findViewById<MaterialButton>(R.id.buttonSubmitRegistration).setOnClickListener { submit() }
        findViewById<MaterialButton>(R.id.buttonCancelRegistration).setOnClickListener { finish() }
    }

    private fun submit() {
        clearErrors()
        scope.launch {
            when (
                val result = registrationService.register(
                    RegistrationInput(
                        userId = userId.text?.toString().orEmpty(),
                        firstName = firstName.text?.toString().orEmpty(),
                        lastName = lastName.text?.toString().orEmpty(),
                        email = email.text?.toString().orEmpty(),
                        phoneNumber = phone.text?.toString().orEmpty(),
                        password = password.text?.toString().orEmpty(),
                        securityQuestion = securityQuestion.text?.toString().orEmpty(),
                        securityAnswer = securityAnswer.text?.toString().orEmpty()
                    )
                )
            ) {
                RegistrationResult.Success -> {
                    setResult(RESULT_OK, Intent())
                    finish()
                }

                is RegistrationResult.ValidationFailed -> {
                    val e = result.errors
                    showField(firstNameError, e.firstName)
                    showField(lastNameError, e.lastName)
                    showField(emailError, e.email)
                    showField(phoneError, e.phoneNumber)
                    showField(userIdError, e.userId)
                    showField(passwordError, e.password)
                    showField(securityQuestionError, e.securityQuestion)
                    showField(securityAnswerError, e.securityAnswer)
                }
            }
        }
    }

    private fun showField(view: TextView, message: String?) {
        if (message == null) {
            view.visibility = View.GONE
            view.text = ""
        } else {
            view.text = message
            view.visibility = View.VISIBLE
        }
    }

    private fun clearErrors() {
        listOf(
            firstNameError, lastNameError, emailError, phoneError,
            userIdError, passwordError, securityQuestionError, securityAnswerError
        ).forEach { showField(it, null) }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
