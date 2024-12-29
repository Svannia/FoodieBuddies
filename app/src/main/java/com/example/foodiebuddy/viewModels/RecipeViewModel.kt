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

    /**
    * Creates a new Recipe document.
    *
    * @property userID UID of the user who created the recipe
    * @property name title of the recipe
    * @property picture picture of the recipe (empty URI if there is no picture)
    * @property instructions list of strings where each element represents a step of the cooking instructions
    * @property ingredients a list of RecipeIngredient objects representing the ingredients for the recipe
    * @property portion number that indicates for how many servings this recipe is designed
    * @property perPerson if true, the portion is per person, if false it is per piece
    * @property origin origin tag from Origin enum
    * @property diet diet tag from Diet enum
    * @property tags list of tags from Tag enum
    * @param isError block that runs if there is an error executing the function
    * @param callBack block that runs after DB was updated, returning the recipe's ID
    */
    fun createRecipe(
        userID: String,
        name: String,
        picture: Uri,
        instructions: List<String>,
        ingredients: List<RecipeIngredient>,
        portion: Int,
        perPerson: Boolean,
        origin: Origin,
        diet: Diet,
        tags: List<Tag>,
        isError: (Boolean) -> Unit,
        callBack: (String) -> Unit
    ) {
        // remove any empty instruction step
        instructions.filter { instruction -> instruction.isNotBlank() }
        ingredients.forEach { ingredient ->
            // ensure there is no unnamed ingredient
            if (ingredient.displayedName.isBlank()) ingredients.toMutableList().remove(ingredient)
            // remove any trailing white space at the end of ingredient names
            ingredient.displayedName = ingredient.displayedName.trimEnd()
            // add the standardized name of each ingredient
            ingredient.standName = standardizeName(ingredient.displayedName)
        }
        db.createRecipe(userID, name, picture, instructions, ingredients, portion, perPerson, origin, diet, tags, { isError(it) }) {
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

    /**
     * Adds a user's favourite by adding their reference to the recipe.
     *
     * @param userID of the user adding the favourite
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after the DB was updated
     */
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

    /**
     * Removes a user's favourite by removing their reference from the recipe.
     *
     * @param userID of the user removing the favourite
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after the DB was updated
     */
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