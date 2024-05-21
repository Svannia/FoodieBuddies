package com.example.foodiebuddy.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.foodiebuddy.R
import com.example.foodiebuddy.navigation.NavigationActions
import com.example.foodiebuddy.navigation.Route
import com.example.foodiebuddy.ui.LoadingPage
import com.example.foodiebuddy.ui.SecondaryScreen
import com.example.foodiebuddy.ui.account.deleteAuthentication
import com.example.foodiebuddy.ui.account.signOut
import com.example.foodiebuddy.ui.theme.ContrastColor
import com.example.foodiebuddy.ui.theme.ContrastGrey
import com.example.foodiebuddy.ui.theme.MyTypography
import com.example.foodiebuddy.viewModels.UserViewModel

@Composable
fun Settings(userViewModel: UserViewModel, navigationActions: NavigationActions) {
    val context = LocalContext.current

    val alertVisible = remember { mutableStateOf(false) }
    val loading = remember { mutableStateOf(false) }

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
                    SettingCategory(stringResource(R.string.title_account)) {
                        SettingBox(
                            text = R.string.button_signOut,
                            onClick = {
                                navigationActions.navigateTo(Route.LOGIN, true)
                                signOut(context)
                            })
                        SettingBox(
                            text = R.string.button_deleteAccount,
                            onClick = { alertVisible.value = true })
                    }
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
                                    color = ContrastColor,
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
                                color = ContrastColor
                            )
                        }
                    }
                )
            }
        }
    }
}


@Composable
fun SettingCategory(name: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(text = name, style = MyTypography.titleSmall, modifier = Modifier.padding(start = 16.dp))
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) { content() }
        Divider(color = ContrastGrey, thickness = 3.dp)
    }
}
@Composable
fun SettingBox(text: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.CenterStart
    ) { Text(modifier = Modifier.padding(start = 60.dp), text = stringResource(text), style = MyTypography.bodyMedium) }
}
