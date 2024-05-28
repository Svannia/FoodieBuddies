package com.example.foodiebuddy.ui.settings

import android.content.Context
import android.content.res.Resources
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.foodiebuddy.R
import com.example.foodiebuddy.database.ThemeChoice
import com.example.foodiebuddy.navigation.NavigationActions
import com.example.foodiebuddy.navigation.Route
import com.example.foodiebuddy.system.CHANNELS
import com.example.foodiebuddy.system.checkPermission
import com.example.foodiebuddy.system.convertNameToTag
import com.example.foodiebuddy.system.convertTagToName
import com.example.foodiebuddy.system.getCurrentLocale
import com.example.foodiebuddy.system.getNotificationPermissionLauncher
import com.example.foodiebuddy.system.getSupportedLanguages
import com.example.foodiebuddy.system.notificationPermission
import com.example.foodiebuddy.system.sendNotifications
import com.example.foodiebuddy.system.setLanguage
import com.example.foodiebuddy.ui.LoadingPage
import com.example.foodiebuddy.ui.SecondaryScreen
import com.example.foodiebuddy.ui.account.deleteAuthentication
import com.example.foodiebuddy.ui.account.signOut
import com.example.foodiebuddy.ui.theme.DarkGrey
import com.example.foodiebuddy.ui.theme.LightGrey
import com.example.foodiebuddy.ui.theme.MyTypography
import com.example.foodiebuddy.viewModels.PreferencesViewModel
import com.example.foodiebuddy.viewModels.UserViewModel

const val HEIGHT = 52
const val OFFSET = 45

@Composable
fun Settings(userViewModel: UserViewModel, prefViewModel: PreferencesViewModel, navigationActions: NavigationActions) {
    val context = LocalContext.current
    val permissionLauncher = getNotificationPermissionLauncher(context)

    val alertVisible = remember { mutableStateOf(false) }
    val loading = remember { mutableStateOf(false) }

    val themeChoice = convertThemeToText(prefViewModel.currentTheme.collectAsState().value)
    val themeChoices = ThemeChoice.entries.map { convertThemeToText(it) }
    val themeChoiceState = remember { mutableStateOf(themeChoice) }

    val darkTheme = stringResource(R.string.txt_systemDark)
    val lightTheme = stringResource(R.string.txt_systemLight)
    val languageChoices = getSupportedLanguages().map { convertTagToName(it) }
    val languageChoice = convertTagToName(getCurrentLocale(context))
    val languageChoiceState = remember { mutableStateOf(languageChoice) }

    val notificationStates = prefViewModel.notificationStates.collectAsState()
    val userNotif = CHANNELS[0]
    val recipeNotif = CHANNELS[1]
    val favouriteNotif = CHANNELS[2]
    val newUserState = remember { mutableStateOf(notificationStates.value[userNotif] ?: false) }
    val newRecipeState = remember { mutableStateOf(notificationStates.value[recipeNotif] ?: false) }
    val newFavouriteState = remember { mutableStateOf(notificationStates.value[favouriteNotif] ?: false) }


    if (loading.value) {
        LoadingPage()
    } else {
        SecondaryScreen(
            title = stringResource(R.string.title_settings),
            navigationActions = navigationActions,
            navExtraActions = {},
            topBarIcons = {}) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                item {
                    SettingCategory(stringResource(R.string.title_theme)) {
                        ToggleOptions(themeChoices.size, themeChoiceState, themeChoices) { newChoice ->
                            var newTheme = ThemeChoice.SYSTEM_DEFAULT
                            if (newChoice == lightTheme)
                            { newTheme = ThemeChoice.LIGHT }
                            else if (newChoice == darkTheme)
                            { newTheme = ThemeChoice.DARK }
                            prefViewModel.setTheme(newTheme)
                        }
                    }
                }
                item {
                    SettingCategory(stringResource(R.string.title_language)) {
                        ToggleOptions(languageChoices.size, languageChoiceState, languageChoices) { newChoice ->
                            val newLanguage = convertNameToTag(newChoice)
                            setLanguage(context, newLanguage)
                        }
                    }
                }
                item {
                    SettingCategory(stringResource(R.string.title_notifications)) {
                        SwitchNotificationBox(
                            name = stringResource(R.string.notifChannel_newUser),
                            context = context,
                            notifState = newUserState,
                            notifKey = userNotif,
                            prefViewModel = prefViewModel,
                            permissionLauncher = permissionLauncher
                        )
                        SwitchNotificationBox(
                            name = stringResource(R.string.notifChannel_newRecipe),
                            context = context,
                            notifState = newRecipeState,
                            notifKey = recipeNotif,
                            prefViewModel = prefViewModel,
                            permissionLauncher = permissionLauncher
                        )
                        SwitchNotificationBox(
                            name = stringResource(R.string.notifChannel_newFavourite),
                            context = context,
                            notifState = newFavouriteState,
                            notifKey = favouriteNotif,
                            prefViewModel = prefViewModel,
                            permissionLauncher = permissionLauncher
                        )
                    }
                }
                item {
                    SettingCategory(stringResource(R.string.title_account)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(HEIGHT.dp)
                                .clickable {
                                    navigationActions.navigateTo(Route.LOGIN, true)
                                    signOut(context)
                                },
                            contentAlignment = Alignment.CenterStart
                        ) { Text(modifier = Modifier.padding(start = OFFSET.dp), text = stringResource(R.string.button_signOut), style = MyTypography.bodyMedium) }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(HEIGHT.dp)
                                .clickable { alertVisible.value = true },
                            contentAlignment = Alignment.CenterStart
                        ) { Text(modifier = Modifier.padding(start = OFFSET.dp), text = stringResource(R.string.button_deleteAccount), style = MyTypography.bodyMedium, color = Color.Red) }
                    }
                }
                item {
                   Text(text = "notif test", modifier = Modifier.clickable {
                       val intentData = "mT1pydILvNWmitE5HIrtTo1L0r02"
                       sendNotifications(context, 0, "Hey there!", "OMG IT WORKS YAYYYYY LESGO", intentData, prefViewModel)
                       navigationActions.navigateTo("${Route.PROFILE}/$intentData")
                   })
                }
            }
            if (alertVisible.value) {
                AlertDialog(
                    onDismissRequest = { alertVisible.value = false },
                    text = {
                        Text(
                            text = stringResource(R.string.alert_deleteAccount),
                            style = MyTypography.bodyMedium
                        )
                    },
                    confirmButton = {
                        TextButton(
                            modifier = Modifier
                                .border(
                                    width = 2.dp,
                                    color = Color.Red,
                                    shape = RoundedCornerShape(50)
                                )
                                .background(
                                    color = Color.Transparent,
                                    shape = RoundedCornerShape(50)
                                ),
                            onClick = {
                                loading.value = true
                                userViewModel.deleteUser {
                                    signOut(context)
                                    deleteAuthentication(context)
                                    alertVisible.value = false
                                    loading.value = false
                                    navigationActions.navigateTo(Route.LOGIN, true)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = stringResource(R.string.button_delete),
                                style = MyTypography.bodyMedium,
                                color = Color.Red
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(
                            modifier = Modifier
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.inversePrimary,
                                    shape = RoundedCornerShape(50)
                                )
                                .background(
                                    color = Color.Transparent,
                                    shape = RoundedCornerShape(50)
                                ),
                            onClick = { alertVisible.value = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = stringResource(R.string.button_cancel),
                                style = MyTypography.bodyMedium,
                                color = MaterialTheme.colorScheme.inversePrimary
                            )
                        }
                    }
                )
            }
        }
    }
}


