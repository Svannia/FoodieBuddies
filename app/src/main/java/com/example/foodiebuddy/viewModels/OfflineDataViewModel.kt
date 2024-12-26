package com.example.foodiebuddy.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodiebuddy.data.RecipeDraft
import com.example.foodiebuddy.database.DataStoreManager
import com.example.foodiebuddy.database.ThemeChoice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for data that can be accessed offline because it is stored locally.
 *
 * @property application to access the DataStoreManager
 */
class OfflineDataViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStoreManager = DataStoreManager(application)

    private val _currentTheme = MutableStateFlow(ThemeChoice.SYSTEM_DEFAULT)
    val currentTheme: StateFlow<ThemeChoice> = _currentTheme

    val drafts: StateFlow<List<RecipeDraft>> = dataStoreManager.drafts.stateIn(
        viewModelScope, SharingStarted.Eagerly, emptyList()
    )

    init {
        loadTheme()
    }

    /**
     * Sets a specific theme on the entire app (dark or light) and updates that choice in local data.
     *
     * @param themeChoice theme be be set
     */
    fun setTheme(themeChoice: ThemeChoice) {
        viewModelScope.launch {
            _currentTheme.value = themeChoice
            dataStoreManager.setThemeChoice(themeChoice)
        }
    }

    /**
     * Fetches the user-chosen theme from local data.
     */
    private fun loadTheme() {
        viewModelScope.launch {
            dataStoreManager.themeChoice.collect { themeName ->
                _currentTheme.value = ThemeChoice.valueOf(themeName)
            }
        }
    }

    fun saveDraft(draft: RecipeDraft) {
        viewModelScope.launch { dataStoreManager.saveDraft(draft) }
    }

    fun deleteDraft(draftId: String) {
        viewModelScope.launch { dataStoreManager.deleteDraft(draftId) }
    }
}