package com.example.foodiebuddy.errors

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.foodiebuddy.R

fun HandleError(context: Context, errorMssg: String, e: Exception ?= null) {
    if (!isNetworkAvailable(context)) {
        Toast.makeText(context, context.getString(R.string.toast_internetCo), Toast.LENGTH_SHORT).show()
        Log.d("Error", "Network connection issue")
    } else {
        Toast.makeText(context, context.getString(R.string.toast_unknownError), Toast.LENGTH_SHORT).show()
        Log.d("Error","$errorMssg ${e?.let { "with error: $it" }}")
    }
}