@Composable
private fun SettingCategory(name: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(text = name, style = MyTypography.titleSmall, modifier = Modifier.padding(start = 16.dp))
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) { content() }
        Divider(color = MaterialTheme.colorScheme.outline, thickness = 3.dp)
        Spacer(modifier = Modifier.size(16.dp))
    }
}
@Composable
private fun ToggleOptions(numberChoices: Int, currentChoice: MutableState<String>, choicesNames: List<String>, onToggle: (String) -> Unit) {
    var toggledIndex by remember { mutableIntStateOf(choicesNames.indexOf(currentChoice.value)) }

    for (i in 0 until numberChoices) {
        ToggleBox(choicesNames[i], toggledIndex == i) {
            toggledIndex = i
            currentChoice.value = choicesNames[i]
            onToggle(choicesNames[i])
        }
    }
}
@Composable
private fun ToggleBox(name: String, isToggled: Boolean, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(HEIGHT.dp)
            .clickable { onToggle() },
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = OFFSET.dp, end = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                modifier = Modifier.size(20.dp),
                selected = isToggled,
                onClick = { onToggle() },
                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary))
            Text(text = name, style = MyTypography.bodyMedium)
        }
    }
}

@Composable
private fun SwitchNotificationBox(
    name: String,
    context: Context,
    notifState: MutableState<Boolean>,
    notifKey: String,
    prefViewModel: PreferencesViewModel,
    permissionLauncher: ManagedActivityResultLauncher<String, Boolean>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(HEIGHT.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = OFFSET.dp, end = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = name, style = MyTypography.bodyMedium)
            Switch(
                checked = notifState.value,
                onCheckedChange = {newState ->
                    if (newState) {
                        checkPermission(context, notificationPermission(), permissionLauncher) {
                            notifState.value = true
                            prefViewModel.setNotificationState(notifKey, true)
                        }
                    } else {
                        notifState.value = false
                        prefViewModel.setNotificationState(notifKey, false)
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.tertiary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.tertiary,
                    uncheckedTrackColor = MaterialTheme.colorScheme.outline
                ))
        }
    }
}


@Composable
private fun convertThemeToText(theme: ThemeChoice): String {
    return when (theme) {
        ThemeChoice.SYSTEM_DEFAULT -> stringResource(R.string.txt_systemDefault)
        ThemeChoice.LIGHT -> stringResource(R.string.txt_systemDark)
        ThemeChoice.DARK -> stringResource(R.string.txt_systemLight)
    }
}
