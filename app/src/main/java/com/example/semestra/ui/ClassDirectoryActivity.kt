package com.example.semestra.ui

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
import kotlinx.coroutines.launch

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
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = ClassDirectoryAdapter()
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
}
