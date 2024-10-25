package com.example.foodiebuddy.ui.ingredients

import android.content.Context
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.foodiebuddy.R
import com.example.foodiebuddy.data.OwnedIngredient
import com.example.foodiebuddy.data.User
import com.example.foodiebuddy.data.UserPersonal
import com.example.foodiebuddy.errors.handleError
import com.example.foodiebuddy.navigation.NavigationActions
import com.example.foodiebuddy.navigation.Route
import com.example.foodiebuddy.ui.DialogWindow
import com.example.foodiebuddy.ui.LoadingAnimation
import com.example.foodiebuddy.ui.LoadingPage
import com.example.foodiebuddy.ui.MiniLoading
import com.example.foodiebuddy.ui.PrimaryScreen
import com.example.foodiebuddy.ui.theme.MyTypography
import com.example.foodiebuddy.viewModels.UserViewModel

@Composable
fun GroceriesHome(userViewModel: UserViewModel, navigationActions: NavigationActions) {
    val screenState = remember { mutableStateOf(ScreenState.VIEWING) }
    val loading = remember { mutableStateOf(false) }
    // pressing the Android back button on this screen does not change it
    BackHandler {
        navigationActions.navigateTo(Route.GROCERIES, true)
        if (screenState.value == ScreenState.EDITING) screenState.value = ScreenState.VIEWING
    }
    val context = LocalContext.current

    val userPersonal by userViewModel.userPersonal.collectAsState()
    val groceries = remember { mutableStateOf(userPersonal.groceryList) }

    val newItems = groceries.value.mapValues { mutableListOf<OwnedIngredient>() }
    val removedItems = groceries.value.mapValues { mutableListOf<String>() }
    val editedCategories = mutableMapOf<String, String>()
    val newCategoryName = remember { mutableStateOf("") }
    val newCategories = remember { mutableStateOf(mapOf<String, MutableList<OwnedIngredient>>()) }
    val removedCategories = remember { mutableStateListOf<String>() }
    val unavailableCategoryNames = groceries.value.keys.toMutableStateList()

    val showAlert = remember { mutableStateOf(false) }
    var deletingCategory = ""


    LaunchedEffect(Unit) {
        screenState.value = ScreenState.LOADING
        userViewModel.fetchUserData({
            if (it) { handleError(context, "Could not fetch user data") }
        }){}
        userViewModel.fetchUserPersonal({
            if (it) { handleError(context, "Could not fetch user personal") }
        }){
            screenState.value = ScreenState.VIEWING
        }
    }

    LaunchedEffect(userPersonal) {
        groceries.value = userPersonal.groceryList.toMutableMap()
        Log.d("Debug", "Groceries: userPersonal is $userPersonal")
        Log.d("Debug", "groceries contain ${groceries.value}")
    }

    if (loading.value) {
        LoadingPage()
    } else {
        PrimaryScreen(
            navigationActions = navigationActions,
            title = stringResource(R.string.title_groceries),
            navigationIndex = 1,
            topBarIcons = {},
            userViewModel = userViewModel,
            floatingButton = { FloatingButton(screenState) {
                loading.value = true
                loadModifications(userViewModel, userPersonal, {it.groceryList}, false, context, loading, groceries, newItems, removedItems, editedCategories, newCategories, removedCategories)
            }},
            content = { paddingValues ->
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
                            clearTemporaryModifications(userPersonal, groceries, {it.groceryList}, newItems, removedItems, editedCategories, newCategories, removedCategories, unavailableCategoryNames)

                            if (groceries.value.isNotEmpty() || groceries.value.any { it.value.isNotEmpty() }) {
                                items(groceries.value.toSortedMap().keys.toList(), key = {it}) { category ->
                                    if (groceries.value[category]?.isNotEmpty() == true) {
                                        IngredientCategoryView(category, groceries.value[category] ?: mutableListOf(), true) { ingredient, isTicked ->
                                            userViewModel.updateIngredientTick(ingredient.uid, isTicked, {
                                                if (it) { handleError(context, "Could not update ingredient") }
                                            }) {}
                                        }
                                    }
                                }
                            } else {
                                item {
                                    Text(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 16.dp),
                                        text = stringResource(R.string.txt_emptyGroceries),
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
                            items(newCategories.value.keys.reversed().toList(), key = {it})  { category ->
                                IngredientCategoryEdit(
                                    category,
                                    true,
                                    newCategories.value[category] ?: mutableListOf(),
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
                                    }
                                )
                            }
                            items(groceries.value.toSortedMap().keys.toList(), key = {it}) { category ->
                                if (category !in removedCategories) {
                                    IngredientCategoryEdit(
                                        category,
                                        true,
                                        groceries.value[category] ?: mutableListOf(),
                                        newItems[category] ?: mutableListOf(),
                                        editedCategories,
                                        onRemoveItem = { uid, _ ->
                                            removedItems[category]?.add(uid)
                                        },
                                        onRemoveCategory = {
                                            deletingCategory = it
                                            showAlert.value = true
                                        }
                                    )
                                }
                            }
                        }

                    }
                }
                if (showAlert.value) {
                    DialogWindow(
                        visible = showAlert,
                        content = stringResource(R.string.alert_deleteCatGroceries),
                        confirmText = stringResource(R.string.button_delete),
                        confirmColour = Color.Red,
                        additionOnDismiss = { deletingCategory = "" }
                    ) {
                        removedCategories.add(deletingCategory)
                        groceries.value = groceries.value.filterKeys { key -> key !in removedCategories }
                        unavailableCategoryNames.remove(deletingCategory)
                        deletingCategory = ""
                        showAlert.value = false
                    }
                }
            }
        )
    }
}

