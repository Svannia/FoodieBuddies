package com.example.foodiebuddy.ui.recipes

import android.content.Context
import android.net.Uri
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import com.example.foodiebuddy.R
import com.example.foodiebuddy.data.Diet
import com.example.foodiebuddy.data.Origin
import com.example.foodiebuddy.data.RecipeIngredient
import com.example.foodiebuddy.data.Tag
import com.example.foodiebuddy.data.getString
import com.example.foodiebuddy.data.measuresMap
import com.example.foodiebuddy.system.checkPermission
import com.example.foodiebuddy.system.imagePermissionVersion
import com.example.foodiebuddy.ui.CustomNumberField
import com.example.foodiebuddy.ui.CustomTextField
import com.example.foodiebuddy.ui.IconSquareImage
import com.example.foodiebuddy.ui.OptionsMenu
import com.example.foodiebuddy.ui.images.SetPicture
import com.example.foodiebuddy.ui.theme.MyTypography

private const val MAX_PICTURES = 3
/**
 * The content of a screen that can edit the data of a recipe.
 *
 * @param context used to access various resources
 * @param onGoBack block that runs when pressing the Back button
 * @param title tile at the top of the screen
 * @param name editable name of the recipe
 * @param pictures editable list of recipe pictures
 * @param instructions editable list of instruction steps
 * @param ingredients editable list of RecipeIngredient objects
 * @param portion editable number of portions this recipe serves
 * @param perPerson editable boolean; if true, the portion is per person, if false per piece
 * @param origin editable Origin tag
 * @param diet editable Diet tag
 * @param tags editable list of miscellaneous Tag objects
 * @param dataEdited optional, stores whether or not any data was edited
 * @param editingExistingRecipe if true, the Save button will stay disabled if no recipe data was edited and there will be a Delete button
 * @param onEditPicture block that runs when pressing the prompt to edit a new picture
 * @param onRemovePicture block that runs when deleting the recipe picture
 * @param onDraftSave block that runs when saving the current recipe data into a draft
 * @param onSave block that runs when pressing the Save button
 * @param onDelete optional block to run when pressing the eventual Delete button
 */
