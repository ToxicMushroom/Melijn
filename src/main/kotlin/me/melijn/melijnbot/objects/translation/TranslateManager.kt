package me.melijn.melijnbot.objects.translation

import me.melijn.melijnbot.objects.command.CommandContext
import net.dv8tion.jda.api.utils.data.DataObject
import org.slf4j.LoggerFactory
import java.util.*

val i18n = TranslateManager()
const val BASE_BUNDLE_NAME = "strings"

class TranslateManager {

    private val logger = LoggerFactory.getLogger(TranslateManager::class.java)

    private val defaultResourceBundle: ResourceBundle = ResourceBundle.getBundle(BASE_BUNDLE_NAME)

    suspend fun getTranslation(context: CommandContext, path: String): String {
        return getTranslation(context.getLanguage(), path)
    }

    fun getTranslation(language: String, path: String): String {
        val bundle = when (language.toUpperCase()) {
            DEFAULT_LANGUAGE -> defaultResourceBundle
            else -> defaultResourceBundle
        }
        return if (bundle.containsKey(path)) {
            bundle.getString(path)
        } else {
            if (pathPattern.matches(path)) {
                logger.warn("missing string: $path")
            }
            path
        }
    }

    fun getTranslations(lang: String): DataObject {
        val map = DataObject.empty()
        defaultResourceBundle.keySet().forEach { key ->
            map.put(key, getTranslation(lang, key))
        }
        return map
    }

    companion object {
        val pathPattern = "^(?:[a-z0-9]+\\.)+[a-z0-9]+$".toRegex(RegexOption.IGNORE_CASE)
    }
}
