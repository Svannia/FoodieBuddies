package com.example.foodiebuddy

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.foodiebuddy.ui.account.LoginScreen
import com.example.foodiebuddy.database.DatabaseConnection
import com.example.foodiebuddy.errors.handleError
import com.example.foodiebuddy.navigation.NavigationActions
import com.example.foodiebuddy.navigation.Route
import com.example.foodiebuddy.ui.account.AccountSettings
import com.example.foodiebuddy.ui.account.Buddies
import com.example.foodiebuddy.ui.account.CreateAccount
import com.example.foodiebuddy.ui.account.Profile
import com.example.foodiebuddy.ui.ingredients.FridgeHome
import com.example.foodiebuddy.ui.ingredients.GroceriesHome
import com.example.foodiebuddy.ui.recipes.RecipeEdit
import com.example.foodiebuddy.ui.recipes.RecipeView
import com.example.foodiebuddy.ui.recipes.RecipesHome
import com.example.foodiebuddy.ui.settings.Settings
import com.example.foodiebuddy.ui.theme.FoodieBuddyTheme
import com.example.foodiebuddy.viewModels.OfflinePreferencesViewModel
import com.example.foodiebuddy.viewModels.RecipeViewModel
import com.example.foodiebuddy.viewModels.UserViewModel
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private var startDestination = Route.START

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        auth = FirebaseAuth.getInstance()
        val db = DatabaseConnection()

        setContent {
            val offlinePrefViewModel: OfflinePreferencesViewModel = viewModel()
            val currentTheme by offlinePrefViewModel.currentTheme.collectAsState()

            FoodieBuddyTheme(themeChoice = currentTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current

                    val navController = rememberNavController()
                    val navigationActions = NavigationActions(navController)

                    var userVM: UserViewModel = viewModel()

                    NavHost(navController, startDestination) {
                        composable(Route.START) {
                            val currentUser = remember { auth.currentUser }
                            // if there currently exists an authenticated user ->
                            if (currentUser != null) {
                                userVM = viewModel {
                                    UserViewModel(currentUser.uid)
                                }
                                Log.d("Login", "Logging in with user ${currentUser.uid}")
                                // check if the user profile exists ->
                                db.userExists(
                                    userID = currentUser.uid,
                                    onSuccess = { userExists ->
                                        // if the user exists -> start app on the home page
                                        if (userExists) {
                                            Log.d("Login", "Successfully logged in user ${currentUser.uid}")
                                            navigationActions.navigateTo(Route.RECIPES_HOME)
                                        // if the user does not exist (it means they have already auth with Google but haven't finished creating an account
                                        // -> start app on the create account page
                                        } else {
                                            Log.d("Login", "Creating account for user ${currentUser.uid}")
                                            navigationActions.navigateTo(Route.CREATE_ACCOUNT)
                                        }
                                    },
                                    // if checking for user existence failed -> start app on login page
                                    onFailure = { e ->
                                        handleError(context, "Failed to check user existence", e)
                                        navigationActions.navigateTo(Route.LOGIN)
                                    }
                                )
                            // if there is no currently authenticated user -> start app on login page
                            } else {
                                navigationActions.navigateTo(Route.LOGIN)
                            }
                        }


                        // Composables for account-related routes
                        composable(Route.LOGIN) {
                            userVM = viewModel()
                            LoginScreen(navigationActions)
                            Log.d("Compose", "Successfully composed screen Login screen")
                        }
                        composable(Route.CREATE_ACCOUNT) {
                            val currentUser = remember { auth.currentUser }
                            if (currentUser != null) {
                                userVM = viewModel {
                                    UserViewModel(currentUser.uid)
                                }
                                CreateAccount(userVM, navigationActions)
                                Log.d("Compose", "Successfully composed screen Create Account")
                            }
                        }
                        composable(Route.PROFILE) {
                            val currentUser = remember { auth.currentUser }
                            if (currentUser != null) {
                                Profile(userVM, navigationActions)
                                Log.d("Compose", "Successfully composed screen Profile")
                            }
                        }
                        composable(
                            route = "${Route.PROFILE}/{userID}",
                            arguments = listOf(navArgument("userID") { type = NavType.StringType })) {
                                backStackEntry ->
                            val userID =
                                backStackEntry.arguments?.getString("userID")
                            if (userID != null) {
                                Profile(userVM, navigationActions)
                                Log.d("Compose", "Successfully composed screen Profile of user $userID")
                            }
                        }
                        composable(Route.ACCOUNT_SETTINGS) {
                            val currentUser = remember { auth.currentUser }
                            if (currentUser != null) {
                                AccountSettings(userVM, navigationActions)
                                Log.d("Compose", "Successfully composed screen Account Settings")
                            }
                        }
                        composable(Route.BUDDIES) {
                            val currentUser = remember { auth.currentUser }
                            if (currentUser != null) {
                                Buddies(userVM, navigationActions)
                                Log.d("Compose", "Successfully composed screen Buddies")
                            }
                        }


                        // Composables for recipes-related routes
                        composable(Route.RECIPES_HOME) {
                            val currentUser = remember { auth.currentUser }
                            if (currentUser != null) {
                                if (userVM.getVmUid().isEmpty()) userVM = viewModel {
                                    UserViewModel(currentUser.uid)
                                }
                                RecipesHome(userVM, navigationActions)
                                Log.d("Compose", "Successfully composed screen Recipes Home")
                            }
                        }
                        composable(
                            route = "${Route.RECIPE}/{recipeID}",
                            arguments = listOf(navArgument("recipeID") { type = NavType.StringType })) {
                                backStackEntry ->
                            val recipeID =
                                backStackEntry.arguments?.getString("recipeID")
                            if (recipeID != null) {
                                val recipeVM: RecipeViewModel = viewModel {
                                    RecipeViewModel(recipeID)
                                }
                                RecipeView(recipeVM, navigationActions)
                                Log.d("Compose", "Successfully composed screen Recipe of recipe $recipeID")
                            }
                        }
                        composable(
                            route = "${Route.RECIPE_EDIT}/?recipeID={recipeID}",
                            arguments = listOf(navArgument("recipeID") { type = NavType.StringType; nullable = true })
                        ) { backStackEntry ->
                            val currentUser = remember { auth.currentUser }
                            if (currentUser != null) {
                                val recipeID = backStackEntry.arguments?.getString("recipeID")
                                val recipeVM: RecipeViewModel = if (!recipeID.isNullOrBlank()) {
                                    viewModel { RecipeViewModel(recipeID) }
                                } else viewModel { RecipeViewModel() }
                                RecipeEdit(userVM, recipeVM, navigationActions)
                                Log.d("Compose", "Successfully composed screen Recipe Edit for recipeID $recipeID")
                            }
                        }


                        // Composables for groceries-related routes
                        composable(Route.GROCERIES) {
                            val currentUser = remember { auth.currentUser }
                            if (currentUser != null) {
                                GroceriesHome(userVM, navigationActions)
                                Log.d("Compose", "Successfully composed screen Groceries Home")
                            }
                        }

                        // Composables for fridge-related routes
                        composable(Route.FRIDGE) {
                            val currentUser = remember { auth.currentUser }
                            if (currentUser != null) {
                                FridgeHome(userVM, navigationActions)
                                Log.d("Compose", "Successfully composed screen Groceries Home")
                            }
                        }


                        // Composables for settings and system
                        composable(Route.SETTINGS) {
                            val currentUser = remember { auth.currentUser }
                            if (currentUser != null) {
                                Settings(userVM, offlinePrefViewModel,  navigationActions)
                                Log.d("Compose", "Successfully composed screen Settings")
                            }
                        }
                    }
                }
            }
        }
    }
}