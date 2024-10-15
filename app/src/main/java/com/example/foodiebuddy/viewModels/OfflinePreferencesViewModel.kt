package com.example.foodiebuddy.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodiebuddy.database.DataStoreManager
import com.example.foodiebuddy.database.ThemeChoice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class OfflinePreferencesViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStoreManager = DataStoreManager(application)

    private val _currentTheme = MutableStateFlow(ThemeChoice.SYSTEM_DEFAULT)
    val currentTheme: StateFlow<ThemeChoice> = _currentTheme

    init {
        loadTheme()
    }

    fun setTheme(themeChoice: ThemeChoice) {
        viewModelScope.launch {
            _currentTheme.value = themeChoice
            dataStoreManager.setThemeChoice(themeChoice)
        }
    }

    private fun loadTheme() {
        viewModelScope.launch {
            dataStoreManager.themeChoice.collect { themeName ->
                _currentTheme.value = ThemeChoice.valueOf(themeName)
            }
        }
    }
}