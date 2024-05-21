package com.example.foodiebuddy.ui.account

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.foodiebuddy.R
import com.example.foodiebuddy.database.DatabaseConnection
import com.example.foodiebuddy.errors.handleError
import com.example.foodiebuddy.navigation.NavigationActions
import com.example.foodiebuddy.navigation.Route
import com.example.foodiebuddy.ui.theme.ContrastColor
import com.example.foodiebuddy.ui.theme.ContrastGrey
import com.example.foodiebuddy.ui.theme.MyTypography
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth

@Composable
fun LoginScreen(navigationActions: NavigationActions) {
    val context = LocalContext.current
    val signInLauncher =
        rememberLauncherForActivityResult(FirebaseAuthUIActivityResultContract()) { res ->
            onSignInResult(res, context, navigationActions)
        }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {item {
        Image(
            painter = painterResource(R.drawable.app_logo),
            contentDescription = stringResource(R.string.desc_appLogo),
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .width(189.dp)
                .height(189.dp)
        )
        Spacer(Modifier.size(67.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MyTypography.titleLarge,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.size(120.dp))
        Button(
            onClick = {
                val signInIntent = AuthUI
                    .getInstance()
                    .createSignInIntentBuilder()
                    .setAvailableProviders(arrayListOf(AuthUI.IdpConfig.GoogleBuilder().build()))
                    .setIsSmartLockEnabled(false)
                    .build()
                signInLauncher.launch(signInIntent)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            ),
            modifier = Modifier
                .border(width = 2.dp, color = ContrastGrey, shape = RoundedCornerShape(50))
                .background(color = Color.Transparent, shape = RoundedCornerShape(50))
                .width(302.dp)
                .height(76.dp),
            shape = RoundedCornerShape(50)
        ) {
            Image(
                painter = painterResource(R.drawable.google),
                contentDescription = stringResource(R.string.desc_googleLogo),
                modifier = Modifier.width(36.dp)
            )
            Spacer(Modifier.size(16.dp))
            Text(
                text = stringResource(R.string.button_signIn),
                color = ContrastColor,
                style = MyTypography.bodyMedium
            )
        }
    } }
}

private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult, context: Context, navigationActions: NavigationActions) {
    if (result.resultCode == Activity.RESULT_OK) {
        val userID = FirebaseAuth.getInstance().currentUser?.uid
        if (userID != null) {
            val db = DatabaseConnection()
            db.userExists(
                userID,
                onSuccess = {userExists ->
                    if (userExists) {
                        navigationActions.navigateTo(Route.RECIPES_HOME)
                        Log.d("Login", "Successfully logged in app for user $userID")
                    } else {
                        navigationActions.navigateTo(Route.CREATE_ACCOUNT)
                        Log.d("Login", "Successfully started creating account for user $userID")
                    }
                },
                onFailure = { e ->
                    handleError(context,"Failed to check user existence", e)
                }
            )
        } else {
            handleError(context, "Failed to get user ID")
        }
    } else {
        handleError(context, "Sign in failed")
    }
}
