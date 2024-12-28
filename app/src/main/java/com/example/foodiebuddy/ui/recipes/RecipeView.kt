package com.example.foodiebuddy.ui.recipes

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.motionEventSpy
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.foodiebuddy.R
import com.example.foodiebuddy.data.Diet
import com.example.foodiebuddy.data.Measure
import com.example.foodiebuddy.data.Origin
import com.example.foodiebuddy.data.Recipe
import com.example.foodiebuddy.data.RecipeIngredient
import com.example.foodiebuddy.data.Tag
import com.example.foodiebuddy.data.getString
import com.example.foodiebuddy.errors.handleError
import com.example.foodiebuddy.navigation.NavigationActions
import com.example.foodiebuddy.navigation.Route
import com.example.foodiebuddy.ui.LoadingPage
import com.example.foodiebuddy.ui.SecondaryScreen
import com.example.foodiebuddy.ui.SquareImage
import com.example.foodiebuddy.ui.theme.MyTypography
import com.example.foodiebuddy.viewModels.RecipeViewModel
import com.example.foodiebuddy.viewModels.UserViewModel
import kotlin.concurrent.timerTask

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
    val origin = remember { mutableStateOf(Origin.NONE) }
    val diet = remember { mutableStateOf(Diet.NONE) }
    val tags = remember { mutableStateListOf<Tag>() }
    val favouriteOf = remember { mutableStateListOf<String>() }

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
                ownerName.value = recipe.ownerName
                name.value = recipe.name
                picture.value = recipe.picture
                instructions.clear()
                instructions.addAll(recipe.instructions)
                ingredients.clear()
                ingredients.addAll(recipe.ingredients)
                origin.value = recipe.origin
                diet.value = recipe.diet
                tags.clear()
                tags.addAll(recipe.tags)
                favouriteOf.clear()
                favouriteOf.addAll(recipe.favouriteOf)
            } else {
                handleError(context, "Could not fetch data, recipe is empty")
            }
            loadingData.value = false
        }
    }
    LaunchedEffect(recipeData) {
        if (recipeData != Recipe.empty()) {
            ownerID.value = recipeData.owner
            ownerName.value = recipeData.ownerName
            name.value = recipeData.name
            picture.value = recipeData.picture
            instructions.clear()
            instructions.addAll(recipeData.instructions)
            ingredients.clear()
            ingredients.addAll(recipeData.ingredients)
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
                                contentDescription = stringResource(R.string.desc_recipePicture))
                        }
                    }
                    // title, creator and tags
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // star icon
                            IconButton(
                                onClick = {
                                    if (isFavourite) {
                                        recipeVM.removeUserFromFavourites(uid, {
                                            if (it) handleError(context, "Could not remove favourite")
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
                                text =  if (isFavourite) stringResource(R.string.txt_removeFav)
                                        else stringResource(R.string.txt_addFav),
                                style = MyTypography.bodySmall
                            )
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
                            Spacer(modifier = Modifier.size(16.dp))
                            Text(text = stringResource(R.string.title_ingredients), style = MyTypography.titleMedium)
                            ingredients.forEach { ingredient ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text =
                                        formatQuantity(ingredient.quantity)
                                            + " " + (if (ingredient.unit == Measure.NONE) "" else ingredient.unit.getString(context)),
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
                        }
                    }
                }
        }
    }
}

@SuppressLint("DefaultLocale")
private fun formatQuantity(quantity: Float): String {
    return when {
        quantity == 0f -> ""
        quantity % 1 == 0f -> quantity.toInt().toString()
        quantity % 1 == 0.5f -> if (quantity.toInt() == 0) "½" else "${quantity.toInt()}½"
        quantity % 1 == 0.25f -> if (quantity.toInt() == 0) "¼" else "${quantity.toInt()}¼"
        quantity % 1 == 0.75f -> if (quantity.toInt() == 0) "¾" else "${quantity.toInt()}¾"
        else -> String.format("%.2f", quantity)
    }
}
