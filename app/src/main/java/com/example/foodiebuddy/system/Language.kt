package com.example.foodiebuddy.system

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList

/**
 * Fetches the language set for the phone's system.
 *
 * @param context to access system service
 * @return the tag of the system's language, or fr (French tag) if the language could not be fetched
 */
fun getCurrentLocale(context: Context) : String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val localeList = context.getSystemService(LocaleManager::class.java).applicationLocales
        if (!localeList.isEmpty) {
            localeList[0].toLanguageTag()
        } else {
            SUPPORTED_LANGUAGES_TAGS[0]
        }
    } else {
        SUPPORTED_LANGUAGES_TAGS[0]
    }
}

/**
 * Sets the app's language (for now French and English are supported).
 *
 * @param context to access system service
 * @param localeTag defines which language should be set
 */
fun setLanguage(context: Context, localeTag: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.getSystemService(LocaleManager::class.java).applicationLocales =
            LocaleList.forLanguageTags(localeTag)
    }
}

/**
 * Fetches the tags of the currently supported languages.
 *
 * @return a list of tags (fr, en)
 */
fun getSupportedLanguages(): List<String> {
    return SUPPORTED_LANGUAGES_TAGS
}

/**
 * Converts a supported language tag to its actual language name.
 *
 * @param tag language tag to be converted
 * @return full language name
 */
fun convertTagToName(tag: String): String {
    val index = SUPPORTED_LANGUAGES_TAGS.indexOf(tag)
    return SUPPORTED_LANGUAGES_NAMES[index]
}

/**
 * Converts a supported language name to its tag.
 *
 * @param name language name to be converted
 * @return language tag
 */
fun convertNameToTag(name: String): String {
    val index = SUPPORTED_LANGUAGES_NAMES.indexOf(name)
    return SUPPORTED_LANGUAGES_TAGS[index]
}

private val SUPPORTED_LANGUAGES_TAGS = listOf("fr", "en")
private val SUPPORTED_LANGUAGES_NAMES = listOf("Français", "English")