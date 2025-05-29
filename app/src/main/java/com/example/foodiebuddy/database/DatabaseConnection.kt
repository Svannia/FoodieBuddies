package com.example.foodiebuddy.database

import android.net.Uri
import com.example.foodiebuddy.data.Diet
import com.example.foodiebuddy.data.Measure
import com.example.foodiebuddy.data.Origin
import com.example.foodiebuddy.data.OwnedIngredient
import com.example.foodiebuddy.data.Recipe
import com.example.foodiebuddy.data.RecipeIngredient
import com.example.foodiebuddy.data.Tag
import com.example.foodiebuddy.data.User
import com.example.foodiebuddy.data.UserPersonal
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.tasks.await
import timber.log.Timber

private const val USERNAME = "username"
private const val PICTURE = "picture"
private const val BIO = "bio"
private const val NUMBER_RECIPES = "numberRecipes"

private const val GROCERIES = "groceryList"
private const val FRIDGE = "fridge"
private const val NOTES = "notes"

private const val OWNER = "owner"
private const val DISPLAY_NAME = "displayName"
private const val STAND_NAME = "standName"
private const val CATEGORY = "category"
private const val IS_TICKED = "isTicked"

private const val NAME = "name"
private const val PICTURES = "pictures"
private const val INSTRUCTIONS = "instructions"
private const val INGREDIENTS = "ingredients"
private const val QUANTITY = "quantity"
private const val UNIT = "unit"
private const val PORTION = "portion"
private const val PER_PERSON = "perPerson"
private const val ORIGIN = "origin"
private const val DIET = "diet"
private const val TAGS = "tags"
private const val FAVOURITE = "favouriteOf"

private const val defaultPicturePath = "userData/default.jpg"

