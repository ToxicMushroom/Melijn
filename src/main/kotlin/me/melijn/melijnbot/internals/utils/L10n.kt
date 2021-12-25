package me.melijn.melijnbot.internals.utils
import org.jetbrains.annotations.PropertyKey
import java.text.FieldPosition
import java.text.MessageFormat
import java.util.*

internal const val BUNDLE = "strings"

fun translate(locale: Locale, @PropertyKey(resourceBundle = BUNDLE) key: String): String =
    ResourceBundle.getBundle(BUNDLE, locale).getString(key)

fun translate(locale: Locale, @PropertyKey(resourceBundle = BUNDLE) key: String, params: Array<out Any?>): String =
    MessageFormat(translate(locale, key), locale).format(params, StringBuffer(), FieldPosition(0)).toString()