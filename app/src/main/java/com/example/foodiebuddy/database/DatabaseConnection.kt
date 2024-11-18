package com.example.foodiebuddy.database

import android.net.Uri
import android.util.Log
import com.example.foodiebuddy.data.OwnedIngredient
import com.example.foodiebuddy.data.Recipe
import com.example.foodiebuddy.data.User
import com.example.foodiebuddy.data.UserPersonal
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.tasks.await

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
     * Creates a new user profile given all the input information, and its corresponding userPersonal document.
     * The userID and username are mandatory, bio can be empty and the default profile picture is used if no picture was input.
     *
     * @param userID ID of the user to create
     * @param username of the new user
     * @param picture Uri of the new user's picture or default picture
     * @param bio of the new user
     * @param isError block that runs if there is an error executing the function
     */
    suspend fun createUser(userID: String, username: String, picture: Uri, bio: String, isError: (Boolean) -> Unit) {
        // process the input data to create a document
        val formattedBio = bio.replace("\n", "\\n")
        val user = hashMapOf(USERNAME to username, BIO to formattedBio, NUMBER_RECIPES to 0, PICTURE to picture.toString())
        // create the new userData document
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
        // if the user input their own profile picture -> add it to the storage (the new path is automatically created)
        if (picture != getDefaultPicture()) {
            updateUserPicture(userID, picture, { isError(it) }) {}
        // else -> copy the default profile picture in the new user's storage path
        } else {
            copyDefaultPicture(userID) { isError(it) }
        }
        // create a document for the new user's personal data
        createPersonal(userID, isError)
    }

    /**
     * Fetches all of a user's profile data.
     *
     * @param userID ID of the user whose data to retrieve
     * @return User data object with all profile data
     */
    suspend fun fetchUserData(userID: String): User {
        if (userID.isEmpty()) { return User.empty() }

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
        // only user and bio text fields can be modified by the user
        val formattedBio = bio.replace("\n", "\\n")
        val task = hashMapOf(USERNAME to username, BIO to formattedBio)
        // update those modifications to the document
        userDataCollection
            .document(userID)
            .update(task as Map<String, Any>)
            .addOnSuccessListener {
                // if the modification was successful -> check if picture also needs to be updated
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
        // delete the userData document of the user to be deleted
        userDataCollection
            .document(userID)
            .delete()
            .addOnSuccessListener {
                // if document deletion was successful -> also delete the profile picture
                deleteProfilePicture(userID, { isError(it) }) {
                    deleteUserPersonal(userID, {isError(it)}) { callBack() }
                }
                isError(false)
                Log.d("DB", "Successfully deleted user $userID")
            }
            .addOnFailureListener { e ->
                isError(true)
                Log.d("DB", "Failed to delete user $userID with error $e")
            }
    }


    // user personal

    /**
     * Creates a new userPersonal. This function is only called when a new user is created.
     *
     * @param userID ID of the user
     * @param isError block that runs if there is an error executing the function
     */
    private fun createPersonal(userID: String, isError: (Boolean) -> Unit) {
        // the initial document only contains empty lists or maps
        val user = hashMapOf(FAV_RECIPES to emptyList<String>(), GROCERIES to emptyMap<String, List<String>>(), FRIDGE to emptyMap<String, List<String>>())
        // add the new document to userPersonal, setting its reference to be the user UID
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

    /**
     * Fetches all of a user's personal data.
     *
     * @param userID ID of the user
     * @return UserPersonal object with all personal data
     */
    suspend fun fetchUserPersonal(userID: String): UserPersonal {
        // check that a correct userID was given
        if (userID.isEmpty()) {
            Log.d("DB", "userID is null")
            return UserPersonal.empty()
        }

        // fetches the userPersonal document
        val document = userPersonalCollection.document(userID).get().await()
        return if (document.exists()) {
            // TODO: fetch list of favourite recipes
           /* val favouriteRecipesRefs = document.get(FAV_RECIPES) as? List<DocumentReference> ?: emptyList()
            val favouriteRecipes = favouriteRecipesRefs.mapNotNull { ref ->
                ref.get().await().toObject(Recipe::class.java)
            }*/
            // fetch each ingredient from the groceries map
            val groceryListRefs = document.get(GROCERIES) as? Map<String, List<DocumentReference>> ?: emptyMap()
            val groceryList = groceryListRefs.mapValues { entry ->
                entry.value.map { ref ->
                    fetchIngredient(ref)
                }
            }
            // fetch each ingredient from the fridge map
            val fridgeListRefs = document.get(FRIDGE) as? Map<String, List<DocumentReference>> ?: emptyMap()
            val fridgeList = fridgeListRefs.mapValues { entry ->
                entry.value.map { ref ->
                    fetchIngredient(ref)
                }
            }
            // Create and return the UserPersonal object
            Log.d("DB", "Successfully fetched user personal")
            UserPersonal(userID, emptyList(), groceryList, fridgeList)
        } else {
            Log.d("DB", "Failed to fetch user personal for userID $userID")
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
                        Log.d("DB", "Successfully deleted ingredients for user $userID")
                        // delete the userPersonal document of the user to be deleted
                        userPersonalCollection
                            .document(userID)
                            .delete()
                            .addOnSuccessListener {
                                callBack()
                                isError(false)
                                Log.d("DB", "Successfully deleted user personal for $userID")
                            }
                            .addOnFailureListener { e ->
                                isError(true)
                                Log.d("DB", "Failed to delete user personal for $userID with error $e")
                            }
                    }
                    .addOnFailureListener { e ->
                        isError(true)
                        Log.d("DB", "Failed to delete ingredients for user $userID with error $e")
                    }
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
     suspend fun addCategory(owner: String, category: String, ingredients: List<OwnedIngredient>, isInFridge: Boolean, isError: (Boolean) -> Unit, callBack: () -> Unit ) {
         // if the ingredients list is empty ->
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
                    Log.d("DB", "Successfully added the empty category $category")
                }
                .addOnFailureListener { e ->
                    isError(true)
                    Log.d("DB", "Failed to add empty category $category with error $e")
                }
         // if there are new ingredients to be added ->
        } else {
            // check how many ingredients are left to update before callBack
            var remaining = ingredients.size
            for (ingredient in ingredients) {
                // add new ingredient
                createIngredient(owner, ingredient, isInFridge, { isError(it) }) {
                    remaining--
                    if (remaining <= 0) {
                        callBack()
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
                                Log.d("DB", "Successfully updated category of ingredient $ref")
                            }
                            .addOnFailureListener { e ->
                                isError(true)
                                Log.d("DB", "Failed to udpate category of ingredient $ref with error $e")
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
                        Log.d("DB", "Successfully updated category in groceryList and fridge")
                    }.addOnFailureListener { e ->
                        isError(true)
                        Log.d("DB", "Failed to update category with error: $e")
                    }
                }
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
                        ingredientsToDelete.forEach { ref ->
                            ref.delete()
                                .addOnSuccessListener {
                                    remaining--
                                    if (remaining <= 0) {
                                        // if all ingredients have been deleted ->
                                        userPersonalCollection
                                            .document(owner)
                                            // delete the category entry from groceries and fridge maps
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

    /**
     * Checks if some user owns an ingredient with given name in given category, in their groceries list.
     *
     * @param userID ID of the user
     * @param category category the ingredient should be in
     * @param ingredient displayed name of the ingredient looked for
     * @param onSuccess block that runs if the check succeeds (whether or not the ingredient exists)
     * @param onFailure block that runs if there is an error executing the function
     */
    fun ingredientExistsInCategory(userID: String, category: String, ingredientName: String, onSuccess: (Boolean) -> Unit, onFailure: (Exception) -> Unit) {
        userPersonalCollection.document(userID).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val groceriesMap = document.get(GROCERIES) as? Map<*, *>
                    val ingredientRefs = groceriesMap?.get(category) as? List<DocumentReference>

                    if (ingredientRefs != null) {
                        var remaining = ingredientRefs.size
                        Log.d("Debug", "starting to check for size ${ingredientRefs.size}")
                        if (ingredientRefs.isEmpty()) {
                            Log.d("DB", "Successfully found that ingredient does not exist")
                            onSuccess(false)
                            return@addOnSuccessListener
                        }
                        ingredientRefs.forEach { ref ->
                            ref.get()
                                .addOnSuccessListener { ingredient ->
                                    val displayName = ingredient.getString(DISPLAY_NAME)
                                    if (displayName == ingredientName) {
                                        Log.d("DB", "Successfully found that ingredient exists")
                                        onSuccess(true)
                                        return@addOnSuccessListener
                                    }
                                    Log.d("Debug", "checking $displayName")
                                    remaining--
                                    if (remaining <= 0) {
                                        Log.d("DB", "Successfully found that ingredient does not exist")
                                        onSuccess(false)
                                    }
                                }
                                .addOnFailureListener { e ->
                                    remaining--
                                    Log.d("DB", "Failed to check ingredient existence because fetching ingredient ref failed with error $e")
                                    onFailure(e)
                                }
                        }
                    } else {
                        onSuccess(false)
                        Log.d("DB", "Failed to check ingredient existence because ingredient references are null")
                    }
                } else {
                    onSuccess(false)
                    Log.d("DB", "Failed to check ingredient existence because userPersonal document is null or does not exist")
                }
            }
            .addOnFailureListener { e ->
                Log.d("DB", "Failed to check ingredient existence because could not access userPersonal with error $e")
                onFailure(e)
            }
    }

    /**
     * Creates a new ingredient document and adds the necessary references.
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
                Log.d("DB", "Successfully created user ingredient")
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

    /**
     * Fetches all ingredient data.
     *
     * @param ref DocumentReference (contained in groceries and fridge maps of userPersonal)
     * @return OwnedIngredient object with all ingredient data
     */
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
                Log.d("DB", "Successfully updated ingredient")
            }
            .addOnFailureListener { e ->
                isError(true)
                Log.d("DB", "Failed to update ingredient with error $e")
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
                Log.d("DB", "Successfully deleted ingredient ref from user $owner")

                // if the reference was correctly removed -> delete the ingredient document
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

                    // if the map is already no empty -> no ingredients to clear
                    if (targetMap.isEmpty()) {
                        isError(false)
                        Log.d("Debug", "callback on empty map")
                        callBack()
                        Log.d("DB", "Map $targetField is already empty")
                    }

                    // if the map is not empty -> clear it
                    else {
                        var remainingCat = targetMap.size
                        targetMap.forEach { (category, refs) ->
                            var remainingRefs = refs.size

                            // if this category does not contain any reference -> just increase counter
                            if (refs.isEmpty()) {
                                remainingCat--
                                if (remainingCat <= 0) {
                                    isError(false)
                                    callBack()
                                    Log.d("DB", "Successfully cleared all ingredients for $targetField")
                                }
                            } else {
                                refs.forEach { ref ->
                                    ref.delete()
                                        .addOnSuccessListener {
                                            remainingRefs--
                                            if (remainingRefs <= 0) {
                                                // once all references items have been deleted -> empty the references list
                                                userPersonalCollection.document(owner)
                                                    .update("$targetField.$category", emptyList<DocumentReference>())
                                                    .addOnSuccessListener {
                                                        remainingCat--
                                                        if (remainingCat <= 0) {
                                                            isError(false)
                                                            callBack()
                                                            Log.d("Debug", "callback on success")
                                                            Log.d("DB", "Successfully cleared all ingredients for $targetField")
                                                        }
                                                    }
                                                    .addOnFailureListener { e ->
                                                        isError(true)
                                                        Log.d(
                                                            "DB",
                                                            "Failed to delete references in category $category with error $e"
                                                        )
                                                    }
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            isError(true)
                                            Log.d(
                                                "DB",
                                                "Failed to delete ingredient $ref with error $e"
                                            )
                                        }
                                }
                            }


                        }
                    }

                } else {
                    isError(true)
                    Log.d("DB", "Failed to clear ingredients for user $owner: document does not exist")
                }
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
     * Copies the stores default profile picture into the user's profile picture data in Storage.
     *
     * @param userID ID of the user
     * @param isError block that runs if there is an error executing the function
     */
    private fun copyDefaultPicture(userID: String, isError: (Boolean) -> Unit) {
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
                        pictureRef.downloadUrl.addOnSuccessListener { uri ->
                            // add the new URI as the picture field in the new user's userData document
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
        // adds the new picture in storage (path is automatically created if it does not exist)
        pictureRef
            .putFile(picture)
            .addOnSuccessListener {
                // if the picture was correctly added -> fetch its URL
                pictureRef.downloadUrl.addOnSuccessListener { uri ->
                    // add the new URI as the picture field in the userData document
                    userDataCollection.document(userID).update(PICTURE, uri.toString())
                        isError(false)
                        callBack()
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
        // deletes all items in the user's storage data path
        val folderRef = storage.child(userPath(userID))
        folderRef.listAll().addOnSuccessListener { result ->
            // check how many items are left to be deleted before callBack
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
