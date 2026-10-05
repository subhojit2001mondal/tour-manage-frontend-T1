package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.repository.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tour_manage_preferences")

data class DemoSessionData(
    val email: String,
    val name: String
)

class AppDataStore(private val context: Context) {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEMO_ACTIVE = booleanPreferencesKey("demo_session_active")
        val DEMO_EMAIL = stringPreferencesKey("demo_email")
        val DEMO_NAME = stringPreferencesKey("demo_name")
    }

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            when (preferences[PreferencesKeys.THEME_MODE]) {
                "LIGHT" -> ThemeMode.LIGHT
                "DARK" -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    val demoSessionFlow: Flow<DemoSessionData?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val active = preferences[PreferencesKeys.DEMO_ACTIVE] ?: false
            if (active) {
                val email = preferences[PreferencesKeys.DEMO_EMAIL] ?: "demo@tourmanage.com"
                val name = preferences[PreferencesKeys.DEMO_NAME] ?: "Demo Traveler"
                DemoSessionData(email = email, name = name)
            } else {
                null
            }
        }

    suspend fun saveDemoSession(email: String, name: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEMO_ACTIVE] = true
            preferences[PreferencesKeys.DEMO_EMAIL] = email
            preferences[PreferencesKeys.DEMO_NAME] = name
        }
    }

    suspend fun clearDemoSession() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEMO_ACTIVE] = false
            preferences.remove(PreferencesKeys.DEMO_EMAIL)
            preferences.remove(PreferencesKeys.DEMO_NAME)
        }
    }
}
