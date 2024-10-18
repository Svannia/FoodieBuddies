package com.example.foodiebuddy.viewModels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodiebuddy.data.OwnedIngredient
import com.example.foodiebuddy.data.User
import com.example.foodiebuddy.data.UserPersonal
import com.example.foodiebuddy.database.DatabaseConnection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing user-related data in the Database.
 *
 * @property userID of the user (can be null if the user has not been created yet)
 */
class UserViewModel
@Inject
constructor(private val userID: String ?= null) : ViewModel() {
    private val db = DatabaseConnection()

    private val _userData = MutableStateFlow(User.empty())
    val userData: StateFlow<User> = _userData

    private val _userPersonal = MutableStateFlow(UserPersonal.empty())
    val userPersonal: StateFlow<UserPersonal> = _userPersonal

    /**
     * Creates a new user in DB.
     *
     * @param username input by user
     * @param picture Uri for the profile picture
     * @param bio input by user (can be empty)
     * @param isError block that runs if there is an error executing the function
     * @return
     */
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

    /**
     * Fetches all profile data of this ViewModel's user.
     *
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after all data was retrieved
     */
    fun fetchUserData(isError: (Boolean) -> Unit, callBack: () -> Unit) {
        if (userID != null) {
            // only fetches data if user exists
            db.userExists(
                userID = userID,
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
                    } else {
                        Log.d("VM", "Failed to retrieve user data: user does not exist.")
                    }
                },
                onFailure = { e ->
                    Log.d("VM", "Failed to check user existence when fetching in VM with error $e")
                }
            )
        }
    }

    /**
     * Fetches the default user profile picture.
     *
     * @return Uri of the default profile picture
     */
    suspend fun getDefaultPicture(): Uri {
        return db.getDefaultPicture()
    }

    /**
     * Edits the user profile data.
     *
     * @param username new username
     * @param picture new profile picture
     * @param bio new user bio
     * @param updatePicture whether or not the profile picture was changed
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after the data was updated in the DB
     */
    fun updateUser(username: String, picture: Uri, bio: String, updatePicture: Boolean, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        if (userID != null) {
            db.updateUser(userID, username, picture, bio, updatePicture, { isError(it) }) {
                fetchUserData({ isError(it) }) {callBack()}
            }
        }  else {
            Log.d("VM", "Failed to update user: ID is null")
        }
    }

    /**
     * Fetches the current user's UID.
     *
     * @return the user UID
     */
    fun getCurrentUserID(): String {
        return db.getCurrentUserID()
    }

    /**
     * Deletes a user from the DB.
     *
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after the user was deleted from th DB
     */
    fun deleteUser(isError: (Boolean) -> Unit, callBack: () -> Unit) {
        if (userID != null) {
            db.deleteUser(userID, { isError(it) }, callBack)
        } else {
            Log.d("VM", "Failed to delete user: ID is null")
        }
    }

    fun fetchUserPersonal(isError: (Boolean) -> Unit, callBack: () -> Unit) {
        if (userID != null) {
            // only fetches data if user exists
            db.userExists(
                userID = userID,
                onSuccess = { userExists ->
                    if (userExists) {
                        viewModelScope.launch {
                            val newUserPersonal = db.fetchUserPersonal(userID)
                            if (newUserPersonal.isEmpty()) { isError(true) }
                            else {
                                isError(false)
                                _userPersonal.value = newUserPersonal
                                callBack()
                            }
                        }
                    } else {
                        Log.d("VM", "Failed to retrieve user data: user does not exist.")
                    }
                },
                onFailure = { e ->
                    Log.d("VM", "Failed to check user existence when fetching in VM with error $e")
                }
            )
        }
        Log.d("VM", "userID is null")
    }

    fun updateCategories(editedCategories: MutableMap<String, String>, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        if (userID != null) {
            editedCategories.forEach { (key, value) ->
                db.updateCategory(userID, key, value, {isError(it)}) {
                    callBack()
                }
            }
            fetchUserPersonal( { isError(it) } ) { callBack() }
        }
    }

    fun addIngredients(newItems: Map<String, MutableList<String>>, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        viewModelScope.launch {
            if (userID != null) {
                newItems.forEach { (category, ingredients) ->
                    if (ingredients.isNotEmpty()) {
                        ingredients.forEach { ingredient ->
                            db.createIngredient(
                                userID, ingredient, ingredient, category, false, {isError(it) }) {
                            }
                        }
                    }
                }
                fetchUserPersonal( {isError(it) } ) { callBack() }
            } else {
                isError(true)
                Log.d("VM", "Failed to add ingredients: ID is null")
            }
        }
    }

    fun updateIngredientTick(uid: String, ticked: Boolean, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        if (userID != null) {
            db.updateIngredientTick(uid, ticked, { isError(it) }) {
                Log.d("Debug", "After updated to $ticked")
                fetchUserPersonal({ isError(it) }) {
                    Log.d("Debug", "new userPersonal is ${userPersonal.value}")
                    callBack()}
            }
        }  else {
            Log.d("VM", "Failed to update user: ID is null")
        }
    }

    fun removeIngredients(removedItems: Map<String, List<String>>, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        if (userID != null) {
            removedItems.forEach { (category, ingredients) ->
                if (ingredients.isNotEmpty()) {
                    ingredients.forEach { ingredient ->
                        db.deleteIngredient(ingredient, userID, category, {isError(it)})
                        { callBack() }
                    }
                }
            }
            fetchUserPersonal( {isError(it) } ) { callBack() }
        } else {
            Log.d("VM", "Failed to delete user: ID is null")
        }
    }
}
