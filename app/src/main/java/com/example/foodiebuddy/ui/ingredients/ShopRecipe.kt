package com.example.foodiebuddy.ui.ingredients

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import com.example.foodiebuddy.R
import com.example.foodiebuddy.data.OwnedIngredient
import com.example.foodiebuddy.data.Recipe
import com.example.foodiebuddy.data.RecipeIngredient
import com.example.foodiebuddy.errors.handleError
import com.example.foodiebuddy.navigation.NavigationActions
import com.example.foodiebuddy.navigation.Route
import com.example.foodiebuddy.ui.CustomTextField
import com.example.foodiebuddy.ui.LoadingPage
import com.example.foodiebuddy.ui.recipes.BottomSaveBar
import com.example.foodiebuddy.ui.recipes.RecipeSecondaryScreen
import com.example.foodiebuddy.ui.theme.MyTypography
import com.example.foodiebuddy.viewModels.RecipeViewModel
import com.example.foodiebuddy.viewModels.UserViewModel

private const val PLACEHOLDER = "-"
@Composable
fun ShopRecipe(userVM: UserViewModel, recipeVM: RecipeViewModel, navigationActions: NavigationActions) {
    val context = LocalContext.current
    val loadingData = remember { mutableStateOf(false) }

    val recipeData by recipeVM.recipeData.collectAsState()
    val userPersonal by userVM.userPersonal.collectAsState()

    val ingredients = remember { mutableStateListOf<RecipeIngredient>() }
    val groceries = remember { mutableStateOf(userPersonal.groceryList) }
    val fridge = remember { mutableStateOf(userPersonal.fridge) }
    val newNames = remember { mutableStateListOf<String>() }
    val existingIngredients = remember { mutableStateMapOf<String, List<Triple<Boolean, String, String>>>() }
    val ingredientsToAdd = remember { mutableStateListOf<OwnedIngredient>() }
    val isEnabled = remember { mutableStateOf(ingredientsToAdd.all { it.category != PLACEHOLDER } && ingredientsToAdd.isNotEmpty()) }


    LaunchedEffect(Unit) {
        loadingData.value = true
        recipeVM.fetchRecipeData({
            if (it) {
                handleError(context, "Could not fetch recipe data")
                loadingData.value = false
            }
        }) {
            val recipe = recipeVM.recipeData.value
            if (recipe != Recipe.empty()) {
                ingredients.clear()
                ingredients.addAll(recipe.ingredients)
                newNames.clear()
                newNames.addAll(ingredients.map { it.displayedName })
                ingredientsToAdd.addAll(recipe.ingredients.map { it.toOwned(PLACEHOLDER) })
                userVM.fetchUserPersonal({
                    if (it) {
                        handleError(context, "Could not fetch user personal")
                        loadingData.value = false
                    }
                }) {
                    ingredients.forEach { ingredient ->
                        userVM.ingredientExistsWhere(ingredient.standName, {
                            if (it) {
                                handleError(context, "Could not check for ingredient existence")
                                loadingData.value = false
                            }
                        }) { exists, matches ->
                            if (exists) {
                                existingIngredients[ingredient.displayedName] = matches
                                ingredientsToAdd.removeIf { it.displayedName == ingredient.displayedName }
                            }
                        }
                    }
                    loadingData.value = false
                }
            } else {
                handleError(context, "Could not fetch data, recipe is empty")
            }
        }
    }

    LaunchedEffect(recipeData) {
        if (recipeData != Recipe.empty()) {
            ingredients.clear()
            ingredients.addAll(recipeData.ingredients)
            newNames.clear()
            newNames.addAll(ingredients.map { it.displayedName })
        }
    }

    LaunchedEffect(userPersonal) {
        groceries.value = userPersonal.groceryList.toMutableMap()
        fridge.value = userPersonal.fridge.toMutableMap()
    }

    if (loadingData.value) LoadingPage()
    else {
        RecipeSecondaryScreen(
            title = stringResource(R.string.title_recipeIngredients),
            onGoBack = { navigationActions.goBack() },
            actions = {},
            bottomBar = {
                BottomSaveBar(stringResource(R.string.button_shop), isEnabled.value) {
                    loadingData.value = true
                    ingredientsToAdd.forEach { ingredient ->
                        val index = ingredients.indexOfFirst { it.displayedName == ingredient.displayedName }
                        val newName = newNames[index]
                        if (newName.isNotBlank()) {
                            ingredient.displayedName = newName
                            ingredient.standName = standardizeName(newName)
                        }
                    }
                    val newItems: Map<String, List<OwnedIngredient>> = ingredientsToAdd.groupBy { it.category }
                    userVM.addIngredients(newItems, false, {
                        if (it) {
                            handleError(context, "Could not add new ingredients to groceries")
                            loadingData.value = false
                        }
                    }) {
                        navigationActions.navigateTo(Route.GROCERIES)
                        loadingData.value = false
                    }
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(ingredients) { index, ingredient ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // editable variables for this ingredient
                        val ingredientExists =
                            existingIngredients.keys.contains(ingredient.displayedName)
                        val isTicked = remember { mutableStateOf(!ingredientExists) }
                        val ownedIngredients =
                            remember { mutableStateListOf<Triple<Boolean, String, String>>() }
                        val items = remember { mutableStateOf("") }
                        LaunchedEffect(ingredientExists) {
                            isTicked.value = !ingredientExists
                            if (ingredientExists) {
                                ownedIngredients.addAll(
                                    existingIngredients[ingredient.displayedName] ?: emptyList()
                                )
                                val ownedItems = ownedIngredients.map { item ->
                                    val location =
                                        if (item.first) context.getString(R.string.txt_yourFridge)
                                        else context.getString(R.string.txt_yourGroceries)
                                    context.getString(
                                        R.string.txt_existingIngredient,
                                        item.third,
                                        item.second,
                                        location
                                    )
                                }
                                items.value =
                                    context.getString(R.string.txt_youHave) + ownedItems.joinToString(
                                        ", "
                                    )
                            }
                        }
                        val chosenCategory = remember { mutableStateOf(
                                if (isTicked.value) ingredientsToAdd.find { it.displayedName == ingredient.displayedName }?.category
                                    ?: PLACEHOLDER
                                else PLACEHOLDER
                        )}

                        // checkbox that is automatically unchecked if a similar enough ingredient is found in fridge/groceries
                        Checkbox(
                            checked = isTicked.value,
                            onCheckedChange = {
                                isTicked.value = !isTicked.value
                                if (!isTicked.value) {
                                    ingredientsToAdd.removeIf { it.displayedName == ingredient.displayedName }
                                    chosenCategory.value = PLACEHOLDER
                                } else {
                                    ingredientsToAdd.add(ingredient.toOwned(PLACEHOLDER))
                                }
                                isEnabled.value = ingredientsToAdd.all { it.category != PLACEHOLDER } && ingredientsToAdd.isNotEmpty()
                            }
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 0.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // name of the ingredient
                            CustomTextField(
                                value = newNames[index],
                                onValueChange = { newNames[index] = it },
                                icon = -1,
                                placeHolder = ingredient.displayedName,
                                singleLine = true,
                                maxLength = 21,
                                showMaxChara = false,
                                width = 250.dp
                            )

                            // dropdown menu to choose in which category to put the ingredient
                            val expanded = remember { mutableStateOf(false) }
                            Box{
                                OutlinedTextField(
                                    modifier = Modifier
                                        .width(250.dp)
                                        .clickable { expanded.value = true },
                                    value = chosenCategory.value,
                                    textStyle = MyTypography.bodyMedium,
                                    onValueChange = {},
                                    label = { Text(stringResource(R.string.txt_category)) },
                                    enabled = false,
                                    colors = TextFieldDefaults.colors(
                                        disabledIndicatorColor = MaterialTheme.colorScheme.primary,
                                        disabledPlaceholderColor = MaterialTheme.colorScheme.inversePrimary,
                                        disabledTextColor = MaterialTheme.colorScheme.inversePrimary,
                                        disabledContainerColor = Color.Transparent
                                    )
                                )
                                DropdownMenu(
                                    modifier = Modifier.clickable { expanded.value = true },
                                    expanded = expanded.value,
                                    onDismissRequest = { expanded.value = false }
                                ) {
                                    groceries.value.keys.forEach { category ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = category,
                                                    style = MyTypography.bodyMedium
                                                )
                                            },
                                            onClick = {
                                                chosenCategory.value = category
                                                val alreadyInList = ingredientsToAdd.find { it.displayedName == ingredient.displayedName }
                                                if (alreadyInList != null) alreadyInList.category = category
                                                else ingredientsToAdd.add(ingredient.toOwned(category))
                                                isTicked.value = true
                                                isEnabled.value = ingredientsToAdd.all { it.category != PLACEHOLDER } && ingredientsToAdd.isNotEmpty()
                                                expanded.value = false
                                            }
                                        )
                                    }
                                }
                            }

                            // message explaining if some ingredient is already owned
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = if (ingredientExists) {
                                    items.value
                                } else "",
                                style = MyTypography.bodySmall
                            )

                            Spacer(modifier = Modifier.size(8.dp))
                        }
                    }
                }
                // spacing for the keyboard (cuz doing things properly with ime paddings fucks things up)
                item {
                    Spacer(modifier = Modifier
                        .fillMaxWidth()
                        .height(
                            (WindowInsets.ime
                                .asPaddingValues()
                                .calculateBottomPadding()
                                    - paddingValues.calculateBottomPadding())
                                .coerceAtLeast(0.dp)
                        ))
                }
            }
        }
    }
}