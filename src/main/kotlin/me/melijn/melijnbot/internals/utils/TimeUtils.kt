package me.melijn.melijnbot.internals.utils

import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.utils.message.sendRsp
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd kk:mm:ss O")
val dateTimeMillisFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd kk:mm:ss.SSS O")
val simpleDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd kk:mm:ss")
val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd O")
val purgeTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("kk:mm:ss")

object TimeUtils {

}

/** interprets the long as millis duration string **/
//fun Long.getAsDurationString(): String {
//    return getDurationString(this)
//}

//fun Long.asEpochMillisToDateTime(): String {
//    val offsetDateTime = Instant.ofEpochMilli(this).atOffset(ZoneOffset.UTC)
//    return offsetDateTime.asLongLongGMTString()
//}

suspend fun getZoneId(daoManager: DaoManager?, guildId: Long? = null, userId: Long? = null): ZoneId {

    val guildTimezone = guildId?.let {
        val zoneId = daoManager?.timeZoneWrapper?.getTimeZone(it)
        if (zoneId?.isBlank() == true) null
        else {
            zoneId?.let { zid ->
                manualSupporterTimeZone(zid)
            } ?: ZoneId.of(zoneId)
        }
    }

    val userTimezone = userId?.let {
        val zoneId = daoManager?.timeZoneWrapper?.getTimeZone(it)
        if (zoneId?.isBlank() == true) null
        else {
            zoneId?.let { zid ->
                manualSupporterTimeZone(zid)
            } ?: ZoneId.of(zoneId)
        }
    }

    return userTimezone ?: guildTimezone ?: ZoneId.of("GMT")
}

fun manualSupporterTimeZone(zoneId: String): ZoneId? {
    return when (zoneId) {
        "EST" -> ZoneId.of("GMT-5")
        else -> null
    }
}

suspend fun Long.asEpochMillisToDateTime(daoManager: DaoManager?, guildId: Long? = null, userId: Long? = null): String {
    val guildTimezone = guildId?.let {
        val zoneId = daoManager?.timeZoneWrapper?.getTimeZone(it)
        if (zoneId?.isBlank() == true) null
        else ZoneId.of(zoneId)
    }

    val userTimezone = userId?.let {
        val zoneId = daoManager?.timeZoneWrapper?.getTimeZone(it)
        if (zoneId?.isBlank() == true) null
        else ZoneId.of(zoneId)
    }

    val timeZone = userTimezone ?: guildTimezone ?: ZoneId.of("GMT")
    val offsetDateTime = Instant.ofEpochMilli(this).atZone(timeZone)
    return offsetDateTime.asLongLongGMTString()
}

fun Long.asEpochMillisToDateTime(zoneId: ZoneId): String {
    val offsetDateTime = Instant.ofEpochMilli(this).atZone(zoneId)
    return offsetDateTime.asLongLongGMTString()
}

fun Long.asEpochMillisToDateTimeMillis(zoneId: ZoneId): String {
    val offsetDateTime = Instant.ofEpochMilli(this).atZone(zoneId)
    return offsetDateTime.asLongLongLongGMTString()
}

fun OffsetDateTime.asEpochMillisToDate(zoneId: ZoneId): String {
    val offsetDateTime = this.atZoneSameInstant(zoneId) ?: throw IllegalArgumentException("ANGRY")

    return offsetDateTime.asLongDateGMTString()
}

fun OffsetDateTime.asEpochMillisToTimeInvis(zoneId: ZoneId): String {
    val offsetDateTime = this.atZoneSameInstant(zoneId) ?: throw IllegalArgumentException("ANGRY")
    return offsetDateTime.asAsLongTimeAndInvisOffset()
}

fun OffsetDateTime.asEpochMillisToDateTime(zoneId: ZoneId): String {
    return this.atZoneSameInstant(zoneId).asLongLongGMTString()
}

fun OffsetDateTime.asLongLongGMTString(): String = this.format(dateTimeFormatter)
fun OffsetDateTime.asLongDateGMTString(): String = this.format(dateFormatter)
fun ZonedDateTime.asLongLongLongGMTString(): String = this.format(dateTimeMillisFormatter)
fun ZonedDateTime.asLongLongGMTString(): String = this.format(dateTimeFormatter)
fun ZonedDateTime.asLongDateGMTString(): String = this.format(dateFormatter)
fun ZonedDateTime.asAsLongTimeAndInvisOffset(): String = this.format(purgeTimeFormatter)


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

val holyPattern = Pattern.compile("(\\d+)([a-zA-Z]+)")
suspend fun getDurationByArgsNMessage(context: CommandContext, leftBound: Int, rightBound: Int, timeStamps: List<String> = context.args): Long? {
    val corruptTimeStamps = timeStamps.subList(leftBound, rightBound).toMutableList()
    val holyTimeStamps = mutableListOf<String>()
    var totalTime = 0L


    //merge numbed with their right neighbour so the number type is present along with the number itself
    for ((index, corruptTimeStamp) in corruptTimeStamps.withIndex()) {
        if (corruptTimeStamp.isPositiveNumber()) {
            if (corruptTimeStamps.size >= index + 1) continue
            val corruptTimeType = corruptTimeStamps[index + 1]
            if (!corruptTimeType.matches("[a-zA-Z]+".toRegex())) continue

            holyTimeStamps.add(corruptTimeStamp + corruptTimeType)
        } else if (holyPattern.matcher(corruptTimeStamp).matches()) {
            holyTimeStamps.add(corruptTimeStamp)
        }
    }

    if (holyTimeStamps.isEmpty()) {
        val msg = context.getTranslation("message.unknown.timeduration")
            .withVariable("arg", timeStamps.joinToString(" "))

        sendRsp(context, msg)
        return null
    }

    //CorruptTimeStamps aren't corrupt anymore
    for (corruptTimeStamp in holyTimeStamps) {
        val matcher = holyPattern.matcher(corruptTimeStamp)
        require(matcher.find()) { "should always find a match" }

        val amount = matcher.group(1).toLongOrNull()
        if (amount == null) {
            val msg = context.getTranslation("message.numbertobig")
                .withVariable("arg", matcher.group(1))

            sendRsp(context, msg)
            return null
        }

        val typeNorm = matcher.group(2)
        val type = typeNorm.toLowerCase()
        val multiplier = when {
            ("M" == typeNorm || arrayOf("month", "months").contains(type)) -> 30 * 24 * 60 * 60
            arrayOf("s", "second", "seconds").contains(type) -> 1
            arrayOf("m", "minute", "minutes").contains(type) -> 60
            arrayOf("h", "hour", "hours").contains(type) -> 60 * 60
            arrayOf("d", "day", "days").contains(type) -> 24 * 60 * 60
            arrayOf("w", "week", "weeks").contains(type) -> 7 * 24 * 60 * 60
            arrayOf("y", "year", "years").contains(type) -> 52 * 7 * 24 * 60 * 60
            else -> null
        }

        if (multiplier == null) {
            val msg = context.getTranslation("message.unknown.timeunit")
                .withVariable("arg", matcher.group(2))

            sendRsp(context, msg)
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
