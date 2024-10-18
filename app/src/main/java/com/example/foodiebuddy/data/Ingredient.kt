package com.example.foodiebuddy.data

import android.net.Uri

data class OwnedIngredient(
    val uid: String,
    val displayedName: String,
    val standName: String,
    val category: String,
    var isTicked: Boolean
) {
    companion object {
        /**
         * Creates an empty User data object.
         *
         * @return empty User data object
         */
        fun empty(): OwnedIngredient {
            return OwnedIngredient("", "", "", "", false)
        }
    }
    /**
     * Checks if this User data object is empty.
     *
     * @return true if the User data object is empty
     */
    fun isEmpty(): Boolean {
        return this == empty()
    }
}