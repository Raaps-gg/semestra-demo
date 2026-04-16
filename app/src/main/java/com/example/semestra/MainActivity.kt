package com.example.semestra

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    // This runs when the app first opens
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This will eventually link to your XML layout (the visual screen)
        // setContentView(R.layout.activity_main)
    }

    /**
     * Requirement 002: User Login
     * Manages the transition from Logged Out to Logged In state.
     */
    fun login() {
        // Implementation for authentication logic
    }

    /**
     * Requirement 003: Syllabus Upload
     * Triggers the Android file-picker for PDF selection[cite: 133, 158].
     */
    fun uploadSyllabus() {
        // Code to open the file picker [cite: 133]
    }
}