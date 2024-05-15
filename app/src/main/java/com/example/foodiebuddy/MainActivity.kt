package com.example.foodiebuddy

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.foodiebuddy.ui.account.LoginScreen
import com.example.foodiebuddy.data.DatabaseConnection
import com.example.foodiebuddy.errors.HandleError
import com.example.foodiebuddy.navigation.NavigationActions
import com.example.foodiebuddy.navigation.Route
import com.example.foodiebuddy.errors.isNetworkAvailable
import com.example.foodiebuddy.ui.account.CreateAccount
import com.example.foodiebuddy.ui.recipes.RecipesHome
import com.example.foodiebuddy.ui.theme.FoodieBuddyTheme
import com.example.foodiebuddy.viewModels.RecipeListViewModel
import com.example.foodiebuddy.viewModels.UserViewModel
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        val db = DatabaseConnection()

        setContent {
            FoodieBuddyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current
                    val navController = rememberNavController()
                    val navigationActions = NavigationActions(navController)
                    val startDestination = Route.START

                    NavHost(navController, startDestination) {
                        composable(Route.START) {
                            val currentUser = remember { auth.currentUser }
                            if (currentUser != null) {
                                db.userExists(
                                    uid = currentUser.uid,
                                    onSuccess = { userExists ->
                                        if (userExists) {
                                            navController.navigate(Route.RECIPES_HOME)
                                        } else {
                                            navController.navigate(Route.CREATE_ACCOUNT)
                                        }
                                    },
                                    onFailure = { e ->
                                        navController.navigate(Route.LOGIN)
                                        HandleError(context, "Failed to check user existence", e)
                                    }
                                )
                            } else {
                                navController.navigate(Route.LOGIN)
                            }
                        }
                        // Composables for account-related routes
                        composable(Route.LOGIN) {
                            LoginScreen(navigationActions)
                            Log.d("Nav", "Successfully navigated to Login screen")
                        }
                        composable(Route.CREATE_ACCOUNT) {
                            if (auth.currentUser != null) {
                                val userViewModel = remember { UserViewModel() }
                                CreateAccount(userViewModel, navigationActions)
                                Log.d("Nav", "Successfully navigated to Create Account")
                            }
                        }
                        // Composables for recipes-related routes
                        composable(Route.RECIPES_HOME) {
                            val currentUser = remember { auth.currentUser }
                            if (currentUser != null) {
                                val recipesViewModel = remember { RecipeListViewModel(currentUser.uid) }
                                RecipesHome(currentUser.uid, recipesViewModel, navigationActions)
                                Log.d("Nav", "Successfully navigated to Create Account")
                            }
                        }
                    }
                }
            }
        }
    }
}