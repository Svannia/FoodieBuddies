package com.example.foodiebuddy.ui.recipes

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.foodiebuddy.R
import com.example.foodiebuddy.data.Diet
import com.example.foodiebuddy.data.Measure
import com.example.foodiebuddy.data.Origin
import com.example.foodiebuddy.data.Recipe
import com.example.foodiebuddy.data.RecipeIngredient
import com.example.foodiebuddy.data.Tag
import com.example.foodiebuddy.data.getString
import com.example.foodiebuddy.data.plural
import com.example.foodiebuddy.errors.handleError
import com.example.foodiebuddy.navigation.NavigationActions
import com.example.foodiebuddy.navigation.Route
import com.example.foodiebuddy.ui.LoadingPage
import com.example.foodiebuddy.ui.SecondaryScreen
import com.example.foodiebuddy.ui.SquareImage
import com.example.foodiebuddy.ui.theme.MyTypography
import com.example.foodiebuddy.viewModels.RecipeViewModel
import com.example.foodiebuddy.viewModels.UserViewModel
import java.text.DecimalFormat

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipeView(userVM: UserViewModel, recipeVM: RecipeViewModel, navigationActions: NavigationActions) {
    val context = LocalContext.current
    val loadingData = remember { mutableStateOf(true) }

    val uid = userVM.getCurrentUserID()
    val recipeData by recipeVM.recipeData.collectAsState()
    val recipeID = recipeVM.getVmUid()

    val ownerID = remember { mutableStateOf("") }
    val ownerName = remember { mutableStateOf("") }
    val name = remember { mutableStateOf("") }
    val picture = remember { mutableStateOf(Uri.EMPTY) }
    val instructions = remember { mutableStateListOf("") }
    val ingredients = remember { mutableStateListOf<RecipeIngredient>() }
    val portion = remember { mutableIntStateOf(1) }
    val perPerson = remember { mutableStateOf(true) }
    val origin = remember { mutableStateOf(Origin.NONE) }
    val diet = remember { mutableStateOf(Diet.NONE) }
    val tags = remember { mutableStateListOf<Tag>() }
    val favouriteOf = remember { mutableStateListOf<String>() }

    val customPortion = remember { mutableIntStateOf(1) }
    val customQuantities = remember { mutableStateListOf<Float>()
    }

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
                ownerID.value = recipe.owner
                name.value = recipe.name
                picture.value = recipe.picture
                instructions.clear()
                instructions.addAll(recipe.instructions)
                ingredients.clear()
                ingredients.addAll(recipe.ingredients)
                customQuantities.clear()
                customQuantities.addAll(recipe.ingredients.map { it.quantity })
                portion.intValue = recipe.portion
                customPortion.intValue = recipe.portion
                perPerson.value = recipe.perPerson
                origin.value = recipe.origin
                diet.value = recipe.diet
                tags.clear()
                tags.addAll(recipe.tags)
                favouriteOf.clear()
                favouriteOf.addAll(recipe.favouriteOf)
            } else {
                handleError(context, "Could not fetch data, recipe is empty")
            }
            userVM.fetchSomeUsername(ownerID.value, {
                if (it) {
                    handleError(context, "Could not fetch owner's username")
                    loadingData.value = false
                }
            }) {
                ownerName.value = it
                loadingData.value = false
            }
        }
    }
    LaunchedEffect(recipeData) {
        if (recipeData != Recipe.empty()) {
            ownerID.value = recipeData.owner
            name.value = recipeData.name
            picture.value = recipeData.picture
            instructions.clear()
            instructions.addAll(recipeData.instructions)
            ingredients.clear()
            ingredients.addAll(recipeData.ingredients)
            customQuantities.clear()
            customQuantities.addAll(recipeData.ingredients.map { it.quantity })
            portion.intValue = recipeData.portion
            customPortion.intValue = recipeData.portion
            perPerson.value = recipeData.perPerson
            origin.value = recipeData.origin
            diet.value = recipeData.diet
            tags.clear()
            tags.addAll(recipeData.tags)
            favouriteOf.clear()
            favouriteOf.addAll(recipeData.favouriteOf)
        }
    }

    if (loadingData.value) LoadingPage()
    else {
        SecondaryScreen(
            title = stringResource(R.string.title_recipe),
            navigationActions = navigationActions,
            route = Route.RECIPES_HOME,
            navExtraActions = {},
            topBarIcons = {
                // only show Edit button if the current user is this recipe's creator
                if (uid == "" || ownerID.value.isBlank()) {
                    handleError(context, "Could not compare UID of current user and recipe owner")
                } else if (uid == ownerID.value) {
                    Text(
                        text = stringResource(R.string.button_edit),
                        style = MyTypography.bodySmall,
                        modifier = Modifier.clickable {
                            navigationActions.navigateTo("${Route.RECIPE_EDIT}/$recipeID")
                        }
                    )
                }
            }) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // big picture (if there is one)
                    item {
                        if (picture.value != Uri.EMPTY) {
                            SquareImage(
                                size = LocalConfiguration.current.screenWidthDp.dp,
                                picture = picture.value,
                                contentDescription = stringResource(R.string.desc_recipePicture)
                            )
                        }
                    }
                    // title, creator and tags
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // recipe name
                            Text(text = name.value, style = MyTypography.titleMedium)
                            // creator name
                            Text(
                                text = stringResource(R.string.txt_recipeCreator, ownerName.value),
                                style = MyTypography.bodySmall,
                            )
                            // list of tags
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(
                                    8.dp,
                                    Alignment.CenterHorizontally
                                ),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (origin.value != Origin.NONE) {
                                    TagLabel(origin.value.getString(context))
                                }
                                if (diet.value != Diet.NONE) {
                                    TagLabel(diet.value.getString(context))
                                }
                                if (tags.isNotEmpty()) {
                                    tags.forEach { tag ->
                                        TagLabel(tag.getString(context))
                                    }
                                }
                            }
                        }
                    }
                    // add to favourite
                    item {
                        val isFavourite = favouriteOf.contains(uid)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // star icon
                            IconButton(
                                onClick = {
                                    if (isFavourite) {
                                        recipeVM.removeUserFromFavourites(uid, {
                                            if (it) handleError(
                                                context,
                                                "Could not remove favourite"
                                            )
                                        }) {}
                                    } else {
                                        recipeVM.addUserToFavourites(uid, {
                                            if (it) handleError(context, "Could not add favourite")
                                        }) {}
                                    }
                                }
                            ) {
                                Icon(
                                    modifier = Modifier.size(28.dp),
                                    painter = painterResource(
                                        if (isFavourite) R.drawable.star_filled
                                        else R.drawable.star
                                    ),
                                    contentDescription = stringResource(R.string.desc_favouriteIcon)
                                )
                            }
                            // accompanying text
                            Text(
                                text = if (isFavourite) stringResource(R.string.txt_removeFav)
                                else stringResource(R.string.txt_addFav),
                                style = MyTypography.bodySmall
                            )
                        }
                    }
                    // ingredients title
                    item {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(R.string.title_ingredients),
                            style = MyTypography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                    // portion functionality
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // row with custom portion setter
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // minus button
                                IconButton(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.inversePrimary,
                                            RoundedCornerShape(20)
                                        )
                                        .padding(6.dp),
                                    onClick = {
                                        if (customPortion.intValue > 1) {
                                            customPortion.intValue--
                                            adjustIngredients(customPortion.intValue, portion.intValue, customQuantities, ingredients)
                                        }
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.minus),
                                        contentDescription = stringResource(R.string.desc_remove)
                                    )
                                }
                                // portion indicator
                                Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally)
                                {
                                    Text(
                                        text = customPortion.intValue.toString(),
                                        style = MyTypography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = if (perPerson.value) {
                                            if (customPortion.intValue > 1) stringResource(R.string.txt_people)
                                            else stringResource(R.string.txt_person)
                                        } else {
                                            if (customPortion.intValue > 1) stringResource(R.string.txt_pieces)
                                            else stringResource(R.string.txt_piece)
                                        },
                                        style = MyTypography.bodyMedium
                                    )
                                }
                                // plus button
                                IconButton(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.inversePrimary,
                                            RoundedCornerShape(20)
                                        )
                                        .padding(6.dp),
                                    onClick = {
                                        if (customPortion.intValue < 20) {
                                            customPortion.intValue++
                                            adjustIngredients(customPortion.intValue, portion.intValue, customQuantities, ingredients)
                                        }
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.plus),
                                        contentDescription = stringResource(R.string.desc_add)
                                    )
                                }
                            }
                            // eventual portion reset
                            if (customPortion.intValue != portion.intValue) {
                                Text(
                                    modifier = Modifier.clickable {
                                        customPortion.intValue = portion.intValue
                                        customQuantities.clear()
                                        customQuantities.addAll(ingredients.map { it.quantity })
                                    },
                                    text = stringResource(R.string.txt_resetQty),
                                    style = MyTypography.bodySmall.copy(textDecoration = TextDecoration.Underline)
                                )
                            }
                            Spacer(modifier = Modifier.size(16.dp))
                        }
                    }
                    // list of ingredients
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ingredients.forEachIndexed { index, ingredient ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text =
                                        formatQuantity(customQuantities[index]) + "  "
                                            + formatUnit(ingredient.unit, customQuantities[index], context),
                                        style = MyTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.weight(1.5f)
                                    )
                                    Spacer(modifier = Modifier.size(8.dp))
                                    Text(
                                        text = ingredient.displayedName,
                                        style = MyTypography.bodyMedium,
                                        modifier = Modifier.weight(3f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.size(16.dp))
                        }
                    }
                    // instructions title
                    item {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(R.string.title_recipeInstructions),
                            style = MyTypography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                    // instruction steps
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            instructions.forEachIndexed { index, step ->
                                Column(
                                    modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically))
                                {
                                    Text(
                                        text = stringResource(R.string.title_stepNb, index+1),
                                        style = MyTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    )
                                    Text(
                                        text = step,
                                        style = MyTypography.bodyMedium,
                                    )
                                }
                            }
                        }
                    }
                }
            }
    }
}

