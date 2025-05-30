package com.example.foodiebuddy.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.foodiebuddy.R
import com.example.foodiebuddy.errors.handleError
import com.example.foodiebuddy.navigation.NavigationActions
import com.example.foodiebuddy.navigation.Route
import com.example.foodiebuddy.ui.ArcShape
import com.example.foodiebuddy.ui.RoundImage
import com.example.foodiebuddy.ui.SecondaryScreen
import com.example.foodiebuddy.ui.theme.MyTypography
import com.example.foodiebuddy.viewModels.UserViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun Profile(userViewModel: UserViewModel, navigationActions: NavigationActions) {

    val context = LocalContext.current
    val userData by userViewModel.userData.collectAsState()

    val nameState = rememberSaveable { mutableStateOf(userData.username) }
    val countState = rememberSaveable { mutableIntStateOf(userData.numberRecipes) }
    val pictureState = remember { mutableStateOf(userData.picture) }
    val bioState = rememberSaveable { mutableStateOf(userData.bio) }
    val dateState = rememberSaveable { mutableStateOf(userData.dateJoined) }

    val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
    val displayDate = remember { mutableStateOf("") }
    try {
        val parsedDate = LocalDate.parse(dateState.value)
        displayDate.value = parsedDate.format(formatter)
    } catch (e: Exception) {
        displayDate.value = ""
    }

    LaunchedEffect(Unit) {
        userViewModel.fetchUserData({
            if (it) { handleError(context, "Could not fetch user data") }
        }){
            nameState.value = userData.username
            countState.intValue = userData.numberRecipes
            bioState.value = userData.bio
            pictureState.value = userData.picture
            dateState.value = userData.dateJoined
            try {
                val parsedDate = LocalDate.parse(dateState.value)
                displayDate.value = parsedDate.format(formatter)
            } catch (e: Exception) {
                displayDate.value = ""
            }
        }
    }
    LaunchedEffect(userData) {
        nameState.value = userData.username
        countState.intValue = userData.numberRecipes
        bioState.value = userData.bio
        pictureState.value = userData.picture
        dateState.value = userData.dateJoined
        try {
            val parsedDate = LocalDate.parse(dateState.value)
            displayDate.value = parsedDate.format(formatter)
        } catch (e: Exception) {
            displayDate.value = ""
        }
    }

    SecondaryScreen(
        title = "",
        navigationActions = navigationActions,
        navExtraActions = {},
        topBarIcons = {
            // when viewing a profile, the Edit button only exists if the user is visiting their own profile
            if (userViewModel.getCurrentUserID() == "") {
                handleError(LocalContext.current, "Could not fetch current user ID")
            } else if (userData.uid == userViewModel.getCurrentUserID()) {
                Text(
                    text = stringResource(R.string.button_edit),
                    style = MyTypography.bodySmall,
                    modifier = Modifier.clickable {
                        navigationActions.navigateTo(Route.ACCOUNT_SETTINGS)
                    }
                )
            }
    }) { paddingValues ->
            // rounded coloured header
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // rounded header shape with profile picture
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(ArcShape())
                                .background(color = MaterialTheme.colorScheme.primary),
                        )
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.size(60.dp))
                            Box(
                                modifier = Modifier
                                    .size(132.dp)
                                    .clip(CircleShape)
                                    .background(color = MaterialTheme.colorScheme.background),
                                contentAlignment = Alignment.Center
                            ) {
                                RoundImage(
                                    120.dp,
                                    pictureState.value,
                                    stringResource(R.string.desc_profilePic)
                                )
                            }
                        }
                    }
                }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // name and date joined
                        Text(text = nameState.value, style = MyTypography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        if (displayDate.value.isNotBlank()) {
                            Text(text = stringResource(R.string.txt_dateJoined, displayDate.value), style = MyTypography.bodyLarge)
                        }

                        // bio
                        if (bioState.value.isNotBlank()) {
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(text = stringResource(R.string.txt_bio), style = MyTypography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Text(text = bioState.value, style = MyTypography.bodyLarge.copy(textAlign = TextAlign.Center))
                        }

                        // recipes added
                        Spacer(modifier = Modifier.size(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier
                                .width(0.5f.dp)
                                .height(68.dp)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.primary))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(
                                modifier = Modifier.fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(text = countState.intValue.toString(), style = MyTypography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.size(16.dp))
                                Text(text = stringResource(R.string.txt_numberRecipes), style = MyTypography.bodyLarge)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Box(modifier = Modifier
                                .width(0.5f.dp)
                                .height(68.dp)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.primary))
                        }
                    }
                }
            }
    }
}