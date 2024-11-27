package com.example.foodiebuddy.ui.account

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.example.foodiebuddy.errors.handleError
import com.example.foodiebuddy.navigation.NavigationActions
import com.example.foodiebuddy.navigation.Route
import com.example.foodiebuddy.viewModels.UserViewModel

@Composable
fun CreateAccount(userViewModel: UserViewModel, navigationActions: NavigationActions) {
    val context = LocalContext.current
    val editingPicture = rememberSaveable { mutableStateOf(false) }
    val showPictureOptions = remember { mutableStateOf(false) }

    // userViewModel
    val currentPicture = rememberSaveable { mutableStateOf(Uri.EMPTY) }
    val nameState = rememberSaveable { mutableStateOf("") }
    val pictureState = remember { mutableStateOf(currentPicture.value) }
    val bioState = rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        currentPicture.value = userViewModel.getDefaultPicture()
        pictureState.value = currentPicture.value
    }

    // the profile and picture editing screens are actually one screen that just displays different elements
    // this way it's easier to keep user-edited values that haven't been saved on the DB yet
    if (editingPicture.value) {
        SetProfilePicture(
            pictureState.value,
            onCancel = {
                editingPicture.value = false
                pictureState.value = currentPicture.value
            }) { uri ->
            pictureState.value = uri
            currentPicture.value = uri
            editingPicture.value = false
        }
        // using the Android back button in the picture editing "screen" just has the same effect as cancelling the picture edition
        BackHandler {
            editingPicture.value = false
            pictureState.value = currentPicture.value
        }
    }
    else {
        EditAccount(
            context,
            navigationActions,
            navExtraActions = {
                signOut(context)
                deleteAuthentication(context)
            },
            nameState,
            pictureState,
            bioState,
            showPictureOptions,
            onEditPicture = { editingPicture.value = true },
            acceptTerms = true
        ) {
            userViewModel.createUser(nameState.value, pictureState.value, bioState.value, {
                if (it) { handleError(context, "Could not create user") }
            }) { navigationActions.navigateTo(Route.RECIPES_HOME, true) }
        }
        BackHandler {
            navigationActions.navigateTo(Route.LOGIN, true)
            signOut(context)
            deleteAuthentication(context)
        }
    }


}