@Composable
fun EditRecipe(
    context: Context,
    onGoBack: () -> Unit,
    title: String,
    name: MutableState<String>,
    pictures: SnapshotStateList<Uri>,
    instructions: SnapshotStateList<String>,
    ingredients: SnapshotStateList<RecipeIngredient>,
    portion: MutableIntState,
    perPerson: MutableState<Boolean>,
    origin: MutableState<Origin>,
    diet: MutableState<Diet>,
    tags: SnapshotStateList<Tag>,
    dataEdited: MutableState<Boolean>?= null,
    editingExistingRecipe: Boolean = false,
    onEditPicture: () -> Unit,
    onRemovePicture: (Int) -> Unit,
    onDraftSave: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit = {}
) {
    // getting image and relevant permissions
    val imageInput = "image/*"
    val getPicture = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { pictureUri ->
            pictures.add(pictureUri)
            onEditPicture()
        }
    }
    val imagePermission = imagePermissionVersion()
    val requestMediaPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) getPicture.launch(imageInput)

        }

    // to recompose screen when modifying ingredient name
    var ingredientTrigger by remember { mutableIntStateOf(0) }
    LaunchedEffect(ingredientTrigger) {}

    RecipeSecondaryScreen(
        title = title,
        onGoBack = { onGoBack() },
        actions = { OptionsMenu(R.drawable.save, stringResource(R.string.button_saveDraft) to { onDraftSave() }) },
        bottomBar = {
            val newData = if (editingExistingRecipe) dataEdited?.value ?: true else true
            val requiredFields =
                name.value.isNotEmpty() &&
                instructions.isNotEmpty() &&
                !instructions.all { step -> step.isBlank() || step.all { it == ' ' } } &&
                portion.intValue >= 1 &&
                origin.value != Origin.NONE &&
                diet.value != Diet.NONE &&
                ingredients.toList().all { ingredient -> ingredient.displayedName.isNotBlank() && !ingredient.displayedName.all { it == ' ' } }
            BottomSaveBar(stringResource(R.string.button_publish), requiredFields && newData) {
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
                    RequiredField(stringResource(R.string.title_recipeName), MyTypography.titleSmall)
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
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp),
                        horizontalArrangement =  Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        itemsIndexed(pictures, key = {_, uri -> uri.toString()}) { index, picture ->
                            IconSquareImage(
                                icon = R.drawable.bin,
                                iconColor = Color.Red,
                                pictureSize = 120.dp,
                                picture = picture,
                                contentDescription = stringResource(R.string.desc_recipePicture)
                            ) {
                                onRemovePicture(index)
                            }
                        }
                        if (pictures.size < MAX_PICTURES) {
                            item {
                                Icon(
                                    painter = painterResource(R.drawable.plus),
                                    contentDescription = stringResource(R.string.desc_add),
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clickable {
                                            checkPermission(context, imagePermission, requestMediaPermissionLauncher)
                                            { getPicture.launch(imageInput) }
                                        }
                                )
                            }
                        }
                    }


                   /* Row(verticalAlignment = Alignment.CenterVertically) {
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
                                if (pictures.isNotEmpty()) {
                                    showPictureOptions.value = true
                                }
                                else {
                                    checkPermission(context, imagePermission, requestMediaPermissionLauncher)
                                    { getPicture.launch(imageInput) }
                                }

                            },
                            text =
                            if (picture.value != Uri.EMPTY) {
                                stringResource(R.string.button_modifyRecipePicture)
                            }
                            else stringResource(R.string.button_addRecipePicture),
                            style = MyTypography.labelMedium
                        )
                    }*/
                    Spacer(modifier = Modifier.size(8.dp))
                    Divider(color = MaterialTheme.colorScheme.outline, thickness = 3.dp)
                    Spacer(modifier = Modifier.size(8.dp))
                }
            }
            // various tags
            item {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(text = stringResource(R.string.title_tag), style = MyTypography.titleSmall)
                    TagDropDown(
                        title = stringResource(R.string.title_origin),
                        required = true,
                        tags = Origin.entries.drop(1).toList(),
                        filtersSet = setOf(origin.value),
                        onlyOne = true,
                        getString = { origin -> origin.getString(context) },
                        onClick = {
                            origin.value = it
                            if (dataEdited != null) dataEdited.value = true
                        }
                    )
                    TagDropDown(
                        title = stringResource(R.string.title_diet),
                        required = true,
                        tags = Diet.entries.drop(1).toList(),
                        filtersSet = setOf(diet.value),
                        onlyOne = true,
                        getString = { diet -> diet.getString(context) },
                        onClick = {
                            diet.value = it
                            if (dataEdited != null) dataEdited.value = true
                        }
                    )
                    TagDropDown(
                        title = stringResource(R.string.title_miscellaneous),
                        tags = Tag.entries.drop(1).toList(),
                        filtersSet = tags.toSet(),
                        getString = { tag -> tag.getString(context) },
                        onClick = { tag ->
                            if (tags.contains(tag)) tags.remove(tag)
                            else tags.add(tag)
                            if (dataEdited != null) dataEdited.value = true
                        }
                    )
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
            // portion
            item {
                val perExpanded = remember { mutableStateOf(false) }
                // row with number field for portion and dropdown field for "per person" or "per piece"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.height(39.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Text(
                            text = stringResource(R.string.txt_for),
                            style = MyTypography.bodyLarge
                        )
                    }
                    Spacer(modifier = Modifier.size(24.dp))
                    CustomNumberField(
                        value = portion.intValue,
                        onValueChange = {
                            portion.intValue = it as Int
                                        },
                        isInteger = true,
                        placeHolder = "-",
                        width = 70.dp
                    )
                    Spacer(modifier = Modifier.size(24.dp))
                    val inverseColor = MaterialTheme.colorScheme.inversePrimary
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(39.dp)
                            .drawWithContent {
                                drawContent()
                                drawLine(
                                    color = inverseColor,
                                    start = Offset(0f, size.height),
                                    end = Offset(size.width, size.height),
                                    strokeWidth = 0.8.dp.toPx()
                                )
                            }
                            .clickable { perExpanded.value = !perExpanded.value },
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Text(
                            text = if (perPerson.value) {
                                if (portion.intValue > 1) stringResource(R.string.txt_people)
                                else stringResource(R.string.txt_person)
                            } else {
                                if (portion.intValue > 1) stringResource(R.string.txt_pieces)
                                else stringResource(R.string.txt_piece)
                            },
                            style = MyTypography.bodyLarge
                        )
                    }
                    Spacer(modifier = Modifier.size(24.dp))
                    Text("*", style = MyTypography.bodyLarge, color = Color.Red)
                }
                // if the dropdown menu is clicked -> can choose "person" or "piece"
                if (perExpanded.value) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = (100).dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        DropdownMenu(
                            expanded = true,
                            onDismissRequest = { perExpanded.value = false },
                            modifier = Modifier.width(120.dp)
                        ) {
                            Column {
                                DropdownMenuItem(
                                    text = { Text(
                                        text =  if (portion.intValue > 1) stringResource(R.string.txt_people)
                                                else stringResource(R.string.txt_person),
                                        style = MyTypography.bodySmall
                                    ) },
                                    onClick = {
                                        perPerson.value = true
                                        perExpanded.value = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(
                                        text =  if (portion.intValue > 1) stringResource(R.string.txt_pieces)
                                                else stringResource(R.string.txt_piece),
                                        style = MyTypography.bodySmall
                                    ) },
                                    onClick = {
                                        perPerson.value = false
                                        perExpanded.value = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            // list of ingredients
            items(ingredients.toList(), key = {it.id}) { ingredient ->
                IngredientItem(
                    context = context,
                    ingredient = ingredient,
                    onValueChange = {
                        ingredientTrigger++
                        if (dataEdited != null) dataEdited.value = true
                    },
                    onDelete = {
                        ingredients.remove(ingredient)
                        if (dataEdited != null) dataEdited.value = true
                    }
                )
            }
            // "Plus" button to add an ingredient
            item {
                AddButton {
                    ingredients.add(RecipeIngredient.empty())
                }
            }
            // recipe instructions title
            item {
                RequiredField(title = stringResource(R.string.title_recipeInstructions), style = MyTypography.titleSmall)
            }
            // list of instruction steps
            itemsIndexed(instructions.toList()) { index, step ->
                StepItem(
                    number = index,
                    last = index > 0 && index == instructions.size -1,
                    step = step,
                    onValueChange = {
                        instructions[index] = it
                        if (dataEdited != null) dataEdited.value = true
                    },
                    onDelete = {
                        instructions.removeLast()
                        if (dataEdited != null) dataEdited.value = true
                    }
                )
            }
            // "Plus" button to add a step
            item {
                AddButton {
                    instructions.add("")
                }
            }
            // eventual Delete button
            if (editingExistingRecipe) {
                item {
                    TextButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(width = 2.dp, color = Color.Red, shape = RoundedCornerShape(50)),
                        onClick = { onDelete() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.button_deleteRecipe),
                            style = MyTypography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = Color.Red
                        )
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
        /*if (showPictureOptions.value) {
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
        }*/
    }
}

/**
 * This creates the layout for a secondary screen with more customized back button and a bottom save button.
 *
 * @param title at the top of the screen
 * @param onGoBack block to run when pressing the back button
 * @param actions optional extra actions at the right of the top bar
 * @param bottomBar composable that stays fixed at the bottom of the screen, e.g a save button
 * @param content screen body
 */
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
                    title = { Text(text = title, style = MyTypography.titleMedium) },
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

/**
 * Creates a text followed by a red asterisk.
 *
 * @param title text preceding the asterisk
 * @param style TextStyle for this text, also applied on the asterisk
 */
@Composable
private fun RequiredField(title: String, style: TextStyle) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text=title, style = style)
        Spacer(modifier = Modifier.size(8.dp))
        Text(text = "*", style = style, color = Color.Red)
    }
}

