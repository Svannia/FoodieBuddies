package com.example.foodiebuddy.data

import android.net.Uri

/**
 * Describes the data related to a user's profile.
 *
 * @property uid unique ID representing the user, first set when they authenticate with Google
 * @property username mandatory for creating an account
 * @property picture either one chosen by the user, or the default profile picture
 * @property numberRecipes added by this user. This field cannot be edited directly by the user
 * @property bio optional description text that the user can add on their profile
 */
data class User(val uid: String, val username: String, val picture: Uri, val numberRecipes: Int, val bio: String) {
    companion object {
        /**
         * Creates an empty User data object.
         *
         * @return empty User data object
         */
        fun empty(): User {
            return User("", "", Uri.EMPTY, 0, "")
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