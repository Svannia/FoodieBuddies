package com.example.foodiebuddy.ui.recipes

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.input.pointer.motionEventSpy
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
                    OptionsMenu(
                        stringResource(R.string.button_newRecipe) to { navigationActions.navigateTo("${Route.RECIPE_EDIT}/") },
                        stringResource(R.string.button_drafts) to {}
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
                        } else if (filteredRecipes.isEmpty()) {
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
                                        userViewModel.updateFilters(RecipeFilters.empty())
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
                                checked = filters.requireOwnedIngredients,
                                onCheckedChange = {
                                    userViewModel.updateFilters(filters.copy(requireOwnedIngredients = it))
                                },
                                colors = CheckboxDefaults.colors(checkmarkColor = MaterialTheme.colorScheme.secondary)
                            )
                            Text(
                                text = stringResource(R.string.txt_allIngredientsOnly),
                                style = MyTypography.bodyMedium
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
 * Menu to display all filters of a same tag family.
 * When minimized, the tags lay on a single line that can be swiped,
 * and when expanded they are all displayed on the screen.
 *
 * @param title name of the tag family
 * @param tags all entries of a tag enum
 * @param filtersSet the current set of filters for this tag family
 * @param getString the getString function that belongs to this enum of tags
 * @param onClick block that runs with the tag that got pressed
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> TagDropDown(
    title: String,
    tags: List<T>,
    filtersSet: Set<T>,
    getString: (T) -> String,
    onClick: (T) -> Unit
) {
    val expanded = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Menu header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 0.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // name of the tag family
            Text(
                text = title,
                style = MyTypography.titleSmall
            )
            // button to expand or minimize the menu of tags
            IconButton(onClick = { expanded.value = !expanded.value }) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = if (expanded.value) painterResource(R.drawable.up)
                        else painterResource(R.drawable.down),
                    contentDescription = stringResource(R.string.desc_dropDownMenu)
                )
            }
        }
        // if expanded -> show all tags at once
        if (expanded.value) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tags.forEach { tag ->
                    TagButton(getString(tag) , filtersSet.contains(tag)) {
                        onClick(tag)
                    }
                }
            }
        // if not expanded -> collapsed lazy row view
        } else {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tags) { tag ->
                    TagButton(getString(tag) , filtersSet.contains(tag)) {
                        onClick(tag)
                    }
                }
            }
        }
    }
}

/**
 * Oval button with a similar look to the TagLabel.
 *
 * @param tagName name of the tag on the button
 * @param enabled whether this button is initially selected (among filters)
 * @param onClick block to run when clicking the button
 * (change of look already implemented inside this function)
 */
@Composable
private fun TagButton(
    tagName: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val selected = remember { mutableStateOf(enabled) }
    Box(
        modifier = Modifier
            .background(
                color = if (selected.value) MaterialTheme.colorScheme.inversePrimary
                else Color.Transparent,
                shape = RoundedCornerShape(50)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.inversePrimary,
                shape = RoundedCornerShape(50)
            )
            .clickable {
                selected.value = !selected.value
                onClick()
            }
    ) {
        Text(
            text = tagName,
            style = MyTypography.bodySmall,
            color = if (selected.value) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.inversePrimary,
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 12.dp)
        )
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

private fun buildCreatorsList(context: Context, userData: User, allUsersData: List<User>): List<Pair<String, String>> {
    val list = mutableListOf(Pair(userData.uid, context.getString(R.string.txt_me)))
    allUsersData.forEach { user ->
        list.add(Pair(user.uid, user.username))
    }
    return list
}