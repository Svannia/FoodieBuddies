package com.example.foodiebuddy.errors

import android.content.Context
import android.widget.Toast
import com.example.foodiebuddy.R
import timber.log.Timber

/**
 * In case of an error, call this function to display a Toast to the user.
 * If the error is due to an unavailable network connection, the toast prompts the user to check their connection.
 * Else, the toast simply indicates that there was an error.
 * The logcat tag Error displays more details about the error.
 *
 * @param context to display the toast
 * @param errorMssg a short text written by and for developers to shortly explain the issue.
 * @param e the caught exception if any
 */
fun handleError(context: Context, errorMssg: String, e: Exception ?= null) {
    if (!isNetworkAvailable(context)) {
        Toast.makeText(context, context.getString(R.string.toast_internetCo), Toast.LENGTH_SHORT).show()
        Timber.tag("Error").d( "Network connection issue")
    } else {
        Toast.makeText(context, context.getString(R.string.toast_unknownError), Toast.LENGTH_SHORT).show()
        Timber.tag("Error").d("$errorMssg ${e?.let { "with error: $it" }}")
    }
}