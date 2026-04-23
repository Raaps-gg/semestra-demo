package com.example.semestra.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.semestra.R
import com.example.semestra.data.AppDatabase
import com.example.semestra.data.CourseProfile
import com.example.semestra.data.SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClassDirectoryActivity : AppCompatActivity() {
    private lateinit var recycler: RecyclerView
    private lateinit var empty: TextView
    private lateinit var adapter: ClassDirectoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_class_directory)
        title = getString(R.string.dashboard_class_directory)

        recycler = findViewById(R.id.recyclerClassDirectory)
        empty = findViewById(R.id.textClassDirectoryEmpty)
        recycler.layoutManager = GridLayoutManager(this, 2)
        adapter = ClassDirectoryAdapter(
            onSaveNotes = { profile, notes ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        AppDatabase.getInstance(applicationContext).courseProfileDao().update(
                            profile.copy(notes = notes)
                        )
                    }
                    Toast.makeText(this@ClassDirectoryActivity, R.string.class_directory_notes_saved, Toast.LENGTH_SHORT).show()
                }
            }
        )
        recycler.adapter = adapter

        val userId = SessionStore.getUserId(this)
        if (userId.isNullOrBlank()) {
            Toast.makeText(this, R.string.session_required, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val db = AppDatabase.getInstance(applicationContext)
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                db.courseProfileDao().insertAll(seedProfiles(userId))
            }
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                db.courseProfileDao().observeForUser(userId).collect { profiles ->
                    adapter.submitList(profiles)
                    val showEmpty = profiles.isEmpty()
                    empty.visibility = if (showEmpty) View.VISIBLE else View.GONE
                    recycler.visibility = if (showEmpty) View.GONE else View.VISIBLE
                }
            }
        }
    }

    private fun seedProfiles(userId: String): List<CourseProfile> = listOf(
        CourseProfile(
            profileId = "${userId}_CSE3315_002",
            userId = userId,
            courseName = "CSE 3315",
            section = "002",
            meetingTimes = "Tu/Th 11:00 AM - 12:20 PM",
            location = "NH 202",
            profEmail = "barasch@exchange.uta.edu",
            taEmail = "Hanani Pankaj",
            gradingScale = "HW 5%, Quizzes 15%, Exams 50%, Final 30%"
        ),
        CourseProfile(
            profileId = "${userId}_CSE3302_001",
            userId = userId,
            courseName = "CSE 3302",
            section = "001",
            meetingTimes = "Tu/Th 12:30 PM - 1:50 PM",
            location = "NH 109",
            profEmail = "jiandong.wang@uta.edu",
            taEmail = "cxh1126@mavs.uta.edu",
            gradingScale = "Labs 25%, HW 35%, Midterm 20%, Final 20%"
        ),
        CourseProfile(
            profileId = "${userId}_CSE3314_004",
            userId = userId,
            courseName = "CSE 3314",
            section = "004",
            meetingTimes = "Tu/Th 3:30 PM - 4:50 PM",
            location = "NH 203",
            profEmail = "nomaan.mufti@uta.edu",
            taEmail = "mohamed.mohamed4@mavs.uta.edu",
            gradingScale = "Exams 100pts, Quizzes 30pts, 5 Assignments"
        ),
        CourseProfile(
            profileId = "${userId}_CSE3310_001",
            userId = userId,
            courseName = "CSE 3310",
            section = "001",
            meetingTimes = "Tu/Th 2:00 PM - 3:20 PM",
            location = "SWSH 221",
            profEmail = "khalili@uta.edu",
            taEmail = "lxs5171@mavs.uta.edu",
            gradingScale = "Mid-term 25%, Final 25%, Term Project 50%"
        )
    )
}
