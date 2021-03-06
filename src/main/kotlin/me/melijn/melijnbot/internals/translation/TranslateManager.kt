package me.melijn.melijnbot.internals.translation

import me.melijn.melijnbot.internals.command.ICommandContext
import net.dv8tion.jda.api.utils.data.DataObject
import org.slf4j.LoggerFactory
import java.util.*

val i18n = TranslateManager()
const val BASE_BUNDLE_NAME = "strings"

class TranslateManager {

    private val logger = LoggerFactory.getLogger(TranslateManager::class.java)

    private val defaultResourceBundle: ResourceBundle = ResourceBundle.getBundle(BASE_BUNDLE_NAME)

    suspend fun getTranslation(context: ICommandContext, path: String): String {
        return getTranslation(context.getLanguage(), path)
    }

    fun getTranslation(language: String, path: String, logMissing: Boolean = true): String {
        val bundle = when (language.uppercase()) {
            DEFAULT_LANGUAGE -> defaultResourceBundle
            else -> defaultResourceBundle
        }
        return if (bundle.containsKey(path)) {
            bundle.getString(path)
        } else {
            if (logMissing) {
                logger.warn("missing translation: $path")
            }
            path
        }
    }

    fun getTranslationN(language: String, path: String, logMissing: Boolean = true): String? {
        val bundle = when (language.uppercase()) {
            DEFAULT_LANGUAGE -> defaultResourceBundle
            else -> defaultResourceBundle
        }
        return if (bundle.containsKey(path)) {
            bundle.getString(path)
        } else {
            if (logMissing) {
                logger.warn("missing translation: $path")
            }
            null
        }
    }

    fun getTranslations(lang: String): DataObject {
        val map = DataObject.empty()
        defaultResourceBundle.keySet().forEach { key ->
            map.put(key, getTranslation(lang, key))
        }
        return map
    }
}
