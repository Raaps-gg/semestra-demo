package com.example.semestra

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.semestra.data.AppDatabase
import com.example.semestra.data.UserRepository
import com.example.semestra.logic.LoginResult
import com.example.semestra.logic.UserLoginService
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var loginService: UserLoginService
    private lateinit var userIdInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var userIdErrorView: TextView
    private lateinit var passwordErrorView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val database = AppDatabase.getInstance(applicationContext)
        val userRepository = UserRepository(database.userDao())
        loginService = UserLoginService(userRepository)

        userIdInput = findViewById(R.id.editTextUserId)
        passwordInput = findViewById(R.id.editTextPassword)
        userIdErrorView = findViewById(R.id.textUserIdError)
        passwordErrorView = findViewById(R.id.textPasswordError)

        findViewById<Button>(R.id.buttonLogin).setOnClickListener {
            login()
        }
    }

    fun login() {
        clearErrors()

        val enteredUserId = userIdInput.text?.toString().orEmpty()
        val enteredPassword = passwordInput.text?.toString().orEmpty()

        activityScope.launch {
            when (val result = loginService.authenticate(enteredUserId, enteredPassword)) {
                LoginResult.Success -> navigateToDashboard()
                is LoginResult.Error -> showInvalidCredentials(result.message)
            }
        }
    }

    fun uploadSyllabus() {
        // Code to open the file picker.
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
    }

    private fun navigateToDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }

    private fun showInvalidCredentials(message: String) {
        passwordErrorView.text = message
        passwordErrorView.visibility = TextView.VISIBLE
    }

    private fun clearErrors() {
        userIdErrorView.text = ""
        userIdErrorView.visibility = TextView.GONE
        passwordErrorView.text = ""
        passwordErrorView.visibility = TextView.GONE
    }
}
