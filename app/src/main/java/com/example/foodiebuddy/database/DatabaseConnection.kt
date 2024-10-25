package com.example.foodiebuddy.database

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import com.example.foodiebuddy.data.OwnedIngredient
import com.example.foodiebuddy.data.Recipe
import com.example.foodiebuddy.data.User
import com.example.foodiebuddy.data.UserPersonal
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.chrono.IsoEra

private const val USERNAME = "username"
private const val PICTURE = "picture"
private const val BIO = "bio"
private const val NUMBER_RECIPES = "numberRecipes"

private const val FAV_RECIPES = "favouriteRecipes"
private const val GROCERIES = "groceryList"
private const val FRIDGE = "fridge"

private const val OWNER = "owner"
private const val DISPLAY_NAME = "displayName"
private const val STAND_NAME = "standName"
private const val CATEGORY = "category"
private const val IS_TICKED = "isTicked"

private const val defaultPicturePath = "userData/default.jpg"

class DatabaseConnection {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // db collections
    /**
     * Contains fields username, picture, bio and numberRecipes
     */
    private val userDataCollection = db.collection("userData")
    /**
     * Contains fields favouriteRecipes, groceryList and fridge
     */
    private val userPersonalCollection = db.collection("userPersonal")
    /**
     * Contains fields owner, displayName, standName, category, isTicked
     */
    private val ingredientsCollection = db.collection("ingredients")
    /**
     * Contains fields owner, picture, recipe, ingredients, origin, diet, tags
     */
    private val recipesCollection = db.collection("recipes")
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
        createPersonal(userID, isError)
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


    // user personal
    private suspend fun createPersonal(userID: String, isError: (Boolean) -> Unit) {
        val user = hashMapOf(FAV_RECIPES to emptyList<String>(), GROCERIES to emptyMap<String, List<String>>(), FRIDGE to emptyMap<String, List<String>>())
        userPersonalCollection
            .document(userID)
            .set(user)
            .addOnSuccessListener {
                isError(false)
                Log.d("DB", "Successfully created user personal")
            }
            .addOnFailureListener {e ->
                isError(true)
                Log.d("DB", "Failed to create user personal with error $e")
                return@addOnFailureListener
            }
    }

    suspend fun fetchUserPersonal(userID: String): UserPersonal {
        if (userID.isEmpty()) {
            return UserPersonal.empty()
        }

        val document = userPersonalCollection.document(userID).get().await()
        return if (document.exists()) {
           /* val favouriteRecipesRefs = document.get(FAV_RECIPES) as? List<DocumentReference> ?: emptyList()
            val favouriteRecipes = favouriteRecipesRefs.mapNotNull { ref ->
                ref.get().await().toObject(Recipe::class.java)
            }*/
            val groceryListRefs = document.get(GROCERIES) as? Map<String, List<DocumentReference>> ?: emptyMap()
            val groceryList = groceryListRefs.mapValues { entry ->
                entry.value.map { ref ->
                    fetchIngredient(ref)
                }
            }

            val fridgeListRefs = document.get(FRIDGE) as? Map<String, List<DocumentReference>> ?: emptyMap()
            val fridgeList = fridgeListRefs.mapValues { entry ->
                entry.value.map { ref ->
                    fetchIngredient(ref)
                }
            }
            /*val fridgeRefs = document.get(FRIDGE) as? Map<String, List<DocumentReference>> ?: emptyMap()
            val fridge = fridgeRefs.mapValues { entry ->
                entry.value.mapNotNull { ref ->
                    ref.get().await().toObject(OwnedIngredient::class.java)
                }
            }*/
            // Create and return the UserPersonal object
            Log.d("DB", "Successfully fetched user personal")
            UserPersonal(userID, emptyList(), groceryList, fridgeList)
        } else {
            Log.d("DB", "Failed to fetch user personal for userID $userID")
            UserPersonal.empty()
        }
    }

    fun updatePersonalIngredients(userID: String, groceries: Map<String, List<OwnedIngredient>>, fridge: Map<String, List<OwnedIngredient>>, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        val groceriesData = groceries.mapValues { entry ->
            entry.value.map { ingredient ->
                ingredientsCollection.document(ingredient.uid)
            }
        }
        val fridgeData = fridge.mapValues { entry ->
            entry.value.map { ingredient ->
                ingredientsCollection.document(ingredient.uid)
            }
        }
        val task = hashMapOf(GROCERIES to groceriesData, FRIDGE to fridgeData)
        userPersonalCollection
            .document(userID)
            .update(task as Map<String, Any>)
            .addOnSuccessListener {
                isError(false)
                callBack()
                Log.d("DB", "Successfully updated user ingredients")
            }
            .addOnFailureListener { e ->
                isError(true)
                Log.d("DB", "Failed to update user ingredients with error $e")
            }
    }

