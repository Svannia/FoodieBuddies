package com.example.foodiebuddy.data

/**
 * Describes an ingredient that is owned by a user AKA it belongs to their fridge or groceries list.
 *
 * @property uid of the ingredient
 * @property displayedName the name display to the user
 * @property standName standardized name that will be used to compare against recipe ingredients
 * @property category which category the ingredient belongs to
 * @property isTicked whether or not the ingredient is ticked. For ingredients in the fridge, this property does not matter.
 */
data class OwnedIngredient(
    val uid: String,
    val displayedName: String,
    val standName: String,
    val category: String,
    var isTicked: Boolean
) {
    companion object {
        /**
         * Creates an empty Owned Ingredient object.
         *
         * @return empty Owned Ingredient object.
         */
        fun empty(): OwnedIngredient {
            return OwnedIngredient("", "", "", "", false)
        }
    }
    /**
     * Checks if this Owned Ingredient object is empty.
     *
     * @return true if the Owned Ingredient object is empty.
     */
    fun isEmpty(): Boolean {
        return this == empty()
    }
}

/**
 * Describes an ingredient that is used in a recipe.
 *
 * @property displayedName the name display to the user
 * @property standName standardized name that will be used to compare against recipe ingredients
 * @property quantity any sentence that the user uses to describe the required quantity
 */
data class RecipeIngredient(
    val displayedName: String,
    val standName: String,
    val quantity: String
) {
    companion object {
        /**
         * Creates an empty Recipe Ingredient object.
         *
         * @return empty Recipe Ingredient object.
         */
        fun empty(): RecipeIngredient {
            return RecipeIngredient("", "", "")
        }
    }
    /**
     * Checks if this Recipe Ingredient object is empty.
     *
     * @return true if the Recipe Ingredient object is empty.
     */
    fun isEmpty(): Boolean {
        return this == empty()
    }
}