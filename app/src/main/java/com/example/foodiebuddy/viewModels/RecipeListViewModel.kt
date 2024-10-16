package com.example.foodiebuddy.viewModels

import androidx.lifecycle.ViewModel
import javax.inject.Inject

/**
 * ViewModel for managing recipes-related in the Database.
 *
 * @property userID who is accessing the recipe data
 */
class RecipeListViewModel
@Inject
constructor(private val userID: String) : ViewModel() {

}
