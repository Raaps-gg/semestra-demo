package com.example.semestra.logic

import android.content.Context
import com.example.semestra.R
import com.example.semestra.data.AppDatabase
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Pushes local [com.example.semestra.data.ExamEvent] rows to Google Calendar.
 *
 * OAuth: ensure a **Web application** OAuth client exists in Google Cloud Console and that
 * the SHA-1 of your signing key is added to the Android OAuth client. Use
 * [com.google.android.gms.auth.api.signin.GoogleSignIn] with
 * [com.google.android.gms.common.api.Scope] of [CalendarScopes.CALENDAR_EVENTS] before calling [syncSavedEvents].
 */
class CalendarServices(private val context: Context) {

    sealed class SyncResult {
        data class Success(val pushedCount: Int) : SyncResult()
        data class Failure(val message: String, val authorizationDenied: Boolean = false) : SyncResult()
    }

    suspend fun syncSavedEvents(
        account: GoogleSignInAccount,
        database: AppDatabase,
        userId: String
    ): SyncResult = withContext(Dispatchers.IO) {
        val dao = database.examEventDao()
        val pending = dao.getUnsyncedSaved(userId)
        if (pending.isEmpty()) {
            return@withContext SyncResult.Failure(context.getString(R.string.calendar_sync_no_events))
        }

        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(CalendarScopes.CALENDAR_EVENTS)
        )
        credential.selectedAccount = account.account

        val service = Calendar.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName(context.getString(R.string.app_name))
            .build()

        try {
            var pushed = 0
            for (event in pending) {
                val start = DateTime(event.eventDate)
                val end = DateTime(event.eventDate + HOUR_MS)
                val body = Event()
                    .setSummary("${event.className}: ${event.examTitle}")
                    .setStart(EventDateTime().setDateTime(start))
                    .setEnd(EventDateTime().setDateTime(end))
                service.events().insert("primary", body).execute()
                dao.markSynced(event.eventId)
                pushed++
            }
            SyncResult.Success(pushed)
        } catch (e: UserRecoverableAuthIOException) {
            SyncResult.Failure(
                context.getString(R.string.calendar_sync_auth_denied),
                authorizationDenied = true
            )
        } catch (e: IOException) {
            SyncResult.Failure(context.getString(R.string.calendar_sync_network_error))
        } catch (e: Exception) {
            SyncResult.Failure(e.message ?: context.getString(R.string.calendar_sync_generic_error))
        }
    }

    private companion object {
        private const val HOUR_MS = 60L * 60L * 1000L
    }
}