/**
 * Adjusts all ingredients quantities depending on the custom portion number.
 *
 * @param customPortion portion chosen by the user
 * @param originalPortion original portion for the original recipe
 * @param customQuantities mutable list containing only the quantities of all ingredients
 * @param ingredients original list of ingredients
 */
private fun adjustIngredients(customPortion: Int, originalPortion: Int, customQuantities: MutableList<Float>, ingredients: List<RecipeIngredient>) {
    customQuantities.forEachIndexed { index, _ ->
        customQuantities[index] = ingredients[index].quantity * customPortion / originalPortion
    }
}

/**
 * Formats the ingredient quantity to remove useless decimal points and change frequent decimal quantities into fractions.
 *
 * @param quantity quantity to format as a float
 * @return formatted string
 */
private fun formatQuantity(quantity: Float): String {
    return when {
        quantity == 0f -> ""
        quantity % 1 == 0f -> quantity.toInt().toString()
        quantity % 1 == 0.5f -> if (quantity.toInt() == 0) "½" else "${quantity.toInt()}½"
        quantity % 1 == 0.25f -> if (quantity.toInt() == 0) "¼" else "${quantity.toInt()}¼"
        quantity % 1 == 0.75f -> if (quantity.toInt() == 0) "¾" else "${quantity.toInt()}¾"
        else -> {
            val decimalFormat = DecimalFormat("#.##")
            decimalFormat.format(quantity)
        }
    }
}

/**
* Formats the ingredient unit of measure to remove NONE units and add plural form when necessary.
*
* @param unit Measure objects to format
* @param quantity quantity to check for plural form
* @return formatted string
*/
private fun formatUnit(unit: Measure, quantity: Float, context: Context): String {
    return if (unit == Measure.NONE) "" else {
        if (quantity > 1f) unit.plural(context)
        else unit.getString(context)
    }
}
