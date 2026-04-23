package com.example.semestra.data

import android.content.Context

/**
 * Persists the signed-in user id for scoping Room queries.
 * Cleared on logout (not implemented in this increment).
 */
object SessionStore {
    private const val PREFS_NAME = "semestra_session"
    private const val KEY_USER_ID = "current_user_id"

    fun saveUserId(context: Context, userId: String) {
        prefs(context).edit().putString(KEY_USER_ID, userId).apply()
    }

    fun getUserId(context: Context): String? =
        prefs(context).getString(KEY_USER_ID, null)?.takeIf { it.isNotBlank() }

    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_USER_ID).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
