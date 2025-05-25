package com.example.foodiebuddy.ui.settings

import android.widget.Toast
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
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.foodiebuddy.system.TelegramBot
import com.example.foodiebuddy.system.convertNameToTag
import com.example.foodiebuddy.system.convertTagToName
import com.example.foodiebuddy.system.getCurrentLocale
import com.example.foodiebuddy.system.getSupportedLanguages
import com.example.foodiebuddy.system.setLanguage
import com.example.foodiebuddy.ui.CustomTextField
import com.example.foodiebuddy.ui.DialogWindow
import com.example.foodiebuddy.ui.InputDialogWindow
import com.example.foodiebuddy.ui.LoadingPage
import com.example.foodiebuddy.ui.SecondaryScreen
import com.example.foodiebuddy.ui.account.deleteAuthentication
import com.example.foodiebuddy.ui.account.signOut
import com.example.foodiebuddy.ui.theme.MyTypography
import com.example.foodiebuddy.ui.theme.ValidGreen
import com.example.foodiebuddy.viewModels.OfflineDataViewModel
import com.example.foodiebuddy.viewModels.UserViewModel
import kotlinx.coroutines.launch

private const val HEIGHT = 52
private const val OFFSET = 45

@Composable
fun Settings(userViewModel: UserViewModel, offDataVM: OfflineDataViewModel, navigationActions: NavigationActions) {

    val context = LocalContext.current
    //val permissionLauncher = getNotificationPermissionLauncher(context)

    // visibility of elements that can appear / disappear
    val dialogVisible = remember { mutableStateOf(false) }
    val alertVisible = remember { mutableStateOf(false) }
    val reportVisible = remember { mutableStateOf(false) }
    val loading = remember { mutableStateOf(false) }

    // variables to handle theme settings
    val themeChoice = convertThemeToText(offDataVM.currentTheme.collectAsState().value)
    val themeChoices = ThemeChoice.entries.map { convertThemeToText(it) }
    val themeChoiceState = remember { mutableStateOf(themeChoice) }
    val darkTheme = stringResource(R.string.txt_systemDark)
    val lightTheme = stringResource(R.string.txt_systemLight)

    // variables to handle language settings
    val languageChoices = getSupportedLanguages().map { convertTagToName(it) }
    val languageChoice = convertTagToName(getCurrentLocale(context))
    val languageChoiceState = remember { mutableStateOf(languageChoice) }

    // variables for bug reporting
    val bugReport = remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    // display the loading screen if some values are changing
    if (loading.value) {
        LoadingPage()
    // else display the settings screen
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
                // main screen body
                // settings category for the theme
                item {
                    SettingCategory(stringResource(R.string.title_theme)) {
                        ToggleOptions(themeChoices.size, themeChoiceState, themeChoices) { newChoice ->
                            var newTheme = ThemeChoice.SYSTEM_DEFAULT
                            if (newChoice == lightTheme)
                            { newTheme = ThemeChoice.LIGHT }
                            else if (newChoice == darkTheme)
                            { newTheme = ThemeChoice.DARK }
                            offDataVM.setTheme(newTheme)
                        }
                    }
                }
                // settings category for the language
                item {
                    SettingCategory(stringResource(R.string.title_language)) {
                        ToggleOptions(languageChoices.size, languageChoiceState, languageChoices) { newChoice ->
                            val newLanguage = convertNameToTag(newChoice)
                            setLanguage(context, newLanguage)
                        }
                    }
                }
                // settings category for account-related actions
                item {
                    SettingCategory(stringResource(R.string.title_account)) {
                        // Log out button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(HEIGHT.dp)
                                .clickable {
                                    navigationActions.navigateTo(Route.LOGIN, true)
                                    signOut(context)
                                },
                            contentAlignment = Alignment.CenterStart
                        ) { Text(modifier = Modifier.padding(start = OFFSET.dp), text = stringResource(R.string.button_signOut), style = MyTypography.bodyLarge) }
                        // Delete account button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(HEIGHT.dp)
                                .clickable { alertVisible.value = true },
                            contentAlignment = Alignment.CenterStart
                        ) { Text(modifier = Modifier.padding(start = OFFSET.dp), text = stringResource(R.string.button_deleteAccount), style = MyTypography.bodyLarge, color = Color.Red) }
                    }
                }
                // settings category for About information
                item{
                    SettingCategory(stringResource(R.string.title_about)) {
                        // Button to view the terms and conditions
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(HEIGHT.dp)
                                .clickable { dialogVisible.value = true },
                            contentAlignment = Alignment.CenterStart
                        ) { Text(modifier = Modifier.padding(start = OFFSET.dp), text = stringResource(R.string.button_terms), style = MyTypography.bodyLarge) }
                        // button to open a dialog for sending bug information
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(HEIGHT.dp)
                                .clickable { reportVisible.value = true }
                        ) { Text(modifier = Modifier.padding(start = OFFSET.dp), text = stringResource(R.string.button_sendBug), style = MyTypography.bodyLarge) }
                    }
                }
            }
            // Delete account confirmation dialog window
            if (alertVisible.value) {
                DialogWindow(
                    alertVisible,
                    stringResource(R.string.alert_deleteAccount),
                    stringResource(R.string.button_delete),
                    Color.Red
                ) {
                    loading.value = true
                    userViewModel.deleteUser({
                        if (it) {
                            handleError(context, "Could not delete user data")
                            loading.value = false
                        }
                    }) {
                        signOut(context)
                        deleteAuthentication(context)
                        alertVisible.value = false
                        loading.value = false
                        navigationActions.navigateTo(Route.LOGIN, true)
                    }
                }
            }
            // terms and conditions dialog window
            if (dialogVisible.value) {
                DialogWindow(
                    dialogVisible,
                    stringResource(R.string.alert_termsConditions),
                    stringResource(R.string.button_accept),
                    ValidGreen
                ) {
                    dialogVisible.value = false
                }
            }
            // report a bug dialog window
            if (reportVisible.value) {
                InputDialogWindow(
                    visible = reportVisible,
                    confirmText = stringResource(R.string.button_accept),
                    confirmColour = ValidGreen,
                    onConfirm = {
                        if (bugReport.value.isNotEmpty()) {
                            coroutineScope.launch {
                                val success = TelegramBot.sendMessage("Bug report: ${bugReport.value}")
                                if (success) {
                                    Toast.makeText(context, context.getString(R.string.toast_bugReport), Toast.LENGTH_SHORT).show()
                                } else {
                                    handleError(context, "Failed to send bug report")
                                }
                            }
                        }
                        bugReport.value = bugReport.value.trimEnd()
                        reportVisible.value = false
                    }
                ) {
                    // title for Report a bug, input text field and log.txt explanation
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = context.getString(R.string.button_sendBug), style = MyTypography.titleSmall)
                        CustomTextField(
                            value = bugReport.value,
                            onValueChange = { bugReport.value = it },
                            icon = -1,
                            placeHolder = stringResource(R.string.field_bugReport),
                            singleLine = false,
                            maxLength = 700,
                            showMaxChara = false,
                            width = 250.dp,
                            height = 350.dp
                        )
                        Text(text = stringResource(R.string.txt_reportBugNote), style = MyTypography.bodyMedium)
                    }
                }
            }
        }
    }
}

