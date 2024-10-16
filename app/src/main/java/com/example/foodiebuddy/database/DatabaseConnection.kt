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

    /**
     * Contains fields username, picture, bio and numberRecipes
     */
    private val userDataCollection = db.collection("userData")
    /**
     * Contains userData/ path for profile pictures and tests/ path for testing picture-adding
     */
    private val storage = FirebaseStorage.getInstance().reference

    /**
     * Fetches the unique ID of the currently logged-in user.
     *
     * @return UID
     */
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
    /**
     * Checks if a user exists in the DB.
     * 
     * @param userID ID of the user whose existence to check
     * @param onSuccess block that runs if the check succeeds (whether or not the user exists)
     * @param onFailure block that runs if there is an error executing the function
     */
    fun userExists(userID: String, onSuccess: (Boolean) -> Unit, onFailure: (Exception) -> Unit) {
        userDataCollection
            .document(userID)
            .get()
            .addOnSuccessListener { document -> onSuccess(document.exists()) }
            .addOnFailureListener { e -> onFailure(e) }
    }

    /**
     * Creates a new user profile given all the input information.
     * The userID and username are mandatory, bio can be empty and the default profile picture is used if no picture was input.
     *
     * @param userID ID of the user to create
     * @param username of the new user
     * @param picture Uri of the new user's picture or default picture
     * @param bio of the new user
     * @param isError block that runs if there is an error executing the function
     */
    suspend fun createUser(userID: String, username: String, picture: Uri, bio: String, isError: (Boolean) -> Unit) {
        val formattedBio = bio.replace("\n", "\\n")
        val user = hashMapOf(USERNAME to username, BIO to formattedBio, NUMBER_RECIPES to 0, PICTURE to picture.toString())
        userDataCollection
            .document(userID)
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
            addUserPicture(userID, picture) { isError(it) }
        } else {
            copyDefaultPicture(userID) { isError(it) }
        }
    }

    /**
     * Fetches all of a user's profile data.
     *
     * @param userID ID of the user whose data to retrieve
     * @return User data structure with all profile data
     */
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
            Log.d("DB", "Failed to fetch user data for userID $userID")
            User.empty()
        }
    }

    /**
     * Modifies a given user with the new input profile data.
     *
     * @param userID ID of the user to edit
     * @param username new input by existing user
     * @param picture of the new input profile picture
     * @param bio new input by existing user
     * @param updatePicture inner function to change profile picture in storage will only be called if new picture was input
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after DB was updated
     */
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

    /**
     * Deletes a user and all their related info from the DB.
     *
     * @param userID ID of the user
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after DB was updated
     */
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
    /**
     * Retrieves the default (empty) profile picture from the storage.
     *
     * @return Uri of the default profile picture
     */
    suspend fun getDefaultPicture(): Uri {
        return storage.child(defaultPicturePath).downloadUrl.await()
    }

    /**
     * Adds the user-input profile picture to storage (creates the user's data path in storage).
     *
     * @param userID ID of the user
     * @param picture Uri of the user-input profile picture
     * @param isError block that runs if there is an error executing the function
     */
    private fun addUserPicture(userID: String, picture: Uri, isError: (Boolean) -> Unit) {
        val pictureRef = storage.child(userPicturePath(userID))
        pictureRef
            .putFile(picture)
            .addOnSuccessListener {
                pictureRef.downloadUrl.addOnSuccessListener { uri ->
                    userDataCollection.document(userID).update(PICTURE, uri.toString())
                    isError(false)
                    Log.d("DB", "Successfully added user profile picture")
                }
            }
            .addOnFailureListener { e ->
                isError(true)
                Log.d("DB", "Failed to add user profile picture with error: $e")
            }

    }

    /**
     * Copies the stores default profile picture into the user's profile picture data in Storage.
     *
     * @param userID ID of the user
     * @param isError block that runs if there is an error executing the function
     */
    private fun copyDefaultPicture(userID: String, isError: (Boolean) -> Unit) {
        val defaultRef = storage.child(defaultPicturePath)
        val pictureRef = storage.child(userPicturePath(userID))

        defaultRef
            .getBytes(Long.MAX_VALUE)
            .addOnSuccessListener { defaultData ->
                pictureRef
                    .putBytes(defaultData)
                    .addOnSuccessListener {
                        pictureRef.downloadUrl.addOnSuccessListener { uri ->
                            userDataCollection.document(userID).update(PICTURE, uri.toString())
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

    /**
     * Accesses the existing user data path in Storage to update new picture.
     *
     * @param userID ID of the user
     * @param picture new picture to be updated in Storage
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after DB was updated
     */
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

    /**
     * Retrieves the data path in Storage to access a user's profile picture.
     *
     * @param userID ID of the user
     * @return path in Storage
     */
    private fun userPicturePath(userID: String) = "userData/$userID/profilePicture.jpg"

    /**
     * Retrieves the data path in Storage to access a user's data.
     *
     * @param userID ID of the user
     * @return path in Storage
     */
    private fun userPath(userID: String) = "userData/$userID"

    /**
     * Deletes a user's profile picture from the Storage.
     * This is only meant to be used when the user deletes their account: the corresponding path in Storage is also deleted.
     *
     * @param userID ID of the user
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after DB was updated
     */
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
    /**
     * For testing purposed, adds picture in a specific path in Storage meant for testing.
     *
     * @param picture that is added in Storage for tests
     */
    fun addExamplePictureToStorage(picture: Uri) {
        val ref = storage.child("tests/test.jpg")
        ref.putFile(picture)
            .addOnSuccessListener{
            }
            .addOnFailureListener{
            }
    }
}
