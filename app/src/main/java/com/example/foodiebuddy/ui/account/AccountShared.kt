package com.example.foodiebuddy.ui.account

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.foodiebuddy.R
import com.example.foodiebuddy.errors.handleError
import com.example.foodiebuddy.navigation.NavigationActions
import com.example.foodiebuddy.system.checkPermission
import com.example.foodiebuddy.system.imagePermissionVersion
import com.example.foodiebuddy.ui.CustomTextField
import com.example.foodiebuddy.ui.DialogWindow
import com.example.foodiebuddy.ui.RoundImage
import com.example.foodiebuddy.ui.SecondaryScreen
import com.example.foodiebuddy.ui.images.SetPicture
import com.example.foodiebuddy.ui.theme.MyTypography
import com.example.foodiebuddy.ui.theme.ValidGreen
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth

/**
 * Screen that allows the user to edit their profile information.
 * It is used for both account creation and account edition since both screens are almost similar.
 *
 * @param context used to check for media access permission
 * @param navigationActions to handle screen navigation
 * @param navExtraActions extra actions taken when navigation back
 * @param name state containing the username
 * @param picture state containing an Uri for the profile picture
 * @param defaultPicture default picture Uri needed for small UI changes
 * @param bio state containing the bio
 * @param showPictureOptions whether or not to show the modal bottom layout for picture edition options
 * @param dataEdited whether or not any data was edited.
 * This is used by the account edition screen to know if new data needs to saved, so this param can be empty for account creation
 * @param onEditPicture block that runs if profile picture is being edited
 * @param onRemovePicture block that runs if profile picture is removed
 * @param acceptTerms whether or not this screen needs to display terms and conditions (only account creation screen does)
 * @param onSave block that runs if the Save button is pressed
 */
@Composable
fun EditAccount(
    context: Context,
    navigationActions: NavigationActions,
    navExtraActions: () -> Unit,
    name: MutableState<String>,
    picture: MutableState<Uri>,
    defaultPicture: Uri,
    bio: MutableState<String>,
    showPictureOptions: MutableState<Boolean>,
    dataEdited: MutableState<Boolean> ?= null,
    onEditPicture: () -> Unit,
    onRemovePicture: () -> Unit,
    acceptTerms: Boolean,
    onSave: () -> Unit
) {
    // getting image and image permissions
    val imageInput = "image/*"
    val getPicture = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { pictureUri ->
            picture.value = pictureUri
            onEditPicture()
        }
    }
    val imagePermission = imagePermissionVersion()
    val requestMediaPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                getPicture.launch(imageInput)
            }
        }

    val termsAccepted = remember { mutableStateOf(false) }
    val dialogVisible = remember { mutableStateOf(false) }

    SecondaryScreen(
        title = "",
        navigationActions = navigationActions,
        navExtraActions = { navExtraActions() },
        topBarIcons = {},
        content = { paddingValue ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValue)
                    .padding(top = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // main screen body
                // round profile picture
                item { RoundImage(100.dp, picture.value, stringResource(R.string.desc_profilePic)) }
                // text button to modify the profile picture
                item {
                    Text(
                        modifier = Modifier.clickable {
                            if (picture.value != defaultPicture) {
                                showPictureOptions.value = true
                            }
                            else {
                                checkPermission(context, imagePermission, requestMediaPermissionLauncher)
                                { getPicture.launch(imageInput) }
                            }

                        },
                        text =
                            if (picture.value != defaultPicture) {
                                stringResource(R.string.button_modifyProfilePicture)
                            }
                            else stringResource(R.string.button_addProfilePicture),
                        style = MyTypography.labelMedium
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                }
                // text field to change the username
                item {
                    CustomTextField(
                        value = name.value,
                        onValueChange = {
                            if (dataEdited != null) { dataEdited.value = true }
                            name.value = it
                        },
                        icon = R.drawable.user,
                        placeHolder = stringResource(R.string.field_username),
                        singleLine = true,
                        maxLength = 15,
                        width = 300.dp
                    )
                }
                // text field to change the bio
                item {
                    CustomTextField(
                        value = bio.value,
                        onValueChange = {
                            if (dataEdited != null) { dataEdited.value = true }
                            bio.value = it
                        },
                        icon = R.drawable.pencil,
                        placeHolder = stringResource(R.string.field_bio),
                        singleLine = false,
                        maxLength = 150,
                        width = 300.dp,
                        height = 200.dp

                    )
                    Spacer(modifier = Modifier.size(16.dp))
                }
                // if it is needed to accept the terms and conditions (account creation) -> show a text button to display those
                if (acceptTerms) {
                    item {
                        Text(
                            modifier = Modifier.clickable { dialogVisible.value = true },
                            text = stringResource(R.string.button_termsConditions),
                            textAlign = TextAlign.Center,
                            style = MyTypography.labelMedium
                        )
                        Spacer(modifier = Modifier.size(16.dp))
                    }
                }
                // Save button is only enabled if there is a username and:
                // if on account creation -> terms and conditions have been accepted
                // if on account edition -> any data has been modified
                item {
                    val isEnabled = dataEdited?.value ?: true
                    val termsNeedAccepting = if (dataEdited == null) termsAccepted.value else true
                    SaveButton(name.value.isNotEmpty() && isEnabled && termsNeedAccepting) { onSave() }
                }
            }
            // terms and conditions dialog window
            if (dialogVisible.value) {
                DialogWindow(
                    dialogVisible,
                    stringResource(R.string.alert_termsConditions),
                    stringResource(R.string.button_accept) ,
                    ValidGreen
                ) {
                    termsAccepted.value = true
                    dialogVisible.value = false
                }
            }
            // modal bottom layout with picture edition options
            if (showPictureOptions.value) {
                PictureOptions(
                    onDismiss = { showPictureOptions.value = false },
                    onChange = {
                        checkPermission(context, imagePermission, requestMediaPermissionLauncher)
                        { getPicture.launch(imageInput) }
                    },
                    onRemove = {
                        onRemovePicture()
                        if (dataEdited != null) { dataEdited.value = true }
                    }
                )
            }
        }
    )
}

