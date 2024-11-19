package com.example.foodiebuddy.data

import android.net.Uri

/**
 * Describe a Recipe object
 *
 * @property uid of the recipe
 * @property owner UID of the user who created the recipe
 */
data class Recipe(val uid: String, val owner: String) {

    companion object {
        /**
         * Creates an empty Recipe data object.
         *
         * @return empty Recipe data object.
         */
        fun empty(): Recipe {
            return Recipe("", "")
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
