package com.example.foodiebuddy.data

import android.content.Context
import com.example.foodiebuddy.R

// The Origin indicates from which country or region the recipe originates from.
enum class Origin {
    NONE, HOMEMADE, SWISS, FRENCH, ITALIAN, SPANISH, PORTUGUESE, GERMAN, BRITISH, NORDIC, GREEK, EASTERN_EUROPEAN,
    INDIAN, THAI, VIETNAMESE, CHINESE, JAPANESE, KOREAN, ASIAN,
    TURKISH, LEBANESE, MOROCCAN, AFRICAN,
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
    Origin.BRITISH to R.string.origin_british,
    Origin.NORDIC to R.string.origin_nordic,
    Origin.GREEK to R.string.origin_greek,
    Origin.EASTERN_EUROPEAN to R.string.origin_eastern_european,
    Origin.INDIAN to R.string.origin_indian,
    Origin.THAI to R.string.origin_thai,
    Origin.VIETNAMESE to R.string.origin_vietnamese,
    Origin.CHINESE to R.string.origin_chinese,
    Origin.JAPANESE to R.string.origin_japanese,
    Origin.KOREAN to R.string.origin_korean,
    Origin.ASIAN to R.string.origin_asian,
    Origin.TURKISH to R.string.origin_turkish,
    Origin.LEBANESE to R.string.origin_lebanese,
    Origin.MOROCCAN to R.string.origin_moroccan,
    Origin.AFRICAN to R.string.origin_african,
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
    NONE, QUICK_MEAL, LONG_PREP_TIME, ONE_POT, MAIN_DISH, SIDE_DISH, SWEET_SNACK, SAVORY_SNACK, APPETIZER, STARTER, DESSERT, DRINK, COCKTAIL
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
    Tag.DESSERT to R.string.tag_dessert,
    Tag.DRINK to R.string.tag_drink,
    Tag.COCKTAIL to R.string.tag_cocktail
)
/**
 * Translates a Tag element into its corresponding string from strings.xml.
 *
 * @param context used to access the string resources
 * @return user-readable string
 */
fun Tag.getString(context: Context): String {
    return context.getString(tagMap[this] ?: R.string.tag_none)
}