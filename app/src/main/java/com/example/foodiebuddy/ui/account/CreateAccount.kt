package com.example.foodiebuddy.ui.account

import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import coil.compose.rememberImagePainter
import com.example.foodiebuddy.R
import com.example.foodiebuddy.navigation.NavigationActions
import com.example.foodiebuddy.navigation.NavigationButton
import com.example.foodiebuddy.navigation.Route
import com.example.foodiebuddy.system.checkPermission
import com.example.foodiebuddy.system.imagePermissionVersion
import com.example.foodiebuddy.ui.ScreenStructure
import com.example.foodiebuddy.viewModels.UserViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.observeOn
import kotlinx.coroutines.launch

@Composable
fun CreateAccount(userViewModel: UserViewModel, navigationActions: NavigationActions) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val editingPicture = rememberSaveable { mutableStateOf(false) }

    // userViewModel
    val userData by userViewModel.userData.collectAsState()
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
