package com.example.foodiebuddy

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.currentCompositionErrors
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.foodiebuddy.ui.account.LoginScreen
import com.example.foodiebuddy.database.DatabaseConnection
import com.example.foodiebuddy.errors.HandleError
import com.example.foodiebuddy.navigation.NavigationActions
import com.example.foodiebuddy.navigation.Route
import com.example.foodiebuddy.ui.account.CreateAccount
import com.example.foodiebuddy.ui.account.SetProfilePicture
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
                    val thisUser = remember { auth.currentUser }
                    val startDestination = Route.START


                    NavHost(navController, startDestination) {
                        composable(Route.START) {
                            val currentUser = remember { auth.currentUser }
                            if (currentUser != null) {
                                Log.d("Login", "Logging in with user ${currentUser.uid}")
                                db.userExists(
                                    uid = currentUser.uid,
                                    onSuccess = { userExists ->
                                        if (userExists) {
                                            Log.d("Login", "Successfully logged in user ${currentUser.uid}")
                                            navigationActions.navigateTo(Route.RECIPES_HOME)
                                        } else {
                                            Log.d("Login", "Creating account for user ${currentUser.uid}")
                                            navigationActions.navigateTo(Route.CREATE_ACCOUNT)
                                        }
                                    },
                                    onFailure = { e ->
                                        HandleError(context, "Failed to check user existence", e)
                                        navigationActions.navigateTo(Route.LOGIN)
                                    }
                                )
                            } else {
                                navigationActions.navigateTo(Route.LOGIN)
                            }
                        }
                        // Composables for account-related routes
                        composable(Route.LOGIN) {
                            LoginScreen(navigationActions)
                            Log.d("Compose", "Successfully composed screen Login screen")
                        }
                        composable(Route.CREATE_ACCOUNT) {
                            val currentUser = remember { auth.currentUser }
                            if (currentUser != null) {
                                val userViewModel = remember { UserViewModel(currentUser.uid) }
                                CreateAccount(userViewModel, navigationActions)
                                Log.d("Compose", "Successfully composed screen Create Account")
                            }
                        }
                        // Composables for recipes-related routes
                        composable(Route.RECIPES_HOME) {
                            val currentUser = remember { auth.currentUser }
                            if (currentUser != null) {
                                val recipesViewModel = remember { RecipeListViewModel(currentUser.uid) }
                                RecipesHome(currentUser.uid, recipesViewModel, navigationActions)
                                Log.d("Compose", "Successfully composed screen Recipes Home")
                            }
                        }
                    }
                }
            }
        }
    }
}