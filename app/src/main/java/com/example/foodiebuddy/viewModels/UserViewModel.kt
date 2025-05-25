package com.example.foodiebuddy.viewModels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodiebuddy.data.OwnedIngredient
import com.example.foodiebuddy.data.Recipe
import com.example.foodiebuddy.data.RecipeFilters
import com.example.foodiebuddy.data.User
import com.example.foodiebuddy.data.UserPersonal
import com.example.foodiebuddy.database.DatabaseConnection
import com.example.foodiebuddy.errors.handleError
import com.example.foodiebuddy.navigation.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
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

    // allUsers contains a list of all public users' info
    private val _allUsers = MutableStateFlow(emptyList<User>())
    val allUsers: StateFlow<List<User>> = _allUsers

    // userPersonal contains the user's private data (can only be seen by its owner)
    private val _userPersonal = MutableStateFlow(UserPersonal.empty())
    val userPersonal: StateFlow<UserPersonal> = _userPersonal

    // allRecipes contains a list of all recipes on the app
    private val _allRecipes = MutableStateFlow(emptyList<Recipe>())
    val allRecipes: StateFlow<List<Recipe>> = _allRecipes

    // filters contains all the filters that the user has currently activated
    private val _filters = MutableStateFlow(RecipeFilters.empty())
    val filters: StateFlow<RecipeFilters> = _filters

    // filtered recipes contain the current filters applied on the list of all recipes
    private val _filteredRecipes = MutableStateFlow<List<Recipe>>(emptyList())
    val filteredRecipes: StateFlow<List<Recipe>> = _filteredRecipes

    /**
     * Fetches this UserViewModel's UID.
     *
     * @return the UserViewModel's UID if it is non-null, an empty string otherwise
     */
    fun getVmUid(): String { return userID ?: "" }

    // user profile
    /**
     * Creates a new user in DB.
     *
     * @param username input by user
     * @param picture Uri for the profile picture
     * @param bio input by user (can be empty)
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after data was created
     */
    fun createUser(username: String, picture: Uri, bio: String, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        viewModelScope.launch {
            if (userID != null) {
                db.createUser(userID, username, picture, bio, { isError(it) }) { callBack() }
            } else {
                isError(true)
                Timber.tag("UserVM").d( "Failed to create user: ID is null")
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
                            var errorOccurred = false
                            val newUser = db.fetchUserData(userID) { if (it) errorOccurred = true }
                            _userData.value = newUser
                            isError(errorOccurred)
                            if (!errorOccurred) callBack()
                        }
                    } else {
                        isError(true)
                        Timber.tag("UserVM").d( "Failed to retrieve user data: user does not exist.")
                    }
                },
                onFailure = { e ->
                    isError(true)
                    Timber.tag("UserVM").d( "Failed to check user existence when fetching in VM with error $e")
                }
            )
        } else {
            isError(true)
            Timber.tag("UserVM").d( "Failed to fetch user data: ID is null")
        }
    }

    fun fetchSomeUsername(uid: String, isError: (Boolean) -> Unit, callBack: (String) -> Unit) {
        db.userExists(
            userID = uid,
            onSuccess = { userExists ->
                if (userExists) {
                    viewModelScope.launch {
                        var errorOccurred = false
                        val thisUser = db.fetchUserData(uid) { if (it) errorOccurred = true }
                        isError(errorOccurred)
                        if (!errorOccurred) callBack(thisUser.username)
                    }
                } else {
                    isError(true)
                    Timber.tag("UserVM").d( "Failed to retrieve user data: user does not exist.")
                }
            },
            onFailure = { e ->
                isError(true)
                Timber.tag("UserVM").d( "Failed to check user existence when fetching in VM with error $e")
            }
        )
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
     * @param removePicture whether or not needs to replace profile picture with default
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after the data was updated in the DB
     */
    fun updateUser(username: String, picture: Uri, bio: String, updatePicture: Boolean, removePicture: Boolean, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        if (userID != null) {
            db.updateUser(userID, username, picture, bio, updatePicture, removePicture, { isError(it) }) {
                fetchUserData({ isError(it) }) { callBack() }
            }
        }  else {
            isError(true)
            Timber.tag("UserVM").d( "Failed to update user: ID is null")
        }
    }

    /**
     * Checks if a username is already taken in the database.
     *
     * @param username username to check
     * @param onSuccess block that runs if the check succeeds (whether or not the username exists)
     * @param onFailure block that runs if there is an error executing the function
     */
    fun usernameAvailable(username: String, onSuccess: (Boolean) -> Unit, onFailure: (Exception) -> Unit) {
        db.usernameAvailable(username,
            onSuccess = { onSuccess(it) },
            onFailure = { onFailure(it) })
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
            db.deleteUser(userID, { isError(it) }) { callBack() }
        } else {
            isError(true)
            Timber.tag("UserVM").d( "Failed to delete user: ID is null")
        }
    }


    // all users
    /**
     * Fetches all users' data except for the one calling this function.
     *
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after all data was retrieved
     */
    fun fetchAllUsers(isError: (Boolean) -> Unit, callBack: () -> Unit) {
        if (userID != null) {
            // only fetches data if user exists
            db.userExists(
                userID = userID,
                onSuccess = { userExists ->
                    if (userExists) {
                        viewModelScope.launch {
                            var errorOccurred = false
                            val allUsers = db.fetchAllUsers(userID) { if (it) errorOccurred = true }
                            _allUsers.value = allUsers
                            isError(errorOccurred)
                            if (!errorOccurred) callBack()
                        }
                    } else {
                        isError(true)
                        Timber.tag("UserVM").d( "Failed to retrieve all users: user does not exist.")
                    }
                },
                onFailure = { e ->
                    isError(true)
                    Timber.tag("UserVM").d( "Failed to check user existence when fetching in VM with error $e")
                }
            )
        } else {
            isError(true)
            Timber.tag("UserVM").d( "Failed to fetch all users data: ID is null")
        }
    }


    // recipes and filters
    /**
     * Fetches all recipes' data.
     *
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after all data was retrieved
     */
    fun fetchAllRecipes(isError: (Boolean) -> Unit, callBack: () -> Unit) {
        viewModelScope.launch {
            var errorOccurred = false
            val allRecipes = db.fetchAllRecipes { if (it) errorOccurred = true }
            _allRecipes.value = allRecipes
            isError(errorOccurred)
            if (!errorOccurred) callBack()
        }
    }

    /**
     * Updates the ViewModel's filters with new ones.
     *
     * @param newFilters RecipeFilters object for new filters
     */
    fun updateFilters(newFilters: RecipeFilters) {
        _filters.value = newFilters
    }

    /**
     * Updates the filtered recipes with new ones.
     *
     * @param newFilteredRecipeFilters list of Recipe objects for new filtered recipes
     */
    fun updateFilteredRecipes(newFilteredRecipeFilters: List<Recipe>) {
        _filteredRecipes.value = newFilteredRecipeFilters
    }


    // personal information (fridge, groceries and notes)
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
                            var errorOccurred = false
                            val newUserPersonal = db.fetchUserPersonal(userID) { if (it) errorOccurred = true }
                            _userPersonal.value = newUserPersonal
                            isError(errorOccurred)
                            if (!errorOccurred) callBack()
                        }
                    } else {
                        isError(true)
                        Timber.tag("UserVM").d( "Failed to retrieve user data: user does not exist.")
                    }
                },
                onFailure = { e ->
                    isError(true)
                    Timber.tag("UserVM").d( "Failed to check user existence when fetching in VM with error $e")
                }
            )
        } else {
            isError(true)
            Timber.tag("UserVM").d( "Failed to fetch user personal: ID is null")
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
                    var errorOccurred = false
                    // for each new category, create it (also adds the potential new ingredients)
                    newCategories.forEach { (category, ingredients) ->
                        db.addCategory(userID, category, ingredients, isInFridge, {
                            if (it) {
                                errorOccurred = true
                                remaining--
                                if (remaining <= 0) {
                                    editCategoryNames(editedCategories, { isError(true) }) {
                                        isError(true)
                                    }
                                }
                                Timber.tag("UserVM").d( "Failed to update categories: error in DB")
                            }
                        }) {
                            remaining--
                            // if all categories have been added -> process the name changes
                            if (remaining <= 0) {
                                editCategoryNames(editedCategories, { if (it) errorOccurred = true }) {
                                    isError(errorOccurred)
                                    if (errorOccurred) Timber.tag("UserVM").d( "Failed to update categories: error in DB")
                                    else callBack()
                                }
                            }
                        }
                    }
                // if there are no categories to add -> process name changes
                } else {
                    editCategoryNames(editedCategories, { isError(it) }) { callBack() }
                }
            } else {
                isError(true)
                Timber.tag("UserVM").d( "Failed to update categories: userID is null")
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
                var errorOccurred = false
                removedCategories.forEach { category ->
                    db.deleteCategory(userID, category, {
                        if (it) {
                            errorOccurred = true
                            remaining--
                            if (remaining <= 0) {
                                isError(true)
                                fetchUserPersonal({}, {})
                            }
                            Timber.tag("UserVM").d( "Failed to delete categories")
                        }
                    }) {
                        remaining--
                        if (remaining <= 0) {
                            if (errorOccurred) {
                                isError(true)
                                Timber.tag("UserVM").d( "Failed to delete categories")
                                fetchUserPersonal({}, {})
                            } else {
                                fetchUserPersonal( { isError(it) } ) { callBack() }
                            }
                        }
                    }
                }
            // if there are no categories to delete -> directly callBack
            } else {
                isError(false)
                callBack()
            }
        } else {
            isError(true)
            Timber.tag("UserVM").d( "Could not delete categories: userID is null")
        }
    }

    /**
     * Checks if some user owns an ingredient and, if it does, returns its emplacement (groceries or fridge and which category).
     * This check is approximate as it compares standardized names.
     *
     * @param ingredient standardized name of the ingredient looked for
     * @param isError block that runs if there is an error executing the function
     * @param onResult block that runs if the function succeeded, returning;
     * whether or not the ingredient exists, and information about the match as a Triple (isInFridge, Category, displayName)
     */
    fun ingredientExistsWhere(ingredient: String, isError: (Boolean) -> Unit, onResult: (Boolean, List<Triple<Boolean, String, String>>) -> Unit) {
        if (userID != null) {
            // only fetches data if user exists
            db.ingredientExistsWhere(
                userID = userID,
                ingredientName = ingredient,
                onSuccess = { ingredientExists, matches ->
                    isError(false)
                    onResult(ingredientExists, matches)
                },
                onFailure = { e ->
                    isError(true)
                    Timber.tag("UserVM").d( "Failed to check ingredient existence with error $e")
                }
            )
        } else {
            isError(true)
            Timber.tag("UserVM").d( "Failed to check ingredient existence: ID is null")
        }
    }

    /**
     * Checks if some user owns an ingredient with given name in given category.
     *
     * @param category category the ingredient should be in
     * @param ingredient displayed name of the ingredient looked for
     * @param isInFridge whether or not to check for ingredient existence in the fridge or in groceries
     * @param isError block that runs if there is an error executing the function
     * @param onResult block that runs if the function succeeded, with the existence result
     */
    fun ingredientExistsInCategory(category: String, ingredient: String, isInFridge: Boolean, isError: (Boolean) -> Unit, onResult: (Boolean) -> Unit) {
        if (userID != null) {
            // only fetches data if user exists
            db.ingredientExistsInCategory(
                userID = userID,
                category = category,
                ingredientName = ingredient,
                isInFridge = isInFridge,
                onSuccess = { ingredientExists ->
                    isError(false)
                    onResult(ingredientExists)
                },
                onFailure = { e ->
                    isError(true)
                    Timber.tag("UserVM").d( "Failed to check ingredient existence with error $e")
                }
            )
        } else {
            isError(true)
            Timber.tag("UserVM").d( "Failed to check ingredient existence: ID is null")
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
                    var remainingItems = newItems.size
                    var errorOccurred = false
                    // for each category, if there are new ingredients to add ->
                    newItems.forEach { (_, ingredients) ->
                        if (ingredients.isNotEmpty()) {
                            // check how many ingredients are left to add before decreasing category counter
                            var remaining = ingredients.size
                            ingredients.forEach { ingredient ->
                                db.createIngredient(userID, ingredient, isInFridge, {
                                    if (it) {
                                        errorOccurred = true
                                        remaining--
                                        if (remaining <= 0) {
                                            remainingItems--
                                            if (remainingItems <= 0) {
                                                isError(true)
                                            }
                                        }
                                        Timber.tag("UserVM").d( "Failed to add ingredient")
                                    }
                                }) {
                                    remaining--
                                    if (remaining <= 0) {
                                        remainingItems--
                                        if (remainingItems <= 0) {
                                            if (errorOccurred) Timber.tag("UserVM").d( "Failed to add ingredient")
                                            else callBack()
                                        }
                                    }
                                }
                            }
                        // if this category does not have any new ingredients -> decrease category counter
                        } else { remainingItems--
                            if (remainingItems <= 0) {
                                isError(errorOccurred)
                                if (errorOccurred) Timber.tag("UserVM").d( "Failed to add ingredients")
                                else callBack()
                            }
                        }
                    }
                // if the map is empty -> callBack
                } else {
                    isError(false)
                    callBack()
                }
            } else {
                isError(true)
                Timber.tag("UserVM").d( "Failed to add ingredients: userID is null")
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
                fetchUserPersonal({ isError(it) }) { callBack()}
            }
        } else {
            isError(true)
            Timber.tag("UserVM").d( "Failed to update ingredient tick: userID is null")
        }
    }

    /**
     * Removes a list of ingredients.
     *
     * @param removedItems maps category names to a list of ingredients ids to be deleted
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after all ingredients were deleted
     */
    fun deleteIngredients(removedItems: Map<String, List<String>>, isInFridge: Boolean, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        if (userID != null) {
            // if the map is not empty
            if (removedItems.isNotEmpty()) {
                var remainingItems = removedItems.size
                var errorOccurred = false
                removedItems.forEach { (category, ingredients) ->
                    // if this category has ingredients to delete ->
                    if (ingredients.isNotEmpty()) {
                        var remaining = ingredients.size
                        ingredients.forEach { ingredient ->
                            db.deleteIngredient(ingredient, userID, category, isInFridge, {
                                if (it) {
                                    errorOccurred = true
                                    remaining--
                                    if (remaining <= 0) {
                                        remainingItems--
                                        if (remainingItems <= 0) {
                                            isError(true)
                                        }
                                    }
                                    Timber.tag("UserVM").d( "Failed to delete ingredient")
                                }
                            }){
                                remaining--
                                if (remaining <= 0) {
                                    remainingItems--
                                    if (remainingItems <= 0) {
                                        isError(errorOccurred)
                                        if (errorOccurred) Timber.tag("UserVM").d( "Failed to delete ingredient")
                                        else callBack()
                                    }
                                }
                            }
                        }
                    // if this category does not have any ingredient to delete -> decrease category counter
                    } else {
                        remainingItems--
                        if (remainingItems <= 0) {
                            isError(errorOccurred)
                            if (errorOccurred) Timber.tag("UserVM").d( "Failed to delete ingredient")
                            else callBack()
                        }
                    }
                }
            // if the map is empty -> callBack
            } else {
                isError(false)
                callBack()
            }
        } else {
            isError(true)
            Timber.tag("UserVM").d( "Failed to delete ingredient: userID is null")
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
                var errorOccurred = false
                editedCategories.forEach { (old, new) ->
                    db.updateCategory(userID, old, new, {
                        if (it) {
                            errorOccurred = true
                            remaining--
                            if (remaining <= 0) {
                                isError(true)
                            }
                            Timber.tag("UserVM").d( "Failed to edit category names")
                        }
                    })
                    {
                        remaining--
                        if (remaining <= 0) {
                            isError(errorOccurred)
                            if (errorOccurred) Timber.tag("UserVM").d( "Failed to edit category names")
                            else callBack()
                        }
                    }
                }
            // if the map is empty -> callBack
            } else {
                isError(false)
                callBack()
            }
        } else {
            isError(true)
            Timber.tag("UserVM").d( "Failed to edit category name: userID is null")
        }
    }

    /**
     * Clears all ingredients from either the fridge or the groceries. The category names are retained.
     *
     * @param isInFridge whether the fridge list should be cleared. If false, the groceries list is cleared instead.
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after the list was cleared
     */
    fun clearIngredients(isInFridge: Boolean, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        if (userID != null) {
            db.clearIngredients(userID, isInFridge, { isError(it) }) { callBack() }
        } else {
            isError(true)
            Timber.tag("UserVM").d( "Failed to clear all ingredients: userID is null")
        }
    }

    /**
     * Sends ticked items from the grocery list to the fridge and removes them from the groceries list.
     *
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after the items were transferred
     */
    fun groceriesToFridge(isError: (Boolean) -> Unit, callBack: () -> Unit) {
        if (userID != null) {
            db.groceriesToFridge(userID, { isError(it) }) { callBack() }
        } else {
            isError(true)
            Timber.tag("UserVM").d( "Failed to transfer items to fridge: userID is null")
        }
    }

    /**
     * Filters a list of ingredients to only keep those for which a user owns all the ingredients.
     *
     * @param allRecipes mutable list of Recipe objects to be filtered
     * @param isError block that runs if there is an error executing the function
     * @param onResult block that runs with the new list of recipes after they were filtered
     */
    fun recipesWithOwnedIngredients(allRecipes: MutableList<Recipe>, isError: (Boolean) -> Unit, onResult: (List<Recipe>) -> Unit) {
        if (userID != null) {
            // only fetches data if user exists
            db.recipesWithOwnedIngredients(userID, allRecipes,
                onSuccess = { recipes ->
                    isError(false)
                    onResult(recipes)
                },
                onFailure = { e ->
                    isError(true)
                    Timber.tag("UserVM").d( "Failed to check ingredient existence with error $e")
                }
            )
        } else {
            isError(true)
            Timber.tag("UserVM").d( "Failed to check ingredient existence: userID is null")
        }
    }

    /**
     * Updates the notes for a given recipe.
     *
     * @param recipeID ID of the recipe that the note is attached to
     * @param note written by the user
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after the DB was updated
     */
    fun updateNotes(recipeID: String, note: String, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        if (userID != null) {
            db.updateNotes(userID, recipeID, note, { isError(it) }) {
                fetchUserPersonal({ isError(it) }) { callBack()}
            }
        } else {
            isError(true)
            Timber.tag("UserVM").d( "Failed to update notes: userID is null")
        }
    }

    /**
     * Deletes the note for a given recipe.
     *
     * @param recipeID ID of the recipe that the note was attached to
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after the DB was updated
     */
    fun deleteNote(recipeID: String, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        if (userID != null) {
            db.deleteNote(userID, recipeID, { isError(it) }) {
                fetchUserPersonal({ isError(it) }) { callBack()}
            }
        } else {
            isError(true)
            Timber.tag("UserVM").d( "Failed to delete note: userID is null")
        }
    }
}
