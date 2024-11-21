package com.example.foodiebuddy.data

import android.net.Uri

/**
 * Describes a user's personal, private data.
 *
 * @property uid of the user
 * @property groceryList maps a user's grocery list: entry keys are categories, values are lists of ingredients
 * @property fridge maps a user's "my fridge": entry keys are categories, values are lists of ingredients
 */
data class UserPersonal(
    val uid: String,
    val groceryList: Map<String, List<OwnedIngredient>>,
    val fridge: Map<String, List<OwnedIngredient>>
) {
    companion object {
        /**
         * Creates an empty UserPersonal object.
         *
         * @return empty UserPersonal object.
         */
        fun empty(): UserPersonal {
            return UserPersonal("", emptyMap(), emptyMap())
        }
    }
    /**
     * Checks if this User data object is empty.
     *
     * @return true if the User data object is empty.
     */
    fun isEmpty(): Boolean {
        return this == empty()
    }
}
