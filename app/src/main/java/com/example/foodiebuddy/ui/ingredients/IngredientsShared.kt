package com.example.foodiebuddy.ui.ingredients

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.foodiebuddy.R
import com.example.foodiebuddy.data.OwnedIngredient
import com.example.foodiebuddy.data.UserPersonal
import com.example.foodiebuddy.errors.handleError
import com.example.foodiebuddy.ui.CustomTextField
import com.example.foodiebuddy.ui.theme.MyTypography
import com.example.foodiebuddy.viewModels.UserViewModel

private const val HEIGHT = 42
private const val OFFSET = 45
private const val INLINE_ICON = 20
private const val MAX_CHARA = 21

enum class ScreenState { LOADING, VIEWING, EDITING }

// shared UI components

/**
 * Floating button that changes functionality whether the user is currently viewing or editing their ingredients.
 *
 * @param screenState ScreenState to switch between the button's functionalities
 * @param onSave block that runs when the user is in Editing mode and saves their modifications
 */
@Composable
fun FloatingButton(
    screenState: MutableState<ScreenState>,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(6.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.End
    ) {
        Button(
            modifier = Modifier
                .size(58.dp)
                .clip(MaterialTheme.shapes.medium),
            onClick = {
                // switch the icon displayed on the floating button
                when (screenState.value) {
                    ScreenState.LOADING -> {}
                    ScreenState.VIEWING -> { screenState.value = ScreenState.EDITING }
                    ScreenState.EDITING -> {
                        screenState.value = ScreenState.VIEWING
                        onSave()
                    }
                }
            },
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                modifier = Modifier.size(32.dp),
                painter = painterResource(
                    when (screenState.value) {
                        ScreenState.LOADING -> R.drawable.pencil
                        ScreenState.VIEWING -> R.drawable.pencil
                        ScreenState.EDITING -> R.drawable.tick
                    }
                ),
                contentDescription = stringResource(R.string.desc_edit)
            )
        }
    }
}

/**
 * Element that allows the user to enter a new category name.
 *
 * @param newCategoryName new name input by the user
 * @param newCategories map of new category names to their list of new ingredients
 * @param unavailableCategoryNames list of category names that already exist
 * @param context used to display Toast
 */
@Composable
fun AddCategory(
    newCategoryName: MutableState<String>,
    newCategories: MutableState<Map<String, MutableList<OwnedIngredient>>>,
    unavailableCategoryNames: SnapshotStateList<String>,
    context: Context
) {
    val focusRequester = remember { FocusRequester() }
    val isFocused = remember { mutableStateOf(false) }

    Column (
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // add button next to the new category text field
            IconButton(
                onClick = {
                    if (isFocused.value) {
                        newCategoryName.value = newCategoryName.value.trimEnd()
                        if (newCategoryName.value.isNotBlank()) {
                            if (unavailableCategoryNames.contains(newCategoryName.value)) {
                                Toast.makeText(context, context.getString(R.string.toast_categoryName), Toast.LENGTH_SHORT).show()
                            } else {
                                val mutableNewCategories = newCategories.value.toMutableMap()
                                mutableNewCategories[newCategoryName.value] = mutableListOf()
                                newCategories.value = mutableNewCategories
                                unavailableCategoryNames.add(newCategoryName.value)
                                focusRequester.freeFocus()
                                isFocused.value = false
                            }
                        }
                        newCategoryName.value = ""

                    } else {
                        focusRequester.requestFocus()
                    }
                }
            ) {
                Icon(
                    painterResource(R.drawable.add),
                    modifier = Modifier.size((INLINE_ICON + 6).dp),
                    contentDescription = stringResource(R.string.desc_add)
                )
            }
            CustomTextField(
                value = newCategoryName.value,
                onValueChange = {
                    newCategoryName.value = it
                    isFocused.value = true
                },
                icon = -1,
                placeHolder = stringResource(R.string.field_addCategory),
                singleLine = true,
                maxLength = MAX_CHARA,
                showMaxChara = false,
                width = 300.dp,
                focusRequester = focusRequester,
                onFocusedChanged = { isFocused.value = it.isFocused },
                keyboardActions = KeyboardActions(
                    onDone = {
                        newCategoryName.value = newCategoryName.value.trimEnd()
                        if (newCategoryName.value.isNotBlank()) {
                            if (unavailableCategoryNames.contains(newCategoryName.value)) {
                                Toast.makeText(context, context.getString(R.string.toast_categoryName), Toast.LENGTH_SHORT).show()
                            } else {
                                val mutableNewCategories = newCategories.value.toMutableMap()
                                mutableNewCategories[newCategoryName.value] = mutableListOf()
                                newCategories.value = mutableNewCategories
                                unavailableCategoryNames.add(newCategoryName.value)
                                focusRequester.freeFocus()
                                isFocused.value = false
                            }
                        }
                        newCategoryName.value = ""
                    }
                ),
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done
                )
            )
        }
        Divider(color = MaterialTheme.colorScheme.outline, thickness = 3.dp)
    }
}

