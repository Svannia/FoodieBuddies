package com.example.foodiebuddy.ui.settings

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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import com.example.foodiebuddy.errors.handleError
import com.example.foodiebuddy.navigation.NavigationActions
import com.example.foodiebuddy.navigation.Route
import com.example.foodiebuddy.system.convertNameToTag
import com.example.foodiebuddy.system.convertTagToName
import com.example.foodiebuddy.system.getCurrentLocale
import com.example.foodiebuddy.system.getSupportedLanguages
import com.example.foodiebuddy.system.setLanguage
import com.example.foodiebuddy.ui.DialogWindow
import com.example.foodiebuddy.ui.LoadingPage
import com.example.foodiebuddy.ui.SecondaryScreen
import com.example.foodiebuddy.ui.account.deleteAuthentication
import com.example.foodiebuddy.ui.account.signOut
import com.example.foodiebuddy.ui.theme.MyTypography
import com.example.foodiebuddy.ui.theme.ValidGreen
import com.example.foodiebuddy.viewModels.OfflinePreferencesViewModel
import com.example.foodiebuddy.viewModels.UserViewModel

const val HEIGHT = 52
const val OFFSET = 45

@Composable
fun Settings(userViewModel: UserViewModel, offPrefViewModel: OfflinePreferencesViewModel, navigationActions: NavigationActions) {

    val context = LocalContext.current
    //val permissionLauncher = getNotificationPermissionLauncher(context)

    val dialogVisible = remember { mutableStateOf(false) }
    val alertVisible = remember { mutableStateOf(false) }
    val loading = remember { mutableStateOf(false) }

    val themeChoice = convertThemeToText(offPrefViewModel.currentTheme.collectAsState().value)
    val themeChoices = ThemeChoice.entries.map { convertThemeToText(it) }
    val themeChoiceState = remember { mutableStateOf(themeChoice) }

    val darkTheme = stringResource(R.string.txt_systemDark)
    val lightTheme = stringResource(R.string.txt_systemLight)
    val languageChoices = getSupportedLanguages().map { convertTagToName(it) }
    val languageChoice = convertTagToName(getCurrentLocale(context))
    val languageChoiceState = remember { mutableStateOf(languageChoice) }

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
                            offPrefViewModel.setTheme(newTheme)
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
                item{
                    SettingCategory(stringResource(R.string.title_about)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(HEIGHT.dp)
                                .clickable { dialogVisible.value = true },
                            contentAlignment = Alignment.CenterStart
                        ) { Text(modifier = Modifier.padding(start = OFFSET.dp), text = stringResource(R.string.button_terms), style = MyTypography.bodyMedium) }
                    }
                }
            }
            if (alertVisible.value) {
                DialogWindow(
                    alertVisible,
                    stringResource(R.string.alert_deleteAccount),
                    stringResource(R.string.button_delete),
                    Color.Red
                )
                    {
                        loading.value = true
                        userViewModel.deleteUser({
                            if (it) { handleError(context, "Could not delete user data") }
                        }) {
                            signOut(context)
                            deleteAuthentication(context)
                            alertVisible.value = false
                            loading.value = false
                            navigationActions.navigateTo(Route.LOGIN, true)
                        }
                    }
            }
            if (dialogVisible.value) {
                DialogWindow(
                    dialogVisible,
                    stringResource(R.string.alert_termsConditions),
                    stringResource(R.string.button_accept) ,
                    ValidGreen
                ) {
                    dialogVisible.value = false
                }
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
private fun convertThemeToText(theme: ThemeChoice): String {
    return when (theme) {
        ThemeChoice.SYSTEM_DEFAULT -> stringResource(R.string.txt_systemDefault)
        ThemeChoice.LIGHT -> stringResource(R.string.txt_systemDark)
        ThemeChoice.DARK -> stringResource(R.string.txt_systemLight)
    }
}
