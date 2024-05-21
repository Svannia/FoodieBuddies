package com.example.foodiebuddy


import android.os.Bundle
import android.util.Log
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
import com.example.foodiebuddy.database.DatabaseConnection
import com.example.foodiebuddy.errors.handleError
import com.example.foodiebuddy.navigation.NavigationActions
import com.example.foodiebuddy.navigation.Route
import com.example.foodiebuddy.ui.account.AccountSettings
import com.example.foodiebuddy.ui.account.CreateAccount
import com.example.foodiebuddy.ui.account.Profile
import com.example.foodiebuddy.ui.recipes.RecipesHome
import com.example.foodiebuddy.ui.settings.Settings
import com.example.foodiebuddy.ui.theme.FoodieBuddyTheme
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
                                        handleError(context, "Failed to check user existence", e)
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
                        composable(Route.PROFILE) {
                            val currentUser = remember { auth.currentUser }
                            if (currentUser != null) {
                                val userViewModel = remember { UserViewModel(currentUser.uid) }
                                Profile(userViewModel, navigationActions)
                                Log.d("Compose", "Successfully composed screen Account Settings")
                            }
                        }
                        composable(Route.ACCOUNT_SETTINGS) {
                            val currentUser = remember { auth.currentUser }
                            if (currentUser != null) {
                                val userViewModel = remember { UserViewModel(currentUser.uid) }
                                AccountSettings(userViewModel, navigationActions)
                                Log.d("Compose", "Successfully composed screen Profile")
                            }
                        }
                        // Composables for recipes-related routes
                        composable(Route.RECIPES_HOME) {
                            val currentUser = remember { auth.currentUser }
                            if (currentUser != null) {
                                val userViewModel = remember { UserViewModel(currentUser.uid) }
                                RecipesHome(userViewModel, navigationActions)
                                Log.d("Compose", "Successfully composed screen Recipes Home")
                            }
                        }
                        // Composables for settings and system
                        composable(Route.SETTINGS) {
                            val currentUser = remember { auth.currentUser }
                            if (currentUser != null) {
                                val userViewModel = remember { UserViewModel(currentUser.uid) }
                                Settings(userViewModel, navigationActions)
                                Log.d("Compose", "Successfully composed screen Settings")
                            }
                        }
                    }
                }
            }
        }
    }
}