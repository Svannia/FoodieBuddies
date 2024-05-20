package com.example.foodiebuddy.ui.account

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.example.foodiebuddy.navigation.NavigationActions
import com.example.foodiebuddy.navigation.Route
import com.example.foodiebuddy.viewModels.UserViewModel

@Composable
fun CreateAccount(userViewModel: UserViewModel, navigationActions: NavigationActions) {
    val context = LocalContext.current
    val editingPicture = rememberSaveable { mutableStateOf(false) }

    // userViewModel
    val defaultPicture = rememberSaveable { mutableStateOf(Uri.EMPTY) }
    val nameState = rememberSaveable { mutableStateOf("") }
    val pictureState = remember { mutableStateOf(defaultPicture.value) }
    val bioState = rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        defaultPicture.value = userViewModel.getDefaultPicture()
        pictureState.value = defaultPicture.value
    }

    if (editingPicture.value) {
        SetProfilePicture(
            pictureState.value,
            onCancel = {
                editingPicture.value = false
                pictureState.value = defaultPicture.value
            }) { uri ->
            pictureState.value = uri
            editingPicture.value = false
        }
        BackHandler {
            editingPicture.value = false
            pictureState.value = Uri.parse("")
        }
    }
    else {
        NewAccount(context, userViewModel, navigationActions, nameState, pictureState, bioState) {
            editingPicture.value = true
        }
        BackHandler {
            navigationActions.navigateTo(Route.LOGIN, true)
        }
    }


}
