package com.taxivn.dispatch.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.taxivn.dispatch.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "taxivn_session_prefs")

class SessionPreferences(private val context: Context) {

    companion object {
        private val KEY_SERVER_URL = stringPreferencesKey("server_url")
        private val KEY_USERNAME = stringPreferencesKey("username")
        private val KEY_ROLE = stringPreferencesKey("user_role")
        private val KEY_TOKEN = stringPreferencesKey("auth_token")
    }

    val serverUrlFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SERVER_URL]?.takeIf { it.isNotBlank() } ?: BuildConfig.DEFAULT_TAXIVN_SERVER_URL
    }

    val savedSessionFlow: Flow<SavedSession?> = context.dataStore.data.map { prefs ->
        val username = prefs[KEY_USERNAME]
        val role = prefs[KEY_ROLE]
        val token = prefs[KEY_TOKEN]
        if (!username.isNullOrBlank() && !role.isNullOrBlank()) {
            SavedSession(username = username, roleName = role, token = token.orEmpty())
        } else {
            null
        }
    }

    suspend fun saveServerUrl(url: String) {
        val normalized = if (url.endsWith("/")) url else "$url/"
        context.dataStore.edit { prefs ->
            prefs[KEY_SERVER_URL] = normalized
        }
    }

    suspend fun saveSession(username: String, roleName: String, token: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USERNAME] = username
            prefs[KEY_ROLE] = roleName
            prefs[KEY_TOKEN] = token
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_USERNAME)
            prefs.remove(KEY_ROLE)
            prefs.remove(KEY_TOKEN)
        }
    }
}

data class SavedSession(
    val username: String,
    val roleName: String,
    val token: String
)
