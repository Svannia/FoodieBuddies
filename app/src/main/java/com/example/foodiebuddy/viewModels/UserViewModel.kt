package com.example.foodiebuddy.viewModels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodiebuddy.data.User
import com.example.foodiebuddy.database.DatabaseConnection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UserViewModel(private val userID: String ?= null ) : ViewModel() {
    private val db = DatabaseConnection()
    private val _userData = MutableStateFlow(User.empty())
    val userData: StateFlow<User> = _userData

    fun createUser(username: String, picture: Uri, bio: String) {
        viewModelScope.launch {
            if (userID != null) {
                db.createUser(userID, username, picture, bio)
            } else {
                Log.d("VM", "Failed to create user: ID is null")
            }
        }
    }
    fun fetchUserData() {
        viewModelScope.launch { _userData.value = userID?.let { db.fetchUserData(it) }!! }
    }

    suspend fun getDefaultPicture(): Uri {
        return db.getDefaultPicture()
    }


}
