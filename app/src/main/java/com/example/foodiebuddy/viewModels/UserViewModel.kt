package com.example.foodiebuddy.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.foodiebuddy.data.User
import com.example.foodiebuddy.database.DatabaseConnection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class UserViewModel(private val userID: String ?= null ) : ViewModel() {
    private val db = DatabaseConnection()
    private val _userData = MutableStateFlow<User>(User(""))
    val userData: StateFlow<User> = _userData

    init {
        if (userID != null) {
            // todo
        } else {
            Log.d("VM", "UserViewModel initiated without ID")
        }
    }

}
