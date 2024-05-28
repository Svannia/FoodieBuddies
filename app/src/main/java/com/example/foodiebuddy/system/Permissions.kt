package com.example.foodiebuddy.system

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat

fun checkPermission(
    context: Context,
    permission: String,
    launcher: ManagedActivityResultLauncher<String, Boolean>,
    alreadyGranted: () -> Unit
) {
    if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
        launcher.launch(permission)
    } else {
        alreadyGranted()
    }
}

fun imagePermissionVersion(): String {
    return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        // For older Android versions, use READ_EXTERNAL_STORAGE permission
        "android.permission.READ_EXTERNAL_STORAGE"
    } else {
        "android.permission.READ_MEDIA_IMAGES"
    }
}

fun notificationPermission(): String {
    return "android.permission.POST_NOTIFICATIONS"
}

@Composable
fun getNotificationPermissionLauncher(context: Context):  ManagedActivityResultLauncher<String, Boolean> {
    return rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            createNotifChannels(context)
        }
    }
}
