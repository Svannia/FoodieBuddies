package com.example.foodiebuddy.ui.ingredients

import android.util.Log
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.foodiebuddy.R
import com.example.foodiebuddy.data.OwnedIngredient
import com.example.foodiebuddy.ui.CustomTextField
import com.example.foodiebuddy.ui.theme.MyTypography

private const val HEIGHT = 42
private const val OFFSET = 45
private const val INLINE_ICON = 20

enum class ScreenState { VIEWING, EDITING }
@Composable
fun FloatingButton(screenState: MutableState<ScreenState>, onSave: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.End
    ) {
        Button(
            modifier = Modifier
                .size(62.dp)
                .clip(MaterialTheme.shapes.medium),
            onClick = {
                // switch the icon displayed on the floating button
                when (screenState.value) {
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
                        ScreenState.VIEWING -> R.drawable.pencil
                        ScreenState.EDITING -> R.drawable.tick
                    }
                ),
                contentDescription = stringResource(R.string.desc_edit)
            )
        }
    }
}

@Composable
fun IngredientCategoryView(
    name: String,
    ingredients: List<OwnedIngredient>,
    onTick: (OwnedIngredient, Boolean) -> Unit
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
                IngredientItemView(ingredient, onTick)
            }
        }
        Divider(color = MaterialTheme.colorScheme.outline, thickness = 3.dp)
    }
}

@Composable
fun IngredientCategoryEdit(
    name: String,
    ingredients: List<OwnedIngredient>,
    addedItems: MutableList<String>,
    removedItems: MutableList<String>,
    editedCategories: MutableMap<String, String>,
    removedCategories: MutableList<String>
) {
    val isEditingName = remember { mutableStateOf(false) }
    val editedName = remember { mutableStateOf(name) }

    val newItemName = remember { mutableStateOf("") }
    val sortedIngredients = ingredients.sortedBy { it.displayedName }
    val allTempIngredients = remember { mutableStateListOf(*sortedIngredients.toTypedArray()) }

    Log.d("Debug", "allIngredients contains: $allTempIngredients")

    Column {
        Spacer(modifier = Modifier.size(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth().height(60.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ){
            if (isEditingName.value) {
                Spacer(modifier = Modifier.size(16.dp))
                CustomTextField(
                    value = editedName.value,
                    onValueChange = { editedName.value = it },
                    icon = -1,
                    placeHolder = stringResource(R.string.button_addItem),
                    singleLine = true,
                    maxLength = 15,
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
                    )
                )
            } else {
                Text(text = editedName.value, style = MyTypography.titleSmall, modifier = Modifier.padding(start = 16.dp))
            }
            Row(
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    modifier = Modifier
                        .height(24.dp)
                        .width(40.dp)
                        .padding(end = 16.dp),
                    onClick = { isEditingName.value = !isEditingName.value }
                ){
                    Icon(painterResource(R.drawable.pencil), contentDescription = stringResource(R.string.desc_edit))
                }
                IconButton(
                    modifier = Modifier
                        .height(24.dp)
                        .width(40.dp)
                        .padding(end = 16.dp),
                    onClick = { removedCategories.add(name) }
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
                IngredientItemEdit(ingredient) {
                    allTempIngredients.remove(ingredient)
                    removedItems.add(ingredient.uid)
                }
            }
            AddIngredient(newItemName) { displayName ->
                allTempIngredients.add(OwnedIngredient("", displayName, "", name, false))
                addedItems.add(displayName)
                Log.d("Debug", "allIngredients contains after adding item: $allTempIngredients")
            }
        }
        Divider(color = MaterialTheme.colorScheme.outline, thickness = 3.dp)
    }
}

@Composable
private fun IngredientItemView(
    ingredient: OwnedIngredient,
    onTick: (OwnedIngredient, Boolean) -> Unit
) {
    val isTicked = remember { mutableStateOf(ingredient.isTicked) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(HEIGHT.dp)
            .clickable {
                isTicked.value = !isTicked.value
                onTick(ingredient, isTicked.value)
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
            Checkbox(
                modifier = Modifier.size(INLINE_ICON.dp),
                checked = isTicked.value,
                onCheckedChange = {
                    isTicked.value = !isTicked.value
                    onTick(ingredient, isTicked.value)
                },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.secondary)
            )
            Text(
                text = ingredient.displayedName,
                style =
                if (isTicked.value) MyTypography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.outline,
                    textDecoration = TextDecoration.LineThrough
                )
                else MyTypography.bodyMedium
            )
        }
    }
}

@Composable
private fun IngredientItemEdit(
    ingredient: OwnedIngredient,
    onDelete: () -> Unit
) {
    val isTicked = remember { mutableStateOf(ingredient.isTicked) }
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
                Checkbox(
                    modifier = Modifier.size(INLINE_ICON.dp),
                    checked = isTicked.value,
                    onCheckedChange = {},
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.secondary)
                )
                Text(
                    text = ingredient.displayedName,
                    style =
                    if (isTicked.value) MyTypography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.outline,
                        textDecoration = TextDecoration.LineThrough
                    )
                    else MyTypography.bodyMedium
                )
            }
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

@Composable
private fun AddIngredient(displayName: MutableState<String>, onAdd: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height((HEIGHT + 16).dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (OFFSET - 3).dp, end = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painterResource(R.drawable.add),
                modifier = Modifier.size((INLINE_ICON + 6).dp),
                contentDescription = stringResource(R.string.desc_delete)
            )
            CustomTextField(
                value = displayName.value,
                onValueChange = { displayName.value = it },
                icon = -1,
                placeHolder = stringResource(R.string.button_addItem),
                singleLine = true,
                maxLength = 50,
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (displayName.value.isNotBlank()) {
                            onAdd(displayName.value)
                            displayName.value = ""
                        }
                    }
                ),
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done
                )
            )
        }
    }
}