/**
 * Creates the layout for a category (family) of settings on the Settings screen.
 *
 * @param name name of the settings category
 * @param content elements of the settings category contained in a column
 */
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

/**
 * For a specific setting, handles a list of options where exactly one option can and must be selected.
 *
 * @param numberChoices number of options in the list
 * @param currentChoice option that is currently selected
 * @param choicesNames list of all the options' names
 * @param onToggle block that runs when a new option is toggled on, with the name of the new option selected
 */
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

/**
 * Creates the layout of a single option within a list of options that can be selected / toggled on.
 *
 * @param name displayed as the option's name
 * @param isToggled whether this specific option is toggled on or not
 * @param onToggle block that runs if this option is toggled on
 */
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
            Text(text = name, style = MyTypography.bodyLarge)
        }
    }
}

/**
 * Converts the themes objects understood by the system as a name that can be displayed to the user.
 *
 * @param theme ThemeChoice to be converted
 * @return name of the ThemeChoice as a string
 */
@Composable
private fun convertThemeToText(theme: ThemeChoice): String {
    return when (theme) {
        ThemeChoice.SYSTEM_DEFAULT -> stringResource(R.string.txt_systemDefault)
        ThemeChoice.DARK -> stringResource(R.string.txt_systemDark)
        ThemeChoice.LIGHT -> stringResource(R.string.txt_systemLight)
    }
}
