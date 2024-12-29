package com.example.foodiebuddy.ui.recipes

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.foodiebuddy.R
import com.example.foodiebuddy.data.Diet
import com.example.foodiebuddy.data.Origin
import com.example.foodiebuddy.data.RecipeDraft
import com.example.foodiebuddy.data.RecipeIngredient
import com.example.foodiebuddy.data.Tag
import com.example.foodiebuddy.data.getString
import com.example.foodiebuddy.errors.handleError
import com.example.foodiebuddy.navigation.NavigationActions
import com.example.foodiebuddy.navigation.Route
import com.example.foodiebuddy.ui.DialogWindow
import com.example.foodiebuddy.viewModels.OfflineDataViewModel
import com.example.foodiebuddy.viewModels.RecipeViewModel
import com.example.foodiebuddy.viewModels.UserViewModel
import java.util.UUID

@Composable
fun RecipeCreate(userVM: UserViewModel, recipeVM: RecipeViewModel, offDataVM: OfflineDataViewModel, navigationActions: NavigationActions) {
    val context = LocalContext.current
    val editingPicture = remember { mutableStateOf(false) }
    val showPictureOptions = remember { mutableStateOf(false) }
    val showAlert = remember { mutableStateOf(false) }

    val userID = userVM.getCurrentUserID()

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
            })
        BackHandler {
            editingPicture.value = false
            pictureState.value = currentPicture.value
        }
    } else {
        EditRecipe(
            context = context,
            onGoBack = { showAlert.value = true },
            title = stringResource(R.string.title_createRecipe),
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
            onEditPicture = { editingPicture.value = true },
            onRemovePicture = {
                pictureState.value = Uri.EMPTY
                currentPicture.value = Uri.EMPTY
            },
            onDraftSave = {
                val draftId = UUID.randomUUID().toString()
                val newDraft = RecipeDraft(
                    id = draftId,
                    name = nameState.value,
                    picture = if (pictureState == Uri.EMPTY) "" else pictureState.value.toString(),
                    instructions = instructionsState.toList(),
                    ingredients = ingredientsState.map { ingredient ->
                        mapOf(
                            "displayedName" to ingredient.displayedName,
                            "standName" to ingredient.standName,
                            "quantity" to ingredient.quantity.toString(),
                            "unit" to ingredient.unit.getString(context),
                            "id" to ingredient.id
                        )
                    },
                    portion = portionState.intValue,
                    perPerson = perPersonState.value,
                    origin = originState.value,
                    diet = dietState.value,
                    tags = tagsState.toList()
                )
                offDataVM.saveDraft(newDraft)
                Toast.makeText(context, context.getString(R.string.toast_savedDraft), Toast.LENGTH_SHORT).show()
                navigationActions.navigateTo(Route.RECIPES_HOME)

            },
            onSave = {
                recipeVM.createRecipe(
                    userID, nameState.value, pictureState.value,
                    instructionsState, ingredientsState,
                    portionState.intValue, perPersonState.value,
                    originState.value, dietState.value, tagsState,
                    { if (it) handleError(context, "Could not create recipe") }
                ) {
                    navigationActions.navigateTo("${Route.RECIPE}/${it}")
                }
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