package com.example.semestra

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.semestra.data.AppDatabase
import com.example.semestra.data.SessionStore
import com.example.semestra.data.UserRepository
import com.example.semestra.logic.ForgotPasswordService
import com.example.semestra.logic.ForgotPasswordResult
import com.example.semestra.logic.LoginResult
import com.example.semestra.logic.UserLoginService
import com.example.semestra.ui.DashboardActivity
import com.example.semestra.ui.RegistrationActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var loginService: UserLoginService
    private lateinit var forgotPasswordService: ForgotPasswordService
    private lateinit var userIdInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var userIdErrorView: TextView
    private lateinit var passwordErrorView: TextView

    private val registerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Toast.makeText(this, R.string.registration_success, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val database = AppDatabase.getInstance(applicationContext)
        val userRepository = UserRepository(database.userDao())
        loginService = UserLoginService(userRepository)
        forgotPasswordService = ForgotPasswordService(userRepository)

        userIdInput = findViewById(R.id.editTextUserId)
        passwordInput = findViewById(R.id.editTextPassword)
        userIdErrorView = findViewById(R.id.textUserIdError)
        passwordErrorView = findViewById(R.id.textPasswordError)

        findViewById<MaterialButton>(R.id.buttonLogin).setOnClickListener { login() }
        findViewById<MaterialButton>(R.id.buttonRegister).setOnClickListener {
            registerLauncher.launch(Intent(this, RegistrationActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.buttonForgotPassword).setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    private fun login() {
        clearErrors()

        val enteredUserId = userIdInput.text?.toString().orEmpty()
        val enteredPassword = passwordInput.text?.toString().orEmpty()

        activityScope.launch {
            when (val result = loginService.authenticate(enteredUserId, enteredPassword)) {
                LoginResult.Success -> {
                    SessionStore.saveUserId(applicationContext, enteredUserId.trim())
                    navigateToDashboard()
                }
                is LoginResult.Error -> showLoginErrors(result)
            }
        }
    }

    private fun showForgotPasswordDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_forgot_password_step1, null)
        val userField = dialogView.findViewById<EditText>(R.id.editForgotUserId)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.forgot_password_title)
            .setView(dialogView)
            .setPositiveButton(R.string.forgot_password_continue, null)
            .setNegativeButton(R.string.registration_cancel) { d, _ -> d.dismiss() }
            .show()
            .also { dialog ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val uid = userField.text?.toString().orEmpty().trim()
                    if (uid.isBlank()) {
                        userField.error = getString(R.string.login_user_id_error)
                        return@setOnClickListener
                    }
                    activityScope.launch {
                        val user = withContext(Dispatchers.IO) {
                            AppDatabase.getInstance(applicationContext).userDao().getUserByUserId(uid)
                        }
                        if (user == null) {
                            Toast.makeText(
                                this@MainActivity,
                                R.string.invalid_credentials,
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            dialog.dismiss()
                            showSecurityAnswerDialog(uid, user.securityQuestion)
                        }
                    }
                }
            }
    }

    private fun showSecurityAnswerDialog(userId: String, question: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_forgot_password_step2, null)
        dialogView.findViewById<TextView>(R.id.textSecurityQuestionPreview).text = question
        val answerField = dialogView.findViewById<EditText>(R.id.editForgotSecurityAnswer)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.forgot_password_title)
            .setView(dialogView)
            .setPositiveButton(R.string.forgot_password_submit, null)
            .setNegativeButton(R.string.registration_cancel) { d, _ -> d.dismiss() }
            .show()
            .also { dialog ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val answer = answerField.text?.toString().orEmpty()
                    activityScope.launch {
                        when (
                            val result = forgotPasswordService.resetPassword(userId, answer)
                        ) {
                            is ForgotPasswordResult.Success -> {
                                dialog.dismiss()
                                MaterialAlertDialogBuilder(this@MainActivity)
                                    .setTitle(R.string.temp_password_title)
                                    .setMessage(
                                        getString(R.string.temp_password_message, result.temporaryPassword)
                                    )
                                    .setPositiveButton(R.string.ok) { d, _ -> d.dismiss() }
                                    .show()
                            }
                            is ForgotPasswordResult.Error -> {
                                Toast.makeText(
                                    this@MainActivity,
                                    result.message,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
            }
    }

    private fun showLoginErrors(error: LoginResult.Error) {
        if (error.userIdMessage != null) {
            userIdErrorView.text = error.userIdMessage
            userIdErrorView.visibility = View.VISIBLE
        }
        if (error.passwordMessage != null) {
            passwordErrorView.text = error.passwordMessage
            passwordErrorView.visibility = View.VISIBLE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
    }

    private fun navigateToDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }

    private fun clearErrors() {
        userIdErrorView.text = ""
        userIdErrorView.visibility = View.GONE
        passwordErrorView.text = ""
        passwordErrorView.visibility = View.GONE
    }
}
