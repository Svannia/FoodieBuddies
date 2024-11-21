package com.example.foodiebuddy.ui.recipes

import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.foodiebuddy.R
import com.example.foodiebuddy.data.Diet
import com.example.foodiebuddy.data.Origin
import com.example.foodiebuddy.data.Recipe
import com.example.foodiebuddy.data.RecipeFilters
import com.example.foodiebuddy.data.getString
import com.example.foodiebuddy.errors.handleError
import com.example.foodiebuddy.navigation.NavigationActions
import com.example.foodiebuddy.navigation.Route
import com.example.foodiebuddy.ui.CustomTextField
import com.example.foodiebuddy.ui.MiniLoading
import com.example.foodiebuddy.ui.PrimaryScreen
import com.example.foodiebuddy.ui.SquareImage
import com.example.foodiebuddy.ui.theme.MyTypography
import com.example.foodiebuddy.viewModels.UserViewModel


@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RecipesHome(userViewModel: UserViewModel, navigationActions: NavigationActions) {

    // pressing the Android back button on this screen does not change it
    BackHandler {
        navigationActions.navigateTo(Route.RECIPES_HOME, true)
    }
    val context = LocalContext.current
    val loading = remember { mutableStateOf(false) }
    val showFilters = remember { mutableStateOf(false) }

    val allRecipesData by userViewModel.allRecipes.collectAsState()
    val allRecipes = remember { mutableStateOf(allRecipesData) }

    val filters = remember { mutableStateOf(RecipeFilters.empty()) }
    val filteredRecipes = remember { mutableStateOf(allRecipes.value) }
    val keywords = remember { mutableStateOf("") }


    LaunchedEffect(Unit) {
        loading.value = true
        userViewModel.fetchUserData({
            if (it) { handleError(context, "Could not fetch user data") }
        }){}
        userViewModel.fetchAllRecipes({
            if (it) { handleError(context, "Could not fetch all recipes") }
        }){
            allRecipes.value = allRecipesData
            loading.value = false
        }
    }
    LaunchedEffect(allRecipesData) {
        loading.value = true
        userViewModel.fetchAllRecipes({
            if (it) { handleError(context, "Could not fetch all recipes") }
        }){
            allRecipes.value = allRecipesData
            loading.value = false
        }
    }
    LaunchedEffect(filters.value, allRecipes.value) {
        // when the list of recipes or the filters are updated -> update the filtered recipes list
        loading.value = true
        if (filters.value.requireOwnedIngredients) {
            userViewModel.recipesWithOwnedIngredients(allRecipes.value.toMutableList(), { if(it) {
                handleError(context, "Could not filter recipes with owned ingredients")
            } }) { recipes ->
                filteredRecipes.value = filterRecipes(recipes, filters.value, userViewModel)
                loading.value = false
            }
        } else {
            filteredRecipes.value = filterRecipes(allRecipes.value, filters.value, userViewModel)
            loading.value = false
        }
        Log.d("Debug", "after filtering, filter is ${filters.value}")
        Log.d("Debug", "after filtering, recipes are ${filteredRecipes.value}")
    }

    if (!showFilters.value) {
        // main app screen with a list of all recipes
        PrimaryScreen(
            navigationActions = navigationActions,
            title = stringResource(R.string.title_recipes),
            navigationIndex = 0,
            topBarIcons = {
                IconButton(
                    onClick = { showFilters.value = !showFilters.value }
                ) {
                    Icon(
                        painterResource(R.drawable.filter),
                        modifier = Modifier.size(28.dp),
                        contentDescription = stringResource(R.string.desc_filters)
                    )
                }

            },
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
                        // if there are no recipes on the app
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
                        // if there are no recipes corresponding to the set of filters
                        } else if (filteredRecipes.value.isEmpty()) {
                            item {
                                Text(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp),
                                    text = stringResource(R.string.txt_noResults),
                                    style = MyTypography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        // display filtered recipes (shows all recipes if filters are empty)
                        else {
                            filteredRecipes.value.forEach { recipe ->
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
                                            // first a row, with first the picture ->
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
    // if the filters are showing
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                Box {
                    CenterAlignedTopAppBar(
                        title = { Text(
                            text = stringResource(R.string.title_filters),
                            style = MyTypography.titleMedium)
                        },
                        // "go back button" that simply hides the filters
                        navigationIcon = {
                            IconButton(
                                onClick = { showFilters.value = false }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.go_back),
                                    contentDescription = stringResource(R.string.desc_goBack)
                                )
                            }
                        },
                        // reset button to emtpy the filters
                        actions = {
                            Text(
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .clickable {
                                        filters.value = RecipeFilters.empty()
                                        keywords.value = ""
                                        showFilters.value = false
                                    },
                                text = stringResource(R.string.button_reset),
                                style = MyTypography.bodySmall
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background, scrolledContainerColor = MaterialTheme.colorScheme.background)
                    )
                }
            },
            // save button always shown at the bottom
            bottomBar = {
                BottomSaveBar(stringResource(R.string.button_seeResults)) {
                    showFilters.value = false
                }
            },
            content = { paddingValues ->  
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // text field to input keywords looked for in the recipes names
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CustomTextField(
                                value = keywords.value,
                                onValueChange = {
                                    keywords.value = it
                                    val splitKeywords = it.split(" ")
                                    filters.value = filters.value.copy(keywords = splitKeywords)
                                },
                                icon = R.drawable.search,
                                placeHolder = stringResource(R.string.field_keywords),
                                singleLine = true,
                                maxLength = 25,
                                width = 300.dp
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.size(16.dp)) }
                    // checkbox to only show favourite recipes
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Checkbox(
                                modifier = Modifier.size(20.dp),
                                checked = filters.value.requireFavourite,
                                onCheckedChange = {
                                    filters.value = filters.value.copy(requireFavourite = it)
                                },
                                colors = CheckboxDefaults.colors(checkmarkColor = MaterialTheme.colorScheme.secondary)
                            )
                            Text(
                                text = stringResource(R.string.txt_favouriteOnly),
                                style = MyTypography.bodyMedium
                            )
                        }
                    }
                    // checkbox to only show recipes for which user has all ingredients
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Checkbox(
                                modifier = Modifier.size(20.dp),
                                checked = filters.value.requireOwnedIngredients,
                                onCheckedChange = {
                                    filters.value = filters.value.copy(requireOwnedIngredients = it)
                                },
                                colors = CheckboxDefaults.colors(checkmarkColor = MaterialTheme.colorScheme.secondary)
                            )
                            Text(
                                text = stringResource(R.string.txt_allIngredientsOnly),
                                style = MyTypography.bodyMedium
                            )
                        }
                    }
                    item { Divider(modifier = Modifier.width(3.dp), color = MaterialTheme.colorScheme.outline) }
                    // menu with all the origin tags
                    item {

                    }
                }
            }
        )
    }
}

