package com.example.semestra.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.google.android.material.snackbar.Snackbar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.semestra.R
import com.example.semestra.data.AppDatabase
import com.example.semestra.data.SessionStore
import com.example.semestra.logic.CalendarServices
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.material.button.MaterialButton
import com.google.api.services.calendar.CalendarScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardActivity : AppCompatActivity() {

    private lateinit var calendarServices: CalendarServices

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            runCalendarSync(account)
        } catch (e: ApiException) {
            showSyncMessage(getString(R.string.calendar_sync_auth_denied))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        title = getString(R.string.dashboard_title)
        calendarServices = CalendarServices(applicationContext)

        findViewById<MaterialButton>(R.id.buttonUploadSyllabus).setOnClickListener {
            startActivity(Intent(this, UploadActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.buttonViewSchedule).setOnClickListener {
            startActivity(Intent(this, ScheduleActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.buttonSyncGoogleCalendar).setOnClickListener {
            beginGoogleCalendarFlow()
        }
    }

    private fun beginGoogleCalendarFlow() {
        val userId = SessionStore.getUserId(this)
        if (userId.isNullOrBlank()) {
            showSyncMessage(getString(R.string.session_required))
            return
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(CalendarScopes.CALENDAR_EVENTS))
            .build()
        val client = GoogleSignIn.getClient(this, gso)
        googleSignInLauncher.launch(client.signInIntent)
    }

    private fun runCalendarSync(account: GoogleSignInAccount) {
        val userId = SessionStore.getUserId(this) ?: return
        val root = findViewById<View>(android.R.id.content)
        lifecycleScope.launch {
            when (
                val result = withContext(Dispatchers.IO) {
                    calendarServices.syncSavedEvents(
                        account,
                        AppDatabase.getInstance(applicationContext),
                        userId
                    )
                }
            ) {
                is CalendarServices.SyncResult.Success -> {
                    Snackbar.make(
                        root,
                        getString(R.string.calendar_sync_success, result.pushedCount),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
                is CalendarServices.SyncResult.Failure -> {
                    val bar = Snackbar.make(root, result.message, Snackbar.LENGTH_LONG)
                    bar.setAction(R.string.retry) {
                        beginGoogleCalendarFlow()
                    }
                    bar.show()
                }
            }
        }
    }

    private fun showSyncMessage(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show()
    }
}