/**
 * How a category is displayed in View mode.
 *
 * @param name of the category
 * @param ingredients list of ingredients in this category
 * @param canTick whether this ingredient should display a checkBox
 * @param onTick block that runs with the ingredient and its ticked value after ticking it
 */
@Composable
fun IngredientCategoryView(
    name: String,
    ingredients: List<OwnedIngredient>,
    canTick: Boolean,
    onTick: (OwnedIngredient, Boolean) -> Unit = { _, _ -> }
) {
    val sortedIngredients = ingredients.sortedBy { it.displayedName }
    Column {
        Spacer(modifier = Modifier.size(16.dp))
        Text(text = name, style = MyTypography.titleSmall, modifier = Modifier.padding(start = 16.dp))
        Spacer(modifier = Modifier.size(16.dp))
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            sortedIngredients.forEach { ingredient ->
                IngredientItemView(ingredient, canTick, onTick)
            }
        }
        Divider(color = MaterialTheme.colorScheme.outline, thickness = 3.dp)
    }
}

/**
 * How a category is displayed in Edit mode.
 *
 * @param name of the category
 * @param ingredients list of ingredients in this category
 * @param canTick whether this ingredient should display a checkBox
 * @param addedItems list of ingredients added in this category
 * @param editedCategories maps old category names to new ones
 * @param onRemoveItem block that runs with an the ingredient' ID and its displayed name after removing it
 * @param onRemoveCategory block that runs with this category's name after removing it
 * @param context used to show toast
 * @param userViewModel used to check for ingredient existence in groceries list
 * @param newGroceryItems map of ingredients from fridge to add to groceries list
 */
