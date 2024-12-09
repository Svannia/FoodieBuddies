package com.example.foodiebuddy.ui.recipes

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ColorScheme
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import com.example.foodiebuddy.R
import com.example.foodiebuddy.data.Diet
import com.example.foodiebuddy.data.Origin
import com.example.foodiebuddy.data.OwnedIngredient
import com.example.foodiebuddy.data.RecipeIngredient
import com.example.foodiebuddy.data.Tag
import com.example.foodiebuddy.errors.handleError
import com.example.foodiebuddy.navigation.NavigationActions
import com.example.foodiebuddy.system.checkPermission
import com.example.foodiebuddy.system.imagePermissionVersion
import com.example.foodiebuddy.ui.CustomNumberField
import com.example.foodiebuddy.ui.CustomTextField
import com.example.foodiebuddy.ui.OptionsMenu
import com.example.foodiebuddy.ui.SquareImage
import com.example.foodiebuddy.ui.account.PictureOptions
import com.example.foodiebuddy.ui.images.SetPicture
import com.example.foodiebuddy.ui.ingredients.standardizeName
import com.example.foodiebuddy.ui.theme.MyTypography
import com.example.foodiebuddy.viewModels.UserViewModel


@Composable
fun EditRecipe(
    context: Context,
    onGoBack: () -> Unit,
    title: String,
    name: MutableState<String>,
    picture: MutableState<Uri>,
    recipe: MutableState<String>,
    ingredients: SnapshotStateList<RecipeIngredient>,
    origin: MutableState<Origin>,
    diet: MutableState<Diet>,
    tags: SnapshotStateList<Tag>,
    showPictureOptions: MutableState<Boolean>,
    dataEdited: MutableState<Boolean>?= null,
    onEditPicture: () -> Unit,
    onRemovePicture: () -> Unit,
    onSave: () -> Unit
) {
    // getting image and relevant permissions
    val imageInput = "image/*"
    val getPicture = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { pictureUri ->
            picture.value = pictureUri
            onEditPicture()
        }
    }
    val imagePermission = imagePermissionVersion()
    val requestMediaPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) getPicture.launch(imageInput)

        }

    RecipeSecondaryScreen(
        title = title,
        onGoBack = { onGoBack() },
        actions = { OptionsMenu( "Save to drafts" to {
            // todo
        }) },
        bottomBar = {
            val newData = dataEdited?.value ?: true
            val requiredFields = name.value.isNotEmpty() && recipe.value.isNotEmpty() && origin.value != Origin.NONE && diet.value != Diet.NONE
            BottomSaveBar(stringResource(R.string.button_save), requiredFields && newData) {
                onSave()
            }
        }
        // main screen body
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(top = 32.dp, start = 16.dp, end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // requirement instructions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "*", style = MyTypography.bodySmall, color = Color.Red)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = stringResource(R.string.txt_recipeRequirements),
                        style = MyTypography.bodySmall
                    )
                }
            }
            // text field for recipe name
            item {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp))
                {
                    RequiredField(stringResource(R.string.title_recipeName))
                    CustomTextField(
                        value = name.value,
                        onValueChange = {
                            if (dataEdited != null) dataEdited.value = true
                            name.value = it
                        },
                        icon = -1,
                        placeHolder = stringResource(R.string.field_recipeName),
                        singleLine = true,
                        maxLength = 60,
                        width = 350.dp
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Divider(color = MaterialTheme.colorScheme.outline, thickness = 3.dp)
                    Spacer(modifier = Modifier.size(8.dp))
                }
            }
            // add a picture
            item {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(text = stringResource(R.string.title_foodPic), style = MyTypography.titleSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (picture.value != Uri.EMPTY) {
                            SquareImage(
                                size = 68.dp,
                                picture = picture.value,
                                contentDescription = stringResource(R.string.desc_recipePicture)
                            )
                            Spacer(modifier = Modifier.size(16.dp))
                        }
                        Text(
                            modifier = Modifier.clickable {
                                if (picture.value != Uri.EMPTY) {
                                    showPictureOptions.value = true
                                }
                                else {
                                    checkPermission(context, imagePermission, requestMediaPermissionLauncher)
                                    { getPicture.launch(imageInput) }
                                }

                            },
                            text =
                            if (picture.value != Uri.EMPTY) {
                                stringResource(R.string.button_modifyProfilePicture)
                            }
                            else stringResource(R.string.button_addProfilePicture),
                            style = MyTypography.labelMedium
                        )
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    Divider(color = MaterialTheme.colorScheme.outline, thickness = 3.dp)
                    Spacer(modifier = Modifier.size(8.dp))
                }
            }
            // ingredients title
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.title_ingredients),
                        style = MyTypography.titleSmall
                    )
                }
            }
            // list of ingredients
            items(ingredients.toList(), key = {it.id}) { ingredient ->
                IngredientItem(ingredient) {
                    ingredients.remove(ingredient)
                    ingredients.forEach { Log.d("Debug", "ingredients after removing are $${it.displayedName}") }
                }
                Log.d("Debug", "ingredients contain ${ingredients.toList()}")
            }
            // "Plus" button to add an ingredient
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    IconButton(
                        onClick = {
                            ingredients.add(RecipeIngredient("", "", 0f, ""))
                            ingredients.forEach { Log.d("Debug", "ingredients after adding are $${it.displayedName}") }
                        }
                    ) {
                        Icon(
                            painterResource(R.drawable.add),
                            modifier = Modifier.size(26.dp),
                            contentDescription = stringResource(R.string.desc_add)
                        )
                    }
                }
                Divider(color = MaterialTheme.colorScheme.outline, thickness = 3.dp)
                Spacer(modifier = Modifier.size(16.dp))
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
        if (showPictureOptions.value) {
            PictureOptions(
                onDismiss = { showPictureOptions.value = false },
                onChange = {
                    checkPermission(context, imagePermission, requestMediaPermissionLauncher)
                    { getPicture.launch(imageInput) }
                },
                onRemove = {
                    onRemovePicture()
                    if (dataEdited != null) dataEdited.value = true
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeSecondaryScreen(
    title: String,
    onGoBack: () -> Unit,
    actions: @Composable (RowScope.() -> Unit) = {},
    bottomBar: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Box {
                CenterAlignedTopAppBar(
                    title = { Text(
                        text = title,
                        style = MyTypography.titleMedium)
                    },
                    // "go back button" that doesn't necessarily navigate back in navigation graph
                    navigationIcon = {
                        IconButton(
                            onClick = { onGoBack() }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.go_back),
                                contentDescription = stringResource(R.string.desc_goBack)
                            )
                        }
                    },
                    // reset button to emtpy the filters
                    actions = { actions() },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background, scrolledContainerColor = MaterialTheme.colorScheme.background)
                )
            }
        },
        // save button always shown at the bottom
        bottomBar = { bottomBar() },
        content = { content(it) }
    )
}