fun clearTemporaryModifications(
    userPersonal: UserPersonal,
    list: MutableState<Map<String, List<OwnedIngredient>>>,
    fieldToRead: (UserPersonal) -> Map<String, List<OwnedIngredient>>,
    newItems: Map<String, MutableList<OwnedIngredient>>,
    removedItems: Map<String, MutableList<String>>,
    editedCategories: MutableMap<String, String>,
    newCategories: MutableState<Map<String, MutableList<OwnedIngredient>>>,
    removedCategories: SnapshotStateList<String>,
    unavailableCategoryNames: SnapshotStateList<String>
) {
    list.value = fieldToRead(userPersonal).toMutableMap()
    newItems.forEach { (_, value) -> value.clear() }
    removedItems.forEach { (_, value) -> value.clear() }
    editedCategories.clear()
    val mutableNewCategories = newCategories.value.toMutableMap().also {it.clear()}
    newCategories.value = mutableNewCategories
    removedCategories.clear()
    unavailableCategoryNames.clear()
    unavailableCategoryNames.addAll(list.value.keys)
}

fun loadModifications(
    userViewModel: UserViewModel,
    userPersonal: UserPersonal,
    fieldToRead: (UserPersonal) -> Map<String, List<OwnedIngredient>>,
    isInFridge: Boolean,
    context: Context,
    loading: MutableState<Boolean>,
    list: MutableState<Map<String, List<OwnedIngredient>>>,
    newItems: Map<String, MutableList<OwnedIngredient>>,
    removedItems: Map<String, MutableList<String>>,
    editedCategories: MutableMap<String, String>,
    newCategories: MutableState<Map<String, MutableList<OwnedIngredient>>>,
    removedCategories: SnapshotStateList<String>
) {
    var remainingUpdates = 4
    userViewModel.removeIngredients(removedItems, {
        if (it) handleError(context, "Could not remove ingredient")
    }) {
        remainingUpdates--
        Log.d("Debug", "came back from removing ingredients")

        if (remainingUpdates <= 0) {
            userViewModel.fetchUserPersonal({
                if (it) { handleError(context, "Could not fetch user personal") }
            }){
                list.value = fieldToRead(userPersonal)
                loading.value = false
            }
        }
    }
    userViewModel.addIngredients(newItems, isInFridge, {
        if (it) handleError(context, "Could not update owned ingredients list")
    }) {
        remainingUpdates--
        Log.d("Debug", "came back from adding ingredients")

        if (remainingUpdates <= 0) {
            userViewModel.fetchUserPersonal({
                if (it) { handleError(context, "Could not fetch user personal") }
            }){
                list.value = fieldToRead(userPersonal)
                loading.value = false
            }
        }
    }
    userViewModel.updateCategories(newCategories.value, editedCategories, {
        if (it) {handleError(context, "Could not update category names")}
    }) {
        remainingUpdates--
        Log.d("Debug", "came back from updating categories")

        if (remainingUpdates <= 0) {
            userViewModel.fetchUserPersonal({
                if (it) { handleError(context, "Could not fetch user personal") }
            }){
                list.value = fieldToRead(userPersonal)
                loading.value = false
            }
        }
    }
    userViewModel.deleteCategories(removedCategories, {
        if (it) {handleError(context, "Could not update category names")}
    }) {
        remainingUpdates--
        Log.d("Debug", "came back from deleting categories")

        if (remainingUpdates <= 0) {
            userViewModel.fetchUserPersonal({
                if (it) { handleError(context, "Could not fetch user personal") }
            }){
                list.value = fieldToRead(userPersonal)
                loading.value = false
            }
        }
    }
}