@Composable
fun IngredientCategoryEdit(
    name: String,
    ingredients: List<OwnedIngredient>,
    canTick: Boolean,
    addedItems: MutableList<OwnedIngredient>,
    editedCategories: MutableMap<String, String>,
    onRemoveItem: (String, String) -> Unit,
    onRemoveCategory: (String) -> Unit,
    context: Context,
    userViewModel: UserViewModel,
    newGroceryItems: MutableMap<String, MutableList<OwnedIngredient>> = emptyMap<String, MutableList<OwnedIngredient>>().toMutableMap()
) {
    val isEditingName = remember { mutableStateOf(false) }
    val editedName = remember { mutableStateOf(name) }

    // show all existing ingredients in alphabetical order, then newly added ones
    val newItemName = remember { mutableStateOf("") }
    val sortedIngredients = ingredients.sortedBy { it.displayedName }
    val allTempIngredients = remember { mutableStateListOf(*sortedIngredients.toTypedArray()) }

    Column (
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Spacer(modifier = Modifier.size(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ){
            // if the user is editing the category's name -> show a text field
            if (isEditingName.value) {
                Spacer(modifier = Modifier.size(16.dp))
                CustomTextField(
                    value = editedName.value,
                    onValueChange = { editedName.value = it },
                    icon = -1,
                    placeHolder = stringResource(R.string.field_addItem),
                    singleLine = true,
                    maxLength = MAX_CHARA,
                    showMaxChara = false,
                    width = 200.dp,
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (editedName.value.isNotBlank()) {
                                if (editedName.value != name) {
                                    editedCategories[name] = editedName.value
                                }
                                isEditingName.value = false
                            }
                        }
                    ),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done
                    ))
            } else {
                Text(text = editedName.value, style = MyTypography.titleSmall, modifier = Modifier.padding(start = 16.dp))
            }
            Row(
                horizontalArrangement = Arrangement.End
            ) {
                // edit button next to the category's name (can be used to bring the text field in and out of focus)
                IconButton(
                    modifier = Modifier
                        .height(24.dp)
                        .width(40.dp)
                        .padding(end = 16.dp),
                    onClick = {
                        isEditingName.value = !isEditingName.value
                        if (editedName.value.isNotBlank()) {
                            if (editedName.value != name) {
                                editedCategories[name] = editedName.value
                            }
                        }
                    }
                ){
                    Icon(painterResource(R.drawable.pencil), contentDescription = stringResource(R.string.desc_edit))
                }
                // delete button next to the category's name to delete it
                IconButton(
                    modifier = Modifier
                        .height(24.dp)
                        .width(40.dp)
                        .padding(end = 16.dp),
                    onClick = { onRemoveCategory(name) }
                ){
                    Icon(painterResource(R.drawable.bin), contentDescription = stringResource(R.string.desc_delete))
                }
            }
        }
        Spacer(modifier = Modifier.size(16.dp))
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            allTempIngredients.forEach { ingredient ->
                IngredientItemEdit(ingredient, canTick, context, userViewModel, newGroceryItems) {
                    // when removing an ingredient ->
                    allTempIngredients.remove(ingredient)
                    // if ingredient is already in DB -> add to items to be deleted
                    if (sortedIngredients.contains(ingredient)) {
                        onRemoveItem(ingredient.uid, ingredient.displayedName)
                    // else remove item from list of items to be added
                    } else {
                        addedItems.remove(ingredient)
                    }
                }
            }
        }
        newItemName.value = ""
        AddIngredient(newItemName) { displayName ->
            val standName = standardizeName(displayName.value)
            val newIngredient = OwnedIngredient("", displayName.value, standName, name, false)
            allTempIngredients.add(newIngredient)
            addedItems.add(newIngredient)
        }
        Spacer(modifier = Modifier.size(16.dp))
        Divider(color = MaterialTheme.colorScheme.outline, thickness = 3.dp)
    }
}

/**
 * How an ingredient is displayed in View mode.
 *
 * @param ingredient OwnedIngredient object that represents the ingredient
 * @param canTick whether this ingredient should display a checkBox
 * @param onTick block that runs with the ingredient and its ticked value after ticking it
 */
@Composable
private fun IngredientItemView(
    ingredient: OwnedIngredient,
    canTick: Boolean,
    onTick: (OwnedIngredient, Boolean) -> Unit
) {
    val isTicked = remember { mutableStateOf(ingredient.isTicked) }
    Box(
        modifier = if (canTick) {
            Modifier
                .fillMaxWidth()
                .height(HEIGHT.dp)
                .clickable {
                    isTicked.value = !isTicked.value
                    onTick(ingredient, isTicked.value)
                }
        } else {
            Modifier
                .fillMaxWidth()
                .height(HEIGHT.dp)
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = OFFSET.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // if the ingredient can be ticked -> display a checkBox
            if (canTick) {
                Checkbox(
                    modifier = Modifier.size(INLINE_ICON.dp),
                    checked = isTicked.value,
                    onCheckedChange = {
                        isTicked.value = !isTicked.value
                        onTick(ingredient, isTicked.value)
                    },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.secondary)
                )
            }
            Text(
                text = ingredient.displayedName,
                style =
                if (isTicked.value && canTick) MyTypography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.outline,
                    textDecoration = TextDecoration.LineThrough
                )
                else MyTypography.bodyMedium
            )
        }
    }
}

/**
 * How an ingredient is displayed in Edit mode.
 *
 * @param ingredient OwnedIngredient object that represents the ingredient
 * @param canTick whether this ingredient should display a checkBox
 * @param context used to show toast
 * @param userViewModel used to check for ingredient existence in groceries list
 * @param newGroceryItems map of ingredients from fridge to add to groceries list
 * @param onDelete block that runs after deleting this ingredient
 */
