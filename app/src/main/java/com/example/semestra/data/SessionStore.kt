package com.example.semestra.data

import android.content.Context

/**
 * Persists the signed-in user id for scoping Room queries.
 * Cleared on logout (not implemented in this increment).
 */
object SessionStore {
    private const val PREFS_NAME = "semestra_session"
    private const val KEY_USER_ID = "current_user_id"
    private const val KEY_DEMO_PARSED = "demo_parsed"

    fun saveUserId(context: Context, userId: String) {
        prefs(context).edit().putString(KEY_USER_ID, userId).apply()
    }

    fun getUserId(context: Context): String? =
        prefs(context).getString(KEY_USER_ID, null)?.takeIf { it.isNotBlank() }

    fun markDemoParsed(context: Context, parsed: Boolean) {
        prefs(context).edit().putBoolean(KEY_DEMO_PARSED, parsed).apply()
    }

    fun isDemoParsed(context: Context): Boolean = prefs(context).getBoolean(KEY_DEMO_PARSED, false)

    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_USER_ID).remove(KEY_DEMO_PARSED).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
