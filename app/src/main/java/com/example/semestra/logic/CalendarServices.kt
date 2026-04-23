package com.example.semestra.logic

import android.content.Context
import com.example.semestra.R
import com.example.semestra.data.AppDatabase
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.http.javanet.NetHttpTransport
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
        // #region agent log
        runCatching {
            java.io.File("/Users/vanthiang/Semestra/.cursor/debug-751471.log").appendText(
                """{"sessionId":"751471","runId":"pre-fix","hypothesisId":"H1","location":"CalendarServices.kt:syncSavedEvents","message":"Entered calendar sync","data":{"userIdPresent":${userId.isNotBlank()}},"timestamp":${System.currentTimeMillis()}}""" + "\n"
            )
        }
        // #endregion
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

        // #region agent log
        runCatching {
            java.io.File("/Users/vanthiang/Semestra/.cursor/debug-751471.log").appendText(
                """{"sessionId":"751471","runId":"pre-fix","hypothesisId":"H2","location":"CalendarServices.kt:Calendar.Builder","message":"Building Calendar service transport","data":{"pendingCount":${pending.size}},"timestamp":${System.currentTimeMillis()}}""" + "\n"
            )
        }
        // #endregion
        val service = Calendar.Builder(
            NetHttpTransport(),
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
                val created = service.events().insert("primary", body).execute()
                val googleId = created.id.orEmpty()
                if (googleId.isNotBlank()) {
                    dao.markSynced(event.eventId, googleId)
                }
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

    suspend fun deleteFromCalendarIfSynced(
        account: GoogleSignInAccount,
        database: AppDatabase,
        eventId: String,
        googleEventId: String
    ): SyncResult = withContext(Dispatchers.IO) {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(CalendarScopes.CALENDAR_EVENTS)
        )
        credential.selectedAccount = account.account
        val service = Calendar.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName(context.getString(R.string.app_name))
            .build()
        try {
            service.events().delete("primary", googleEventId).execute()
            database.examEventDao().deleteById(eventId)
            SyncResult.Success(1)
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
