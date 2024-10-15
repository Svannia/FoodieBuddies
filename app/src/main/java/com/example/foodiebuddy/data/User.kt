package com.example.foodiebuddy.data

import android.net.Uri

data class User(val uid: String, val username: String, val picture: Uri, val numberRecipes: Int, val bio: String) {
    companion object {
        fun empty(): User {
            return User("", "", Uri.EMPTY, 0, "")
        }
    }
    fun isEmpty(): Boolean {
        return this == User.empty()
    }
}