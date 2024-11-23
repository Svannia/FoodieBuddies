package com.example.foodiebuddy.ui.ingredients

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.foodiebuddy.R
import com.example.foodiebuddy.data.OwnedIngredient
import com.example.foodiebuddy.errors.handleError
import com.example.foodiebuddy.navigation.NavigationActions
import com.example.foodiebuddy.navigation.Route
import com.example.foodiebuddy.ui.DialogWindow
import com.example.foodiebuddy.ui.LoadingPage
import com.example.foodiebuddy.ui.MiniLoading
import com.example.foodiebuddy.ui.PrimaryScreen
import com.example.foodiebuddy.ui.theme.MyTypography
import com.example.foodiebuddy.viewModels.UserViewModel

@Composable
fun FridgeHome(userViewModel: UserViewModel, navigationActions: NavigationActions) {
    val screenState = remember { mutableStateOf(ScreenState.VIEWING) }
    val loading = remember { mutableStateOf(false) }

    // pressing the Android back button on this screen does not change it
    BackHandler {
        navigationActions.navigateTo(Route.FRIDGE, true)
        if (screenState.value == ScreenState.EDITING) screenState.value = ScreenState.VIEWING
    }

    val context = LocalContext.current

    val userPersonal by userViewModel.userPersonal.collectAsState()
    val fridge = remember { mutableStateOf(userPersonal.fridge) }

    // these variables hold all the modifications the user is making before they are bulked updated if the user saves the modifications
    val newItems = fridge.value.mapValues { mutableListOf<OwnedIngredient>() }
    val removedItems = fridge.value.mapValues { mutableListOf<String>() }
    val editedCategories = mutableMapOf<String, String>()
    val newCategoryName = remember { mutableStateOf("") }
    val newCategories = remember { mutableStateOf(mapOf<String, MutableList<OwnedIngredient>>()) }
    val removedCategories = remember { mutableStateListOf<String>() }
    val unavailableCategoryNames = fridge.value.keys.toMutableStateList()
    val newGroceryItems = fridge.value.mapValues { mutableListOf<OwnedIngredient>() }.toMutableMap()

    val showCategoryAlert = remember { mutableStateOf(false) }
    var deletingCategory = ""
    val showDeleteAlert = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        screenState.value = ScreenState.LOADING
        userViewModel.fetchUserData({
            if (it) { handleError(context, "Could not fetch user data") }
        }){}
        userViewModel.fetchUserPersonal({
            if (it) {
                handleError(context, "Could not fetch user personal")
                screenState.value = ScreenState.VIEWING
            }
        }){
            screenState.value = ScreenState.VIEWING
        }
    }

    LaunchedEffect(userPersonal) {
        fridge.value = userPersonal.fridge.toMutableMap()
    }

    if (loading.value) {
        LoadingPage()
    } else {
        PrimaryScreen(
            navigationActions = navigationActions,
            title = stringResource(R.string.title_fridge),
            navigationIndex = 2,
            topBarIcons = { OptionsMenu(
                stringResource(R.string.button_clearList) to { showDeleteAlert.value = true }
            ) },
            userViewModel = userViewModel,
            floatingButton = { FloatingButton(screenState) {
                loading.value = true
                // add the ingredients sent from fridge to grocery list
                userViewModel.addIngredients(newGroceryItems, false, {
                    if (it) handleError(context, "Could not update owned ingredients list")
                }) {
                    loadModifications(userViewModel, userPersonal, fridge, {it.fridge}, true, context, loading, newItems, removedItems, editedCategories, newCategories, removedCategories)
                }
            }},
            content = {paddingValues ->
                when (screenState.value) {
                    ScreenState.LOADING -> {
                        MiniLoading(paddingValues)
                    }
                    ScreenState.VIEWING -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                        ) {
                            clearTemporaryModifications(userPersonal, fridge, {it.fridge}, newItems, removedItems, editedCategories, newCategories, removedCategories, unavailableCategoryNames)
                            newGroceryItems.forEach { (_, value) -> value.clear() }

                            if (fridge.value.isNotEmpty() && !fridge.value.all { it.value.isEmpty() }) {
                                items(fridge.value.toSortedMap().keys.toList(), key = {it}) { category ->
                                    if (fridge.value[category]?.isNotEmpty() == true) {
                                        IngredientCategoryView(category, fridge.value[category] ?: mutableListOf(), false)
                                    }
                                }
                            } else {
                                item {
                                    Text(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 16.dp),
                                        text = stringResource(R.string.txt_emptyFridge),
                                        style = MyTypography.bodyMedium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                    ScreenState.EDITING -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                        ) {
                            newCategoryName.value = ""
                            item {
                                AddCategory(newCategoryName, newCategories, unavailableCategoryNames, context)
                            }

                            // new categories are displayed first, in added order, to make editing easier
                            items(newCategories.value.keys.reversed().toList(), key = {it})  { category ->
                                IngredientCategoryEdit(
                                    category,
                                    newCategories.value[category] ?: mutableListOf(),
                                    false,
                                    newCategories.value[category] ?: mutableListOf(),
                                    editedCategories,
                                    onRemoveItem = { _, name ->
                                        newCategories.value[category]?.removeIf { ingredient ->
                                            ingredient.displayedName == name
                                        }
                                    },
                                    onRemoveCategory = {
                                        val mutableNewCategories = newCategories.value.toMutableMap()
                                        mutableNewCategories.remove(it)
                                        newCategories.value = mutableNewCategories
                                        unavailableCategoryNames.remove(it)
                                        newGroceryItems.remove(category)
                                    },
                                    context,
                                    userViewModel,
                                    newGroceryItems
                                )
                            }

                            // existing categories are display next in alphabetical order
                            items(fridge.value.toSortedMap().keys.toList(), key = {it}) { category ->
                                if (category !in removedCategories) {
                                    IngredientCategoryEdit(
                                        category,
                                        fridge.value[category] ?: mutableListOf(),
                                        false,
                                        newItems[category] ?: mutableListOf(),
                                        editedCategories,
                                        onRemoveItem = { uid, _ ->
                                            removedItems[category]?.add(uid)
                                        },
                                        onRemoveCategory = {
                                            deletingCategory = it
                                            showCategoryAlert.value = true
                                        },
                                        context,
                                        userViewModel,
                                        newGroceryItems
                                    )
                                }
                            }
                        }
                    }
                }
                if (showCategoryAlert.value) {
                    DialogWindow(
                        visible = showCategoryAlert,
                        content = stringResource(R.string.alert_deleteCatFridge),
                        confirmText = stringResource(R.string.button_delete),
                        confirmColour = Color.Red,
                        additionOnDismiss = { deletingCategory = "" }
                    ) {
                        removedCategories.add(deletingCategory)
                        fridge.value = fridge.value.filterKeys { key -> key !in removedCategories }
                        unavailableCategoryNames.remove(deletingCategory)
                        newGroceryItems.remove(deletingCategory)
                        deletingCategory = ""
                        showCategoryAlert.value = false
                    }
                }
                if (showDeleteAlert.value) {
                    DialogWindow(
                        visible = showDeleteAlert,
                        content = stringResource(R.string.alert_clearList),
                        confirmText = stringResource(R.string.button_delete),
                        confirmColour = Color.Red
                    ) {
                        showDeleteAlert.value = false
                        loading.value = true
                        userViewModel.clearIngredients(true, {
                            if (it) {
                                handleError(context, "Failed to clear fridge")
                                loading.value = false
                            }
                        }) {
                            userViewModel.fetchUserPersonal({
                                if (it) {
                                    handleError(context, "Could not fetch user personal")
                                    loading.value = false
                                }
                            }) {
                                loading.value = false
                            }
                        }
                    }
                }
            }
        )
    }
}