package com.example.foodiebuddy.database

import android.graphics.Picture
import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class DatabaseConnection {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // db collections
    private val userDataCollection = db.collection("userData")
    private val storage = FirebaseStorage.getInstance().reference

    fun userExists(uid: String, onSuccess: (Boolean) -> Unit, onFailure: (Exception) -> Unit) {
        userDataCollection
            .document(uid)
            .get()
            .addOnSuccessListener { document -> onSuccess(document.exists()) }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun addExamplePictureToStorage(picture: Uri) {
        val ref = storage.child("tests/test.jpg")
        ref.putFile(picture)
            .addOnSuccessListener{
                Log.d("Debug", "image added to storage")
            }
            .addOnFailureListener{
                Log.d("Debug", "failed to add image to storage")
            }
    }

}
