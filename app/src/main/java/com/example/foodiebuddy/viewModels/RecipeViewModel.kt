package com.example.foodiebuddy.viewModels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodiebuddy.data.Diet
import com.example.foodiebuddy.data.Origin
import com.example.foodiebuddy.data.Recipe
import com.example.foodiebuddy.data.RecipeIngredient
import com.example.foodiebuddy.data.Tag
import com.example.foodiebuddy.database.DatabaseConnection
import com.example.foodiebuddy.ui.ingredients.standardizeName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.lang.Thread.State
import javax.inject.Inject

/**
 * ViewModel for viewing and filtering all recipes in the Database.
 *
 * @property recipeID of the recipe (can be null if the recipe has not been created yet)
 */
class RecipeViewModel
@Inject
constructor(private val recipeID: String ?= null) : ViewModel() {
    private val db = DatabaseConnection()
    private val _recipeData = MutableStateFlow(Recipe.empty())
    val recipeData: StateFlow<Recipe> = _recipeData

    /**
     * Fetches this RecipeViewModel's UID.
     *
     * @return the RecipeViewModel's UID if it is non-null, an empty string otherwise
     */
    fun getVmUid(): String { return recipeID ?: "" }

    fun createRecipe(
        userID: String,
        owner: String,
        name: String,
        picture: Uri,
        instructions: List<String>,
        ingredients: List<RecipeIngredient>,
        origin: Origin,
        diet: Diet,
        tags: List<Tag>,
        isError: (Boolean) -> Unit,
        callBack: (String) -> Unit
    ) {
        instructions.filter { instruction -> instruction.isNotBlank() }
        ingredients.forEach { ingredient ->
            if (ingredient.displayedName.isBlank()) ingredients.toMutableList().remove(ingredient)
            ingredient.displayedName = ingredient.displayedName.trimEnd()
            ingredient.standName = standardizeName(ingredient.displayedName)
        }
        db.createRecipe(userID, owner, name, picture, instructions, ingredients, origin, diet, tags, { isError(it) }) {
            callBack(it)
        }
    }

    /**
     * Fetches all recipe data.
     *
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after all data was retrieved
     */
    fun fetchRecipeData(isError: (Boolean) -> Unit, callBack: () -> Unit) {
        if (recipeID != null) {
            // only fetches data if recipe exists
            db.recipeExists(
                recipeID = recipeID,
                onSuccess = { recipeExists ->
                    if (recipeExists) {
                        viewModelScope.launch {
                            var errorOccurred = false
                            val newRecipe = db.fetchRecipeData(recipeID) { if (it) errorOccurred = true }
                            _recipeData.value = newRecipe
                            isError(errorOccurred)
                            if (!errorOccurred) {
                                callBack()
                            }
                        }
                    } else {
                        isError(true)
                        Log.d("RecipeVM", "Failed to retrieve recipe data: recipe does not exist.")
                    }
                },
                onFailure = { e ->
                    isError(true)
                    Log.d("RecipeVM", "Failed to check recipe existence when fetching in VM with error $e")
                }
            )
        } else {
            isError(true)
            Log.d("RecipeVM", "Failed to fetch recipe data: ID is null")
        }
    }

    fun addUserToFavourites(userID: String, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        if (recipeID != null) {
            db.addUserToFavourites(recipeID, userID, { isError(it) }) {
                fetchRecipeData({ isError(it) }) { callBack() }
            }
        } else {
            isError(true)
            Log.d("RecipeVM", "Could not add user to favourites: recipeID is null")
        }
    }

    fun removeUserFromFavourites(userID: String, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        if (recipeID != null) {
            db.removeUserFromFavourites(recipeID, userID, { isError(it) }) {
                fetchRecipeData({ isError(it) }) { callBack() }
            }
        } else {
            isError(true)
            Log.d("RecipeVM", "Could not remove user from favourites: recipeID is null")
        }
    }

}