package me.melijn.melijnbot.objects.utils

import me.melijn.melijnbot.objects.command.CommandContext
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
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

/** interprets the long as millis duration string **/
fun Long.getAsDurationString(): String {
    return getDurationString(this)
}

fun Long.asEpochMillisToDateTime(): String {
    val calendar = Calendar.getInstance()
    calendar.timeZone = TimeZone.getTimeZone(ZoneId.of("GMT"))
    calendar.timeInMillis = this
    val offsetDateTime = Instant.ofEpochMilli(this).atOffset(ZoneOffset.UTC)
    return offsetDateTime.asLongLongGMTString()
}

fun OffsetDateTime.asLongLongGMTString(): String {
    return this.format(DateTimeFormatter.ofPattern("yyyy-MM-dd kk:mm:ss O"))
}

fun getDurationString(milliseconds: Long): String {
    return getDurationString(milliseconds.toDouble())
}

const val HUNDRED_YEARS_MILLIS = 3_153_600_000_000
fun getDurationString(milliseconds: Double): String {
    if (milliseconds >= Double.MAX_VALUE || milliseconds >= Double.POSITIVE_INFINITY || milliseconds >= HUNDRED_YEARS_MILLIS) {
        return "infinite"
    }

    if (milliseconds < 0.0) {
        return "infinite"
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
    appendTimePart(minutes, sb, true, canAddZeros = true)
    appendTimePart(seconds, sb, false, canAddZeros = true)

    return sb.toString()
}

suspend fun getDurationByArgsNMessage(context: CommandContext, timeStamps: List<String>, leftBound: Int, rightBound: Int): Long? {
    val corruptTimeStamps = timeStamps.subList(leftBound, rightBound).toMutableList()
    val holyTimeStamps = mutableListOf<String>()
    var totalTime = 0L
    val holyPattern = Pattern.compile("(\\d+)([a-zA-Z]+)")

    //merge numbed with their right neighbour so the number type is present along with the number itself
    for ((index, corruptTimeStamp) in corruptTimeStamps.withIndex()) {
        if (corruptTimeStamp.matches("\\d+".toRegex())) {
            if (corruptTimeStamps.size >= index + 1) continue
            val corruptTimeType = corruptTimeStamps[index + 1]
            if (!corruptTimeType.matches("[a-zA-Z]+".toRegex())) continue

            holyTimeStamps.add(corruptTimeStamp + corruptTimeType)
        } else if (holyPattern.matcher(corruptTimeStamp).matches()) {
            holyTimeStamps.add(corruptTimeStamp)
        }
    }

    if (holyTimeStamps.isEmpty()) {
        val msg = context.getTranslation("unknown.time")
            .replace("%args%", timeStamps.joinToString(" "))

        sendMsg(context, msg)
        return null
    }

    //CorruptTimeStamps aren't corrupt anymore
    for (corruptTimeStamp in holyTimeStamps) {
        val matcher = holyPattern.matcher(corruptTimeStamp)
        require(matcher.find()) { "should always find a match" }

        val amount = matcher.group(1).toLongOrNull()
        if (amount == null) {
            val msg = context.getTranslation("message.numbertobig")
                .replace("%args%", matcher.group(1))

            sendMsg(context, msg, null)
            return null
        }

        val typeNorm = matcher.group(2)
        val type = typeNorm.toLowerCase()
        val multiplier = when {
            arrayOf("s", "second", "seconds").contains(type) -> 1
            arrayOf("m", "minute", "minutes").contains(type) -> 60
            arrayOf("h", "hour", "hours").contains(type) -> 60 * 60
            arrayOf("d", "day", "days").contains(type) -> 24 * 60 * 60
            arrayOf("w", "week", "weeks").contains(type) -> 7 * 24 * 60 * 60
            "M" == type || arrayOf("month", "months").contains(type) -> 30 * 24 * 60 * 60
            arrayOf("y", "year", "years").contains(type) -> 52 * 7 * 24 * 60 * 60
            else -> null
        }

        if (multiplier == null) {
            val msg = context.getTranslation("unknown.timeunit")
                .replace("%args%", matcher.group(2))

            sendMsg(context, msg)
            return null
        }

        totalTime += amount * multiplier
    }

    return totalTime
}

private fun appendTimePart(timePart: Long, sb: StringBuilder, colon: Boolean = true, canAddZeros: Boolean = false) {
    if (timePart != 0L || canAddZeros) {
        if (timePart < 10) sb.append(0)
        sb.append(timePart)
        if (colon) sb.append(":")
    }
}

fun String.remove(vararg strings: String): String {
    var newString = this
    for (string in strings) {
        newString = newString.replace(string, "")
    }
    return newString
}