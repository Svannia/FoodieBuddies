package com.example.foodiebuddy.ui.recipes

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.example.foodiebuddy.navigation.NavigationActions
import com.example.foodiebuddy.viewModels.RecipeViewModel
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
fun RecipeEdit(recipeVM: RecipeViewModel, navigationActions: NavigationActions) {
    val context = LocalContext.current
    val editingPicture = remember { mutableStateOf(false) }
    val loadingData = remember { mutableStateOf(false) }
    val dataEdited = remember { mutableStateOf(false) }
    val pictureEdited = remember { mutableStateOf(false) }
    val pictureRemoved = remember { mutableStateOf(false) }
    val showPictureOptions = remember { mutableStateOf(false) }
    val showAlert = remember { mutableStateOf(false) }
    val showDeleteAlert = remember { mutableStateOf(false) }

    val recipeData by recipeVM.recipeData.collectAsState()

    val nameState = remember { mutableStateOf("") }
    val currentPicture = remember { mutableStateOf(Uri.EMPTY) }
    val pictureState = remember { mutableStateOf(Uri.EMPTY) }
    val instructionsState = remember { mutableStateListOf("") }
    val ingredientsState = remember { mutableStateListOf<RecipeIngredient>() }
    val portionState = remember { mutableIntStateOf(1) }
    val perPersonState = remember { mutableStateOf(true) }
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
                portionState.intValue = recipeData.portion
                perPersonState.value = recipeData.perPerson
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
            portionState.intValue = recipeData.portion
            perPersonState.value = recipeData.perPerson
            originState.value = recipeData.origin
            dietState.value = recipeData.diet
            tagsState.clear()
            tagsState.addAll(recipeData.tags)
        }
    }

    if (loadingData.value) LoadingPage()
    else {
        if (editingPicture.value) {
            // screen to edit a picture for the recipe
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
                onGoBack = {
                    if (dataEdited.value) showAlert.value = true
                    else navigationActions.goBack()
                },
                title = stringResource(R.string.title_editRecipe),
                name = nameState,
                picture = pictureState,
                instructions = instructionsState,
                ingredients = ingredientsState,
                portion = portionState,
                perPerson = perPersonState,
                origin = originState,
                diet = dietState,
                tags = tagsState,
                showPictureOptions = showPictureOptions,
                dataEdited = dataEdited,
                editingExistingRecipe = true,
                onEditPicture = { editingPicture.value = true },
                onRemovePicture = {
                    pictureState.value = Uri.EMPTY
                    currentPicture.value = Uri.EMPTY
                    pictureEdited.value = false
                    pictureRemoved.value = true
                },
                // no draft option when editing an existing recipe
                onDraftSave = {},
                onSave = {
                    loadingData.value = true
                    recipeVM.updateRecipe(
                        nameState.value,
                        pictureState.value,
                        pictureEdited.value,
                        pictureRemoved.value,
                        instructionsState,
                        ingredientsState,
                        portionState.intValue,
                        perPersonState.value,
                        originState.value,
                        dietState.value,
                        tagsState,
                        { if (it) {
                            handleError(context, "Could not update recipe")
                            loadingData.value = false
                        } }
                    ) {
                        navigationActions.goBack()
                        loadingData.value = false
                    }
                },
                onDelete = { showDeleteAlert.value = true }
            )
            BackHandler {
                if (dataEdited.value) showAlert.value = true
                else navigationActions.goBack()

            }
            // alert for deleting the recipe

            if (showDeleteAlert.value) {
                DialogWindow(
                    visible = showDeleteAlert,
                    content = stringResource(R.string.alert_deleteRecipe),
                    confirmText = stringResource(R.string.button_delete),
                    confirmColour = Color.Red,
                    onConfirm = {
                        showDeleteAlert.value = false
                        loadingData.value = true
                        recipeVM.deleteRecipe(recipeData.owner, {
                            if (it) {
                                handleError(context, "Could not delete recipe")
                                loadingData.value = false
                            }
                        }) {
                            loadingData.value = false
                            navigationActions.navigateTo(Route.RECIPES_HOME)
                        }
                    })
            }
            // alert for leaving the recipe edition page
            if (showAlert.value) {
                DialogWindow(
                    visible = showAlert,
                    content = stringResource(R.string.alert_exitRecipeEdit),
                    confirmText = stringResource(R.string.button_quit),
                    confirmColour = Color.Red
                ) {
                    showAlert.value = false
                    navigationActions.goBack()
                }
            }
        }
    }
}

