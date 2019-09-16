package me.melijn.melijnbot.objects.translation

import java.util.*

val i18n = TranslateManager()
const val BASE_BUNDLE_NAME = "strings"

class TranslateManager {

    private val defaultRecourseBundle: ResourceBundle = ResourceBundle.getBundle(BASE_BUNDLE_NAME)
    //val dutchBelgianRecourseBundle: ResourceBundle = ResourceBundle.getBundle(BASE_BUNDLE_NAME, Locale("nl_BE"))

    fun getTranslation(language: String, path: String = ""): String {
        val bundle = when (language.toUpperCase()) {
            DEFAULT_LANGUAGE -> defaultRecourseBundle
            else -> defaultRecourseBundle
        }
        return bundle.getString(path)
    }
}