@Composable
private fun IngredientItemEdit(
    ingredient: OwnedIngredient,
    canTick: Boolean,
    context: Context,
    userViewModel: UserViewModel,
    newGroceryItems: MutableMap<String, MutableList<OwnedIngredient>>,
    onDelete: () -> Unit
) {
    val isTicked = remember { mutableStateOf(ingredient.isTicked) }
    val canShop = remember { mutableStateOf(true) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(HEIGHT.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = OFFSET.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // if the ingredient can be ticked -> display a checkBox
                if (canTick) {
                    Checkbox(
                        modifier = Modifier.size(INLINE_ICON.dp),
                        checked = isTicked.value,
                        onCheckedChange = {},
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.secondary)
                    )
                }
                Text(
                    text = ingredient.displayedName,
                    style =
                    if (isTicked.value && canTick) MyTypography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.outline,
                        textDecoration = TextDecoration.LineThrough
                    )
                    else MyTypography.bodyMedium
                )
            }
            Row(
                horizontalArrangement = Arrangement.End
            ) {
                // if the items cannot be ticked (means it is in fridge) ->
                if (!canTick) {
                    // "add to groceries" button next to the ingredient
                    IconButton(
                        modifier = Modifier
                            .height(24.dp)
                            .width(40.dp)
                            .padding(end = 16.dp),
                        onClick = {
                            if (canShop.value) {
                                // check that this ingredient hasn't already been added to the grocery list
                                userViewModel.ingredientExistsInCategory(ingredient.category, ingredient.displayedName, false,
                                    { if (it) { handleError(context, "Could not check ingredient existence") }})
                                { exists ->
                                    if (exists) {
                                        Toast.makeText(context, context.getString(R.string.toast_ingredientExists), Toast.LENGTH_SHORT).show()
                                    } else {
                                        newGroceryItems[ingredient.category]?.add(ingredient) ?: run {
                                            newGroceryItems[ingredient.category] = mutableListOf(ingredient)
                                        }
                                        canShop.value = !canShop.value
                                    }
                                }
                            }
                            else {
                                newGroceryItems[ingredient.category]?.remove(ingredient)
                                canShop.value = !canShop.value
                            }

                        }
                    ){
                        if (canShop.value) {
                            Icon(
                                painterResource(R.drawable.cart_add),
                                contentDescription = stringResource(R.string.desc_shop)
                            )
                        } else {
                            Icon(
                                painterResource(R.drawable.undo),
                                contentDescription = stringResource(R.string.desc_shop)
                            )
                        }
                    }
                }
                // delete button next to the ingredient
                IconButton(
                    modifier = Modifier
                        .height(24.dp)
                        .width(40.dp)
                        .padding(end = 16.dp),
                    onClick = { onDelete() }
                ){
                    Icon(painterResource(R.drawable.bin), contentDescription = stringResource(R.string.desc_delete))
                }
            }
        }
    }
}

/**
 * Element that allows the user to enter a new ingredient name.
 *
 * @param displayName name input by the user
 * @param onAdd block that runs with the ingredient name after adding it
 */
@Composable
fun AddIngredient(displayName: MutableState<String>, onAdd: (MutableState<String>) -> Unit) {
    val focusRequester = remember { FocusRequester() }
    val isFocused = remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height((HEIGHT + 16).dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (OFFSET - 3).dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // add button next to the new ingredient text field
            IconButton(
                onClick = {
                    if (isFocused.value) {
                        displayName.value = displayName.value.trimEnd()
                        if (displayName.value.isNotBlank()) {
                            onAdd(displayName)
                            focusRequester.freeFocus()
                            isFocused.value = false
                        }
                        displayName.value = ""

                    } else {
                        focusRequester.requestFocus()
                    }
                }
            ) {
                Icon(
                    painterResource(R.drawable.add),
                    modifier = Modifier.size((INLINE_ICON + 6).dp),
                    contentDescription = stringResource(R.string.desc_add)
                )
            }
            CustomTextField(
                value = displayName.value,
                onValueChange = { displayName.value = it },
                icon = -1,
                placeHolder = stringResource(R.string.field_addItem),
                singleLine = true,
                maxLength = MAX_CHARA,
                showMaxChara = false,
                width = 300.dp,
                focusRequester = focusRequester,
                onFocusedChanged = { isFocused.value = it.isFocused },
                keyboardActions = KeyboardActions(
                    onDone = {
                        displayName.value = displayName.value.trimEnd()
                        if (displayName.value.isNotBlank()) {
                            onAdd(displayName)
                            focusRequester.freeFocus()
                            isFocused.value = false
                        }
                        displayName.value = ""
                    }
                ),
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done
                )
            )
        }
    }
}

