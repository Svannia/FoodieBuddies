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
                    val startDestination =
                        if (thisUser != null) {
                            db.userExists(
                                uid = thisUser.uid,
                                onSuccess = { userExists ->
                                    if (userExists) {
                                        Route.RECIPES_HOME
                                    } else {
                                        Route.CREATE_ACCOUNT
                                    }
                                },
                                onFailure = { e ->
                                    HandleError(context, "Failed to check user existence", e)
                                    Route.LOGIN
                                }
                            )
                            Route.CREATE_ACCOUNT
                        } else {
                            Route.LOGIN
                        }

                    NavHost(navController, startDestination) {
                        // Composables for account-related routes
                        composable(Route.LOGIN) {
                            LoginScreen(navigationActions)
                            Log.d("Compose", "Successfully composed screen Login screen")
                        }
                        composable(Route.CREATE_ACCOUNT) {
                            if (auth.currentUser != null) {
                                val userViewModel = remember { UserViewModel() }
                                CreateAccount(userViewModel, navigationActions)
                                Log.d("Compose", "Successfully composed screen Create Account")
                            }
                        }
                        composable(
                            route = "${Route.PROFILE_PICTURE}/{uri}",
                            arguments = listOf(navArgument("uri") { type = NavType.StringType })) {
                                backStackEntry ->
                            Log.d("Debug", "got here")
                            val uri = Uri.decode(backStackEntry.arguments?.getString("uri"))
                            val currentUser = remember { auth.currentUser }
                            if (uri != null && currentUser != null) {
                                val userViewModel = remember { UserViewModel(currentUser.uid) }
                                SetProfilePicture(userViewModel, Uri.parse(uri), navigationActions)
                                Log.d("MyPrint", "Successfully navigated to Settings")
                            }
                        }
                        // Composables for recipes-related routes
                        composable(Route.RECIPES_HOME) {
                            val currentUser = remember { auth.currentUser }
                            if (currentUser != null) {
                                val recipesViewModel = remember { RecipeListViewModel(currentUser.uid) }
                                RecipesHome(currentUser.uid, recipesViewModel, navigationActions)
                                Log.d("Compose", "Successfully composed screen Create Account")
                            }
                        }
                    }
                }
            }
        }
    }
}