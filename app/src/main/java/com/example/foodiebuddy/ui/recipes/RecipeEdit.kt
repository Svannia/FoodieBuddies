package com.example.foodiebuddy.ui.recipes

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
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
import com.example.foodiebuddy.system.imagePermissionVersion
import com.example.foodiebuddy.ui.DialogWindow
import com.example.foodiebuddy.ui.LoadingPage
import com.example.foodiebuddy.ui.OptionsMenu
import com.example.foodiebuddy.ui.SecondaryScreen

@Composable
fun RecipeEdit(userVM: UserViewModel, recipeVM: RecipeViewModel, navigationActions: NavigationActions) {
    val context = LocalContext.current
    val editingPicture = remember { mutableStateOf(false) }
    val loadingData = remember { mutableStateOf(false) }
    val dataEdited = remember { mutableStateOf(false) }
    val pictureEdited = remember { mutableStateOf(false) }
    val pictureRemoved = remember { mutableStateOf(false) }
    val showPictureOptions = remember { mutableStateOf(false) }
    val showAlert = remember { mutableStateOf(false) }

    val recipeData by recipeVM.recipeData.collectAsState()

    val nameState = remember { mutableStateOf("") }
    val currentPicture = remember { mutableStateOf(Uri.EMPTY) }
    val pictureState = remember { mutableStateOf(Uri.EMPTY) }
    val instructionsState = remember { mutableStateListOf("") }
    val ingredientsState = remember { mutableStateListOf<RecipeIngredient>() }
    val originState = remember { mutableStateOf(Origin.NONE) }
    val dietState = remember { mutableStateOf(Diet.NONE) }
    val tagsState = remember { mutableStateListOf<Tag>() }

    LaunchedEffect(Unit) {
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
                instructionsState.clear()
                instructionsState.addAll(recipeData.instructions)
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
    LaunchedEffect(recipeData) {
        if (recipeData != Recipe.empty()) {
            nameState.value = recipeData.name
            currentPicture.value = recipeData.picture
            pictureState.value = recipeData.picture
            instructionsState.clear()
            instructionsState.addAll(recipeData.instructions)
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
            SetRecipePicture(
                picture = pictureState.value,
                onCancel = {
                    editingPicture.value = false
                    pictureState.value = currentPicture.value
                },
                onSave = { uri ->
                    pictureState.value = uri
                    currentPicture.value = uri
                    editingPicture.value = false
                    dataEdited.value = true
                    pictureEdited.value = true
                })
            BackHandler {
                editingPicture.value = false
                pictureState.value = currentPicture.value
            }
        } else {
            EditRecipe(
                context = context,
                onGoBack = { showAlert.value = true },
                title = stringResource(R.string.title_editRecipe),
                name = nameState,
                picture = pictureState,
                instructions = instructionsState,
                ingredients = ingredientsState,
                origin = originState,
                diet = dietState,
                tags = tagsState,
                showPictureOptions = showPictureOptions,
                onEditPicture = { editingPicture.value = true },
                onRemovePicture = {
                    pictureState.value = Uri.EMPTY
                    currentPicture.value = Uri.EMPTY
                    pictureEdited.value = false
                    pictureRemoved.value = true },
                onDraftSave = {},
                onSave = {
                    // todo
                }
            )
            BackHandler {
                showAlert.value = true
            }
            // alert for leaving the recipe edition page
            if (showAlert.value) {
                DialogWindow(
                    visible = showAlert,
                    content = stringResource(R.string.alert_exitRecipe),
                    confirmText = stringResource(R.string.button_quit),
                    confirmColour = Color.Red
                ) {
                    showAlert.value = false
                    navigationActions.navigateTo(Route.RECIPES_HOME)
                }
            }
        }
    }
}

