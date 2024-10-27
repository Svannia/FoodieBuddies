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

    // userData contains the user's profile information (can be seen by everyone)
    private val _userData = MutableStateFlow(User.empty())
    val userData: StateFlow<User> = _userData

    // userPersonal contains the user's private data (can only be seen by its owner)
    private val _userPersonal = MutableStateFlow(UserPersonal.empty())
    val userPersonal: StateFlow<UserPersonal> = _userPersonal

    /**
     * Creates a new user in DB.
     *
     * @param username input by user
     * @param picture Uri for the profile picture
     * @param bio input by user (can be empty)
     * @param isError block that runs if there is an error executing the function
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

    /**
     * Fetches all personal data of this ViewModel's user.
     *
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after all data was retrieved
     */
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
        } else {
            Log.d("VM", "userID is null")
        }
    }

    /**
     * Updates changes to categories (new categories added and name change of existing categories).
     *
     * @param newCategories maps new category names to lists of new ingredients
     * @param editedCategories maps old category names to new ones
     * @param isInFridge whether the new ingredients should be added in the fridge. If false, they're added in groceries
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after all categories were updated
     */
    fun updateCategories(newCategories: Map<String, List<OwnedIngredient>>, editedCategories: Map<String, String>, isInFridge: Boolean, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        viewModelScope.launch {
            if (userID != null) {
                // if there are new categories to add ->
                if (newCategories.isNotEmpty()) {
                    // check how many newCategories are left to add before callBack
                    var remaining = newCategories.size
                    // for each new category, create it (also adds the potential new ingredients)
                    newCategories.forEach { (category, ingredients) ->
                        db.addCategory(userID, category, ingredients, isInFridge, {isError(it)}) {
                            remaining--
                            // if all categories have been added, process the name changes
                            if (remaining <= 0) { editCategoryNames(editedCategories, {isError(it)}) { callBack() }
                            }
                        }
                    }
                // if there are no categories to add -> process name changes
                } else {
                    editCategoryNames(editedCategories, {isError(it)}) { callBack() }
                }
            } else {
                Log.d("VM", "Could not update categories, userID is null")
            }
        }
    }

    /**
     * Deletes a list of categories.
     *
     * @param removedCategories list of category names to be deleted
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after all categories were deleted
     */
    fun deleteCategories(removedCategories: List<String>, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        if (userID != null) {
            // if there are categories to delete ->
            if (removedCategories.isNotEmpty()) {
                // check how many categories left to delete before callBack
                var remaining = removedCategories.size
                removedCategories.forEach { category ->
                    db.deleteCategory(userID, category, { isError(it) }) {
                        remaining--
                        if (remaining <= 0) { fetchUserPersonal( { isError(it) } ) { callBack() } }
                    }
                }
            // if there are no categories to delete -> directly callBack
            } else {
                callBack()
            }
        } else {
            Log.d("VM", "Could not delete categories, userID is null")
        }
    }

    /**
     * Adds a list of ingredients to existing categories.
     *
     * @param newItems maps category names to their lists of new ingredients
     * @param isInFridge whether the new ingredients should be added in the fridge. If false, they're added in groceries
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after all ingredients were added
     */
    fun addIngredients(newItems: Map<String, List<OwnedIngredient>>, isInFridge: Boolean, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        viewModelScope.launch {
            if (userID != null) {
                // if the map is not empty ->
                if (newItems.isNotEmpty()) {
                    // check how many categories still have ingredients left to add before callBack
                    var remainingItems = newItems.size
                    // for each category, if there are new ingredients to add ->
                    newItems.forEach { (_, ingredients) ->
                        if (ingredients.isNotEmpty()) {
                            // check how many ingredients are left to add before decreasing category counter
                            var remaining = ingredients.size
                            ingredients.forEach { ingredient ->
                                db.createIngredient(userID, ingredient, isInFridge, {isError(it) }) {
                                    remaining--
                                    if (remaining <= 0) {
                                        remainingItems--
                                        if (remainingItems <= 0) {
                                            callBack()
                                        }
                                    }
                                }
                            }
                        // if this category does not have any new ingredients -> decrease category counter
                        } else { remainingItems--
                            if (remainingItems <= 0) {
                                callBack()
                            }
                        }
                    }
                // if the map is empty -> callBack
                } else { callBack() }
            } else {
                isError(true)
                Log.d("VM", "Failed to add ingredients: ID is null")
            }
        }
    }

    /**
     * Changes the isTicked field of an ingredient.
     *
     * @param uid ID of the ingredient
     * @param ticked new isTicked value
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after ingredient's isTicked was updated
     */
    fun updateIngredientTick(uid: String, ticked: Boolean, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        if (userID != null) {
            db.updateIngredientTick(uid, ticked, { isError(it) }) {
                fetchUserPersonal({ isError(it) }) { callBack()} }
        }  else {
            Log.d("VM", "Failed to update user: ID is null")
        }
    }

    /**
     * Removes a list of ingredients.
     *
     * @param removedItems maps category names to a list of ingredients ids to be deleted
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after all ingredients were deleted
     */
    fun deleteIngredients(removedItems: Map<String, List<String>>, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        if (userID != null) {
            // if the map is not empty
            if (removedItems.isNotEmpty()) {
                // check how many categories are left with ingredients to delete before callBack
                var remainingItems = removedItems.size
                removedItems.forEach { (category, ingredients) ->
                    // if this category has ingredients to delete ->
                    if (ingredients.isNotEmpty()) {
                        // check how ingredients are left to delete before decreasing category counter
                        var remaining = ingredients.size
                        ingredients.forEach { ingredient ->
                            db.deleteIngredient(ingredient, userID, category, { isError(it) }){
                                remaining--
                                if (remaining <= 0) {
                                    remainingItems--
                                    if (remainingItems <= 0) {
                                        callBack()
                                    }
                                }
                            }
                        }
                    // if this category does not have any ingredient to delete -> decrease category counter
                    } else { remainingItems--
                        if (remainingItems <= 0) {
                            callBack()
                        }
                    }
                }
            // if the map is empty -> callBack
            } else { callBack() }
        } else {
            Log.d("VM", "Failed to delete user: ID is null")
        }
    }

    /**
     * Updates changes in category names.
     *
     * @param editedCategories maps old category names to new ones
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after all category names were updated
     */
    private fun editCategoryNames(editedCategories: Map<String, String>, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        if (userID != null) {
            // if the map is not emtpy ->
            if (editedCategories.isNotEmpty()) {
                // check how many categories are left to update before callBack
                var remaining = editedCategories.size
                editedCategories.forEach { (old, new) ->
                    db.updateCategory(userID, old, new, {isError(it)})
                    {
                        remaining--
                        if (remaining <= 0) { callBack() }
                    }
                }
            // if the map is empty -> callBack
            } else { callBack() }
        }
    }
}
