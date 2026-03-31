package com.incidencias.session

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "session_prefs")

class SessionManager(private val context: Context) {

    companion object {
        private val TOKEN_KEY = stringPreferencesKey("jwt_token")
        private val ROLE_KEY = stringPreferencesKey("user_role")
        private val USER_ID_KEY = longPreferencesKey("user_id")
        private val FIRST_NAME_KEY = stringPreferencesKey("first_name")
        private val LAST_NAME_KEY = stringPreferencesKey("last_name")
        private val EMAIL_KEY = stringPreferencesKey("email")
        private val TEAM_ID_KEY = longPreferencesKey("team_id")
        private val TEAM_NAME_KEY = stringPreferencesKey("team_name")
        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
    }

    val darkModeFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DARK_MODE_KEY] ?: false
    }

    val tokenFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[TOKEN_KEY]
    }

    val roleFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[ROLE_KEY]
    }

    val userIdFlow: Flow<Long?> = context.dataStore.data.map { preferences ->
        preferences[USER_ID_KEY]
    }

    val firstNameFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[FIRST_NAME_KEY]
    }

    val lastNameFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[LAST_NAME_KEY]
    }

    val emailFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[EMAIL_KEY]
    }

    val teamIdFlow: Flow<Long?> = context.dataStore.data.map { preferences ->
        preferences[TEAM_ID_KEY]
    }

    val teamNameFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[TEAM_NAME_KEY]
    }

    suspend fun saveDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = enabled
        }
    }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
        }
    }

    suspend fun saveUserData(
        userId: Long,
        firstName: String,
        lastName: String,
        email: String,
        role: String,
        teamId: Long?,
        teamName: String?
    ) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = userId
            preferences[FIRST_NAME_KEY] = firstName
            preferences[LAST_NAME_KEY] = lastName
            preferences[EMAIL_KEY] = email
            preferences[ROLE_KEY] = role

            if (teamId != null) {
                preferences[TEAM_ID_KEY] = teamId
            } else {
                preferences.remove(TEAM_ID_KEY)
            }

            if (teamName != null) {
                preferences[TEAM_NAME_KEY] = teamName
            } else {
                preferences.remove(TEAM_NAME_KEY)
            }
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.remove(TOKEN_KEY)
            preferences.remove(ROLE_KEY)
            preferences.remove(USER_ID_KEY)
            preferences.remove(FIRST_NAME_KEY)
            preferences.remove(LAST_NAME_KEY)
            preferences.remove(EMAIL_KEY)
            preferences.remove(TEAM_ID_KEY)
            preferences.remove(TEAM_NAME_KEY)
        }
    }
}
