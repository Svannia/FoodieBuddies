package com.example.foodiebuddy.data

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.runtime.saveable.Saver
import com.example.foodiebuddy.R

/**
 * Describe a Recipe object
 *
 * @property uid of the recipe
 * @property owner UID of the user who created the recipe
 * @property ownerName username of the recipe author
 * @property name title of the recipe
 * @property picture optional picture of the recipe
 * @property instructions list of strings where each element represents a step of the cooking instructions
 * @property ingredients a list of RecipeIngredient objects representing the ingredients for the recipe
 * @property origin origin tag from Origin enum
 * @property diet diet tag from Diet enum
 * @property tags list of tags from Tag enum
 * @property favouriteOf list of UID from all the users who have this recipe in their favourites
 */
data class Recipe(
    val uid: String,
    val owner: String,
    val ownerName: String,
    val name: String,
    val picture: Uri,
    val instructions: List<String>,
    val ingredients: List<RecipeIngredient>,
    val origin: Origin,
    val diet: Diet,
    val tags: List<Tag>,
    val favouriteOf: List<String>
) {

    companion object {
        /**
         * Creates an empty Recipe data object.
         *
         * @return empty Recipe data object.
         */
        fun empty(): Recipe {
            return Recipe("", "", "", "", Uri.EMPTY, listOf(""), emptyList(), Origin.NONE, Diet.NONE, emptyList(), emptyList())
        }
    }
    /**
     * Checks if this Recipe data object is empty.
     *
     * @return true if the Recipe data object is empty.
     */
    fun isEmpty(): Boolean {
        return this == empty()
    }

}

// The Origin indicates from which country or region the recipe originates from.
enum class Origin {
    NONE, HOMEMADE, SWISS, FRENCH, ITALIAN, SPANISH, PORTUGUESE, GERMAN, ENGLISH, SWEDISH, GREEK, EASTERN_EUROPEAN,
    INDIAN, THAI, VIETNAMESE, CHINESE, JAPANESE, KOREAN,
    TURKISH, LEBANESE, MOROCCAN, SOUTH_AFRICAN,
    AMERICAN, MEXICAN, PERUVIAN
}
val originMap = mapOf(
    Origin.HOMEMADE to R.string.origin_homemade,
    Origin.SWISS to R.string.origin_swiss,
    Origin.FRENCH to R.string.origin_french,
    Origin.ITALIAN to R.string.origin_italian,
    Origin.SPANISH to R.string.origin_spanish,
    Origin.PORTUGUESE to R.string.origin_portuguese,
    Origin.GERMAN to R.string.origin_german,
    Origin.ENGLISH to R.string.origin_english,
    Origin.SWEDISH to R.string.origin_swedish,
    Origin.GREEK to R.string.origin_greek,
    Origin.EASTERN_EUROPEAN to R.string.origin_eastern_european,
    Origin.INDIAN to R.string.origin_indian,
    Origin.THAI to R.string.origin_thai,
    Origin.VIETNAMESE to R.string.origin_vietnamese,
    Origin.CHINESE to R.string.origin_chinese,
    Origin.JAPANESE to R.string.origin_japanese,
    Origin.KOREAN to R.string.origin_korean,
    Origin.TURKISH to R.string.origin_turkish,
    Origin.LEBANESE to R.string.origin_lebanese,
    Origin.MOROCCAN to R.string.origin_moroccan,
    Origin.SOUTH_AFRICAN to R.string.origin_south_african,
    Origin.AMERICAN to R.string.origin_american,
    Origin.MEXICAN to R.string.origin_mexican,
    Origin.PERUVIAN to R.string.origin_peruvian
)
/**
 * Translates an Origin element into its corresponding string from strings.xml.
 *
 * @param context used to access the string resources
 * @return user-readable string
 */
fun Origin.getString(context: Context): String {
    return context.getString(originMap[this] ?: R.string.origin_none)
}

// The Diet indicates the type of meat diet this recipe is.
enum class Diet {
    NONE, MEAT, FISH, VEGETARIAN
}
val dietMap = mapOf(
    Diet.NONE to R.string.diet_none,
    Diet.MEAT to R.string.diet_meat,
    Diet.FISH to R.string.diet_fish,
    Diet.VEGETARIAN to R.string.diet_vegetarian
)
/**
 * Translates a Diet element into its corresponding string from strings.xml.
 *
 * @param context used to access the string resources
 * @return user-readable string
 */
fun Diet.getString(context: Context): String {
    return context.getString(dietMap[this] ?: R.string.diet_none)
}

// The Tag indicates various information about the recipe.
enum class Tag {
    NONE, QUICK_MEAL, LONG_PREP_TIME, ONE_POT, MAIN_DISH, SIDE_DISH, SWEET_SNACK, SAVORY_SNACK, APPETIZER, STARTER, DESSERT
}
val tagMap = mapOf(
    Tag.NONE to R.string.tag_none,
    Tag.QUICK_MEAL to R.string.tag_quick_meal,
    Tag.LONG_PREP_TIME to R.string.tag_long_prep_time,
    Tag.ONE_POT to R.string.tag_one_pot,
    Tag.MAIN_DISH to R.string.tag_main_dish,
    Tag.SIDE_DISH to R.string.tag_side_dish,
    Tag.SWEET_SNACK to R.string.tag_sweet_snack,
    Tag.SAVORY_SNACK to R.string.tag_savory_snack,
    Tag.APPETIZER to R.string.tag_appetizer,
    Tag.STARTER to R.string.tag_starter,
    Tag.DESSERT to R.string.tag_dessert
    )
/**
 * Translates a Tag element into its corresponding string from strings.xml.
 *
 * @param context used to access the string resources
 * @return user-readable string
 */fun Tag.getString(context: Context): String {
    return context.getString(tagMap[this] ?: R.string.tag_none)
}

/**
 * Contains all the different filters that can be applied on the list of recipes.
 *
 * @property keywords list of words that are looked for in the recipe title
 * @property authors set of UIDs from recipe authors to filter
 * @property origins set of Origin enum elements to filter
 * @property diets set of Diet enum elements to filter
 * @property tags set of Tag enum elements to filter
 * @property requireOwnedIngredients whether or not to filter recipes that only require owned ingredients
 * @property requireFavourite whether or not to filter recipes that are in the user's favourites
 */
data class RecipeFilters(
    val keywords: List<String>,
    val authors: Set<String>,
    val origins: Set<Origin>,
    val diets: Set<Diet>,
    val tags: Set<Tag>,
    val requireOwnedIngredients: Boolean,
    val requireFavourite: Boolean
) {

    companion object {
        /**
         * Creates an empty RecipeFilter data object.
         *
         * @return empty RecipeFilter data object.
         */
        fun empty(): RecipeFilters {
            return RecipeFilters(emptyList(), emptySet(), emptySet(), emptySet(), emptySet(),
                requireOwnedIngredients = false,
                requireFavourite = false
            )
        }
    }

    /**
     * Checks if this RecipeFilter data object is empty.
     *
     * @return true if the RecipeFilter data object is empty.
     */
    fun isEmpty(): Boolean {
        return this == empty()
    }

}