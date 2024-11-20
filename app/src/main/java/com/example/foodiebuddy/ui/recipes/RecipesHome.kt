package com.example.foodiebuddy.ui.recipes

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
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
import com.example.foodiebuddy.data.Diet
import com.example.foodiebuddy.data.Origin
import com.example.foodiebuddy.data.Tag
import com.example.foodiebuddy.data.getString
import com.example.foodiebuddy.errors.handleError
import com.example.foodiebuddy.navigation.NavigationActions
import com.example.foodiebuddy.navigation.Route
import com.example.foodiebuddy.ui.MiniLoading
import com.example.foodiebuddy.ui.PrimaryScreen
import com.example.foodiebuddy.ui.SquareImage
import com.example.foodiebuddy.ui.theme.MyTypography
import com.example.foodiebuddy.viewModels.RecipeListViewModel
import com.example.foodiebuddy.viewModels.UserViewModel


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipesHome(userViewModel: UserViewModel, recipesListVM: RecipeListViewModel, navigationActions: NavigationActions) {

    // pressing the Android back button on this screen does not change it
    BackHandler {
        navigationActions.navigateTo(Route.RECIPES_HOME, true)
    }
    val context = LocalContext.current
    val loading = remember { mutableStateOf(false) }

    val allRecipesData by recipesListVM.allRecipes.collectAsState()
    val allRecipes = remember { mutableStateOf(allRecipesData) }

    LaunchedEffect(Unit) {
        loading.value = true
        userViewModel.fetchUserData({
            if (it) { handleError(context, "Could not fetch user data") }
        }){}
        recipesListVM.fetchAllRecipes({
            if (it) { handleError(context, "Could not fetch all recipes") }
        }){
            allRecipes.value = allRecipesData
            loading.value = false
        }
    }
    LaunchedEffect(allRecipesData) {
        loading.value = true
        recipesListVM.fetchAllRecipes({
            if (it) { handleError(context, "Could not fetch all recipes") }
        }){
            allRecipes.value = allRecipesData
            loading.value = false
        }
    }

    PrimaryScreen(
        navigationActions = navigationActions,
        title = stringResource(R.string.title_recipes),
        navigationIndex = 0,
        topBarIcons = {},
        userViewModel = userViewModel,
        floatingButton = {},
        content = { paddingValues ->
            if (loading.value) {
                MiniLoading(paddingValues)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    if (allRecipes.value.isEmpty()) {
                        item {
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                text = stringResource(R.string.txt_noRecipes),
                                style = MyTypography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        allRecipes.value.forEach { recipe ->
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { navigationActions.navigateTo("${Route.RECIPE}/${recipe.uid}") },
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                        horizontalAlignment = Alignment.Start
                                    ) {
                                        // first a row with first the picture
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (recipe.picture != Uri.EMPTY) {
                                                SquareImage(
                                                    size = 68.dp,
                                                    picture = recipe.picture,
                                                    contentDescription = stringResource(R.string.desc_recipePicture)
                                                )
                                            }
                                            // next to the picture two elements in a column ->
                                            Column(
                                                verticalArrangement = Arrangement.Top,
                                                horizontalAlignment = Alignment.Start
                                            ) {
                                                // name of the recipe
                                                Text(
                                                    text = recipe.name,
                                                    style = MyTypography.bodyMedium
                                                )
                                                // creator of the recipe
                                                Text(
                                                    text = stringResource(R.string.txt_recipeCreator, recipe.ownerName),
                                                    style = MyTypography.bodySmall,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }
                                        // lastly the tags
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (recipe.origin != Origin.NONE) {
                                                TagLabel(recipe.origin.getString(context))
                                            }
                                            if (recipe.diet != Diet.NONE) {
                                                TagLabel(recipe.diet.getString(context))
                                            }
                                            if (recipe.tags.isNotEmpty()) {
                                                recipe.tags.forEach { tag ->
                                                    TagLabel(tag.getString(context))
                                                }
                                            }
                                        }
                                        Divider(color = MaterialTheme.colorScheme.outline, thickness = 3.dp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}
