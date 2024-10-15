package com.example.foodiebuddy.database

import android.net.Uri
import android.util.Log
import com.example.foodiebuddy.data.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.tasks.await

private const val USERNAME = "username"
private const val PICTURE = "picture"
private const val BIO = "bio"
private const val NUMBER_RECIPES = "numberRecipes"

private const val defaultPicturePath = "userData/default.jpg"

class DatabaseConnection {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // db collections
    private val userDataCollection = db.collection("userData")
    private val storage = FirebaseStorage.getInstance().reference

    fun getCurrentUserID(): String {
        val userID = FirebaseAuth.getInstance().currentUser?.uid
        return if (userID != null) {
            Log.d("DB", "Successfully identified user $userID")
            userID
        } else {
            Log.d("DB", "Failed to identify user")
            ""
        }
    }
    // user data
    fun userExists(uid: String, onSuccess: (Boolean) -> Unit, onFailure: (Exception) -> Unit) {
        userDataCollection
            .document(uid)
            .get()
            .addOnSuccessListener { document -> onSuccess(document.exists()) }
            .addOnFailureListener { e -> onFailure(e) }
    }

    suspend fun createUser(uid: String, username: String, picture: Uri, bio: String, isError: (Boolean) -> Unit) {
        val formattedBio = bio.replace("\n", "\\n")
        val user = hashMapOf(USERNAME to username, BIO to formattedBio, NUMBER_RECIPES to 0, PICTURE to picture.toString())
        userDataCollection
            .document(uid)
            .set(user)
            .addOnSuccessListener {
                isError(false)
                Log.d("DB", "Successfully created user")
            }
            .addOnFailureListener {e ->
                isError(true)
                Log.d("DB", "Failed to create user with error $e")
                return@addOnFailureListener
            }
        if (picture != getDefaultPicture()) {
            addUserPicture(uid, picture) { isError(it) }
        } else {
            copyDefaultPicture(uid) { isError(it) }
        }
    }

    suspend fun fetchUserData(userID: String): User {
        if (userID.isEmpty()) { return User.empty()}

        val document = userDataCollection.document(userID).get().await()
        return if (document.exists()) {
            val username = document.getString(USERNAME) ?: ""
            val picture = Uri.parse(document.getString(PICTURE)) ?: Uri.EMPTY
            val numberRecipes = document.getLong(NUMBER_RECIPES)?.toInt() ?: 0
            val bio = document.getString(BIO) ?: ""
            val formattedBio = bio.replace("\\n", "\n")
            User(userID, username, picture, numberRecipes, formattedBio)
        } else {
            Log.d("DB", "Failed to fetch user data for uid $userID")
            User.empty()
        }
    }
    fun updateUser(userID: String, username: String, picture: Uri, bio: String, updatePicture: Boolean, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        val formattedBio = bio.replace("\n", "\\n")
        val task = hashMapOf(USERNAME to username, BIO to formattedBio)
        userDataCollection
            .document(userID)
            .update(task as Map<String, Any>)
            .addOnSuccessListener {
                if (updatePicture) { updateUserPicture(userID, picture, { isError(it) }, callBack) }
                else { callBack() }
                isError(false)
                Log.d("DB", "Successfully update user data")
            }
            .addOnFailureListener { e ->
                isError(true)
                Log.d("DB", "Failed to update user data with error $e")
            }
    }
    fun deleteUser(userID: String, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        userDataCollection
            .document(userID)
            .delete()
            .addOnSuccessListener {
                deleteProfilePicture(userID, { isError(it) }, callBack)
                isError(false)
                Log.d("DB", "Successfully deleted user $userID")
            }
            .addOnFailureListener { e ->
                isError(true)
                Log.d("DB", "Failed to delete user $userID with error $e")
            }
    }


    // storage pictures
    suspend fun getDefaultPicture(): Uri {
        return storage.child(defaultPicturePath).downloadUrl.await()
    }
    private fun addUserPicture(uid: String, picture: Uri, isError: (Boolean) -> Unit) {
        val pictureRef = storage.child(userPicturePath(uid))
        pictureRef
            .putFile(picture)
            .addOnSuccessListener {
                pictureRef.downloadUrl.addOnSuccessListener { uri ->
                    userDataCollection.document(uid).update(PICTURE, uri.toString())
                    isError(false)
                    Log.d("DB", "Successfully added user profile picture")
                }
            }
            .addOnFailureListener { e ->
                isError(true)
                Log.d("DB", "Failed to add user profile picture with error: $e")
            }

    }
    private fun copyDefaultPicture(uid: String, isError: (Boolean) -> Unit) {
        val defaultRef = storage.child(defaultPicturePath)
        val pictureRef = storage.child(userPicturePath(uid))

        defaultRef
            .getBytes(Long.MAX_VALUE)
            .addOnSuccessListener { defaultData ->
                pictureRef
                    .putBytes(defaultData)
                    .addOnSuccessListener {
                        pictureRef.downloadUrl.addOnSuccessListener { uri ->
                            userDataCollection.document(uid).update(PICTURE, uri.toString())
                            isError(false)
                            Log.d("DB", "Successfully added user profile picture")
                        }
                    }
                    .addOnFailureListener { e ->
                        isError(true)
                        Log.d("DB", "Failed to add user profile picture with error: $e")
                    }
            }
    }
    private fun updateUserPicture(userID: String, picture: Uri, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        val pictureRef = storage.child(userPicturePath(userID))
        pictureRef
            .putFile(picture)
            .addOnSuccessListener {
                pictureRef.downloadUrl.addOnSuccessListener { uri ->
                    userDataCollection.document(userID).update(PICTURE, uri.toString())
                        .addOnSuccessListener {
                            isError(false)
                            callBack()
                        }
                }
                Log.d("DB", "Successfully updated user profile picture")
            }
            .addOnFailureListener { e ->
                val errorCode = (e as? StorageException)?.errorCode
                val httpErrorCode = (e as? StorageException)?.httpResultCode
                isError(true)
                Log.d("DB", "Failed to update user profile picture with error $e\n" +
                        "errorCode is $errorCode\n" +
                        "and http status is $httpErrorCode")
            }
    }
    private fun userPicturePath(uid: String) = "userData/$uid/profilePicture.jpg"
    private fun userPath(uid: String) = "userData/$uid"
    private fun deleteProfilePicture(userID: String, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        val folderRef = storage.child(userPath(userID))
        folderRef.listAll().addOnSuccessListener { result ->
            var deletedCount = 0
            val totalCount = result.items.size
            result.items.forEach { item ->
                item.delete().addOnSuccessListener {
                    deletedCount++
                    isError(false)
                    Log.d("DB", "Successfully deleted stored files from user $userID")
                    if (deletedCount == totalCount) { callBack() }
                }.addOnFailureListener { e ->
                    isError(true)
                    Log.d("DB", "Failed to delete stored files from user $userID with error $e")
                }
            }
        }.addOnFailureListener { e ->
            isError(true)
            Log.d("DB", "Failed to list stored files from user $userID with error $e")
        }
    }


    // db tests
    fun addExamplePictureToStorage(picture: Uri) {
        val ref = storage.child("tests/test.jpg")
        ref.putFile(picture)
            .addOnSuccessListener{
            }
            .addOnFailureListener{
            }
    }
}
