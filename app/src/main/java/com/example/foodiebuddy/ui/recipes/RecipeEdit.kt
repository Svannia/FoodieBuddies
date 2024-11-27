package com.example.foodiebuddy.ui.recipes

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.example.foodiebuddy.navigation.NavigationActions
import com.example.foodiebuddy.viewModels.RecipeViewModel
import com.example.foodiebuddy.viewModels.UserViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.foodiebuddy.R
import com.example.foodiebuddy.data.Diet
import com.example.foodiebuddy.data.Origin
import com.example.foodiebuddy.data.Recipe
import com.example.foodiebuddy.data.RecipeIngredient
import com.example.foodiebuddy.data.Tag
import com.example.foodiebuddy.errors.handleError
import com.example.foodiebuddy.navigation.Route
import com.example.foodiebuddy.ui.DialogWindow
import com.example.foodiebuddy.ui.LoadingPage

@Composable
fun RecipeEdit(userVM: UserViewModel, recipeVM: RecipeViewModel, navigationActions: NavigationActions) {
    val context = LocalContext.current
    val editingPicture = remember { mutableStateOf(false) }
    val loadingData = remember { mutableStateOf(false) }
    val dataEdited = remember { mutableStateOf(false) }
    val pictureEdited = remember { mutableStateOf(false) }
    val showAlert = remember { mutableStateOf(false) }

    val recipeID = recipeVM.getVmUid()
    val recipeData by recipeVM.recipeData.collectAsState()

    val nameState = remember { mutableStateOf("") }
    val currentPicture = remember { mutableStateOf(Uri.EMPTY) }
    val pictureState = remember { mutableStateOf(Uri.EMPTY) }
    val recipeState = remember { mutableStateOf("") }
    val ingredientsState = remember { mutableStateListOf<RecipeIngredient>() }
    val originState = remember { mutableStateOf(Origin.NONE) }
    val dietState = remember { mutableStateOf(Diet.NONE) }
    val tagsState = remember { mutableStateListOf<Tag>() }

    LaunchedEffect(Unit) {
        if (recipeID.isNotEmpty()) {
            loadingData.value = true
            recipeVM.fetchRecipeData({
                if (it) {
                    handleError(context, "Could not fetch recipe data")
                    loadingData.value = false
                }
            }) {
                if (recipeData != Recipe.empty()) {
                    nameState.value = recipeData.name
                    currentPicture.value = recipeData.picture
                    pictureState.value = recipeData.picture
                    recipeState.value = recipeData.recipe
                    ingredientsState.clear()
                    ingredientsState.addAll(recipeData.ingredients)
                    originState.value = recipeData.origin
                    dietState.value = recipeData.diet
                    tagsState.clear()
                    tagsState.addAll(recipeData.tags)
                }
                loadingData.value = false
            }
        }
    }
    LaunchedEffect(recipeData) {
        if (recipeData != Recipe.empty()) {
            nameState.value = recipeData.name
            currentPicture.value = recipeData.picture
            pictureState.value = recipeData.picture
            recipeState.value = recipeData.recipe
            ingredientsState.clear()
            ingredientsState.addAll(recipeData.ingredients)
            originState.value = recipeData.origin
            dietState.value = recipeData.diet
            tagsState.clear()
            tagsState.addAll(recipeData.tags)
        }
    }

    if (loadingData.value) LoadingPage()
    else {
        if (editingPicture.value) {
            SetRecipePicture()
            BackHandler {
                editingPicture.value = false
                pictureState.value = currentPicture.value
            }
        } else {
            EditRecipe()
            BackHandler {
                showAlert.value = true
            }
            if (showAlert.value) {
                DialogWindow(
                    visible = showAlert,
                    content = stringResource(R.string.alert_exitRecipe),
                    confirmText = stringResource(R.string.button_quit),
                    confirmColour = Color.Red
                ) {
                    navigationActions.navigateTo(Route.RECIPES_HOME, true)
                }
            }
        }
    }
}

@Composable
private fun EditRecipe() {

}

@Composable
private fun SetRecipePicture() {

}
