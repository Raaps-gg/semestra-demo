package com.example.semestra.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.semestra.R // This fixes the red 'R'

class ScheduleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // You will create this layout in the next step
        setContentView(R.layout.activity_schedule)

        title = getString(R.string.schedule_title)
    }
}