/**
 * Screen to edit a picture for the recipe.
 *
 * @param picture to be edited
 * @param onCancel block that runs when pressing the Cancel button
 * @param onSave block that runs when pressing the Save button, returning the edited picture
 */
@Composable
fun SetRecipePicture(picture: Uri, onCancel: () -> Unit, onSave: (Uri) -> Unit) {
    SetPicture(
        picture = picture,
        roundMask = false,
        onCancel = { onCancel() },
        onSave = { onSave(it) }
    )
}

/**
 * A single Add button to add ingredients or instruction steps at the end of the current list.
 *
 * @param onClick block that runs when pressing the Add button
 */
@Composable
fun AddButton(
    onClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        IconButton(
            onClick = { onClick() }
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

/**
 * Creates the look for an editable recipe ingredient.
 *
 * @param context used to access string resources
 * @param ingredient RecipeIngredient object whose information is displayed in this item
 * @param onValueChange block that runs when making any change in the ingredient's data
 * @param onDelete block that runs when pressing the Delete button for this ingredient
 */
@Composable
fun IngredientItem(
    context: Context,
    ingredient: RecipeIngredient,
    onValueChange: () -> Unit,
    onDelete: () -> Unit
) {
    val displayName = remember { mutableStateOf(ingredient.displayedName) }
    val quantity = remember { mutableFloatStateOf(ingredient.quantity) }
    val unit = remember { mutableStateOf(ingredient.unit) }
    val unitsExpanded = remember { mutableStateOf(false) }

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
                Row(verticalAlignment = Alignment.CenterVertically){
                    CustomTextField(
                        value = displayName.value,
                        onValueChange = {
                            displayName.value = it
                            ingredient.displayedName = it
                            onValueChange()
                        },
                        icon = -1,
                        placeHolder = stringResource(R.string.field_ingredientName),
                        singleLine = true,
                        maxLength = 21,
                        showMaxChara = false,
                        width = 200.dp
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    Text(text = "*", style = MyTypography.titleSmall, color = Color.Red)
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
                        quantity.floatValue = it as Float
                        ingredient.quantity = it
                        onValueChange()
                    },
                    isInteger = false,
                    placeHolder = "-",
                    width = 100.dp
                )
                // unit
                Text(text = stringResource(R.string.txt_unit), style = MyTypography.bodySmall)
                val inverseColor = MaterialTheme.colorScheme.inversePrimary
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(39.dp)
                        .drawWithContent {
                            drawContent()
                            drawLine(
                                color = inverseColor,
                                start = Offset(0f, size.height),
                                end = Offset(size.width, size.height),
                                strokeWidth = 0.8.dp.toPx()
                            )
                        }
                        .clickable { unitsExpanded.value = !unitsExpanded.value },
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(text = unit.value.getString(context), style = MyTypography.bodyLarge)
                }
            }
        }
    }
    // DropdownMenu with measure units options
    if (unitsExpanded.value) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = (300).dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            DropdownMenu(
                expanded = true,
                onDismissRequest = { unitsExpanded.value = false },
                modifier = Modifier.width(120.dp)
            ) {
                Column(modifier = Modifier
                    .heightIn(max = 200.dp)
                    .verticalScroll(rememberScrollState())) {
                    measuresMap.forEach { (entry, stringID) ->
                        val optionText = stringResource(stringID)
                        DropdownMenuItem(
                            text = { Text(text = optionText, style = MyTypography.bodySmall) },
                            onClick = {
                                unit.value = entry
                                ingredient.unit = entry
                                onValueChange()
                                unitsExpanded.value = false
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Creates the look for an editable instruction step.
 *
 * @param number position of this step in the instructions list
 * @param last whether or not this step can be deleted
 * @param step user-input text that makes the instruction
 * @param onValueChange block that runs when changing the instruction text, returning the user input,
 * @param onDelete block that runs when deleting this step
 */
@Composable
fun StepItem(
    number: Int,
    last: Boolean,
    step: String,
    onValueChange: (String) -> Unit,
    onDelete: () -> Unit
) {
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
                // step numbering
                Text(
                    modifier = Modifier.padding(start = 16.dp),
                    text = stringResource(R.string.title_stepNb, number+1),
                    style = MyTypography.titleSmall
                )
                // delete button
                if (last) {
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
            CustomTextField(
                value = step,
                onValueChange = { onValueChange(it) },
                icon = -1,
                placeHolder = stringResource(R.string.field_instructions),
                singleLine = false,
                maxLength = 500,
                width = 350.dp,
                height = 260.dp
            )
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
 * @param required adds a red asterisk next to the title if true
 * @param tags all entries of a tag enum
 * @param filtersSet the current set of filters for this tag family
 * @param onlyOne if true only one tag can be enabled at once
 * @param getString the getString function that belongs to this enum of tags
 * @param onClick block that runs with the tag that got pressed
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> TagDropDown(
    title: String,
    required: Boolean = false,
    tags: List<T>,
    filtersSet: Set<T>,
    onlyOne: Boolean = false,
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
            Row(verticalAlignment = Alignment.CenterVertically)
            {
                Text(
                    text = title,
                    style = MyTypography.titleSmall
                )
                if (required) {
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = "*", style = MyTypography.titleSmall, color = Color.Red)
                }
            }
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
                        if (onlyOne) filtersSet.toMutableSet().apply {
                            this.clear()
                            this.add(tag)
                        }
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
                        if (onlyOne) filtersSet.toMutableSet().apply {
                            this.clear()
                            this.add(tag)
                        }
                        onClick(tag)
                    }
                }
            }
        }
    }
}

/**
 * Save button that always stays at the bottom of the screen.
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
                style = MyTypography.bodyLarge,
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
    Box(
        modifier = Modifier
            .background(
                color = if (enabled) MaterialTheme.colorScheme.inversePrimary
                else Color.Transparent,
                shape = RoundedCornerShape(50)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.inversePrimary,
                shape = RoundedCornerShape(50)
            )
            .clickable {
                onClick()
            }
    ) {
        Text(
            text = tagName,
            style = MyTypography.bodySmall,
            color = if (enabled) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.inversePrimary,
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 12.dp)
        )
    }
}