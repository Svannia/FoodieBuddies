package com.example.foodiebuddy.viewModels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodiebuddy.database.DataStoreManager
import com.example.foodiebuddy.database.ThemeChoice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PreferencesViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStoreManager = DataStoreManager(application)

    private val _currentTheme = MutableStateFlow(ThemeChoice.SYSTEM_DEFAULT)
    val currentTheme: StateFlow<ThemeChoice> = _currentTheme

    private val _notificationStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val notificationStates: StateFlow<Map<String, Boolean>> = _notificationStates

    init {
        loadTheme()
        loadNotificationStates()
    }

    fun setTheme(themeChoice: ThemeChoice) {
        viewModelScope.launch {
            _currentTheme.value = themeChoice
            dataStoreManager.setThemeChoice(themeChoice)
        }
    }
    fun getNotificationState(id: String): Boolean {
        return _notificationStates.value[id] ?: false
    }
    fun setNotificationState(notificationName: String, isEnabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setNotificationState(notificationName, isEnabled)
            loadNotificationStates()
        }
    }

    private fun loadTheme() {
        viewModelScope.launch {
            dataStoreManager.themeChoice.collect { themeName ->
                _currentTheme.value = ThemeChoice.valueOf(themeName)
            }
        }
    }
    private fun loadNotificationStates() {
        viewModelScope.launch {
            dataStoreManager.allNotifications().collect {
                _notificationStates.value = it
            }
        }
    }
}