/**
 * Design for the Save button.
 *
 * @param isEnabled whether or not the button should be enabled (different design)
 * @param onClick block that runs when button is pressed
 */
@Composable
private fun SaveButton(isEnabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = { onClick() },
        modifier = Modifier.width(300.dp),
        enabled = isEnabled,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Text(stringResource(R.string.button_save), style = MyTypography.bodyLarge)
    }
}

/**
 * Deletes the Google authentication of the current user.
 * This is used if a new user has logged in with Google but has not finished completing their profile.
 *
 * @param context for error handling
 */
fun deleteAuthentication(context: Context) {
    val user = FirebaseAuth.getInstance().currentUser
    user?.delete()
        ?.addOnCompleteListener {
            if (it.isSuccessful) {
                Log.d("Login", "Successfully deleted authenticated user")
            } else {
                handleError(context, "Could not delete authenticated user")
            }
        }
}

/**
 * Signs out the current user.
 *
 * @param context for error handling and authentication
 */
fun signOut(context: Context) {
    AuthUI.getInstance().signOut(context).addOnCompleteListener{
        if (it.isSuccessful) {
            Log.d("Login", "Successfully signed out")
        } else {
            handleError(context, "Could not delete sign out user")
        }
    }
}


/**
 * Modal bottom layout that shows options for editing a picture
 *
 * @param onDismiss block that runs when dismissing the layout
 * @param onChange block that runs when choosing the "change picture" option
 * @param onRemove block that runs when choosing the "remove picture" option
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PictureOptions(
    onDismiss: () -> Unit,
    onChange: () -> Unit,
    onRemove: () -> Unit
) {
    ModalBottomSheet(
        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
        onDismissRequest = { onDismiss() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Change the picture
            Box(
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxHeight()
                    .clickable {
                        onChange()
                        onDismiss()
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceAround
                ) {
                    Icon(
                        painterResource(R.drawable.gallery),
                        contentDescription = stringResource(R.string.desc_changePicture)
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    Text(
                        text = stringResource(R.string.button_changeImage),
                        style = MyTypography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
            // Remove the picture
            Box(
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxHeight()
                    .clickable {
                        onRemove()
                        onDismiss()
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceAround
                ) {
                    Icon(
                        painterResource(R.drawable.remove),
                        contentDescription = stringResource(R.string.desc_removePicture)
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    Text(
                        text = stringResource(R.string.button_removeImage),
                        style = MyTypography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Screen where the user can modify their profile picture.
 *
 * @param picture Uri of the user-input picture
 * @param onCancel block that runs if the user cancels the picture modification
 * @param onSave block that runs if the user saves their new picture
 */
@Composable
fun SetProfilePicture(picture: Uri, onCancel: () -> Unit, onSave: (Uri) -> Unit) {
    SetPicture(
        picture = picture,
        roundMask = true,
        onCancel = { onCancel() },
        onSave = { onSave(it) }
    )
}
