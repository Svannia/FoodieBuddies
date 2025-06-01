package com.example.foodiebuddy.data

import android.net.Uri

/**
 * Describe a Recipe object
 *
 * @property uid of the recipe
 * @property owner UID of the user who created the recipe
 * @property name title of the recipe
 * @property pictures list of pictures for the recipe (can be an empty list)
 * @property instructions list of strings where each element represents a step of the cooking instructions
 * @property ingredients maps sections names to lists of RecipeIngredient objects
 * @property portion number that indicates for how many servings this recipe is designed
 * @property perPerson if true, the portion is per person, if false it is per piece
 * @property origin origin tag from Origin enum
 * @property diet diet tag from Diet enum
 * @property tags list of tags from Tag enum
 * @property favouriteOf list of UID from all the users who have this recipe in their favourites
 */
data class Recipe(
    val uid: String,
    val owner: String,
    val name: String,
    val pictures: List<Uri>,
    val instructions: List<String>,
    val ingredients: Map<String,List<RecipeIngredient>>,
    val portion: Int,
    val perPerson: Boolean,
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
            return Recipe("", "", "", emptyList(), listOf(""), emptyMap(), 1, true,  Origin.NONE, Diet.NONE, emptyList(), emptyList())
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

/**
 * Describe a Recipe Draft object
 *
 * @property id uid of the draft
 * @property name title of the recipe
 * @property pictures optional list of pictures of the recipe
 * @property instructions list of strings where each element represents a step of the cooking instructions
 * @property ingredients maps section names to lists of mappings that represent the various ingredients attributes
 * @property sectionsOrder list of section names in the order they should be displayed
 * @property portion number that indicates for how many servings this recipe is designed for
 * @property perPerson if true, the portion is per person, if false it is per piece
 * @property origin origin tag from Origin enum
 * @property diet diet tag from Diet enum
 * @property tags list of tags from Tag enum
 */
data class RecipeDraft(
    val id: String,
    val name: String,
    val pictures: List<String>,
    val instructions: List<String>,
    val ingredients: Map<String, List<Map<String, String>>>,
    val sectionsOrder: List<String>,
    val portion: Int,
    val perPerson: Boolean,
    val origin: Origin,
    val diet: Diet,
    val tags: List<Tag>,
) {
    companion object {
        /**
         * Creates an empty RecipeDraft data object.
         *
         * @return empty RecipeDraft data object.
         */
        fun empty(): RecipeDraft {
            return RecipeDraft("", "", emptyList(), listOf(""), emptyMap(), emptyList(),
                1, true, Origin.NONE, Diet.NONE, emptyList())
        }
    }
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