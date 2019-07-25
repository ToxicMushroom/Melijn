package me.melijn.melijnbot.objects.utils

import java.util.*
import java.util.concurrent.TimeUnit
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

fun getDurationString(milliseconds: Long): String {
    return getDurationString(java.lang.Double.valueOf(milliseconds.toDouble()))
}

fun getDurationString(milliseconds: Double): String {
    if (milliseconds < 0.0) {
        return "error"
    }

    var millis = milliseconds.toLong()
    val days = TimeUnit.MILLISECONDS.toDays(millis)
    millis -= TimeUnit.DAYS.toMillis(days)
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    millis -= TimeUnit.HOURS.toMillis(hours)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    millis -= TimeUnit.MINUTES.toMillis(minutes)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis)

    val sb = StringBuilder(64)
    if (days != 0L) {
        sb.append(days)
        sb.append("d ")
    }
    appendTimePart(hours, sb)
    appendTimePart(minutes, sb)
    if (seconds < 10) sb.append(0)
    sb.append(seconds)
    sb.append("s")

    return sb.toString()
}

private fun appendTimePart(hours: Long, sb: StringBuilder) {
    if (hours != 0L) {
        if (hours < 10) sb.append(0)
        sb.append(hours)
        sb.append(":")
    }
}