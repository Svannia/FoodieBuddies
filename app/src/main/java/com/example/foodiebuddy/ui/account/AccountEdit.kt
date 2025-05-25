package com.example.foodiebuddy.ui.account

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.example.foodiebuddy.data.User
import com.example.foodiebuddy.errors.handleError
import com.example.foodiebuddy.navigation.NavigationActions
import com.example.foodiebuddy.ui.LoadingPage
import com.example.foodiebuddy.viewModels.UserViewModel

@Composable
fun AccountEdit(userViewModel: UserViewModel, navigationActions: NavigationActions) {
    val context = LocalContext.current
    val editingPicture = rememberSaveable { mutableStateOf(false) }
    val loadingData = rememberSaveable { mutableStateOf(false) }

    val userData by userViewModel.userData.collectAsState()

    val nameState = rememberSaveable { mutableStateOf(userData.username) }
    val validUsernameState = remember { mutableStateOf(false) }
    val currentPicture = rememberSaveable { mutableStateOf(userData.picture) }
    val pictureState = remember { mutableStateOf(userData.picture) }
    val bioState = rememberSaveable { mutableStateOf(userData.bio) }

    val defaultPicture = remember { mutableStateOf(Uri.EMPTY) }

    val dataEdited = rememberSaveable { mutableStateOf(false) }
    val pictureEdited = rememberSaveable { mutableStateOf(false) }
    val pictureRemoved = remember { mutableStateOf(false) }
    val showPictureOptions = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        loadingData.value = true
        defaultPicture.value = userViewModel.getDefaultPicture()
        userViewModel.fetchUserData({
            if (it) {
                handleError(context, "Could not fetch user data")
                loadingData.value = false
            }
        }){
            if (userData != User.empty()) {
                nameState.value = userData.username
                pictureState.value = userData.picture
                currentPicture.value = userData.picture
                bioState.value = userData.bio
            }
            loadingData.value = false
        }
    }
    LaunchedEffect(userData) {
        if (userData != User.empty()) {
            nameState.value = userData.username
            pictureState.value = userData.picture
            currentPicture.value = userData.picture
            bioState.value = userData.bio
        }
    }

    // see CreateAccount for explanation on profile VS picture editing "screens"
    if (loadingData.value) {
        LoadingPage()
    } else {
        if(editingPicture.value) {
            SetProfilePicture(
                pictureState.value,
                onCancel = {
                    editingPicture.value = false
                    pictureState.value = currentPicture.value
                }
            ) { uri ->
                pictureState.value = uri
                currentPicture.value = uri
                editingPicture.value = false
                dataEdited.value = true
                pictureEdited.value = true
            }
            BackHandler {
                editingPicture.value = false
                pictureState.value = currentPicture.value
            }
        } else {
            EditAccount(
                context,
                navigationActions,
                navExtraActions = {},
                nameState,
                checkUsername = {
                    userViewModel.usernameAvailable(
                        nameState.value,
                        onSuccess = { usernameAvailable ->
                            validUsernameState.value = usernameAvailable
                        },
                        onFailure = { e ->
                            handleError(context,"Failed to check username existence", e)
                        }
                    )
                },
                validUsernameState,
                pictureState,
                defaultPicture.value,
                bioState,
                showPictureOptions,
                dataEdited = dataEdited,
                onEditPicture = { editingPicture.value = true },
                onRemovePicture = {
                    pictureState.value = defaultPicture.value
                    currentPicture.value = defaultPicture.value
                    pictureEdited.value = false
                    pictureRemoved.value = true
                },
                acceptTerms = false
            ) {
                loadingData.value = true
                userViewModel.updateUser(nameState.value, pictureState.value, bioState.value, pictureEdited.value, pictureRemoved.value, {
                    if (it) {
                        handleError(context, "Could not update user data")
                        loadingData.value = false
                    }
                }) {
                    navigationActions.goBack()
                    loadingData.value = false
                }

            }
        }
    }
}