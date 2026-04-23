package com.example.semestra.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.semestra.R
import com.example.semestra.data.AppDatabase
import com.example.semestra.data.SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClassesFragment : Fragment(R.layout.fragment_classes) {
    private lateinit var recycler: RecyclerView
    private lateinit var empty: TextView
    private lateinit var adapter: ClassDirectoryAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recycler = view.findViewById(R.id.recyclerClassDirectory)
        empty = view.findViewById(R.id.textClassDirectoryEmpty)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        val db = AppDatabase.getInstance(requireContext().applicationContext)
        adapter = ClassDirectoryAdapter(
            onSaveNotes = { profile, notes ->
                viewLifecycleOwner.lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        db.courseProfileDao().update(profile.copy(notes = notes))
                    }
                    Toast.makeText(requireContext(), R.string.class_directory_notes_saved, Toast.LENGTH_SHORT).show()
                }
            }
        )
        recycler.adapter = adapter

        val userId = SessionStore.getUserId(requireContext())
        if (userId.isNullOrBlank()) return
        if (!SessionStore.isDemoParsed(requireContext())) {
            adapter.submitList(emptyList())
            empty.text = getString(R.string.classes_waiting_parse)
            empty.visibility = View.VISIBLE
            recycler.visibility = View.GONE
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                db.courseProfileDao().observeForUser(userId).collect { profiles ->
                    adapter.submitList(profiles)
                    val showEmpty = profiles.isEmpty()
                    empty.visibility = if (showEmpty) View.VISIBLE else View.GONE
                    recycler.visibility = if (showEmpty) View.GONE else View.VISIBLE
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                db.examEventDao().observeSavedForUser(userId).collect { events ->
                    adapter.submitClassEvents(events)
                }
            }
        }
    }
}
