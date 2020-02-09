package me.melijn.melijnbot.objects.utils

import java.util.*
import javax.annotation.Nullable
import kotlin.math.pow


object StringUtils {
    private val backTicks = "```".toRegex()
    fun splitMessageWithCodeBlocks(message: String, nextSplitThreshold: Int = 1800, margin: Int = 30, lang: String = ""): List<String> {
        var msg = message
        val messages = ArrayList<String>()
        var shouldAppendBackTicks = false
        var shouldPrependBackTicks = false
        while (msg.length > 2000 - margin) {
            var findLastNewline = msg.substring(0, 1999 - margin)
            if (shouldPrependBackTicks) {
                findLastNewline = "```$lang\n$findLastNewline"
                shouldPrependBackTicks = false
            }

            if (findLastNewline.contains("```")) {

                val triple = getBackTickAmountAndLastTwoIndexes(findLastNewline)

                val amount = triple.first
                val previousIndex = triple.second
                val mostRightIndex = triple.third


                val lastEvenIndex = if (amount % 2 == 0) {
                    mostRightIndex
                } else {
                    previousIndex
                }

                if (lastEvenIndex > (nextSplitThreshold - margin)) {
                    val subMsg = msg.substring(0, lastEvenIndex)
                    messages.add(subMsg)
                    msg = msg.substring(lastEvenIndex)
                    continue
                } else {
                    shouldAppendBackTicks = true
                    shouldPrependBackTicks = true
                }
            }
            val index = getSplitIndex(findLastNewline, nextSplitThreshold, margin)
            messages.add(msg.substring(0, index) +
                if (shouldAppendBackTicks) {
                    "```"
                } else {
                    ""
                }
            )

            msg = msg.substring(index)
        }

        if (shouldAppendBackTicks) {
            msg += "```"
        }
        if (shouldPrependBackTicks) {
            msg = "```$lang\n$msg"
        }

        if (msg.isNotEmpty()) messages.add(msg)
        return messages
    }

    fun humanReadableByteCountBin(bytes: Int): String = humanReadableByteCountBin(bytes.toLong())
    fun humanReadableByteCountBin(bytes: Long): String {
        return when {
            bytes < 1024L -> "$bytes B"
            bytes < 0xfffccccccccccccL shr 40 -> String.format("%.3f KiB", bytes / 2.0.pow(10.0))
            bytes < 0xfffccccccccccccL shr 30 -> String.format("%.3f MiB", bytes / 2.0.pow(20.0))
            bytes < 0xfffccccccccccccL shr 20 -> String.format("%.3f GiB", bytes / 2.0.pow(30.0))
            bytes < 0xfffccccccccccccL shr 10 -> String.format("%.3f TiB", bytes / 2.0.pow(40.0))
            bytes < 0xfffccccccccccccL -> String.format("%.3f PiB", (bytes shr 10) / 2.0.pow(40.0))
            else -> String.format("%.3f EiB", (bytes shr 20) / 2.0.pow(40.0))
        }
    }

    private fun getBackTickAmountAndLastTwoIndexes(findLastNewline: String): Triple<Int, Int, Int> {
        var amount = 0
        var almostMostRight = 0
        var mostRight = 0
        for (result in backTicks.findAll(findLastNewline)) {
            amount++
            almostMostRight = mostRight
            mostRight = result.range.last + 1
        }
        return Triple(amount, almostMostRight, mostRight)
    }

    fun splitMessage(message: String, splitAtLeast: Int = 1800, maxLength: Int = 2000): List<String> {
        var msg = message
        val messages = ArrayList<String>()
        while (msg.length > maxLength) {
            val findLastNewline = msg.substring(0, maxLength - 1)
            val index = getSplitIndex(findLastNewline, splitAtLeast, maxLength - 1)

            messages.add(msg.substring(0, index))
            msg = msg.substring(index)
        }
        if (msg.isNotEmpty()) messages.add(msg)
        return messages
    }

    fun getSplitIndex(findLastNewline: String, splitAtLeast: Int, margin: Int): Int {
        var index = findLastNewline.lastIndexOf("\n")
        if (index < splitAtLeast) {
            index = findLastNewline.lastIndexOf(". ")
        }
        if (index < splitAtLeast) {
            index = findLastNewline.lastIndexOf(" ")
        }
        if (index < splitAtLeast) {
            index = findLastNewline.lastIndexOf(",")
        }
        if (index < splitAtLeast) {
            index = 1999 - margin
        }

        return index
    }
}

/** Returns true if state is positive (yes, enable, enabled...)
 * Returns false if state is negative (no, disable, disabled...)
 * Returns null if state is neither (you can handle this with the elvis operator or a null check)
 * **/
@Nullable
fun boolFromStateArg(state: String): Boolean? {
    return when (state) {
        "disable", "no", "false", "disabled", "off" -> false
        "enable", "yes", "true", "enabled", "on" -> true
        else -> null
    }
}

fun String.remove(vararg strings: String): String {
    var newString = this
    for (string in strings) {
        newString = newString.replace(string, "")
    }
    return newString
}

fun String.removeFirst(vararg strings: String): String {
    var newString = this
    for (string in strings) {
        newString = newString.replaceFirst(string, "")
    }
    return newString
}

fun String.removeFirst(vararg regexes: Regex): String {
    var newString = this
    for (regex in regexes) {
        newString = newString.replaceFirst(regex, "")
    }
    return newString
}

fun String.splitIETEL(delimiter: String): List<String> {
    val res = this.split(delimiter)
    return if (res.first().isEmpty() && res.size == 1) {
        emptyList()
    } else {
        res
    }
}