    suspend fun addCategory(category: String, ingredients: List<OwnedIngredient>, owner: String, isInFridge: Boolean, isError: (Boolean) -> Unit, callBack: () -> Unit ) {
        if (ingredients.isEmpty()) {
            userPersonalCollection
                .document(owner)
                .update(
                    "$GROCERIES.${category}", FieldValue.arrayUnion(),
                    "$FRIDGE.${category}", FieldValue.arrayUnion()
                )
                .addOnSuccessListener {
                    isError(false)
                    callBack()
                    Log.d("DB", "Successfully added the empty category $category")
                }
                .addOnFailureListener { e ->
                    isError(true)
                    Log.d("DB", "Failed to add empty category $category with error $e")
                }
        } else {
            var remaining = ingredients.size
            for (ingredient in ingredients) {
                createIngredient(owner, ingredient, isInFridge, { isError(it) }) {
                    remaining--
                    if (remaining <= 0) {
                        callBack()
                    }
                }
            }
        }

    }

    fun updateCategory(userID: String, old: String, new: String, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        val userRef = userPersonalCollection.document(userID)

        userRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val groceriesMap = document.get(GROCERIES) as? Map<String, List<DocumentReference>> ?: emptyMap()
                    val fridgeMap = document.get(FRIDGE) as? Map<String, List<DocumentReference>> ?: emptyMap()
                    val groceries = groceriesMap[old] ?: emptyList()
                    val fridge = fridgeMap[old] ?: emptyList()
                    val allIngredients = groceries + fridge

                    allIngredients.forEach { ref ->
                        ref.update(CATEGORY, new)
                            .addOnSuccessListener {  }
                            .addOnFailureListener { e ->
                                isError(true)
                                Log.d("DB", "Failed to udpate category of ingredient $ref with error $e")
                            }
                    }

                    val updatedGroceries = groceriesMap.toMutableMap()
                    val updatedFridge = fridgeMap.toMutableMap()

                    val newGroceryList = updatedGroceries.remove(old)?.toMutableList() ?: mutableListOf()
                    updatedGroceries[new] = newGroceryList

                    val newFridgeList = updatedFridge.remove(old)?.toMutableList() ?: mutableListOf()
                    updatedFridge[new] = newFridgeList

                    userRef.update(mapOf(
                        GROCERIES to updatedGroceries,
                        FRIDGE to updatedFridge
                    )).addOnSuccessListener {
                        callBack()  // success callback
                        Log.d("DB", "Successfully updated category in groceryList and fridge")
                    }.addOnFailureListener { e ->
                        isError(true)
                        Log.d("DB", "Failed to update category with error: $e")
                    }
                }
            }
    }

    fun deleteCategory(owner: String, category: String, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        val ingredientsToDelete = mutableListOf<DocumentReference>()

        userPersonalCollection
            .document(owner)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val groceriesMap = document.get(GROCERIES) as? Map<String, List<DocumentReference>>
                    val fridgeMap = document.get(FRIDGE) as? Map<String, List<DocumentReference>>

                    groceriesMap?.get(category)?.let { ingredientsToDelete.addAll(it) }
                    fridgeMap?.get(category)?.let { ingredientsToDelete.addAll(it) }

                    if (ingredientsToDelete.isNotEmpty()) {
                        var remaining = ingredientsToDelete.size
                        ingredientsToDelete.forEach { ref ->
                            ref.delete()
                                .addOnSuccessListener {
                                    remaining--
                                    if (remaining <= 0) {
                                        userPersonalCollection
                                            .document(owner)
                                            .update(
                                                "$GROCERIES.$category",FieldValue.delete(),
                                                "$FRIDGE.$category", FieldValue.delete()
                                            )
                                            .addOnSuccessListener {
                                                Log.d("DB", "Successfully deleted category $category")
                                                isError(false)
                                                callBack()
                                            }
                                            .addOnFailureListener { e ->
                                                Log.d("DB", "Failed to delete category $category with $e")
                                                isError(true)
                                            }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.d("DB", "Failed to delete ingredient $ref with error $e")
                                    isError(true)
                                }
                        }
                    } else {
                        userPersonalCollection
                            .document(owner)
                            .update(
                                "$GROCERIES.$category",FieldValue.delete(),
                                "$FRIDGE.$category", FieldValue.delete()
                            )
                            .addOnSuccessListener {
                                Log.d("DB", "Successfully deleted category $category")
                                isError(false)
                                callBack()
                            }
                            .addOnFailureListener { e ->
                                Log.d("DB", "Failed to delete category $category with $e")
                                isError(true)
                            }
                    }
                } else {
                    Log.d("DB", "Failed to delete category $category because it does not exist")
                    isError(true)
                }
            }
            .addOnFailureListener { e ->
                Log.d("DB", "Failed to retrieve userPersonal for $owner with error $e")
                isError(true)
            }
    }
    fun updateFavourites(userID: String, favourite: Recipe, adding: Boolean, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        val recipeRef = recipesCollection.document(favourite.uid)
        val updateOperation =
            if (adding) { FieldValue.arrayUnion(recipeRef) }
            else { FieldValue.arrayRemove(recipeRef) }
        userPersonalCollection.document(userID)
            .update(FAV_RECIPES, updateOperation)
            .addOnSuccessListener {
                isError(false)
                callBack()
                Log.d("DB", "Successfully updated favourites")
            }
            .addOnFailureListener { e ->
                isError(true)
                Log.d("DB", "Failed to update favourites with error $e")
            }
    }


    // ingredients
    suspend fun createIngredient(owner: String, newItem: OwnedIngredient, isInFridge: Boolean, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        val targetField = if (isInFridge) {"$FRIDGE.${newItem.category}"} else {"$GROCERIES.${newItem.category}"}
        val syncedField = if (isInFridge) {"$GROCERIES.${newItem.category}"} else {"$FRIDGE.${newItem.category}"}
        val ingredient = hashMapOf(OWNER to owner, DISPLAY_NAME to newItem.displayedName, STAND_NAME to newItem.standName, CATEGORY to newItem.category, IS_TICKED to newItem.isTicked)
        ingredientsCollection
            .add(ingredient)
            .addOnSuccessListener {
                Log.d("DB", "Successfully created user ingredient")
                userPersonalCollection
                    .document(owner)
                    .update(
                        targetField, FieldValue.arrayUnion(it),
                        syncedField, FieldValue.arrayUnion()
                    )
                    .addOnSuccessListener {
                        isError(false)
                        callBack()
                        Log.d("DB", "Successfully added the ingredient reference")
                    }
                    .addOnFailureListener {e ->
                        isError(true)
                        Log.d("DB", "Failed to add ingredient reference with error $e")
                    }
            }
            .addOnFailureListener {e ->
                isError(true)
                Log.d("DB", "Failed to create ingredient with error $e")
            }
    }

    private suspend fun fetchIngredient(ref: DocumentReference): OwnedIngredient {
        val document = ref.get().await()
        return if (document.exists()) {
            val displayName = document.getString(DISPLAY_NAME) ?: ""
            val standName = document.getString(STAND_NAME) ?: ""
            val category = document.getString(CATEGORY) ?: ""
            val isTicked = document.getBoolean(IS_TICKED) ?: false
            Log.d("DB", "Successfully fetched ingredient")
            OwnedIngredient(ref.id, displayName, standName, category, isTicked)
        } else {
            OwnedIngredient.empty()
        }

    }

    fun updateIngredientTick(uid: String, isTicked: Boolean, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        ingredientsCollection
            .document(uid)
            .update(IS_TICKED, isTicked)
            .addOnSuccessListener {
                isError(false)
                callBack()
                Log.d("DB", "Successfully updated ingredient")
            }
            .addOnFailureListener { e ->
                isError(true)
                Log.d("DB", "Failed to update ingredient with error $e")
            }
    }

    fun deleteIngredient(uid: String, owner: String, category: String, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        val ref = ingredientsCollection.document(uid)

        userPersonalCollection
            .document(owner)
            .update("$GROCERIES.$category", FieldValue.arrayRemove(ref))
            .addOnSuccessListener {
                isError(false)
                Log.d("DB", "Successfully deleted ingredient ref from user $owner")

                ref.delete()
                    .addOnSuccessListener {
                        isError(false)
                        Log.d("DB", "Sucessfully deleted ingredient $uid")
                        callBack()
                    }
                    .addOnFailureListener { e ->
                        isError(true)
                        Log.d("DB", "Failed to delete ingredient $uid with error $e")
                    }
            }
            .addOnFailureListener { e ->
                isError(true)
                Log.d("DB", "Failed to delete ingredient ref from user with error $e")
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
