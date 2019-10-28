package me.melijn.melijnbot.objects.translation

import me.melijn.melijnbot.objects.command.CommandContext
import java.util.*

val i18n = TranslateManager()
const val BASE_BUNDLE_NAME = "strings"

class TranslateManager {

    private val defaultRecourseBundle: ResourceBundle = ResourceBundle.getBundle(BASE_BUNDLE_NAME)
    //val dutchBelgianRecourseBundle: ResourceBundle = ResourceBundle.getBundle(BASE_BUNDLE_NAME, Locale("nl_BE"))

    suspend fun getTranslation(context: CommandContext, path: String): String {
        return getTranslation(context.getLanguage(), path)
    }

    fun getTranslation(language: String, path: String): String {
        val bundle = when (language.toUpperCase()) {
            DEFAULT_LANGUAGE -> defaultRecourseBundle
            else -> defaultRecourseBundle
        }
        return if (bundle.containsKey(path)) {
            bundle.getString(path)
        } else {
            path
        }
    }
}
