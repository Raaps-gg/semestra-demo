package com.example.semestra.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.semestra.MainActivity
import com.example.semestra.R
import com.example.semestra.data.AppDatabase
import com.example.semestra.data.SessionStore
import com.example.semestra.logic.CalendarServices
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.material.button.MaterialButton
import com.google.api.services.calendar.CalendarScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private lateinit var calendarServices: CalendarServices

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        val account = try {
            task.getResult(ApiException::class.java)
        } catch (_: ApiException) {
            null
        } ?: return@registerForActivityResult
        syncCalendar()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        calendarServices = CalendarServices(requireContext().applicationContext)
        val userId = SessionStore.getUserId(requireContext()).orEmpty()
        view.findViewById<TextView>(R.id.textProfileUserId).text = getString(R.string.settings_user_id, userId)
        view.findViewById<TextView>(R.id.textSettingsEmail).text = getString(R.string.settings_email, "$userId@semestra.demo")
        view.findViewById<TextView>(R.id.textSettingsAvatar).text = userId.firstOrNull()?.uppercase() ?: "U"
        view.findViewById<MaterialButton>(R.id.buttonSettingsSync).setOnClickListener {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(Scope(CalendarScopes.CALENDAR_EVENTS))
                .build()
            signInLauncher.launch(GoogleSignIn.getClient(requireContext(), gso).signInIntent)
        }
        view.findViewById<MaterialButton>(R.id.buttonSignOut).setOnClickListener {
            SessionStore.clear(requireContext())
            startActivity(Intent(requireContext(), MainActivity::class.java))
            requireActivity().finish()
        }
    }

    private fun syncCalendar() {
        val userId = SessionStore.getUserId(requireContext()) ?: return
        val account = GoogleSignIn.getLastSignedInAccount(requireContext()) ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                calendarServices.syncSavedEvents(
                    account = account,
                    database = AppDatabase.getInstance(requireContext().applicationContext),
                    userId = userId
                )
            }
            when (result) {
                is CalendarServices.SyncResult.Success ->
                    Toast.makeText(requireContext(), getString(R.string.calendar_sync_success, result.pushedCount), Toast.LENGTH_LONG).show()
                is CalendarServices.SyncResult.Failure ->
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
            }
        }
    }
}
