package com.example.semestra.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.semestra.R
import com.example.semestra.data.AppDatabase
import com.example.semestra.data.SessionStore
import com.example.semestra.data.Syllabus
import kotlinx.coroutines.launch

class MySyllabiActivity : AppCompatActivity() {
    private lateinit var recycler: RecyclerView
    private lateinit var empty: TextView
    private lateinit var adapter: MySyllabiAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_syllabi)
        title = getString(R.string.dashboard_my_syllabi)

        recycler = findViewById(R.id.recyclerSyllabi)
        empty = findViewById(R.id.textSyllabiEmpty)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = MySyllabiAdapter { openSyllabus(it) }
        recycler.adapter = adapter

        val userId = SessionStore.getUserId(this)
        if (userId.isNullOrBlank()) {
            Toast.makeText(this, R.string.session_required, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        val db = AppDatabase.getInstance(applicationContext)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                db.syllabusDao().observeForUser(userId).collect { syllabi ->
                    adapter.submitList(syllabi)
                    val showEmpty = syllabi.isEmpty()
                    empty.visibility = if (showEmpty) View.VISIBLE else View.GONE
                    recycler.visibility = if (showEmpty) View.GONE else View.VISIBLE
                }
            }
        }
    }

    private fun openSyllabus(syllabus: Syllabus) {
        try {
            val uri = Uri.parse(syllabus.filePath)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, contentResolver.getType(uri))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.my_syllabus_no_viewer, Toast.LENGTH_LONG).show()
        } catch (_: Exception) {
            Toast.makeText(this, R.string.my_syllabus_open_error, Toast.LENGTH_LONG).show()
        }
    }
}
