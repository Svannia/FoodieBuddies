package com.example.foodiebuddy.ui.recipes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.foodiebuddy.R
import com.example.foodiebuddy.navigation.NavigationActions
import com.example.foodiebuddy.navigation.Route
import com.example.foodiebuddy.ui.PrimaryScreen
import com.example.foodiebuddy.viewModels.UserViewModel

@Composable
fun RecipesHome(userViewModel: UserViewModel, navigationActions: NavigationActions) {
    BackHandler {
        navigationActions.navigateTo(Route.RECIPES_HOME, true)
    }
    val userData by userViewModel.userData.collectAsState()
    userViewModel.fetchUserData()

    PrimaryScreen(
        navigationActions = navigationActions,
        title = stringResource(R.string.title_recipes),
        topBarIcons = {},
        userViewModel = userViewModel,
        content = {paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                item {
                    Text(text = "Hello")
                }
            }
        }
    )
}
