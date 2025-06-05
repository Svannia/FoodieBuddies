package com.example.foodiebuddy.data

import android.content.Context
import com.example.foodiebuddy.R
import java.text.DecimalFormat
import java.util.UUID

/**
 * Describes an ingredient that is owned by a user AKA it belongs to their fridge or groceries list.
 *
 * @property uid of the ingredient
 * @property displayedName the name display to the user
 * @property standName standardized name that will be used to compare against recipe ingredients
 * @property category which category the ingredient belongs to
 * @property isTicked whether or not the ingredient is ticked. For ingredients in the fridge, this property does not matter.
 */
data class OwnedIngredient(
    val uid: String,
    var displayedName: String,
    var standName: String,
    var category: String,
    var isTicked: Boolean
) {
    companion object {
        /**
         * Creates an empty Owned Ingredient object.
         *
         * @return empty Owned Ingredient object.
         */
        fun empty(): OwnedIngredient {
            return OwnedIngredient("", "", "", "", false)
        }
    }
    /**
     * Checks if this OwnedIngredient data object is empty.
     *
     * @return true if the OwnedIngredient data object is empty.
     */
    fun isEmpty(): Boolean {
        return this == empty()
    }
}

/**
 * Describes an ingredient that is used in a recipe.
 *
 * @property displayedName the name display to the user
 * @property standName standardized name that will be used to compare against recipe ingredients
 * @property quantity real number for the ingredient's required quantity (can be zero/null)
 * @property unit Measure object for the quantity's unit of measure
 * @property id unique identifier to avoid irregularities when editing ingredients list
 */
data class RecipeIngredient(
    var displayedName: String,
    var standName: String,
    var quantity: Float,
    var unit: Measure,
    val id: String = UUID.randomUUID().toString()
) {
    companion object {
        /**
         * Creates an empty Recipe Ingredient object.
         *
         * @return empty Recipe Ingredient object.
         */
        fun empty(): RecipeIngredient {
            return RecipeIngredient("", "", 0f, Measure.NONE)
        }
    }
    /**
     * Checks if this RecipeIngredient data object is empty.
     *
     * @return true if the RecipeIngredient data object is empty.
     */
    fun isEmpty(): Boolean {
        return this == empty()
    }

    /**
     * Converts a RecipeIngredient into an OwnedIngredient
     *
     * @param category the owned ingredient category this ingredient should be transferred to
     * @return OwnedIngredient object
     */
    fun toOwned(category: String): OwnedIngredient {
        return OwnedIngredient("", this.displayedName, this.standName, category, false)
    }
}

// The units of measure for ingredients.
enum class Measure {
    NONE, G, KG, ML, DL, L, CS, CC, PIN, TASSE, SACHET, DE, BOUQUET, GOUTTE
}
val measuresMap = mapOf(
    Measure.NONE to R.string.unit_none,
    Measure.G to R.string.unit_g,
    Measure.KG to R.string.unit_kg,
    Measure.ML to R.string.unit_ml,
    Measure.DL to R.string.unit_dl,
    Measure.L to R.string.unit_l,
    Measure.CS to R.string.unit_cs,
    Measure.CC to R.string.unit_cc,
    Measure.PIN to R.string.unit_pin,
    Measure.TASSE to R.string.unit_tasse,
    Measure.SACHET to R.string.unit_sachet,
    Measure.DE to R.string.unit_de,
    Measure.BOUQUET to R.string.unit_bouquet,
    Measure.GOUTTE to R.string.unit_goutte
)

/**
 * Translates a Measure Unit element into its corresponding string from strings.xml.
 *
 * @param context used to access the string resources
 * @return user-readable string
 */
fun Measure.getString(context: Context): String {
    return context.getString(measuresMap[this] ?: R.string.unit_none)
}

/**
 * Creates the plural version of a measure unit.
 *
 * @param context used to access the string resources
 * @return user-readable string in plural form
 */
fun Measure.plural(context: Context): String {
    return when (this) {
        Measure.PIN, Measure.TASSE, Measure.SACHET, Measure.DE, Measure.BOUQUET, Measure.GOUTTE -> {
            val singular = context.getString(measuresMap[this] ?: R.string.unit_none)
            if (singular.endsWith("ch")) singular + "es"
            else singular + "s"
        }
        else -> context.getString(measuresMap[this] ?: R.string.unit_none)
    }
}

/**
 * Formats the ingredient quantity to remove useless decimal points and change frequent decimal quantities into fractions.
 *
 * @param quantity quantity to format as a float
 * @return formatted string
 */
fun formatQuantity(quantity: Float): String {
    return when {
        quantity == 0f -> ""
        quantity % 1 == 0f -> quantity.toInt().toString()
        quantity % 1 == 0.5f -> if (quantity.toInt() == 0) "½" else "${quantity.toInt()}½"
        quantity % 1 == 0.25f -> if (quantity.toInt() == 0) "¼" else "${quantity.toInt()}¼"
        quantity % 1 == 0.75f -> if (quantity.toInt() == 0) "¾" else "${quantity.toInt()}¾"
        else -> {
            val decimalFormat = DecimalFormat("#.##")
            decimalFormat.format(quantity)
        }
    }
}

/**
 * Formats the ingredient unit of measure to remove NONE units and add plural form when necessary.
 *
 * @param unit Measure objects to format
 * @param quantity quantity to check for plural form
 * @return formatted string
 */
fun formatUnit(unit: Measure, quantity: Float, context: Context): String {
    return if (unit == Measure.NONE) "" else {
        if (quantity > 1f) unit.plural(context)
        else unit.getString(context)
    }
}
