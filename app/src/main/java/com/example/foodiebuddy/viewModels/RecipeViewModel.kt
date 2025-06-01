package com.example.foodiebuddy.viewModels

import android.net.Uri
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
import timber.log.Timber
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
    * @param userID UID of the user who created the recipe
    * @param name title of the recipe
    * @param pictures list of pictures URI (or empty list for no pictures)
    * @param instructions list of strings where each element represents a step of the cooking instructions
    * @param ingredients maps section names to lists of RecipeIngredient objects
    * @param sectionsOrder list of section names in the order they should be displayed
    * @param portion number that indicates for how many servings this recipe is designed
    * @param perPerson if true, the portion is per person, if false it is per piece
    * @param origin origin tag from Origin enum
    * @param diet diet tag from Diet enum
    * @param tags list of tags from Tag enum
    * @param isError block that runs if there is an error executing the function
    * @param callBack block that runs after DB was updated, returning the recipe's ID
    */
    fun createRecipe(
        userID: String,
        name: String,
        pictures: List<Uri>,
        instructions: List<String>,
        ingredients: Map<String, List<RecipeIngredient>>,
        sectionsOrder: List<String>,
        portion: Int,
        perPerson: Boolean,
        origin: Origin,
        diet: Diet,
        tags: List<Tag>,
        isError: (Boolean) -> Unit,
        callBack: (String) -> Unit
    ) {
        val filteredIngredients = ingredients.filterValues { it.isNotEmpty() }
        val filteredOrder = sectionsOrder.filter { it in filteredIngredients.keys }
        val filteredInstructions = instructions.toMutableList()
        processListData(filteredIngredients.values.flatten(), filteredInstructions)
        db.createRecipe(userID, name, pictures, filteredInstructions, filteredIngredients, filteredOrder, portion, perPerson, origin, diet, tags, { isError(it) }) {
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
                        Timber.tag("RecipeVM").d( "Failed to retrieve recipe data: recipe does not exist.")
                    }
                },
                onFailure = { e ->
                    isError(true)
                    Timber.tag("RecipeVM").d( "Failed to check recipe existence when fetching in VM with error $e")
                }
            )
        } else {
            isError(true)
            Timber.tag("RecipeVM").d( "Failed to fetch recipe data: ID is null")
        }
    }

    /**
     * Updates an existing Recipe document.
     *
     * @param name title of the recipe
     * @param picturesToRemove list of picture URLs (from Firestore Storage) that need to be removed (empty list if no pictures to remove)
     * @param pictures pictures of the recipe (empty list if there is no picture)
     * @param updatePicture whether or not the Storage picture should be updated
     * @param instructions list of strings where each element represents a step of the cooking instructions
     * @param ingredients maps section names to lists of RecipeIngredient objects
     * @param sectionsOrder list of section names in the order they should be displayed
     * @param portion number that indicates for how many servings this recipe is designed
     * @param perPerson if true, the portion is per person, if false it is per piece
     * @param origin origin tag from Origin enum
     * @param diet diet tag from Diet enum
     * @param tags list of tags from Tag enum
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after DB was updated, returning the recipe's ID
     */
    fun updateRecipe(
        name: String,
        picturesToRemove: List<Uri>,
        pictures: List<Uri>,
        updatePicture: Boolean,
        instructions: List<String>,
        ingredients: Map<String, List<RecipeIngredient>>,
        sectionsOrder: List<String>,
        portion: Int,
        perPerson: Boolean,
        origin: Origin,
        diet: Diet,
        tags: List<Tag>,
        isError: (Boolean) -> Unit,
        callBack: () -> Unit
    ) {
        if (recipeID != null) {
            val filteredIngredients = ingredients.filterValues { it.isNotEmpty() }
            val filteredOrder = sectionsOrder.filter { it in filteredIngredients.keys }
            val filteredInstructions = instructions.toMutableList()
            processListData(filteredIngredients.values.flatten(), filteredInstructions)
            db.updateRecipe(recipeData.value.owner, recipeID, name, picturesToRemove, pictures, updatePicture, filteredInstructions, filteredIngredients, filteredOrder, portion, perPerson, origin, diet, tags, { isError(it) })
            {
                fetchRecipeData({ isError(it) }) { callBack() }
            }
        } else {
            isError(true)
            Timber.tag("RecipeVM").d( "Could not update recipe: recipeID is null")
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
        if (recipeID != null)   {
            db.addUserToFavourites(recipeID, userID, { isError(it) }) {
                fetchRecipeData({ isError(it) }) { callBack() }
            }
        } else {
            isError(true)
            Timber.tag("RecipeVM").d( "Could not add user to favourites: recipeID is null")
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
            Timber.tag("RecipeVM").d( "Could not remove user from favourites: recipeID is null")
        }
    }

    /**
     * Deletes this Recipe document.
     *
     * @param userID ID of the recipe's owner
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after the DB was updated
     */
    fun deleteRecipe(userID: String, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        if (recipeID != null) {
            db.deleteRecipe(userID, recipeID, { isError(it) }) { callBack() }
        } else {
            isError(true)
            Timber.tag("RecipeVM").d( "Failed to delete recipe: ID is null")
        }
    }

    /**
     * Processes the ingredients and instructions lists to make sure they don't contain empty strings or only whitespaces.
     *
     * @param ingredients list of RecipeIngredient objects
     * @param instructions list of strings
     */
    private fun processListData(ingredients: List<RecipeIngredient>, instructions: MutableList<String>) {
        // remove any empty instruction step
        instructions.removeAll { it.isBlank() }
        ingredients.forEach { ingredient ->
            // ensure there is no unnamed ingredient
            if (ingredient.displayedName.isBlank() || ingredient.displayedName.all { it == ' ' }) ingredients.toMutableList().remove(ingredient)
            // remove any trailing white space at the end of ingredient names
            ingredient.displayedName = ingredient.displayedName.trimEnd()
            // add the standardized name of each ingredient
            ingredient.standName = standardizeName(ingredient.displayedName)
        }
    }

}