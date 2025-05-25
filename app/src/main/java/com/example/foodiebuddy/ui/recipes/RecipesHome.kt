package com.example.foodiebuddy.ui.recipes

import android.content.Context
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.foodiebuddy.R
import com.example.foodiebuddy.data.Diet
import com.example.foodiebuddy.data.Origin
import com.example.foodiebuddy.data.Recipe
import com.example.foodiebuddy.data.RecipeFilters
import com.example.foodiebuddy.data.Tag
import com.example.foodiebuddy.data.User
import com.example.foodiebuddy.data.getString
import com.example.foodiebuddy.errors.handleError
import com.example.foodiebuddy.navigation.NavigationActions
import com.example.foodiebuddy.navigation.Route
import com.example.foodiebuddy.ui.CustomTextField
import com.example.foodiebuddy.ui.MiniLoading
import com.example.foodiebuddy.ui.OptionsMenu
import com.example.foodiebuddy.ui.PrimaryScreen
import com.example.foodiebuddy.ui.SquareImage
import com.example.foodiebuddy.ui.theme.MyTypography
import com.example.foodiebuddy.viewModels.UserViewModel


@OptIn(ExperimentalLayoutApi::class)
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

    val filters by userViewModel.filters.collectAsState()
    val filteredRecipes by userViewModel.filteredRecipes.collectAsState()
    val keywords = remember { mutableStateOf("") }

    val userData by userViewModel.userData.collectAsState()
    val allCreators = remember { mutableStateOf(emptyList<Pair<String, String>>()) }

    LaunchedEffect(Unit) {
        loading.value = true
        userViewModel.fetchUserData({
            if (it) { handleError(context, "Could not fetch user data") }
        }){
            userViewModel.fetchAllUsers({
                if (it) { handleError(context, "Could not fetch all users") }
            }){
                allCreators.value = buildCreatorsList(context, userData, userViewModel.allUsers.value)
            }
        }
        userViewModel.fetchAllRecipes({
            if (it) {
                handleError(context, "Could not fetch all recipes")
                loading.value = false
            }
        }){
            allRecipes.value = allRecipesData
            loading.value = false
        }
    }
    LaunchedEffect(allRecipesData) {
        loading.value = true
        userViewModel.fetchAllRecipes({
            if (it) {
                handleError(context, "Could not fetch all recipes")
                loading.value = false
            }
        }){
            allRecipes.value = allRecipesData
            loading.value = false
        }
    }
    LaunchedEffect(filters, allRecipes.value) {
        // when the list of recipes or the filters are updated -> update the filtered recipes list
        loading.value = true
        if (filters.requireOwnedIngredients) {
            userViewModel.recipesWithOwnedIngredients(allRecipes.value.toMutableList(), {
                if(it) {
                    handleError(context, "Could not filter recipes with owned ingredients")
                    loading.value = false
                }
            }) { recipes ->
                userViewModel.updateFilteredRecipes(filterRecipes(recipes, filters, userViewModel))
                loading.value = false
            }
        } else {
            userViewModel.updateFilteredRecipes(filterRecipes(allRecipes.value, filters, userViewModel))
            loading.value = false
        }
    }

    if (!showFilters.value) {
        // main app screen with a list of all recipes
        PrimaryScreen(
            navigationActions = navigationActions,
            title = stringResource(R.string.title_recipes),
            navigationIndex = 0,
            topBarIcons = {
                Row(
                    modifier = Modifier.padding(0.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // filters button
                    IconButton(
                        onClick = { showFilters.value = !showFilters.value }
                    ) {
                        Icon(
                            painterResource(R.drawable.filter),
                            modifier = Modifier.size(28.dp),
                            contentDescription = stringResource(R.string.desc_filters)
                        )
                    }
                    // options menu
                    OptionsMenu(R.drawable.options,
                        stringResource(R.string.button_newRecipe) to { navigationActions.navigateTo(Route.RECIPE_CREATE) },
                        stringResource(R.string.button_drafts) to { navigationActions.navigateTo(Route.DRAFTS) }
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
                                    style = MyTypography.bodyLarge,
                                    textAlign = TextAlign.Center
                                )
                            }
                        // if there are no recipes corresponding to the set of filters
                        } else if (filteredRecipes.isEmpty()) {
                            item {
                                Text(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp),
                                    text = stringResource(R.string.txt_noResults),
                                    style = MyTypography.bodyLarge,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        // display filtered recipes (shows all recipes if filters are empty)
                        else {
                            filteredRecipes.forEach { recipe ->
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
                                                if (recipe.pictures.isNotEmpty()) {
                                                    SquareImage(
                                                        size = 68.dp,
                                                        picture = recipe.pictures[0],
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
                                                        style = MyTypography.bodyLarge
                                                    )
                                                    // creator of the recipe
                                                    val ownerName = allCreators.value.find { it.first == recipe.owner }?.second ?: ""
                                                    Text(
                                                        text = stringResource(R.string.txt_recipeCreator, ownerName),
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
                                        }
                                    }
                                    Divider(color = MaterialTheme.colorScheme.outline, thickness = 3.dp)
                                }
                            }
                        }
                    }
                }
            }
        )
    } else {
        // if the filters are showing
        RecipeSecondaryScreen(
            title = stringResource(R.string.title_filters),
            onGoBack = { showFilters.value = false },
            actions = {
                Text(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .clickable {
                            userViewModel.updateFilters(RecipeFilters.empty())
                            keywords.value = ""
                            showFilters.value = false
                        },
                    text = stringResource(R.string.button_reset),
                    style = MyTypography.bodySmall
                )
            },
            bottomBar = {
                BottomSaveBar(stringResource(R.string.button_seeResults), true) {
                    showFilters.value = false
                }
            }
        )
        { paddingValues ->
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
                                userViewModel.updateFilters(filters.copy(keywords = splitKeywords))
                            },
                            icon = R.drawable.search,
                            placeHolder = stringResource(R.string.field_keywords),
                            singleLine = true,
                            maxLength = 25,
                            showMaxChara = false,
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
                            checked = filters.requireFavourite,
                            onCheckedChange = {
                                userViewModel.updateFilters(filters.copy(requireFavourite = it))
                            },
                            colors = CheckboxDefaults.colors(checkmarkColor = MaterialTheme.colorScheme.secondary)
                        )
                        Text(
                            text = stringResource(R.string.txt_favouriteOnly),
                            style = MyTypography.bodyLarge
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
                            checked = filters.requireOwnedIngredients,
                            onCheckedChange = {
                                userViewModel.updateFilters(filters.copy(requireOwnedIngredients = it))
                            },
                            colors = CheckboxDefaults.colors(checkmarkColor = MaterialTheme.colorScheme.secondary)
                        )
                        Text(
                            text = stringResource(R.string.txt_allIngredientsOnly),
                            style = MyTypography.bodyLarge
                        )
                    }
                }
                item { Divider(thickness = 3.dp, color = MaterialTheme.colorScheme.outline) }
                // menu with all the tags
                item {
                    TagDropDown(
                        title = stringResource(R.string.title_tag),
                        tags = Tag.entries.drop(1).toList(),
                        filtersSet = filters.tags,
                        getString = { tag -> tag.getString(context) }
                    ) { tag ->
                        val tags = filters.tags.toMutableSet()
                        if (tags.contains(tag)) tags.remove(tag)
                        else tags.add(tag)
                        userViewModel.updateFilters(filters.copy(tags = tags))
                    }
                }
                item { Divider(thickness = 3.dp, color = MaterialTheme.colorScheme.outline) }
                // menu with all the origin tags
                item {
                    TagDropDown(
                        title = stringResource(R.string.title_origin),
                        tags = Origin.entries.drop(1).toList(),
                        filtersSet = filters.origins,
                        getString = { origin -> origin.getString(context) }
                    ) { origin ->
                        val origins = filters.origins.toMutableSet()
                        if (origins.contains(origin)) origins.remove(origin)
                        else origins.add(origin)
                        userViewModel.updateFilters(filters.copy(origins = origins))
                    }
                }
                item { Divider(thickness = 3.dp, color = MaterialTheme.colorScheme.outline) }
                // menu with all the diet tags
                item {
                    TagDropDown(
                        title = stringResource(R.string.title_diet),
                        tags = Diet.entries.drop(1).toList(),
                        filtersSet = filters.diets,
                        getString = { diet -> diet.getString(context) }
                    ) { diet ->
                        val diets = filters.diets.toMutableSet()
                        if (diets.contains(diet)) diets.remove(diet)
                        else diets.add(diet)
                        userViewModel.updateFilters(filters.copy(diets = diets))
                    }
                }
                item { Divider(thickness = 3.dp, color = MaterialTheme.colorScheme.outline) }
                // menu with all the creators
                item {
                    TagDropDown(
                        title = stringResource(R.string.title_creator),
                        tags = allCreators.value.map { it.first },
                        filtersSet = filters.authors,
                        getString = { authorUID ->
                            allCreators.value.find { it.first == authorUID }?.second ?: ""
                        }
                    ) { authorUID ->
                        val authors = filters.authors.toMutableSet()
                        if (authors.contains(authorUID)) authors.remove(authorUID)
                        else authors.add(authorUID)
                        userViewModel.updateFilters(filters.copy(authors = authors))
                    }
                }
                item { Divider(thickness = 3.dp, color = MaterialTheme.colorScheme.outline) }
            }
        }
        BackHandler {
            showFilters.value = false
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
                (filters.authors.isEmpty() || recipe.owner in filters.authors) &&
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

/**
 * Lists all creators with their ID's and usernames, with the current user being first in the list and their username changed to "Me".
 *
 * @param context used to access the string resources
 * @param userData User object of the current user
 * @param allUsersData list of User objects for all other users
 * @return list of pairs where each pair represents a user: first key is the UID, second key is the username
 */
private fun buildCreatorsList(context: Context, userData: User, allUsersData: List<User>): List<Pair<String, String>> {
    val list = mutableListOf(Pair(userData.uid, context.getString(R.string.txt_me)))
    allUsersData.forEach { user ->
        list.add(Pair(user.uid, user.username))
    }
    return list
}