@Composable
private fun RequiredField(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text=title, style = MyTypography.titleSmall)
        Spacer(modifier = Modifier.size(8.dp))
        Text(text = "*", style = MyTypography.titleSmall, color = Color.Red)
    }
}

@Composable
fun SetRecipePicture(picture: Uri, onCancel: () -> Unit, onSave: (Uri) -> Unit) {
    SetPicture(
        picture = picture,
        roundMask = false,
        onCancel = { onCancel() },
        onSave = { onSave(it) }
    )
}

@Composable
fun IngredientListEdit(
    ingredients: MutableList<RecipeIngredient>,
) {
    // show all existing ingredients in added order
    val newItemName = remember { mutableStateOf("") }
    val newItemQty = remember { mutableIntStateOf(0) }
    val newItemUnit = remember { mutableStateOf("") }

    Column (
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Text(text = stringResource(R.string.title_ingredients), style = MyTypography.titleSmall)
        Spacer(modifier = Modifier.size(16.dp))
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(ingredients, key = {it}) { ingredient ->
                IngredientItem(ingredient) {
                    ingredients.remove(ingredient)
                    ingredients.forEach { Log.d("Debug", "ingredients after removing are $${it.displayedName}") }
                }
            }
        }
        newItemName.value = ""
        newItemQty.intValue = 0
        newItemUnit.value = ""
        // "Plus" button to add an ingredient
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            IconButton(
                onClick = {
                    ingredients.add(RecipeIngredient("", "", 0f, ""))
                    ingredients.forEach { Log.d("Debug", "ingredients after adding are $${it.displayedName}") }
                }
            ) {
                Icon(
                    painterResource(R.drawable.add),
                    modifier = Modifier.size(26.dp),
                    contentDescription = stringResource(R.string.desc_add)
                )
            }
        }
        Spacer(modifier = Modifier.size(16.dp))
        Divider(color = MaterialTheme.colorScheme.outline, thickness = 3.dp)
    }
}

@Composable
fun IngredientItem(
    ingredient: RecipeIngredient,
    onDelete: () -> Unit
) {
    val displayName = remember { mutableStateOf("") }
    val quantity = remember { mutableFloatStateOf(0f) }
    val unit = remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(10)
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // text field to input the ingredient name
                CustomTextField(
                    value = displayName.value,
                    onValueChange = {
                        displayName.value = it.trimEnd()
                        ingredient.displayedName = it.trimEnd()
                        ingredient.standName = standardizeName(it.trimEnd())
                    },
                    icon = -1,
                    placeHolder = stringResource(R.string.field_addIngredient),
                    singleLine = true,
                    maxLength = 21,
                    showMaxChara = false,
                    width = 200.dp
                )
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // quantity
                Text(text = stringResource(R.string.txt_quantity), style = MyTypography.bodySmall)
                CustomNumberField(
                    value = quantity.floatValue,
                    onValueChange = {
                        quantity.floatValue = it
                        ingredient.quantity = it
                    },
                    placeHolder = "-",
                    width = 50.dp
                )
                // unit
                Text(text = stringResource(R.string.txt_unit))
                CustomTextField(
                    value = unit.value,
                    onValueChange = {
                        unit.value = it
                        ingredient.unit = it
                    },
                    icon = -1,
                    placeHolder = "-",
                    singleLine = true,
                    autoCap = false,
                    maxLength = 10,
                    showMaxChara = false,
                    width = 100.dp
                )
            }
        }
    }
}

/**
 * Oval background look for tags.
 *
 * @param tagName name of the tag to be displayed inside
 */
@Composable
fun TagLabel(tagName: String) {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(50)
            )
        ,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = tagName,
            style = MyTypography.bodySmall,
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
        )
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
fun <T> TagDropDown(
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
 * Save button for filters that always stays at the bottom of the screen.
 *
 * @param saveText text to be displayed in the button
 * @param onSave block to run when pressing the button
 */
@Composable
fun BottomSaveBar(
    saveText: String,
    isEnabled: Boolean,
    onSave: () -> Unit
) {
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .height(65.dp)
            .padding(top = 0.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
        tonalElevation = 0.dp,
        containerColor = Color.Transparent
    ) {
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onSave() },
            enabled = isEnabled,
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