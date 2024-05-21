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
    fun fetchUserData(callBack: () -> Unit) {
        if (userID != null) {
            db.userExists(
                uid = userID,
                onSuccess = { userExists ->
                    if (userExists) {
                        viewModelScope.launch {
                            val newUser = db.fetchUserData(userID)
                            _userData.value = newUser
                            callBack()
                        }
                    }
                },
                onFailure = { e ->
                    Log.d("Error", "Failed to check user existence when fetching in VM with error $e")
                }
            )
        }
    }

    suspend fun getDefaultPicture(): Uri {
        return db.getDefaultPicture()
    }
    fun updateUser(username: String, picture: Uri, bio: String, updatePicture: Boolean, callBack: () -> Unit) {
        if (userID != null) {
            db.updateUser(userID, username, picture, bio, updatePicture) {
                fetchUserData {callBack()}
            }
        }  else {
            Log.d("VM", "Failed to update user: ID is null")
        }
    }

    fun getCurrentUserID(): String {
        return db.getCurrentUserID()
    }

    fun deleteUser(callBack: () -> Unit) {
        if (userID != null) {
            db.deleteUser(userID, callBack)
            Log.d("Debug", "deleting user in VM")
        } else {
            Log.d("VM", "Failed to delete user: ID is null")
        }
    }
}
