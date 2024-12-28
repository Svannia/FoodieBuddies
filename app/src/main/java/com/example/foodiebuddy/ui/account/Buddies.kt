package com.example.foodiebuddy.ui.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.foodiebuddy.R
import com.example.foodiebuddy.errors.handleError
import com.example.foodiebuddy.navigation.NavigationActions
import com.example.foodiebuddy.navigation.Route
import com.example.foodiebuddy.ui.MiniLoading
import com.example.foodiebuddy.ui.RoundImage
import com.example.foodiebuddy.ui.SecondaryScreen
import com.example.foodiebuddy.ui.theme.MyTypography
import com.example.foodiebuddy.viewModels.UserViewModel

private const val HEIGHT = 76

@Composable
fun Buddies(userViewModel: UserViewModel, navigationActions: NavigationActions) {
    val context = LocalContext.current
    val loading = remember { mutableStateOf(false) }

    val allUsersData by userViewModel.allUsers.collectAsState()
    val allUsers = remember { mutableStateOf(allUsersData) }

    LaunchedEffect(Unit) {
        loading.value = true
        userViewModel.fetchAllUsers({
            if (it) {
                handleError(context, "Could not fetch all users")
                loading.value = false
            }
        }) {
            allUsers.value = allUsersData
            loading.value = false
        }
    }
    LaunchedEffect(allUsersData) {
        loading.value = true
        userViewModel.fetchAllUsers({
            if (it) {
                handleError(context, "Could not fetch all users")
                loading.value = false
            }
        }) {
            allUsers.value = allUsersData
            loading.value = false
        }
    }


    SecondaryScreen(
        title = stringResource(R.string.dst_buddies),
        navigationActions = navigationActions,
        navExtraActions = {},
        topBarIcons = {}) { paddingValues ->
            if (loading.value) {
                MiniLoading(paddingValues)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(start = 16.dp, end = 16.dp)
                ) {
                    // if there are no other users -> display default text
                    if (allUsers.value.isEmpty()) {
                        item {
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                text = stringResource(R.string.txt_noBuddies),
                                style = MyTypography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        allUsers.value.forEach { user ->
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(HEIGHT.dp)
                                        .clickable { navigationActions.navigateTo("${Route.PROFILE}/${user.uid}") },
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RoundImage(
                                            size = 64.dp,
                                            picture = user.picture,
                                            contentDescription = stringResource(R.string.desc_profilePic)
                                        )
                                        Text(
                                            text = user.username,
                                            style = MyTypography.bodyLarge
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
    }
}