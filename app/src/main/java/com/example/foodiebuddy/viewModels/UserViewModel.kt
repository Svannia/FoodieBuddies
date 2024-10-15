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
import javax.inject.Inject

class UserViewModel
@Inject
constructor(private val userID: String ?= null) : ViewModel() {
    private val db = DatabaseConnection()
    private val _userData = MutableStateFlow(User.empty())
    val userData: StateFlow<User> = _userData

    fun createUser(username: String, picture: Uri, bio: String, isError: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (userID != null) {
                db.createUser(userID, username, picture, bio) {
                    isError(it)
                }
            } else {
                isError(true)
                Log.d("VM", "Failed to create user: ID is null")
            }
        }
    }
    fun fetchUserData(isError: (Boolean) -> Unit, callBack: () -> Unit) {
        if (userID != null) {
            db.userExists(
                uid = userID,
                onSuccess = { userExists ->
                    if (userExists) {
                        viewModelScope.launch {
                            val newUser = db.fetchUserData(userID)
                            if (newUser.isEmpty()) { isError(true) }
                            else {
                                isError(false)
                                _userData.value = newUser
                                callBack()
                            }
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
    fun updateUser(username: String, picture: Uri, bio: String, updatePicture: Boolean, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        if (userID != null) {
            db.updateUser(userID, username, picture, bio, updatePicture, { isError(it) }) {
                fetchUserData({ isError(it) }) {callBack()}
            }
        }  else {
            Log.d("VM", "Failed to update user: ID is null")
        }
    }

    fun getCurrentUserID(): String {
        return db.getCurrentUserID()
    }

    fun deleteUser(isError: (Boolean) -> Unit, callBack: () -> Unit) {
        if (userID != null) {
            db.deleteUser(userID, { isError(it) }, callBack)
        } else {
            Log.d("VM", "Failed to delete user: ID is null")
        }
    }
}