@Suppress("UNCHECKED_CAST")
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
            Timber.tag("MyDB").d( "Successfully identified user $userID")
            userID
        } else {
            Timber.tag("MyDB").d( "Failed to identify user")
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
     * Creates a new user profile given all the input information, and its corresponding userPersonal document.
     * The userID and username are mandatory, bio can be empty and the default profile picture is used if no picture was input.
     *
     * @param userID ID of the user to create
     * @param username of the new user
     * @param picture Uri of the new user's picture or default picture
     * @param bio of the new user
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after the DB was updated
     */
    suspend fun createUser(userID: String, username: String, picture: Uri, bio: String, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        // fetch default user picture
        val defaultPicture = getDefaultPicture()
        // process the input data to create a document
        val formattedBio = bio.replace("\n", "\\n")
        val user = hashMapOf(USERNAME to username.trimEnd(), BIO to formattedBio, NUMBER_RECIPES to 0, PICTURE to picture.toString())
        // create the new userData document
        userDataCollection
            .document(userID)
            .set(user)
            .addOnSuccessListener {
                isError(false)
                Timber.tag("MyDB").d( "Successfully created user")

                // if the user input their own profile picture -> add it to the storage (the new path is automatically created)
                if (picture != defaultPicture) {
                    updateUserPicture(userID, picture, { isError(it) }) {
                        // create a document for the new user's personal data
                        createPersonal(userID, { isError(it) }) {
                            callBack()
                            Timber.tag("MyDB").d( "Successfully finished user creation process")
                        }
                    }
                // else -> copy the default profile picture in the new user's storage path
                } else {
                    copyDefaultPicture(userID, { isError(it) }) {
                        // create a document for the new user's personal data
                        createPersonal(userID,{ isError(it) }) {
                            callBack()
                            Timber.tag("MyDB").d( "Successfully finished user creation process")
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                isError(true)
                Timber.tag("MyDB").d( "Failed to create user with error $e")
            }
    }

    /**
     * Fetches all of a user's profile data.
     *
     * @param userID ID of the user whose data to retrieve
     * @return User data object with all profile data
     */
    suspend fun fetchUserData(userID: String, isError: (Boolean) -> Unit): User {
        if (userID.isEmpty()) {
            isError(true)
            Timber.tag("MyDB").d( "Failed to fetch user data because userID $userID is empty")
            return User.empty()
        }

        val document = userDataCollection.document(userID).get().await()
        return if (document.exists()) {
            val username = document.getString(USERNAME) ?: ""
            val picture = Uri.parse(document.getString(PICTURE)) ?: Uri.EMPTY
            val numberRecipes = document.getLong(NUMBER_RECIPES)?.toInt() ?: 0
            val bio = document.getString(BIO) ?: ""
            val formattedBio = bio.replace("\\n", "\n")
            isError(false)
            Timber.tag("MyDB").d( "Successfully fetched user data")
            User(userID, username, picture, numberRecipes, formattedBio)
        } else {
            isError(true)
            Timber.tag("MyDB").d( "Failed to fetch user data for userID $userID")
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
     * @param updatePicture whether the profile picture needs to be updated
     * @param removePicture whether the profile picture needs to be removed (puts default picture instead)
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after DB was updated
     */
    fun updateUser(userID: String, username: String, picture: Uri, bio: String, updatePicture: Boolean, removePicture: Boolean, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        // only user and bio text fields can be modified by the user
        val formattedBio = bio.replace("\n", "\\n")
        val task = hashMapOf(USERNAME to username.trimEnd(), BIO to formattedBio)
        // update those modifications to the document
        userDataCollection
            .document(userID)
            .update(task as Map<String, Any>)
            .addOnSuccessListener {
                // if the modification was successful -> check if picture also needs to be updated or removed
                if (removePicture) {
                    copyDefaultPicture(userID, { isError(it) }) {
                        callBack()
                        Timber.tag("MyDB").d( "Successfully updated user data with default picture")
                    }
                }
                else if (updatePicture) {
                    updateUserPicture(userID, picture, { isError(it) }) {
                        callBack()
                        Timber.tag("MyDB").d( "Successfully updated user data with new picture")
                    }
                }
                else {
                    isError(false)
                    callBack()
                    Timber.tag("MyDB").d( "Successfully updated user data without new picture")
                }
            }
            .addOnFailureListener { e ->
                isError(true)
                Timber.tag("MyDB").d( "Failed to update user data with error $e")
            }
    }

    /**
     * Checks if a username is already taken in the database.
     *
     * @param username username to check
     * @param onSuccess block that runs if the check succeeds (whether or not the username exists)
     * @param onFailure block that runs if there is an error executing the function
     */
    fun usernameAvailable(username: String, onSuccess: (Boolean) -> Unit, onFailure: (Exception) -> Unit) {
        userDataCollection
            .whereEqualTo(USERNAME, username.trimEnd())
            .get()
            .addOnSuccessListener { documents ->
                onSuccess(documents.isEmpty)
            }
            .addOnFailureListener { e -> onFailure(e)}
    }

    /**
     * Deletes a user and all their related info from the DB.
     *
     * @param userID ID of the user
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after DB was updated
     */
    fun deleteUser(userID: String, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        // delete the userData document of the user to be deleted
        userDataCollection
            .document(userID)
            .delete()
            .addOnSuccessListener {
                // if document deletion was successful -> also delete the profile picture
                deleteUserPictures(userID, { isError(it) }) {
                    // once those deletions are successful -> delete the userPersonal document
                    deleteUserPersonal(userID, { isError(it) }) {
                        // delete all recipes created by this user
                        deleteAllUserRecipes(userID, { isError(it) }) {
                            // in all recipes, remove this user from the list favouriteOf
                            removeUserFromAllFavourites(userID, { isError(it) }) {
                                callBack()
                                Timber.tag("MyDB").d( "Successfully deleted user $userID")
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                isError(true)
                Timber.tag("MyDB").d( "Failed to delete user with error $e")
            }
    }

    /**
     * Fetches all users' data except for the one calling this function.
     *
     * @param userID ID of the user whose data is to be exempted
     * @return List of User data objects from all other users
     */
    suspend fun fetchAllUsers(userID: String, isError: (Boolean) -> Unit): List<User> {
        if (userID.isEmpty()) {
            isError(true)
            Timber.tag("MyDB").d( "Failed to fetch all users because userID is empty")
            return emptyList()
        }

        return try {
            val query = userDataCollection.get().await()
            query.documents
                .filter { it.id != userID }
                .map { document ->
                    val username = document.getString(USERNAME) ?: ""
                    val picture = Uri.parse(document.getString(PICTURE)) ?: Uri.EMPTY
                    val numberRecipes = document.getLong(NUMBER_RECIPES)?.toInt() ?: 0
                    val bio = document.getString(BIO) ?: ""
                    val formattedBio = bio.replace("\\n", "\n")
                    isError(false)
                    Timber.tag("MyDB").d( "Successfully fetched all user data")
                    User(document.id, username, picture, numberRecipes, formattedBio)
                }
        } catch (e: Exception) {
            isError(true)
            Timber.tag("MyDB").d( "Failed to fetch all users data")
            emptyList()
        }
    }


    // user personal

    /**
     * Creates a new userPersonal. This function is only called when a new user is created.
     *
     * @param userID ID of the user
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after DB was updated
     *
     */
    private fun createPersonal(userID: String, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        // the initial document only contains empty lists or maps
        val user = hashMapOf(GROCERIES to emptyMap<String, List<String>>(), FRIDGE to emptyMap(), NOTES to emptyMap<String, String>())
        // add the new document to userPersonal, setting its reference to be the user UID
        userPersonalCollection
            .document(userID)
            .set(user)
            .addOnSuccessListener {
                isError(false)
                callBack()
                Timber.tag("MyDB").d( "Successfully created user personal")
            }
            .addOnFailureListener {e ->
                isError(true)
                Timber.tag("MyDB").d( "Failed to create user personal with error $e")
            }
    }

    /**
     * Fetches all of a user's personal data.
     *
     * @param userID ID of the user
     * @return UserPersonal object with all personal data
     */
    suspend fun fetchUserPersonal(userID: String, isError: (Boolean) -> Unit): UserPersonal {
        // check that a correct userID was given
        if (userID.isEmpty()) {
            isError(true)
            Timber.tag("MyDB").d( "Failed to fetch user personal because userID is empty")
            return UserPersonal.empty()
        }

        // fetches the userPersonal document
        val document = userPersonalCollection.document(userID).get().await()
        return if (document.exists()) {
            var errorOccurred = false

            // fetch each ingredient from the groceries map
            val groceryListRefs = document.get(GROCERIES) as? Map<String, List<DocumentReference>> ?: emptyMap()
            val groceryList = groceryListRefs.mapValues { entry ->
                entry.value.map { ref ->
                    fetchIngredient(ref) { if (it) errorOccurred = true }
                }
            }
            // fetch each ingredient from the fridge map
            val fridgeListRefs = document.get(FRIDGE) as? Map<String, List<DocumentReference>> ?: emptyMap()
            val fridgeList = fridgeListRefs.mapValues { entry ->
                entry.value.map { ref ->
                    fetchIngredient(ref) { if (it) errorOccurred = true }
                }
            }
            // fetch the map of notes
            val notes = document.get(NOTES) as? Map<String, String> ?: emptyMap()
            val formattedNotes = notes.toMutableMap()
            formattedNotes.replaceAll { _, value -> value.replace("\\n", "\n") }
            // Create and return the UserPersonal object
            isError(errorOccurred)
            if (!errorOccurred) {
                Timber.tag("MyDB").d( "Successfully fetched user personal")
                UserPersonal(userID, groceryList, fridgeList, formattedNotes)
            } else {
                Timber.tag("MyDB").d( "Failed to fetch user personal: failed to fetch some ingredient")
                UserPersonal.empty()
            }
        } else {
            isError(true)
            Timber.tag("MyDB").d( "Failed to fetch user personal: document does not exist")
            UserPersonal.empty()
        }
    }

    /**
     * Deletes all ingredients owned by a user and userPersonal document.
     *
     * @param userID ID of the user
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after DB was updated
     */
    private fun deleteUserPersonal(userID: String, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        // delete all documents in ingredients collection where the owner is userID
        ingredientsCollection
            .whereEqualTo(OWNER, userID)
            .get()
            .addOnSuccessListener { query ->
                val batch = Firebase.firestore.batch()
                for (document in query.documents) {
                    batch.delete(document.reference)
                }
                // once all ingredients deletion is complete ->
                batch.commit()
                    .addOnSuccessListener {
                        Timber.tag("MyDB").d( "Successfully deleted ingredients owned by user")
                        // delete the userPersonal document of the user to be deleted
                        userPersonalCollection
                            .document(userID)
                            .delete()
                            .addOnSuccessListener {
                                callBack()
                                isError(false)
                                Timber.tag("MyDB").d( "Successfully deleted user personal")
                            }
                            .addOnFailureListener { e ->
                                isError(true)
                                Timber.tag("MyDB").d( "Failed to delete user personal with error $e")
                            }
                    }
                    .addOnFailureListener { e ->
                        isError(true)
                        Timber.tag("MyDB").d( "Failed to delete ingredients owned by user with error $e")
                    }
            }.addOnFailureListener { e ->
                isError(true)
                Timber.tag("MyDB").d( "Failed to fetch ingredients owned by user with error $e")
            }
    }

    /**
     * Adds a new user-defined category in both the groceries list and fridge.
     *
     * @param owner UID of the user
     * @param category name input by the user
     * @param ingredients potential list of ingredient to be added in the new category (can be empty)
     * @param isInFridge whether the list of new ingredients is to be added in the fridge. Goes in the groceries if false
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after DB was updated
     */
    fun addCategory(owner: String, category: String, ingredients: List<OwnedIngredient>, isInFridge: Boolean, isError: (Boolean) -> Unit, callBack: () -> Unit ) {
         // if the ingredients list is empty -> only add new map entry with empty value
         if (ingredients.isEmpty()) {
            userPersonalCollection
                .document(owner)
                .update(
                    // only add the new category as empty map in both groceries and fridge
                    "$GROCERIES.${category}", FieldValue.arrayUnion(),
                    "$FRIDGE.${category}", FieldValue.arrayUnion()
                )
                .addOnSuccessListener {
                    isError(false)
                    callBack()
                    Timber.tag("MyDB").d( "Successfully added the empty category $category")
                }
                .addOnFailureListener { e ->
                    isError(true)
                    Timber.tag("MyDB").d( "Failed to add empty category $category with error $e")
                }
         // if there are new ingredients to be added ->
         } else {
             // check how many ingredients are left to update before callBack
             var remaining = ingredients.size
             var errorOccurred = false
             for (ingredient in ingredients) {
                 // add new ingredient (category name automatically added)
                 createIngredient(owner, ingredient, isInFridge, {
                     if (it) {
                         errorOccurred = true
                         remaining--
                         if (remaining <= 0) {
                             isError(true)
                             Timber.tag("MyDB").d( "Failed to add new category $category")
                         }
                     }
                 }) {
                     remaining--
                     if (remaining <= 0) {
                         isError(errorOccurred)
                         if (errorOccurred) Timber.tag("MyDB").d( "Failed to add new category $category")
                         else {
                             callBack()
                             Timber.tag("MyDB").d( "Successfully added new category $category")
                         }
                     }
                }
            }
        }

    }

    /**
     * Changes a category name, both in the userPersonal maps of references and the category field of concerned ingredients.
     *
     * @param userID ID of the user
     * @param old former category name, that needs to be changed
     * @param new new category name
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after DB was updated
     */
    fun updateCategory(userID: String, old: String, new: String, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        // fetch the user's personal data document
        val userRef = userPersonalCollection.document(userID)
        userRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // combine together all the ingredients from the groceries and the fridge in the category that is changing
                    val groceriesMap = document.get(GROCERIES) as? Map<String, List<DocumentReference>> ?: emptyMap()
                    val fridgeMap = document.get(FRIDGE) as? Map<String, List<DocumentReference>> ?: emptyMap()
                    val groceries = groceriesMap[old] ?: emptyList()
                    val fridge = fridgeMap[old] ?: emptyList()
                    val allIngredients = groceries + fridge

                    // for each of those ingredients -> change the category field to the new name
                    allIngredients.forEach { ref ->
                        ref.update(CATEGORY, new)
                            .addOnSuccessListener {
                                Timber.tag("MyDB").d( "Successfully updated category of ingredient $ref")
                            }
                            .addOnFailureListener { e ->
                                isError(true)
                                Timber.tag("MyDB").d( "Failed to update category of ingredient $ref with error $e")
                            }
                    }

                    // for both groceries and fridge, create a new map where the old category key is removed, and new category key is put instead
                    val updatedGroceries = groceriesMap.toMutableMap()
                    val updatedFridge = fridgeMap.toMutableMap()

                    val newGroceryList = updatedGroceries.remove(old)?.toMutableList() ?: mutableListOf()
                    updatedGroceries[new] = newGroceryList

                    val newFridgeList = updatedFridge.remove(old)?.toMutableList() ?: mutableListOf()
                    updatedFridge[new] = newFridgeList

                    // update the userPersonal document with the new maps
                    userRef.update(mapOf(
                        GROCERIES to updatedGroceries,
                        FRIDGE to updatedFridge
                    )).addOnSuccessListener {
                        isError(false)
                        callBack()
                        Timber.tag("MyDB").d( "Successfully updated category in groceryList and fridge")
                    }.addOnFailureListener { e ->
                        isError(true)
                        Timber.tag("MyDB").d( "Failed to update category with error: $e")
                    }
                } else {
                    isError(true)
                    Timber.tag("MyDB").d( "Failed to update category: user personal document does not exist")
                }
            }.addOnFailureListener { e ->
                isError(true)
                Timber.tag("MyDB").d( "Failed to get user personal document with error $e")
            }
    }

    /**
     * Deletes a category and all its children ingredients, both in groceries and fridge.
     *
     * @param owner ID of the user
     * @param category name of the category that needs to be deleted
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after DB was updated
     */
    fun deleteCategory(owner: String, category: String, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        val ingredientsToDelete = mutableListOf<DocumentReference>()

        // fetch the userPersonal document
        userPersonalCollection
            .document(owner)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // put together all ingredients from the category that needs to be deleted
                    val groceriesMap = document.get(GROCERIES) as? Map<String, List<DocumentReference>>
                    val fridgeMap = document.get(FRIDGE) as? Map<String, List<DocumentReference>>

                    groceriesMap?.get(category)?.let { ingredientsToDelete.addAll(it) }
                    fridgeMap?.get(category)?.let { ingredientsToDelete.addAll(it) }

                    // if there are any ingredients to delete ->
                    if (ingredientsToDelete.isNotEmpty()) {
                        // check how many ingredients are left to delete before next steps
                        var remaining = ingredientsToDelete.size
                        var errorOccurred = false
                        ingredientsToDelete.forEach { ref ->
                            ref.delete()
                                .addOnSuccessListener {
                                    remaining--
                                    if (remaining <= 0) {
                                        if (errorOccurred) isError(true)
                                        else {
                                            // if all ingredients have been deleted ->
                                            userPersonalCollection
                                                .document(owner)
                                                // delete the category entry from groceries and fridge maps
                                                .update(
                                                    "$GROCERIES.$category",FieldValue.delete(),
                                                    "$FRIDGE.$category", FieldValue.delete()
                                                )
                                                .addOnSuccessListener {
                                                    isError(false)
                                                    callBack()
                                                    Timber.tag("MyDB").d( "Successfully deleted category $category")
                                                }
                                                .addOnFailureListener { e ->
                                                    isError(true)
                                                    Timber.tag("MyDB").d( "Failed to delete category $category with $e")
                                                }
                                        }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    remaining--
                                    errorOccurred = true
                                    if (remaining <= 0) {
                                        isError(true)
                                    }
                                    Timber.tag("MyDB").d( "Failed to delete ingredient with error $e")
                                }
                        }
                    // if there are no ingredients to delete ->
                    } else {
                        userPersonalCollection
                            .document(owner)
                            // delete the category entry from groceries and fridge maps
                            .update(
                                "$GROCERIES.$category",FieldValue.delete(),
                                "$FRIDGE.$category", FieldValue.delete()
                            )
                            .addOnSuccessListener {
                                isError(false)
                                callBack()
                                Timber.tag("MyDB").d( "Successfully deleted category $category")
                            }
                            .addOnFailureListener { e ->
                                isError(true)
                                Timber.tag("MyDB").d( "Failed to delete category $category with $e")
                            }
                    }
                } else {
                    isError(true)
                    Timber.tag("MyDB").d( "Failed to delete category $category because it does not exist")
                }
            }
            .addOnFailureListener { e ->
                isError(true)
                Timber.tag("MyDB").d( "Failed to retrieve userPersonal for $owner with error $e")
            }
    }

    /**
     * Updates the notes field of a userPersonal document by either adding a new entry or updating an existing one.
     *
     * @param userID ID of the user who wrote the note
     * @param recipeID ID of the recipe that the note is attached to
     * @param note written by the user
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after the DB was updated
     */
    fun updateNotes(userID: String, recipeID: String, note: String, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        val userRef = userPersonalCollection.document(userID)

        userRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // replace or add the new note value for the recipeID key
                    val currentNotes = document.get(NOTES) as? Map<String, String> ?: emptyMap()
                    val updatedNotes = currentNotes.toMutableMap()
                    val formattedNote = note.replace("\n", "\\n")
                    updatedNotes[recipeID] = formattedNote

                    userRef.update(NOTES, updatedNotes)
                        .addOnSuccessListener {
                            isError(false)
                            callBack()
                            Timber.tag("MyDB").d( "Successfully updated notes")
                        }
                        .addOnFailureListener { e ->
                            isError(true)
                            Timber.tag("MyDB").d( "Failed to updates notes field with error $e")
                        }
                } else {
                    isError(true)
                    Timber.tag("MyDB").d( "Failed to updates notes: userPersonal document does not exist")
                }
            }
            .addOnFailureListener { e ->
                isError(true)
                Timber.tag("MyDB").d( "Failed to fetch userPersonal document with error $e")
            }
    }

    /**
     * Deletes some user's note for a given recipe.
     *
     * @param userID ID of the user who deletes their note
     * @param recipeID ID of the recipe that the note was attached to
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after the DB was updated
     */
    fun deleteNote(userID: String, recipeID: String, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        val userRef = userPersonalCollection.document(userID)

        userRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // delete the map entry with recipeID key
                    val currentNotes = document.get(NOTES) as? Map<String, String> ?: emptyMap()
                    val updatedNotes = currentNotes.toMutableMap()

                    if (updatedNotes.containsKey(recipeID)) {
                        updatedNotes.remove(recipeID)
                        userRef.update(NOTES, updatedNotes)
                            .addOnSuccessListener {
                                isError(false)
                                callBack()
                                Timber.tag("MyDB").d( "Successfully removed note")
                            }
                            .addOnFailureListener { e ->
                                isError(true)
                                Timber.tag("MyDB").d( "Failed to updates notes field with error $e")
                            }
                    } else {
                        isError(false)
                        callBack()
                        Timber.tag("MyDB").d( "Successfully finished note deletion: the note already did not exist")
                    }
                } else {
                    isError(true)
                    Timber.tag("MyDB").d( "Failed to updates notes: userPersonal document does not exist")
                }
            }
            .addOnFailureListener { e ->
                isError(true)
                Timber.tag("MyDB").d( "Failed to fetch userPersonal document with error $e")
            }
    }


    // ingredients

    /**
     * Checks if some user owns an ingredient and, if it does, returns its emplacement (groceries or fridge and which category).
     * This check is approximate as it compares standardized names.
     *
     * @param userID ID of the user
     * @param ingredientName standardized name of the ingredient looked for
     * @param onSuccess block that runs if the check succeeds (whether or not the ingredient exists)
     * @param onFailure block that runs if there is an error executing the function
     */
    fun ingredientExistsWhere(userID: String, ingredientName: String, onSuccess: (Boolean, List<Triple<Boolean, String, String>>) -> Unit, onFailure: (Exception) -> Unit) {
        userPersonalCollection.document(userID).get()
            .addOnSuccessListener outerListener@ { document ->
                if (document != null && document.exists()) {
                    // set up global variables needed for entire function run
                    val fields = listOf(FRIDGE, GROCERIES)
                    var remainingFields = fields.size
                    val matches = mutableListOf<Triple<Boolean, String, String>>()
                    var errorOccurred = false

                    // check existence in both fridge and groceries
                    fields.forEach { field ->
                        // fetch document field
                        val map = document.get(field) as? Map<String, List<DocumentReference>>
                        if (map != null) {
                            if (map.isEmpty()) {
                                // if the field is empty -> return no matches
                                onSuccess(false, emptyList())
                                return@outerListener
                            }

                            // check the lists of ingredient references of the entire field
                            val allIngredients = map.values.flatten()
                            if (allIngredients.isEmpty()) {
                                remainingFields--
                                if (remainingFields <= 0) {
                                    if (errorOccurred) onFailure(IllegalStateException("An ingredient failed"))
                                    else {
                                        onSuccess(matches.isNotEmpty(), matches)
                                        Timber.tag("MyDB").d( "Successfully collected matches $matches")
                                        return@outerListener
                                    }
                                }
                            }

                            // check all ingredients of this field
                            var remainingRefs = allIngredients.size
                            allIngredients.forEach { ref ->
                                // fetch ingredient reference
                                ref.get()
                                    .addOnSuccessListener { ingredient ->
                                        // if the ingredient was correctly fetched -> check for stand name equality
                                        val standName = ingredient.getString(STAND_NAME)
                                        if (standName == ingredientName) {
                                            // if names are equal -> add new pair to matches
                                            matches.add(Triple(
                                                field == FRIDGE,
                                                ingredient.getString(CATEGORY) ?: "",
                                                ingredient.getString(DISPLAY_NAME) ?: "")
                                            )
                                            Timber.tag("MyDB").d( "Successfully found a match")
                                        }
                                        remainingRefs--
                                        if (remainingRefs <= 0) {
                                            remainingFields--
                                            if (remainingFields <= 0) {
                                                if (errorOccurred) onFailure(IllegalStateException("An ingredient failed"))
                                                else {
                                                    onSuccess(matches.isNotEmpty(), matches)
                                                    Timber.tag("MyDB").d( "Successfully collected matches $matches")
                                                }
                                            }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        // if failed to fetch ingredient reference -> error
                                        remainingRefs--
                                        errorOccurred = true
                                        onFailure(e)
                                        Timber.tag("MyDB").d( "Failed to check ingredient existence; failure to fetch ingredient ref with error $e")
                                        if (remainingRefs <= 0) {
                                            remainingFields--
                                            if (remainingFields <= 0) onFailure(e)
                                        }
                                    }


                            }
                        } else {
                            // if document field could not be fetched -> error
                            onFailure(IllegalStateException("Failed to fetch maps in UserPersonal document"))
                            Timber.tag("MyDB").d( "Failed to check ingredient existence because userPersonal document maps are null")
                        }
                    }

                } else {
                    onFailure(IllegalStateException("UserPersonal document is null or does not exist"))
                    Timber.tag("MyDB").d( "Failed to check ingredient existence because userPersonal document is null or does not exist")
                }
            }
            .addOnFailureListener { e ->
                onFailure(e)
                Timber.tag("MyDB").d( "Failed to check ingredient existence because could not access userPersonal with error $e")
            }
    }

    /**
     * Checks if some user owns an ingredient with given name in given category, in their groceries list.
     *
     * @param userID ID of the user
     * @param category category the ingredient should be in
     * @param ingredientName displayed name of the ingredient looked for
     * @param isInFridge whether or not to check for ingredient existence in the fridge or in groceries
     * @param onSuccess block that runs if the check succeeds (whether or not the ingredient exists)
     * @param onFailure block that runs if there is an error executing the function
     */
    fun ingredientExistsInCategory(userID: String, category: String, ingredientName: String, isInFridge: Boolean, onSuccess: (Boolean) -> Unit, onFailure: (Exception) -> Unit) {
        userPersonalCollection.document(userID).get()
            .addOnSuccessListener outerListener@{ document ->
                if (document != null && document.exists()) {
                    val targetField = if (isInFridge) FRIDGE else GROCERIES
                    val map = document.get(targetField) as? Map<*, *>
                    val ingredientRefs = map?.get(category) as? List<DocumentReference>

                    if (ingredientRefs != null) {
                        // if the there are no ingredients in this category -> ingredient does not exist
                        if (ingredientRefs.isEmpty()) {
                            onSuccess(false)
                            Timber.tag("MyDB").d( "Successfully found that ingredient does not exist")
                            return@outerListener
                        }
                        // else loop over ingredient until it is found or none is found
                        var remaining = ingredientRefs.size
                        var errorOccurred = false
                        var found = false
                        ingredientRefs.forEach { ref ->
                            ref.get()
                                .addOnSuccessListener { ingredient ->
                                    if (found) return@addOnSuccessListener
                                    val displayName = ingredient.getString(DISPLAY_NAME)
                                    if (displayName == ingredientName) {
                                        Timber.tag("MyDB").d( "Successfully found that ingredient exists")
                                        onSuccess(true)
                                        found = true
                                    }
                                    remaining--
                                    if (remaining <= 0) {
                                        if (errorOccurred) {
                                            onFailure(IllegalStateException("Checking some ingredient failed"))
                                            Timber.tag("MyDB").d( "Failed to check ingredient existence because fetching some ingredient ref failed")
                                        } else {
                                            onSuccess(found)
                                            Timber.tag("MyDB").d( "Successfully finished looking for ingredient existence")
                                        }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    remaining--
                                    errorOccurred = true
                                    if (remaining <= 0) {
                                        onFailure(e)
                                    }
                                    Timber.tag("MyDB").d( "Failed to check ingredient existence because fetching ingredient ref failed with error $e")
                                }
                        }
                    } else {
                        onFailure(IllegalStateException("List of ingredient references is null"))
                        Timber.tag("MyDB").d( "Failed to check ingredient existence because ingredient references are null")
                    }
                } else {
                    onFailure(IllegalStateException("UserPersonal document is null or does not exist"))
                    Timber.tag("MyDB").d( "Failed to check ingredient existence because userPersonal document is null or does not exist")
                }
            }
            .addOnFailureListener { e ->
                onFailure(e)
                Timber.tag("MyDB").d( "Failed to check ingredient existence because could not access userPersonal with error $e")
            }
    }

    /**
     * Filters a list of ingredients to only keep those for which a user owns all the ingredients.
     *
     * @param userID ID of the user
     * @param allRecipes mutable list of Recipe objects to be filtered
     * @param onSuccess block that runs if the check succeeds (whether or not the ingredient exists)
     * @param onFailure block that runs if there is an error executing the function
     */
    fun recipesWithOwnedIngredients(userID: String, allRecipes: MutableList<Recipe>, onSuccess: (List<Recipe>) -> Unit, onFailure: (Exception) -> Unit) {
        // check that the list of recipes isn't empty
        if (allRecipes.isEmpty()) {
            Timber.tag("MyDB").d( "Successfully found that list of recipes is empty")
            onSuccess(allRecipes)
        } else {
            // access the user personal data
            userPersonalCollection.document(userID).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // put together all the referenced owned ingredients
                        val fridgeMap = document.get(FRIDGE) as? Map<String, List<DocumentReference>>
                        val ingredientRefs = fridgeMap?.values?.flatten() ?: emptyList()

                        // if user doesn't own any ingredients -> filter recipes that don't have any ingredients
                        if (ingredientRefs.isEmpty()) {
                            Timber.tag("MyDB").d( "Successfully found that user does not own any ingredients")
                            val filteredRecipes = allRecipes.filter { recipe ->
                                recipe.ingredients.isEmpty()
                            }
                            onSuccess(filteredRecipes)
                            return@addOnSuccessListener
                        }

                        // create a list with standName of all owned ingredients
                        val ownedIngredients = mutableListOf<String>()
                        var remainingIngredients = ingredientRefs.size
                        ingredientRefs.forEach { ref ->
                            ref.get()
                                .addOnSuccessListener { document ->
                                    val standName = document.getString(STAND_NAME) ?: ""
                                    ownedIngredients.add(standName)
                                    remainingIngredients--
                                    if (remainingIngredients <= 0) {
                                        // once the list of ingredients is created -> loop over recipes
                                        val filteredRecipes = allRecipes.filter { recipe ->
                                            recipe.ingredients.values.flatten().all { ingredient ->
                                                ingredient.standName in ownedIngredients
                                            }
                                        }
                                        onSuccess(filteredRecipes)
                                    }
                                }
                                .addOnFailureListener { e ->
                                    // don't keep on filtering other ingredients -> fail function
                                    onFailure(e)
                                    Timber.tag("MyDB").d( "Failed to fetch ingredient with error $e")
                                }
                        }
                    } else {
                        onFailure(IllegalStateException("UserPersonal document is null or does not exist"))
                        Timber.tag("MyDB").d( "Failed to filter recipes by owned ingredients: userPersonal document is null")
                    }
                }
                .addOnFailureListener { e ->
                    onFailure(e)
                    Timber.tag("MyDB").d( "Failed to filter recipes by owned ingredients: couldn't get document with error $e")
                }
        }
    }

    /**
     * Creates a new ingredient document and adds the necessary references.
     * The category map entry key is automatically added if it does not exist yet.
     *
     * @param owner ID of the user
     * @param newItem OwnedIngredient object representing the new ingredient
     * @param isInFridge whether the new ingredient should be added in the fridge. If false, it is put in the groceries instead
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after DB was updated
     */
    fun createIngredient(owner: String, newItem: OwnedIngredient, isInFridge: Boolean, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        // determine in which field the ingredient has to be added
        val targetField = if (isInFridge) {"$FRIDGE.${newItem.category}"} else {"$GROCERIES.${newItem.category}"}
        val syncedField = if (isInFridge) {"$GROCERIES.${newItem.category}"} else {"$FRIDGE.${newItem.category}"}

        val ingredient = hashMapOf(OWNER to owner, DISPLAY_NAME to newItem.displayedName, STAND_NAME to newItem.standName, CATEGORY to newItem.category, IS_TICKED to false)

        // create the new ingredient document
        ingredientsCollection
            .add(ingredient)
            .addOnSuccessListener {
                Timber.tag("MyDB").d( "Successfully created ingredient")
                // if the new ingredient was correctly added ->
                userPersonalCollection
                    .document(owner)
                    // add the new document reference to the userPersonal target map
                    // add an empty list in the synced field in case the category is new
                    .update(
                        targetField, FieldValue.arrayUnion(it),
                        syncedField, FieldValue.arrayUnion()
                    )
                    .addOnSuccessListener {
                        isError(false)
                        callBack()
                        Timber.tag("MyDB").d( "Successfully added the ingredient reference")
                    }
                    .addOnFailureListener {e ->
                        isError(true)
                        Timber.tag("MyDB").d( "Failed to add ingredient reference with error $e")
                    }
            }
            .addOnFailureListener { e ->
                isError(true)
                Timber.tag("MyDB").d( "Failed to create ingredient with error $e")
            }
    }

    /**
     * Fetches all ingredient data.
     *
     * @param ref DocumentReference (contained in groceries and fridge maps of userPersonal)
     * @return OwnedIngredient object with all ingredient data
     */
    private suspend fun fetchIngredient(ref: DocumentReference, isError: (Boolean) -> Unit): OwnedIngredient {
        val document = ref.get().await()
        return if (document.exists()) {
            val displayName = document.getString(DISPLAY_NAME) ?: ""
            val standName = document.getString(STAND_NAME) ?: ""
            val category = document.getString(CATEGORY) ?: ""
            val isTicked = document.getBoolean(IS_TICKED) ?: false
            isError(false)
            Timber.tag("MyDB").d( "Successfully fetched ingredient")
            OwnedIngredient(ref.id, displayName, standName, category, isTicked)
        } else {
            isError(true)
            Timber.tag("MyDB").d( "Failed to fetch ingredient because document does not exist")
            OwnedIngredient.empty()
        }

    }

    /**
     * Changed whether an ingredient's tick is changed (only useful for groceries).
     *
     * @param uid ID of the ingredient
     * @param isTicked new ticked state of the ingredient
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after DB was updated
     */
    fun updateIngredientTick(uid: String, isTicked: Boolean, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        ingredientsCollection
            .document(uid)
            .update(IS_TICKED, isTicked)
            .addOnSuccessListener {
                isError(false)
                callBack()
                Timber.tag("MyDB").d( "Successfully updated ingredient tick")
            }
            .addOnFailureListener { e ->
                isError(true)
                Timber.tag("MyDB").d( "Failed to update ingredient tick with error $e")
            }
    }

    /**
     * Deletes an ingredient and its existing references.
     *
     * @param uid ID of the ingredient
     * @param owner ID of the user
     * @param category in which the ingredient is referenced
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after DB was updated
     */
    fun deleteIngredient(uid: String, owner: String, category: String, isInFridge: Boolean, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        val ref = ingredientsCollection.document(uid)
        val targetField = if (isInFridge) {"$FRIDGE.${category}"} else {"$GROCERIES.${category}"}

        // remove the ingredient document reference from the category in which it is
        userPersonalCollection
            .document(owner)
            .update(targetField, FieldValue.arrayRemove(ref))
            .addOnSuccessListener {
                isError(false)
                Timber.tag("MyDB").d( "Successfully deleted ingredient ref")

                // if the reference was correctly removed -> delete the ingredient document
                ref.delete()
                    .addOnSuccessListener {
                        isError(false)
                        Timber.tag("MyDB").d( "Successfully deleted ingredient")
                        callBack()
                    }
                    .addOnFailureListener { e ->
                        isError(true)
                        Timber.tag("MyDB").d( "Failed to delete ingredient with error $e")
                    }
            }
            .addOnFailureListener { e ->
                isError(true)
                Timber.tag("MyDB").d( "Failed to delete ingredient ref from user with error $e")
            }
    }

    /**
     * Clears all ingredients from either the fridge or the groceries. The category names are retained.
     *
     * @param owner ID of the user
     * @param isInFridge whether the fridge list should be cleared. If false, the groceries list is cleared instead
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after DB was updated
     */
    fun clearIngredients(owner: String, isInFridge: Boolean, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        val targetField = if (isInFridge) FRIDGE else GROCERIES

        userPersonalCollection
            .document(owner)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // get the map that needs to be cleared (fridge or groceries)
                    val targetMap = document[targetField] as? Map<String, List<DocumentReference>> ?: emptyMap()

                    // if the map is already empty -> no ingredients to clear
                    if (targetMap.isEmpty()) {
                        isError(false)
                        callBack()
                        Timber.tag("MyDB").d( "Successfully cleared ingredients: map $targetField is already empty")
                        return@addOnSuccessListener
                    }

                    // if the map is not empty -> clear it
                    var remainingCat = targetMap.size
                    var errorOccurred = false
                    targetMap.forEach { (category, refs) ->
                        var remainingRefs = refs.size

                        // if this category does not contain any reference -> just increase counter
                        if (refs.isEmpty()) {
                            remainingCat--
                            if (remainingCat <= 0) {
                                isError(errorOccurred)
                                if (errorOccurred) Timber.tag("MyDB").d( "Failed to clear all ingredients")
                                else {
                                    callBack()
                                    Timber.tag("MyDB").d( "Successfully cleared all ingredients for $targetField")
                                }
                            }
                        } else {
                            refs.forEach { ref ->
                                ref.delete()
                                    .addOnSuccessListener {
                                        remainingRefs--
                                        if (remainingRefs <= 0) {
                                            // once all referenced items have been deleted -> empty the references list
                                            userPersonalCollection.document(owner)
                                                .update("$targetField.$category", emptyList<DocumentReference>())
                                                .addOnSuccessListener {
                                                    remainingCat--
                                                    if (remainingCat <= 0) {
                                                        isError(errorOccurred)
                                                        if (errorOccurred) Timber.tag("MyDB").d( "Failed to clear all ingredients")
                                                        else {
                                                            callBack()
                                                            Timber.tag("MyDB").d( "Successfully cleared all ingredients for $targetField")
                                                        }
                                                    }
                                                }
                                                .addOnFailureListener { e ->
                                                    isError(true)
                                                    Timber.tag("MyDB").d( "Failed to delete references in category $category with error $e")
                                                }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        errorOccurred = true
                                        remainingRefs--
                                        if (remainingRefs <= 0) {
                                            remainingCat--
                                            if (remainingCat <= 0) {
                                                isError(true)
                                            }
                                        }
                                        Timber.tag("MyDB").d( "Failed to delete ingredient with error $e")
                                    }
                            }
                        }
                    }
                } else {
                    isError(true)
                    Timber.tag("MyDB").d( "Failed to clear ingredients: user personal document does not exist")
                }
            }
            .addOnFailureListener { e ->
                isError(true)
                Timber.tag("MyDB").d( "Failed to clear ingredients with error $e")
            }
    }

    /**
     * Sends ticked items from the grocery list to the fridge and removes them from the groceries list.
     *
     * @param owner ID of the user
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after DB was updated
     */
    fun groceriesToFridge(owner: String, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        userPersonalCollection
            .document(owner)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val groceries = document[GROCERIES] as? Map<String, List<DocumentReference>> ?: emptyMap()
                    val fridge = document[FRIDGE] as? Map<String, List<DocumentReference>> ?: emptyMap()
                    val newGroceries = groceries.toMutableMap()
                    val newFridge = fridge.toMutableMap()

                    var remainingItems = groceries.values.flatten().size

                    // if groceries doesn't contain any items -> return
                    if (remainingItems <= 0) {
                        isError(false)
                        callBack()
                        Timber.tag("MyDB").d( "Successfully completed transfer: groceries list is empty")
                        return@addOnSuccessListener
                    }

                    // if there are groceries items to check -> go through them
                    var errorOccurred = false
                    groceries.forEach { (category, refs) ->
                        refs.forEach { ref ->
                            ref.get()
                                .addOnSuccessListener { ingredient ->
                                    // if the ingredient is ticked ->
                                    if (ingredient.exists() && ingredient.getBoolean(IS_TICKED) == true) {
                                        // remove ref from groceries
                                        newGroceries[category] = newGroceries[category]?.filter { it != ref } ?: emptyList()
                                        // add ref to fridge if it doesn't already exist
                                        ingredientExistsInCategory(owner, category, ingredient.getString(DISPLAY_NAME) ?: "", true,
                                            onSuccess = { exists ->
                                                if (!exists) { newFridge[category] = (newFridge[category] ?: emptyList()) + ref }
                                                // decrease counter
                                                remainingItems--
                                                if (remainingItems <= 0) {
                                                    // bring changes to Database
                                                    userPersonalCollection
                                                        .document(owner).update(
                                                            GROCERIES, newGroceries,
                                                            FRIDGE, newFridge
                                                        )
                                                    isError(errorOccurred)
                                                    if (errorOccurred) Timber.tag("MyDB").d( "Failed to transfer all items to fridge")
                                                    else {
                                                        callBack()
                                                        Timber.tag("MyDB").d( "Successfully finished transferring items")
                                                    }
                                                }
                                            },
                                            onFailure = { e ->
                                                errorOccurred = true
                                                // decrease counter
                                                remainingItems--
                                                if (remainingItems <= 0) {
                                                    // bring changes to Database
                                                    userPersonalCollection
                                                        .document(owner).update(
                                                            GROCERIES, newGroceries,
                                                            FRIDGE, newFridge
                                                        )
                                                    isError(true)
                                                }
                                                Timber.tag("MyDB").d( "Failed to check if ingredient already exists in fridge with error $e")
                                            }
                                        )
                                    } else {
                                        remainingItems--
                                        if (remainingItems <= 0) {
                                            // bring changes to Database
                                            userPersonalCollection
                                                .document(owner).update(
                                                    GROCERIES, newGroceries,
                                                    FRIDGE, newFridge
                                                )
                                            isError(errorOccurred)
                                            if (errorOccurred) Timber.tag("MyDB").d( "Failed to transfer all items to fridge")
                                            else {
                                                callBack()
                                                Timber.tag("MyDB").d( "Successfully finished transferring items")
                                            }
                                        }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    errorOccurred = true
                                    remainingItems--
                                    if (remainingItems <= 0) {
                                        userPersonalCollection
                                            .document(owner).update(
                                                GROCERIES, newGroceries,
                                                FRIDGE, newFridge
                                            )
                                        isError(true)
                                    }
                                    Timber.tag("MyDB").d( "Failed to transfer items because $ref does not exist with error $e")
                                }
                        }
                    }
                } else {
                    isError(true)
                    Timber.tag("MyDB").d( "Failed to transfer items to fridge: user personal document does not exist")
                }
            }
            .addOnFailureListener { e ->
                isError(true)
                Timber.tag("MyDB").d( "Failed to transfer items to fridge with error $e")
            }
    }


    // recipes

    /**
     * Checks if a recipe exists in the DB.
     *
     * @param recipeID ID of the recipe whose existence to check
     * @param onSuccess block that runs if the check succeeds (whether or not the recipe exists)
     * @param onFailure block that runs if there is an error executing the function
     */
    fun recipeExists(recipeID: String, onSuccess: (Boolean) -> Unit, onFailure: (Exception) -> Unit) {
        recipesCollection
            .document(recipeID)
            .get()
            .addOnSuccessListener { document -> onSuccess(document.exists()) }
            .addOnFailureListener { e -> onFailure(e) }
    }

    /**
     * Creates a new Recipe document.
     *
     * @param userID UID of the user who created the recipe
     * @param name title of the recipe
     * @param pictures pictures of the recipe (can be an empty list)
     * @param instructions list of strings where each element represents a step of the cooking instructions
     * @param ingredients maps section names to lists of RecipeIngredient objects
     * @param portion number that indicates for how many servings this recipe is designed
     * @param perPerson if true, the portion is per person, if false it is per piece
     * @param origin origin tag from Origin enum
     * @param diet diet tag from Diet enum
     * @param tags list of tags from Tag enum
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after DB was updated, returning the recipe's ID
     */
    fun createRecipe(
        userID: String,
        name: String,
        pictures: List<Uri>,
        instructions: List<String>,
        ingredients: Map<String, List<RecipeIngredient>>,
        portion: Int,
        perPerson: Boolean,
        origin: Origin,
        diet: Diet,
        tags: List<Tag>,
        isError: (Boolean) -> Unit,
        callBack: (String) -> Unit
    ) {
        val recipe = hashMapOf(
            OWNER to userID,
            NAME to name,
            PICTURES to emptyList<String>(),
            INSTRUCTIONS to instructions,
            INGREDIENTS to ingredients.mapValues {(_, ingredientMaps) ->
                ingredientMaps.map { ingredient ->
                    mapOf(
                        DISPLAY_NAME to ingredient.displayedName,
                        STAND_NAME to ingredient.standName,
                        QUANTITY to ingredient.quantity,
                        UNIT to ingredient.unit
                    )
                }
            },
            PORTION to portion,
            PER_PERSON to perPerson,
            ORIGIN to origin.toString(),
            DIET to diet.toString(),
            TAGS to tags.map { it.toString() },
            FAVOURITE to emptyList<String>()
        )

        recipesCollection
            .add(recipe)
            .addOnSuccessListener { document ->
                Timber.tag("MyDB").d( "Successfully created recipe")

                // increment the user's counter for created recipes
                userDataCollection.document(userID)
                    .update(NUMBER_RECIPES, FieldValue.increment(1))
                    .addOnSuccessListener {
                        Timber.tag("MyDB").d( "Successfully incremented recipes counter")
                        // add recipe picture to storage if pictures list is not empty
                        if (pictures.isNotEmpty()) {
                            updateRecipePictures(userID, document.id, pictures, { isError(it) }) {
                                isError(false)
                                callBack(document.id)
                                Timber.tag("MyDB").d( "Successfully finished recipe creation process")
                            }
                        // finish here if there are no pictures
                        } else {
                            isError(false)
                            callBack(document.id)
                            Timber.tag("MyDB").d( "Successfully finished recipe creation process")
                        }
                    }
                    .addOnFailureListener { e ->
                        isError(true)
                        Timber.tag("MyDB").d( "Failed to increment recipes counter with error $e")
                    }
            }
            .addOnFailureListener { e ->
                isError(true)
                Timber.tag("MyDB").d( "Failed to create new recipe with error $e")
            }
    }

    /**
     * Fetches all of a recipe's data.
     *
     * @param recipeID ID of the recipe whose data to retrieve
     * @return Recipe data object with all recipe data
     */
    suspend fun fetchRecipeData(recipeID: String, isError: (Boolean) -> Unit): Recipe {
        if (recipeID.isEmpty()) {
            isError(true)
            Timber.tag("MyDB").d( "Failed to fetch recipe data because recipeID is empty")
            return Recipe.empty()
        }

        val document = recipesCollection.document(recipeID).get().await()
        return if (document.exists()) {
            val owner = document.getString(OWNER) ?: ""
            val name = document.getString(NAME) ?: ""
            val pictures = (document.get(PICTURES) as? List<String>)?.map { Uri.parse(it) } ?: emptyList()
            val instructions = document.get(INSTRUCTIONS) as? List<String> ?: emptyList()
            val formattedInstructions = mutableListOf<String>()
            instructions.forEach {
                val formattedStep = it.replace("\\n", "\n")
                formattedInstructions.add(formattedStep)
            }
            val ingredients = (document.get(INGREDIENTS) as? Map<String, List<Map<String, Any>>>)?.mapValues { (_, ingredientMaps) ->
                ingredientMaps.map { map ->
                    RecipeIngredient(
                        displayedName = (map[DISPLAY_NAME] ?: "").toString(),
                        standName = map[STAND_NAME].toString(),
                        quantity = map[QUANTITY]?.toString()?.toFloatOrNull() ?: 0f,
                        unit = map[UNIT]?.toString()?.let { Measure.valueOf(it) } ?: Measure.NONE,
                    )
                }
            } ?: emptyMap()
            val portion = document.getLong(PORTION)?.toInt() ?: 1
            val perPerson = document.getBoolean(PER_PERSON) ?: true
            val origin = document.getString(ORIGIN)?.let { Origin.valueOf(it) } ?: Origin.NONE
            val diet = document.getString(DIET)?.let { Diet.valueOf(it) } ?: Diet.NONE
            val tagsList = document.get(TAGS) as? List<String> ?: emptyList()
            val tags = tagsList.map { Tag.valueOf(it) }
            val favouriteOf = document.get(FAVOURITE) as? List<String> ?: emptyList()
            isError(false)
            Recipe(document.id, owner, name, pictures, formattedInstructions, ingredients, portion, perPerson, origin, diet, tags, favouriteOf)
        } else {
            isError(true)
            Timber.tag("MyDB").d( "Failed to fetch recipe data")
            Recipe.empty()
        }
    }

    /**
     * Fetches all Recipes from entire database.
     *
     * @param isError block that runs if there is an error executing the function
     * @return list of Recipe objects with all recipes' data
     */
    suspend fun fetchAllRecipes(isError: (Boolean) -> Unit): List<Recipe> {
        return try {
            val query = recipesCollection.get().await()
            query.documents
                .map { document ->
                    val owner = document.getString(OWNER) ?: ""
                    val name = document.getString(NAME) ?: ""
                    val pictures = (document.get(PICTURES) as? List<String>)?.map { Uri.parse(it) } ?: emptyList()
                    val instructions = document.get(INSTRUCTIONS) as? List<String> ?: emptyList()
                    val formattedInstructions = mutableListOf<String>()
                    instructions.forEach {
                        val formattedStep: String = it.replace("\\n", "\n")
                        formattedInstructions.add(formattedStep)
                    }
                    val ingredients = (document.get(INGREDIENTS) as? Map<String, List<Map<String, Any>>>)?.mapValues { (_, ingredientMaps) ->
                        ingredientMaps.map { map ->
                            RecipeIngredient(
                                displayedName = (map[DISPLAY_NAME] ?: "").toString(),
                                standName = map[STAND_NAME].toString(),
                                quantity = map[QUANTITY]?.toString()?.toFloatOrNull() ?: 0f,
                                unit = map[UNIT]?.toString()?.let { Measure.valueOf(it) } ?: Measure.NONE,
                            )
                        }
                    } ?: emptyMap()
                    val portion = document.getLong(PORTION)?.toInt() ?: 1
                    val perPerson = document.getBoolean(PER_PERSON) ?: true
                    val origin = document.getString(ORIGIN)?.let { Origin.valueOf(it) } ?: Origin.NONE
                    val diet = document.getString(DIET)?.let { Diet.valueOf(it) } ?: Diet.NONE
                    val tagsList = document.get(TAGS) as? List<String> ?: emptyList()
                    val tags = tagsList.map { Tag.valueOf(it) }
                    val favouriteOf = document.get(FAVOURITE) as? List<String> ?: emptyList()
                    isError(false)
                    Recipe(document.id, owner, name, pictures, formattedInstructions, ingredients, portion, perPerson, origin, diet, tags, favouriteOf)
                }
        } catch (e: Exception) {
            isError(true)
            Timber.tag("MyDB").d( "Failed to fetch all recipes with error $e")
            emptyList()
        }
    }

    /**
     * Updates an existing Recipe document.
     *
     * @param userID ID of the owner of the recipe
     * @param recipeID ID of the recipe to update
     * @param name title of the recipe
     * @param picturesToRemove list of of pictures that should be deleted from the storage and from the list of references
     * @param pictures pictures of the recipe (can be empty)
     * @param updatePictures whether or not the Storage pictures should be updated
     * @param instructions list of strings where each element represents a step of the cooking instructions
     * @param ingredients a list of RecipeIngredient objects representing the ingredients for the recipe
     * @param portion number that indicates for how many servings this recipe is designed
     * @param perPerson if true, the portion is per person, if false it is per piece
     * @param origin origin tag from Origin enum
     * @param diet diet tag from Diet enum
     * @param tags list of tags from Tag enum
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after DB was updated, returning the recipe's ID
     */
    fun updateRecipe(
        userID: String,
        recipeID: String,
        name: String,
        picturesToRemove: List<Uri>,
        pictures: List<Uri>,
        updatePictures: Boolean,
        instructions: List<String>,
        ingredients: Map<String, List<RecipeIngredient>>,
        portion: Int,
        perPerson: Boolean,
        origin: Origin,
        diet: Diet,
        tags: List<Tag>,
        isError: (Boolean) -> Unit,
        callBack: () -> Unit
    ) {
        val recipe = hashMapOf(
            NAME to name,
            INSTRUCTIONS to instructions,
            INGREDIENTS to ingredients.mapValues {(_, ingredientMaps) ->
                ingredientMaps.map { ingredient ->
                    mapOf(
                        DISPLAY_NAME to ingredient.displayedName,
                        STAND_NAME to ingredient.standName,
                        QUANTITY to ingredient.quantity,
                        UNIT to ingredient.unit
                    )
                }
            },
            PORTION to portion,
            PER_PERSON to perPerson,
            ORIGIN to origin.toString(),
            DIET to diet.toString(),
            TAGS to tags.map { it.toString() },
        )

        recipesCollection
            .document(recipeID)
            .update(recipe as Map<String, Any>)
            .addOnSuccessListener {
                // if there are pictures to remove -> delete them one by one
                if (picturesToRemove.isNotEmpty()) {
                    var remainingToRemove = picturesToRemove.size
                    picturesToRemove.forEach { pictureUri ->
                        deleteSingleRecipePicture(pictureUri.toString(), { isError(it) }) {
                            // if deleting the picture from the Storage was successful -> delete the reference
                            recipesCollection
                                .document(recipeID)
                                .update(PICTURES, FieldValue.arrayRemove(pictureUri.toString()))
                                .addOnSuccessListener {
                                    remainingToRemove--
                                    if (remainingToRemove <= 0) {
                                        isError(false)
                                        Timber.tag("MyDB").d( "Successfully deleted all pictures from recipe")

                                        // once all old pictures have been deleted -> update the new pictures
                                        if (updatePictures) {
                                            updateRecipePictures(userID, recipeID, pictures, { isError(it) }) {
                                                Timber.tag("MyDB").d( "Successfully updated recipe with new pictures")
                                                callBack()
                                            }
                                        } else {
                                            isError(false)
                                            Timber.tag("MyDB").d( "Successfully updated recipe data without picture change")
                                            callBack()
                                        }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    isError(true)
                                    Timber.tag("MyDB").d("Failed to remove picture reference with error $e")
                                }
                        }
                    }

                // directly update new pictures if there are no pictures to delete
                } else if (updatePictures) {
                    updateRecipePictures(userID, recipeID, pictures, { isError(it) }) {
                        Timber.tag("MyDB").d( "Successfully updated recipe with new pictures")
                        callBack()
                    }

                // finish here if there are no pictures to delete or update
                } else {
                    isError(false)
                    Timber.tag("MyDB").d( "Successfully updated recipe data without picture change")
                    callBack()
                }
            }
            .addOnFailureListener { e ->
                isError(true)
                Timber.tag("MyDB").d( "Failed to update recipe with error $e")
            }
    }

    /**
     * Updates the list of pictures of a recipe with the new list.
     * The new list might contain new pictures (those are uploaded) and pictures that were already uploaded (those just check for ordering)
     *
     * @param userID ID of the user who owns the recipe
     * @param recipeID ID of the recipe to update
     * @param pictures list of URIs of pictures to be added (or re-ordered) to the recipe
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after DB was updated
     */
    private fun updateRecipePictures(userID: String, recipeID: String, pictures: List<Uri>, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        val recipeDocRef = recipesCollection.document(recipeID)
        recipeDocRef.get()
            .addOnSuccessListener { document ->
                // get the current list of picture references and prepare the new list
                val currentPictures = document.get(PICTURES) as? List<String> ?: emptyList()
                val newList = MutableList<String?>(pictures.size) { null }

                var errorOccurred = false
                var pendingUploads = pictures.size
                // if this recipe update has no pictures -> empty list of picture references
                if (pendingUploads <= 0) {
                    recipeDocRef.update(PICTURES, newList.filterNotNull())
                        .addOnSuccessListener {
                            isError(false)
                            Timber.tag("MyDB").d( "Successfully updated recipe pictures without any uploads")
                            callBack()
                        }
                        .addOnFailureListener { e ->
                            isError(true)
                            Timber.tag("MyDB").d( "Failed to update recipe pictures without uploads with error $e")
                        }
                    return@addOnSuccessListener
                }

                // if there are pictures -> loop over them
                pictures.forEachIndexed { index, uri ->

                    // if the current picture is a Firebase URL -> means the picture is already uploaded
                    val isFirebaseUrl = uri.toString().startsWith("https://firebasestorage.googleapis.com/")
                    if (isFirebaseUrl) {
                        // the picture is already uploaded but may have changed position -> re-adjust order in references list
                        if (uri.toString() in currentPictures) {
                            newList[index] = uri.toString()
                        } else {
                            errorOccurred = true
                            Timber.tag("MyDB").d( "Failed to update recipe pictures because picture $uri is not in current pictures")
                        }
                        pendingUploads--
                        if (pendingUploads <= 0) {
                            finalizePicturesUpdate(recipeDocRef, newList, { isError(it && errorOccurred) }) {
                                Timber.tag("MyDB").d( "Successfully finished updating all recipe pictures")
                                callBack()
                            }
                        }

                    // if the current picture is not a Firebase URL -> means it's a new picture to upload
                    } else {
                        uploadRecipePicture(userID, recipeID, uri, { isError(it) }) { downloadUrl ->
                            newList[index] = downloadUrl.toString()
                            pendingUploads--
                            if (pendingUploads <= 0) {
                                finalizePicturesUpdate(recipeDocRef, newList, { isError(it && errorOccurred) }) {
                                    Timber.tag("MyDB").d( "Successfully finished updating all recipe pictures")
                                    callBack()
                                }
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                isError(true)
                Timber.tag("MyDB").d( "Failed to update recipe because could not access recipe document with error $e")
            }
    }

    /**
     * Takes the new list of picture references for a recipe (old pictures removed, staying pictures re-ordered and new pictures uploaded)
     * and finalizes by placing the new list of references in the recipe document.
     *
     * @param recipeDocRef reference to the recipe document
     * @param newList new list containing exactly the new state of recipe pictures
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after DB was updated
     */
    private fun finalizePicturesUpdate(recipeDocRef: DocumentReference, newList: List<String?>, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        val cleanedList = newList.filterNotNull()
        recipeDocRef
            .update(PICTURES, cleanedList)
            .addOnSuccessListener {
                isError(false)
                Timber.tag("MyDB").d( "Successfully finalized updating recipe pictures with new references list")
                callBack()
            }
            .addOnFailureListener { e ->
                isError(true)
                Timber.tag("MyDB").d( "Failed to finalize updating recipe pictures with error $e")
            }
    }

    /**
     * Adds a user's favourite by adding their reference to the recipe.
     *
     * @param uid of the recipe
     * @param userID of the user adding the favourite
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after the DB was updated
     */
    fun addUserToFavourites(uid: String, userID: String, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        recipesCollection
            .document(uid)
            // add the user ID to the list favouriteOf of the recipe
            .update(FAVOURITE, FieldValue.arrayUnion(userID))
            .addOnSuccessListener {
                isError(false)
                callBack()
                Timber.tag("MyDB").d( "Successfully added user to favourites")
            }
            .addOnFailureListener { e ->
                isError(true)
                Timber.tag("MyDB").d( "Failed to add user to favourites with error $e")
            }
    }

    /**
     * Removes a user's favourite by removing their reference from the recipe.
     *
     * @param uid of the recipe
     * @param userID of the user removing the favourite
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after the DB was updated
     */
    fun removeUserFromFavourites(uid: String, userID: String, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        recipesCollection
            .document(uid)
            .update(FAVOURITE, FieldValue.arrayRemove(userID))
            .addOnSuccessListener {
                isError(false)
                callBack()
                Timber.tag("MyDB").d( "Successfully removed user from recipe favouritesOf")
            }
            .addOnFailureListener { e ->
                isError(true)
                Timber.tag("MyDB").d( "Failed to remove user from favourites with error $e")
            }
    }

    /**
     * Removes a user's reference in all recipes that they have as "favourite".
     *
     * @param userID ID of the user
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after the DB was updated.
     */
    private fun removeUserFromAllFavourites(userID: String, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        recipesCollection
            .whereArrayContains(FAVOURITE, userID)
            .get()
            .addOnSuccessListener { query ->
                val recipes = query.documents
                // if there are no recipes with this user in their favouritesOf -> all good
                if (recipes.isEmpty()) {
                    isError(false)
                    callBack()
                    Timber.tag("MyDB").d( "Successfully found that user had no favourites")
                    return@addOnSuccessListener
                }

                var remaining = recipes.size
                var errorOccurred = false
                // loop over recipes to remove user from their favouritesOf
                recipes.forEach { recipe ->
                    val id = recipe.id
                    removeUserFromFavourites(id, userID, {
                        if (it) {
                            errorOccurred = true
                            remaining--
                            if (remaining <= 0) {
                                isError(true)
                            }
                            Timber.tag("MyDB").d( "Failed to remove user from recipe $id")
                        }
                    }) {
                        remaining--
                        if (remaining <= 0) {
                            isError(errorOccurred)
                            if (errorOccurred) {
                                Timber.tag("MyDB").d( "Failed to remove user from some recipe")
                            } else {
                                callBack()
                                Timber.tag("MyDB").d("Successfully removed user from recipe favourites")
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                isError(true)
                Timber.tag("MyDB").d( "Failed to query all recipes with user in their favourites with error $e")
            }
    }

    /**
     * Deletes a user's recipe.
     *
     * @param userID ID of the user who created the recipe
     * @param recipeID ID of the recipe to delete
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after DB was updated
     */
    fun deleteRecipe(userID: String, recipeID: String, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        // check if there is a picture that needs to be removed
        recipesCollection.document(recipeID).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val picturesList = (document.get(PICTURES) as? List<String>)?.map { Uri.parse(it) } ?: emptyList()
                    if ( picturesList.isEmpty() ) {
                        // no pictures -> only delete recipe document
                        recipesCollection.document(recipeID).delete()
                            .addOnSuccessListener {
                                // once picture deletion is successful -> decrease creator's recipes counter
                                userDataCollection.document(userID)
                                    .update(NUMBER_RECIPES, FieldValue.increment(-1))
                                    .addOnSuccessListener {
                                        isError(false)
                                        callBack()
                                        Timber.tag("MyDB").d( "Successfully finished recipe deletion process")
                                    }
                                    .addOnFailureListener { e ->
                                        isError(true)
                                        Timber.tag("MyDB").d( "Failed to decrement recipes counter with error $e")
                                    }
                            }
                            .addOnFailureListener { e ->
                                isError(true)
                                Timber.tag("MyDB").d( "Failed to delete recipe with error $e")
                            }
                    } else {
                        // there are pictures -> delete recipe document and its pictures
                        recipesCollection
                            .document(recipeID)
                            .delete()
                            .addOnSuccessListener {
                                // if document was safely deleted -> delete pictures
                                deleteRecipePictures(userID, recipeID, { isError(it) } ) {
                                    // once picture deletion is successful -> decrease creator's recipes counter
                                    userDataCollection.document(userID)
                                        .update(NUMBER_RECIPES, FieldValue.increment(-1))
                                        .addOnSuccessListener {
                                            isError(false)
                                            callBack()
                                            Timber.tag("MyDB").d( "Successfully finished recipe deletion process")
                                        }
                                        .addOnFailureListener { e ->
                                            isError(true)
                                            Timber.tag("MyDB").d( "Failed to decrement recipes counter with error $e")
                                        }
                                }
                            }
                            .addOnFailureListener { e ->
                                isError(true)
                                Timber.tag("MyDB").d( "Failed to delete recipe with error $e")
                            }
                    }
                } else {
                    isError(true)
                    Timber.tag("MyDB").d( "Failed to delete recipe because could not fetch document")
                }
            }
    }

    /**
     * Deletes every recipe created by a user.
     * This is only meant to be used when a user deletes their account.
     *
     * @param userID ID of the user who is deleting their account
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after DB was updated
     */
    private fun deleteAllUserRecipes(userID: String, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        recipesCollection
            .whereEqualTo(OWNER, userID).get()
            .addOnSuccessListener { query ->
                var remaining = query.documents.size
                var errorOccurred = false

                // return if there are no recipes
                if (remaining <= 0) {
                    isError(false)
                    callBack()
                    return@addOnSuccessListener
                }

                // else delete all recipes
                for (document in query.documents) {
                    val id = document.id
                    deleteRecipe(userID, id, {
                        if (it) {
                            errorOccurred = true
                            Timber.tag("MyDB").d( "Failed to delete recipe $id")
                            remaining--
                            if (remaining <= 0) isError(true)
                        }
                    }) {
                        remaining--
                        if (remaining <= 0) {
                            isError(errorOccurred)
                            if (!errorOccurred) {
                                Timber.tag("MyDB").d( "Successfully deleted all user's recipes")
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                isError(true)
                Timber.tag("MyDB").d( "Failed to fetch recipes with given userID as owner, with error $e")
            }
    }


    // storage pictures

    // user profile pictures

    /**
     * Retrieves the default (empty) profile picture from the storage.
     *
     * @return Uri of the default profile picture
     */
    suspend fun getDefaultPicture(): Uri {
        return storage.child(defaultPicturePath).downloadUrl.await()
    }

    /**
     * Copies the stores default profile picture into the user's profile picture data in Storage.
     *
     * @param userID ID of the user
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after DB was updated
     */
    private fun copyDefaultPicture(userID: String, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        val defaultRef = storage.child(defaultPicturePath)
        val pictureRef = storage.child(userPicturePath(userID))

        // copy the default picture
        defaultRef
            .getBytes(Long.MAX_VALUE)
            .addOnSuccessListener { defaultData ->
                // paste the picture in the new user's storage
                pictureRef
                    .putBytes(defaultData)
                    .addOnSuccessListener {
                        // if the picture was correctly copied -> fetch its URL
                        pictureRef.downloadUrl
                            .addOnSuccessListener { uri ->
                                // add the new URI as the picture field in the new user's userData document
                                userDataCollection.document(userID).update(PICTURE, uri.toString())
                                    .addOnSuccessListener {
                                        isError(false)
                                        callBack()
                                        Timber.tag("MyDB").d( "Successfully added user profile picture")
                                    }
                                    .addOnFailureListener { e ->
                                        isError(true)
                                        Timber.tag("MyDB").d( "Failed to update user profile picture with error $e")
                                    }
                            }
                            .addOnFailureListener { e ->
                                isError(true)
                                Timber.tag("MyDB").d( "Failed to add user profile picture with error $e")
                            }
                    }
                    .addOnFailureListener { e ->
                        isError(true)
                        Timber.tag("MyDB").d( "Failed to add user profile picture with error: $e")
                    }
            }
            .addOnFailureListener { e ->
                isError(true)
                Timber.tag("MyDB").d( "Failed to copy default picture bytes with error $e")
            }
    }

    /**
     * Updates an existing picture or adds a new one if the profile picture doesn't exist yet.
     *
     * @param userID ID of the user
     * @param picture new picture to be updated in Storage
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after DB was updated
     */
    private fun updateUserPicture(userID: String, picture: Uri, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        val pictureRef = storage.child(userPicturePath(userID))
        // adds the new picture in storage (path is automatically created if it does not exist)
        pictureRef
            .putFile(picture)
            .addOnSuccessListener {
                // if the picture was correctly added -> fetch its URL
                pictureRef.downloadUrl
                    .addOnSuccessListener { uri ->
                        // add the new URI as the picture field in the userData document
                        userDataCollection.document(userID).update(PICTURE, uri.toString())
                            .addOnSuccessListener {
                                isError(false)
                                callBack()
                                Timber.tag("MyDB").d( "Successfully updated user profile picture")
                            }
                            .addOnFailureListener { e ->
                                isError(true)
                                Timber.tag("MyDB").d( "Failed to update user profile picture with error $e")
                            }
                    }
                    .addOnFailureListener { e ->
                        isError(true)
                        Timber.tag("MyDB").d( "Failed to download picture URI with error $e")
                    }
            }
            .addOnFailureListener { e ->
                val errorCode = (e as? StorageException)?.errorCode
                val httpErrorCode = (e as? StorageException)?.httpResultCode
                isError(true)
                Timber.tag("MyDB").d("Failed to update user profile picture with error \n %s errorCode is %d \nand http status is %d",
                    e.toString(), errorCode, httpErrorCode)
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
    private fun deleteUserPictures(userID: String, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        // deletes all items in the user's storage data path
        val folderRef = storage.child(userPath(userID))
        folderRef.listAll()
            .addOnSuccessListener { result ->
                // check how many items are left to be deleted before callBack
                var remaining = result.items.size
                var errorOccurred = false
                result.items.forEach { item ->
                    item.delete().addOnSuccessListener {
                        remaining--
                        if (remaining <= 0) {
                            isError(errorOccurred)
                            if (errorOccurred) {
                                Timber.tag("MyDB").d( "Failed to delete stored files")
                            } else {
                                callBack()
                                Timber.tag("MyDB").d( "Successfully deleted stored files")
                            }
                        }
                    }.addOnFailureListener { e ->
                        remaining--
                        errorOccurred = true
                        if (remaining <= 0) {
                            isError(true)
                        }
                        Timber.tag("MyDB").d( "Failed to delete stored files with error $e")
                    }
                }
            }.addOnFailureListener { e ->
                isError(true)
                Timber.tag("MyDB").d( "Failed to list stored files with error $e")
            }
    }


    // recipe pictures

    /**
     * Retrieves the data path in Storage to access a recipe's data.
     *
     * @param userID ID of the user
     * @return path in Storage
     */
    private fun recipePath(userID: String, recipeID: String) = "userData/$userID/recipePictures/$recipeID"

    /**
     * Uploads a new recipe picture.
     *
     * @param userID ID of the user who is allowed to modify this recipe's picture (its creator)
     * @param recipeID ID of the recipe to access
     * @param picture URI of the new picture
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after DB was updated
     */
    private fun uploadRecipePicture(userID: String, recipeID: String, picture: Uri, isError: (Boolean) -> Unit, callBack: (Uri) -> Unit) {
        val recipePicsFolder = storage.child(recipePath(userID, recipeID))
        val imageName = picture.toString().substringAfterLast('/')
        val pictureRef = recipePicsFolder.child(imageName)
        // upload new picture to storage
        pictureRef
            .putFile(picture)
            .addOnSuccessListener {
                // fetch picture URL
                pictureRef.downloadUrl
                    .addOnSuccessListener { uri ->
                        isError(false)
                        callBack(uri)
                        Timber.tag("MyDB").d( "Successfully uploaded recipe picture")
                    }
                    .addOnFailureListener { e ->
                        isError(true)
                        Timber.tag("MyDB").d( "Failed to download picture URI with error $e")
                    }
            }
            .addOnFailureListener { e ->
                val errorCode = (e as? StorageException)?.errorCode
                val httpErrorCode = (e as? StorageException)?.httpResultCode
                isError(true)
                Timber.tag("MyDB").d("Failed to upload user profile picture with error \n %s errorCode is %d \nand http status is %d",
                    e.toString(), errorCode, httpErrorCode)
            }
    }

    /**
     * Deletes a single picture from a recipe.
     *
     * @param downloadUrl URL that references the path to the stored image
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after DB was updated
     */
    private fun deleteSingleRecipePicture(downloadUrl: String, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        // get file reference from Firestore URL
        val fileRef = FirebaseStorage.getInstance().getReferenceFromUrl(downloadUrl)

        fileRef.delete()
            .addOnSuccessListener {
                isError(false)
                Timber.tag("MyDB").d( "Successfully deleted recipe picture from Storage")
                callBack()
            }
            .addOnFailureListener { e ->
                isError(true)
                Timber.tag("MyDB").d( "Failed to delete recipe picture from Storage with error $e")
            }
    }

    /**
     * Deletes all of a recipe's pictures from the Storage.
     *
     * @param userID ID of the recipe's owner
     * @param recipeID ID of the recipe
     * @param isError block that runs if there is an error executing the function
     * @param callBack block that runs after DB was updated
     */
    private fun deleteRecipePictures(userID: String, recipeID: String, isError: (Boolean) -> Unit, callBack: () -> Unit) {
        val folderRef = storage.child(recipePath(userID, recipeID))

        // loop over files uploaded at the recipe's Storage path
        folderRef.listAll()
            .addOnSuccessListener { result ->
                var remaining = result.items.size
                if (remaining <= 0) {
                    isError(false)
                    callBack()
                    Timber.tag("MyDB").d( "Successfully deleted stored files for recipe: no pictures to delete")
                    return@addOnSuccessListener
                }
                var errorOccurred = false
                result.items.forEach { item ->
                    item.delete()
                        .addOnSuccessListener {
                            remaining--
                            if (remaining <= 0) {
                                isError(errorOccurred)
                                if (errorOccurred) {
                                    Timber.tag("MyDB").d( "Failed to deleted stored file")
                                } else {
                                    Timber.tag("MyDB").d( "Successfully deleted stored files for recipe")
                                    callBack()
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            remaining--
                            errorOccurred = true
                            if (remaining <= 0) isError(true)
                            Timber.tag("MyDB").d( "Failed to delete stored file with error $e")
                        }
                }
            }
            .addOnFailureListener { e ->
                isError(true)
                Timber.tag("MyDB").d( "Failed to list stored files with error $e")
            }
    }


    // db tests
    /**
     * For testing purposed, adds picture in a specific path in Storage meant for testing.
     *
     * @param picture that is added in Storage for tests
     */
    @Suppress("unused")
    fun addExamplePictureToStorage(picture: Uri) {
        val ref = storage.child("tests/test.jpg")
        ref.putFile(picture)
            .addOnSuccessListener{
            }
            .addOnFailureListener{
            }
    }
}
