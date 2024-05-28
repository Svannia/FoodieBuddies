package com.example.foodiebuddy.database

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.foodiebuddy.system.CHANNELS
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val SETTINGS = "settings"
private const val THEME = "theme_choice"

private val Context.dataStore by preferencesDataStore(name = SETTINGS)

class DataStoreManager(private val context: Context) {
    private val themeKey = stringPreferencesKey(THEME)
    private fun notificationKey(notificationName: String) = booleanPreferencesKey(notificationName)
    private fun allNotificationKeys(): List<Preferences.Key<Boolean>> {
        val notificationNames = CHANNELS
        return notificationNames.map { notificationKey(it) }
    }
    val themeChoice: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[themeKey] ?: ThemeChoice.SYSTEM_DEFAULT.name
        }
    fun allNotifications(): Flow<Map<String, Boolean>> = context.dataStore.data
        .map { preferences ->
            allNotificationKeys().associate { key ->
                key.name to (preferences[key] ?: false) }
        }

    suspend fun setThemeChoice(themeChoice: ThemeChoice) {
        context.dataStore.edit { preferences ->
            preferences[themeKey] = themeChoice.name

        }
    }
    suspend fun setNotificationState(notificationName: String, isEnabled: Boolean) {
        val key = booleanPreferencesKey(notificationName)
        context.dataStore.edit { preferences ->
            preferences[key] = isEnabled
        }
    }
}

enum class ThemeChoice {
    SYSTEM_DEFAULT,
    LIGHT,
    DARK
}