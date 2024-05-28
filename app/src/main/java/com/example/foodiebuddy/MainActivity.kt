package com.example.foodiebuddy


import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.findNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
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
import com.example.foodiebuddy.viewModels.PreferencesViewModel
import com.example.foodiebuddy.viewModels.UserViewModel
import com.firebase.ui.auth.data.model.User
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private var startDestination = Route.START

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        val db = DatabaseConnection()

        handleIntent(intent)

        setContent {
            val prefViewModel: PreferencesViewModel = viewModel()
            val currentTheme by prefViewModel.currentTheme.collectAsState()

            FoodieBuddyTheme(themeChoice = currentTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current

                    val navController = rememberNavController()
                    val navigationActions = NavigationActions(navController)



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
                                Log.d("Compose", "Successfully composed screen Profile")
                            }
                        }
                        composable(
                            route = "${Route.PROFILE}/{userID}",
                            arguments = listOf(navArgument("userID") { type = NavType.StringType })) {
                                backStackEntry ->
                            Log.d("Debug", "backstack has arg ${backStackEntry.arguments}")
                            val userID = backStackEntry.arguments?.getString("userID")
                            Log.d("Debug", "For now userID is $userID")
                            if (userID != null) {
                                Log.d("Debug", "userID is oke $userID")
                                val userViewModel = remember { UserViewModel(userID) }
                                Profile(userViewModel, navigationActions)
                                Log.d("Compose", "Successfully composed screen Profile of user $userID")
                            }
                        }
                        composable(Route.ACCOUNT_SETTINGS) {
                            val currentUser = remember { auth.currentUser }
                            if (currentUser != null) {
                                val userViewModel = remember { UserViewModel(currentUser.uid) }
                                AccountSettings(userViewModel, navigationActions)
                                Log.d("Compose", "Successfully composed screen Account Settings")
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
                                Settings(userViewModel, prefViewModel, navigationActions)
                                Log.d("Compose", "Successfully composed screen Settings")
                            }
                        }
                    }
                }
            }
        }
    }
    private fun handleIntent(intent: Intent) {
        val intentExtras = intent.extras
        val isNotificationIntent = intentExtras?.getBoolean("notification_intent", false)
        if (intentExtras != null && isNotificationIntent == true) {
            val channelID = intentExtras.getInt("notification_channel", -1)
            val intentData = intentExtras.getString("notification_data", "")
            Log.d("Debug", "other extras are $channelID and $intentData")
            if (channelID == 0) {
                startDestination = "${Route.SETTINGS}"
                Log.d("Debug", "destination changed to $startDestination")
            }
            Log.d("Debug", "new intent!!")
        } else {
            Log.d("Debug", "nope for $isNotificationIntent and intent $intent with extras ${intent.extras}")
        }
    }
    @Override
    override fun onNewIntent(intent: Intent) {
        Log.d("Debug", "New intent")
        super.onNewIntent(intent)
        handleIntent(intent)

    }
}