// shared functionalities

/**
 * Creates a standard version of ingredients names to compare more easily with recipe ingredients.
 *
 * @param ingredient name to standardize
 * @return standardized name
 */
fun standardizeName(ingredient: String): String {
    // list of words whose following word should be kept
    val particles = listOf("de", "à", "aux", "d", "l", "pour")
    val nouns = listOf("sauce", "vin")

    // removes trailing whitespaces and plural "s", and puts the entire word in lowercase
    val name = ingredient.trimEnd()
    val words = name.split(" ", "'").map { it.lowercase().removeSuffix("s") }

    val result = mutableListOf(words[0])

    // loop over all the ingredient's words to only keep the most important ones
    var i = 0
    var addNextWord = false
    while (i < words.size) {
        val currentWord = words[i]

        if (currentWord in particles || currentWord in nouns) {
            addNextWord = true
        }
        else if (addNextWord) {
            result.add(currentWord)
            addNextWord = false
        }
        i++
    }
    return result.joinToString(" ")
}

/**
 * Bulk updates all modifications to the database.
 *
 * @param userViewModel to process modifications
 * @param userPersonal UserPersonal object that contains all the private data
 * @param list map read from userPersonal
 * @param fieldToRead which map to read from userPersonal (groceries or fridge)
 * @param isInFridge whether the ingredient modifications should be updated in the fridge. Updates the groceries if false
 * @param context used to handle errors
 * @param loading set to false once all updates have called back
 * @param newItems maps all existing categories to a list of new added ingredients
 * @param removedItems maps all existing categories to a list of deleted ingredients
 * @param editedCategories maps old category names to new ones
 * @param newCategories map of new categories and their list of new ingredients
 * @param removedCategories list of names of deleted category
 */
fun loadModifications(
    userViewModel: UserViewModel,
    userPersonal: UserPersonal,
    list: MutableState<Map<String, List<OwnedIngredient>>>,
    fieldToRead: (UserPersonal) -> Map<String, List<OwnedIngredient>>,
    isInFridge: Boolean,
    context: Context,
    loading: MutableState<Boolean>,
    newItems: Map<String, MutableList<OwnedIngredient>>,
    removedItems: Map<String, MutableList<String>>,
    editedCategories: MutableMap<String, String>,
    newCategories: MutableState<Map<String, MutableList<OwnedIngredient>>>,
    removedCategories: SnapshotStateList<String>
) {
    userViewModel.deleteIngredients(removedItems, isInFridge, {
        if (it) {
            handleError(context, "Could not remove ingredient")
            loading.value = false
        }
    }) {
        userViewModel.addIngredients(newItems, isInFridge, {
            if (it) {
                handleError(context, "Could not update owned ingredients list")
                loading.value = false
            }
        }) {
            userViewModel.updateCategories(newCategories.value, editedCategories, isInFridge, {
                if (it) {
                    handleError(context, "Could not update category names")
                    loading.value = false
                }
            }) {
                userViewModel.deleteCategories(removedCategories, {
                    if (it) {
                        handleError(context, "Could not delete categories")
                        loading.value = false
                    }
                }) {
                    userViewModel.fetchUserPersonal({
                        if (it) {
                            handleError(context, "Could not fetch user personal")
                            loading.value = false
                        }
                    }) {
                        list.value = fieldToRead(userPersonal)
                        loading.value = false
                    }
                }
            }
        }
    }
}

/**
 * Clears all variables that hold temporary modifications.
 *
 * @param userPersonal UserPersonal object that contains all the private data
 * @param list map read from userPersonal
 * @param fieldToRead which map to read from userPersonal (groceries or fridge)
 * @param newItems maps all existing categories to a list of new added ingredients
 * @param removedItems maps all existing categories to a list of deleted ingredients
 * @param editedCategories maps old category names to new ones
 * @param newCategories map of new categories and their list of new ingredients
 * @param removedCategories list of names of deleted category
 * @param unavailableCategoryNames list of category names that are already in use
 */
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
