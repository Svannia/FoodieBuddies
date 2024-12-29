package com.example.foodiebuddy.database

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.foodiebuddy.data.RecipeDraft
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.firstOrNull

private const val SETTINGS = "settings"
private const val THEME = "theme_choice"
private const val DRAFTS = "recipe_drafts"

private val Context.dataStore by preferencesDataStore(name = SETTINGS)

/**
 * The DataStore contains locally-stored information.
 * Here it contains user preferences for the theme, stored in the DataStore under SETTINGS
 *
 * @property context used to access the DataStore
 */
class DataStoreManager(private val context: Context) {
    private val themeKey = stringPreferencesKey(THEME)
    private val draftsKey = stringPreferencesKey(DRAFTS)

    private val gson = Gson()

    // Theme preference
    val themeChoice: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[themeKey] ?: ThemeChoice.SYSTEM_DEFAULT.name
        }

    /**
     * Updates the DataStore with the user's preferred theme.
     *
     * @param themeChoice SYSTEM_DEFAULT, LIGHT or DARK
     */
    suspend fun setThemeChoice(themeChoice: ThemeChoice) {
        context.dataStore.edit { preferences ->
            preferences[themeKey] = themeChoice.name

        }
    }

    // Recipe Drafts
    val drafts: Flow<List<RecipeDraft>> = context.dataStore.data
        .map { preferences ->
            val json = preferences[draftsKey]
            if (!json.isNullOrBlank()) {
                val type = object : TypeToken<List<RecipeDraft>>() {}.type
                gson.fromJson(json, type)
            } else {
                emptyList()
            }
        }

    /**
     * Saves a new draft if it's ID doesn't exist yet, else updates the existing draft.
     *
     * @param draft RecipeDraft to add/update in the local data
     */
    suspend fun saveDraft(draft: RecipeDraft) {
        context.dataStore.edit { preferences ->
            val currentDrafts = drafts.firstOrNull() ?: emptyList()
            val updatedDrafts = currentDrafts.toMutableList()
            val index = updatedDrafts.indexOfFirst { it.id == draft.id }
            if (index >= 0) {
                updatedDrafts[index] = draft
            } else {
                updatedDrafts.add(draft)
            }
            preferences[draftsKey] = gson.toJson(updatedDrafts)
        }
    }

    /**
     * Deletes a draft from the local data.
     *
     * @param draftId ID of the draft to delete
     */
    suspend fun deleteDraft(draftId: String) {
        context.dataStore.edit { preferences ->
            val currentDrafts = drafts.firstOrNull() ?: emptyList()
            val updatedDrafts = currentDrafts.filter { it.id != draftId }
            preferences[draftsKey] = gson.toJson(updatedDrafts)
        }
    }
}

enum class ThemeChoice {
    SYSTEM_DEFAULT,
    LIGHT,
    DARK
}