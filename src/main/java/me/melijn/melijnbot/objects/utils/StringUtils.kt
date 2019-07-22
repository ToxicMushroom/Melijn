package me.melijn.melijnbot.objects.utils

import java.util.*

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