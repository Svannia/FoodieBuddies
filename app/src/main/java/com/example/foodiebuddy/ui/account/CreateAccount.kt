package com.example.foodiebuddy.ui.account

import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.foodiebuddy.R
import com.example.foodiebuddy.navigation.NavigationActions
import com.example.foodiebuddy.navigation.NavigationButton
import com.example.foodiebuddy.navigation.Route
import com.example.foodiebuddy.system.checkPermission
import com.example.foodiebuddy.system.imagePermissionVersion
import com.example.foodiebuddy.ui.ScreenStructure
import com.example.foodiebuddy.viewModels.UserViewModel

@Composable
fun CreateAccount(userViewModel: UserViewModel, navigationActions: NavigationActions) {
    val context = LocalContext.current
    // getting image and image permissions
    val imageInput = "image/*"
    val getPicture = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val pictureLink = Uri.encode(uri.toString())
            val route = "${Route.PROFILE_PICTURE}/$pictureLink"
            Log.d("Debug", "route given is : $route")
            navigationActions.navigateTo(route)
        }
    }
    val imagePermission = imagePermissionVersion()
    val requestPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                getPicture.launch(imageInput)
            }
        }

    ScreenStructure(
        navigationActions = navigationActions,
        title = stringResource(R.string.title_createAccount),
        navButton = NavigationButton.GO_BACK,
        topBarIcons = {},
        content = {
            item {
                Button(
                    onClick = {
                        checkPermission(context, imagePermission, requestPermissionLauncher) {
                            getPicture.launch(imageInput)
                        }
                    }
                ) {
                    Text("gotta try stuff")
                }
            }
        }
    )
}
