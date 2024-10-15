package com.example.foodiebuddy.database

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val SETTINGS = "settings"
private const val THEME = "theme_choice"

private val Context.dataStore by preferencesDataStore(name = SETTINGS)

class DataStoreManager(private val context: Context) {
    private val themeKey = stringPreferencesKey(THEME)

    val themeChoice: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[themeKey] ?: ThemeChoice.SYSTEM_DEFAULT.name
        }

    suspend fun setThemeChoice(themeChoice: ThemeChoice) {
        context.dataStore.edit { preferences ->
            preferences[themeKey] = themeChoice.name

        }
    }
}

enum class ThemeChoice {
    SYSTEM_DEFAULT,
    LIGHT,
    DARK
}