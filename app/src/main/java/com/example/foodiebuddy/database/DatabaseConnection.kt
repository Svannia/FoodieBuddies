package com.example.foodiebuddy.database

import android.graphics.Picture
import android.net.Uri
import android.util.Log
import com.example.foodiebuddy.errors.HandleError
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import kotlin.math.PI

private const val USERNAME = "username"
private const val PICTURE = "picture"
private const val BIO = "bio"
private const val NUMBER_RECIPES = "numberRecipes"

class DatabaseConnection {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // db collections
    private val userDataCollection = db.collection("userData")
    private val storage = FirebaseStorage.getInstance().reference

    // user data
    fun userExists(uid: String, onSuccess: (Boolean) -> Unit, onFailure: (Exception) -> Unit) {
        userDataCollection
            .document(uid)
            .get()
            .addOnSuccessListener { document -> onSuccess(document.exists()) }
            .addOnFailureListener { e -> onFailure(e) }
    }

    suspend fun createUser(uid: String, username: String, picture: Uri, bio: String) {
       val formattedBio = bio.replace("\n", "\\n")
        val user = hashMapOf(USERNAME to username, BIO to formattedBio, NUMBER_RECIPES to 0)
        userDataCollection
            .document(uid)
            .set(user)
            .addOnSuccessListener {
                Log.d("DB", "Successfully created user")
            }
            .addOnFailureListener {e ->
                Log.d("DB", "Failed to create user with error $e")
                return@addOnFailureListener
            }
        if (picture != getDefaultPicture()) {
            addUserPicture(uid, picture)
        } else {
            copyDefaultPicture(uid)
        }
    }

    // storage pictures
    suspend fun getDefaultPicture(): Uri {
        return storage.child("userData/default.jpg").downloadUrl.await()
    }
    private fun addUserPicture(uid: String, picture: Uri) {
        val pictureRef = storage.child("userData/$uid/profilePicture.jpg")
        pictureRef
            .putFile(picture)
            .addOnSuccessListener {
                pictureRef.downloadUrl.addOnSuccessListener { uri ->
                    userDataCollection.document(uid).update(PICTURE, uri.toString())
                    Log.d("DB", "Successfully added user profile picture")
                }
            }
            .addOnFailureListener { e ->
                Log.d("DB", "Failed to add user profile picture with error: $e")
            }

    }
    private fun copyDefaultPicture(uid: String) {
        val defaultRef = storage.child("userData/default.jpg")
        val pictureRef = storage.child("userData/$uid/profilePicture.jpg")

        defaultRef
            .getBytes(Long.MAX_VALUE)
            .addOnSuccessListener { defaultData ->
                pictureRef
                    .putBytes(defaultData)
                    .addOnSuccessListener {
                        pictureRef.downloadUrl.addOnSuccessListener { uri ->
                            userDataCollection.document(uid).update(PICTURE, uri.toString())
                            Log.d("DB", "Successfully added user profile picture")
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.d("DB", "Failed to add user profile picture with error: $e")
                    }
            }
    }

    // db tests
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
