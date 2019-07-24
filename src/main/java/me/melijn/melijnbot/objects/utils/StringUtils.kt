package me.melijn.melijnbot.objects.utils

import java.util.*
import javax.annotation.Nullable

class StringUtils {
    fun splitMessage(message: String, nextSplitThreshold: Int = 1800, margin: Int = 0): List<String> {
        var msg = message
        val messages = ArrayList<String>()
        while (msg.length > 2000 - margin) {
            val findLastNewline = msg.substring(0, 1999 - margin)
            var index = findLastNewline.lastIndexOf("\n")
            if (index < nextSplitThreshold - margin) {
                index = findLastNewline.lastIndexOf(". ")
            }
            if (index < nextSplitThreshold - margin) {
                index = findLastNewline.lastIndexOf(" ")
            }
            if (index < nextSplitThreshold - margin) {
                index = 1999 - margin
            }
            messages.add(msg.substring(0, index))
            msg = msg.substring(index)
        }
        if (msg.isNotEmpty()) messages.add(msg)
        return messages
    }
}

/** Returns true if state is positive (yes, enable, enabled...)
 * Returns false if state is negative (no, disable, disabled...)
 * Returns null if state is neither (you can handle this with the elvis operator or a null check)
 * **/
@Nullable
fun boolFromStateArg(state: String): Boolean? {
    return when (state) {
        "disable", "no", "false", "disabled", "off" -> true
        "enable", "yes", "true", "enabled", "on" -> true
        else -> null
    }
}