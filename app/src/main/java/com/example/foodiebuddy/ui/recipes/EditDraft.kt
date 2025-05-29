package com.example.foodiebuddy.ui.recipes

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import com.example.foodiebuddy.R
import com.example.foodiebuddy.data.Diet
import com.example.foodiebuddy.data.Measure
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

@Composable
fun EditDraft(draftID: String, userVM: UserViewModel, recipeVM: RecipeViewModel, offDataVM: OfflineDataViewModel, navigationActions: NavigationActions) {
    val context = LocalContext.current
    val editingPicture = remember { mutableStateOf(false) }
    val dataEdited = remember { mutableStateOf(false) }
    val showAlert = remember { mutableStateOf(false) }

    val userID = userVM.getCurrentUserID()

    val drafts by offDataVM.drafts.collectAsState()
    val draft = remember { mutableStateOf(RecipeDraft.empty())
    }

    val nameState = remember { mutableStateOf("") }
    val currentPictures = remember { mutableStateListOf<Uri>() }
    val picturesState = remember { mutableStateListOf<Uri>() }
    val instructionsState = remember { mutableStateListOf("") }
    val ingredientsState = remember { mutableStateMapOf<String, List<RecipeIngredient>>() }
    val portionState = remember { mutableIntStateOf(1) }
    val perPersonState = remember { mutableStateOf(true) }
    val originState = remember { mutableStateOf(Origin.NONE) }
    val dietState = remember { mutableStateOf(Diet.NONE) }
    val tagsState = remember { mutableStateListOf<Tag>() }

    LaunchedEffect(Unit) {
        draft.value = drafts.find { it.id == draftID } ?: RecipeDraft.empty()
        nameState.value = draft.value.name
        val pictures = if (draft.value.pictures.isEmpty()) emptyList() else draft.value.pictures.map { it.toUri() }
        currentPictures.clear()
        currentPictures.addAll(pictures)
        picturesState.clear()
        picturesState.addAll(pictures)
        instructionsState.clear()
        instructionsState.addAll(draft.value.instructions)
        ingredientsState.clear()
        ingredientsState.putAll(draft.value.ingredients.mapValues { (_, ingredientMaps) ->
            ingredientMaps.map { map ->
                RecipeIngredient(
                    displayedName = map["displayedName"] ?: "",
                    standName = "",
                    quantity = map["quantity"]?.toFloat() ?: 0f,
                    unit = map["unit"]?.let { Measure.valueOf(it) } ?: Measure.NONE,
                )
            }
        })
        portionState.intValue = draft.value.portion
        perPersonState.value = draft.value.perPerson
        originState.value = draft.value.origin
        dietState.value = draft.value.diet
        tagsState.clear()
        tagsState.addAll(draft.value.tags)
    }

    if (editingPicture.value) {
        SetRecipePicture(
            picture = picturesState[picturesState.size-1],
            onCancel = {
                editingPicture.value = false
                picturesState.clear()
                picturesState.addAll(currentPictures)
            },
            onSave = { uri ->
                picturesState[picturesState.size-1] = uri
                currentPictures.clear()
                currentPictures.addAll(picturesState)
                editingPicture.value = false
                dataEdited.value = true
            })
        BackHandler {
            editingPicture.value = false
            picturesState.clear()
            picturesState.addAll(currentPictures)
        }
    } else {
        EditRecipe(
            context = context,
            onGoBack = {
                if (dataEdited.value) showAlert.value = true
                else navigationActions.navigateTo(Route.DRAFTS)
            },
            title = stringResource(R.string.title_editDraft),
            name = nameState,
            pictures = picturesState,
            instructions = instructionsState,
            ingredients = ingredientsState,
            portion = portionState,
            perPerson = perPersonState,
            origin = originState,
            diet = dietState,
            tags = tagsState,
            dataEdited = dataEdited,
            onEditPicture = { editingPicture.value = true },
            onRemovePicture = { index ->
                picturesState.removeAt(index)
                currentPictures.removeAt(index)
                dataEdited.value = true
            },
            onDraftSave = {
                val newDraft = RecipeDraft(
                    id = draftID,
                    name = nameState.value,
                    pictures = if (picturesState.isEmpty()) emptyList() else picturesState.map { it.toString() },
                    instructions = instructionsState.toList(),
                    ingredients = ingredientsState.mapValues { (_, ingredientMaps) ->
                        ingredientMaps.map { ingredient ->
                            mapOf(
                                "displayedName" to ingredient.displayedName,
                                "standName" to ingredient.standName,
                                "quantity" to ingredient.quantity.toString(),
                                "unit" to ingredient.unit.getString(context),
                                "id" to ingredient.id
                            )
                        }
                    },
                    portion = portionState.intValue,
                    perPerson = perPersonState.value,
                    origin = originState.value,
                    diet = dietState.value,
                    tags = tagsState.toList()
                )
                offDataVM.saveDraft(newDraft)
                Toast.makeText(context, context.getString(R.string.toast_savedDraft), Toast.LENGTH_SHORT).show()
                navigationActions.goBack()

            },
            onSave = {
                recipeVM.createRecipe(
                    userID, nameState.value, picturesState,
                    instructionsState, ingredientsState,
                    portionState.intValue, perPersonState.value,
                    originState.value, dietState.value, tagsState,
                    { if (it) handleError(context, "Could not create recipe") }
                ) {
                    // delete the draft if it is published
                    offDataVM.deleteDraft(draftID)
                    navigationActions.navigateTo("${Route.RECIPE}/${it}")
                }
            }
        )
        BackHandler {
            if (dataEdited.value) showAlert.value = true
            else navigationActions.goBack()
        }
        // alert for leaving the recipe edition page
        if (showAlert.value) {
            DialogWindow(
                visible = showAlert,
                content = stringResource(R.string.alert_exitDraft),
                confirmText = stringResource(R.string.button_quit),
                confirmColour = Color.Red
            ) {
                showAlert.value = false
                navigationActions.goBack()
            }
        }
    }
}