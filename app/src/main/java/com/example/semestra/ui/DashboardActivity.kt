package com.example.semestra.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.example.semestra.R // Ensure R is imported
import com.example.semestra.ui.UploadActivity // Explicitly import sister activities
import com.example.semestra.ui.ScheduleActivity

class DashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        title = getString(R.string.dashboard_title)

        findViewById<MaterialButton>(R.id.buttonUploadSyllabus).setOnClickListener {
            openUploadScreen()
        }
        findViewById<MaterialButton>(R.id.buttonViewSchedule).setOnClickListener {
            openScheduleScreen()
        }
        findViewById<MaterialButton>(R.id.buttonSyncGoogleCalendar).setOnClickListener {
            syncToGoogleCalendar()
        }
    }

    private fun openUploadScreen() {
        startActivity(Intent(this, UploadActivity::class.java))
    }

    private fun openScheduleScreen() {
        startActivity(Intent(this, ScheduleActivity::class.java))
    }

    private fun syncToGoogleCalendar() {
        Toast.makeText(this, R.string.sync_placeholder_message, Toast.LENGTH_SHORT).show()
    }
}