/**
 * Save button for filters that always stays at the bottom of the screen.
 *
 * @param saveText text to be displayed in the button
 * @param onSave block to run when pressing the button
 */
@Composable
private fun BottomSaveBar(
    saveText: String,
    onSave: () -> Unit
) {
    NavigationBar(
        modifier = Modifier
            .height(65.dp)
            .fillMaxWidth()
            .padding(top = 0.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
        tonalElevation = 0.dp,
        containerColor = Color.Transparent
    ) {
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onSave() },
            shape = RoundedCornerShape(50)
        ) {
            Text(
                text = saveText,
                style = MyTypography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Filters all recipes that correspond to a given set of filters.
 *
 * @param allRecipes list of Recipe objects to be filtered through
 * @param filters RecipeFilter object with all filters
 * @param userViewModel used to find the current user's favourite recipes
 * @return a new list of filtered Recipe objects
 */
private fun filterRecipes(allRecipes: List<Recipe>, filters: RecipeFilters, userViewModel: UserViewModel): List<Recipe> {
    val userID = userViewModel.getCurrentUserID()
    return allRecipes.filter { recipe ->
        // recipe names that contain the input keywords
        (filters.keywords.isEmpty() || filters.keywords.all { keyword -> recipe.name.contains(keyword, ignoreCase = true) }) &&
                // recipes creators selected
                (filters.authors.isEmpty() || recipe.ownerName in filters.authors) &&
                // recipe origin tags selected
                (filters.origins.isEmpty() || recipe.origin in filters.origins) &&
                // recipe diet tags selected
                (filters.diets.isEmpty() || recipe.diet in filters.diets) &&
                // recipe tags selected
                (filters.tags.isEmpty() || filters.tags.intersect(recipe.tags.toSet()).isNotEmpty()) &&
                // only recipes that this user has in their favourites
                (!filters.requireFavourite || recipe.favouriteOf.contains(userID))
    }
}
