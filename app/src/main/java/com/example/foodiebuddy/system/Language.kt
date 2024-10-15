package com.example.foodiebuddy.system

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList

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
fun setLanguage(context: Context, localeTag: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.getSystemService(LocaleManager::class.java).applicationLocales =
            LocaleList.forLanguageTags(localeTag)
    }
}

fun getSupportedLanguages(): List<String> {
    return SUPPORTED_LANGUAGES_TAGS
}

fun convertTagToName(tag: String): String {
    val index = SUPPORTED_LANGUAGES_TAGS.indexOf(tag)
    return SUPPORTED_LANGUAGES_NAMES[index]
}
fun convertNameToTag(name: String): String {
    val index = SUPPORTED_LANGUAGES_NAMES.indexOf(name)
    return SUPPORTED_LANGUAGES_TAGS[index]
}

private val SUPPORTED_LANGUAGES_TAGS = listOf("fr", "en")
private val SUPPORTED_LANGUAGES_NAMES = listOf("Français", "English")