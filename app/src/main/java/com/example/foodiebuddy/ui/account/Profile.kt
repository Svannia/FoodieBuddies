package com.example.foodiebuddy.ui.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.foodiebuddy.R
import com.example.foodiebuddy.navigation.NavigationActions
import com.example.foodiebuddy.navigation.Route
import com.example.foodiebuddy.ui.RoundImage
import com.example.foodiebuddy.ui.SecondaryScreen
import com.example.foodiebuddy.ui.theme.MyTypography
import com.example.foodiebuddy.viewModels.UserViewModel

@Composable
fun Profile(userViewModel: UserViewModel, navigationActions: NavigationActions) {

    val userData by userViewModel.userData.collectAsState()

    val nameState = rememberSaveable { mutableStateOf(userData.username) }
    val countState = rememberSaveable { mutableIntStateOf(userData.numberRecipes) }
    val pictureState = remember { mutableStateOf(userData.picture) }
    val bioState = rememberSaveable { mutableStateOf(userData.bio) }

    LaunchedEffect(Unit) {
        userViewModel.fetchUserData{
            nameState.value = userData.username
            countState.intValue = userData.numberRecipes
            bioState.value = userData.bio
            pictureState.value = userData.picture
        }
    }
    LaunchedEffect(userData) {
        nameState.value = userData.username
        countState.intValue = userData.numberRecipes
        bioState.value = userData.bio
        pictureState.value = userData.picture
    }

    SecondaryScreen(
        title = "",
        navigationActions = navigationActions,
        navExtraActions = {},
        topBarIcons = {
            if (userData.uid == userViewModel.getCurrentUserID()) {
                Text(
                    text = stringResource(R.string.button_edit),
                    style = MyTypography.bodySmall,
                    modifier = Modifier.clickable {
                        navigationActions.navigateTo(Route.ACCOUNT_SETTINGS)
                    }
                )
            }
        }) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                RoundImage(150.dp, pictureState.value, stringResource(R.string.desc_profilePic))
                Text(text = nameState.value, style = MyTypography.titleMedium)
                Spacer(modifier = Modifier.size(16.dp))
                Text(text = stringResource(R.string.txt_numberRecipes, countState.intValue), style = MyTypography.bodyMedium)
                Spacer(modifier = Modifier.size(16.dp))
                Text(text = bioState.value, style = MyTypography.bodyMedium)
            }
    }
}