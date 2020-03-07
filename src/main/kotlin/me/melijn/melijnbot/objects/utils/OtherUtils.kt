package me.melijn.melijnbot.objects.utils

import com.google.common.cache.CacheLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import me.melijn.melijnbot.enums.MonthFormat
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.objects.translation.i18n
import net.dv8tion.jda.api.entities.User
import java.awt.Color
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Month
import java.time.Year
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.regex.Pattern


val linuxUptimePattern: Pattern = Pattern.compile(
    "(?:\\s+)?\\d+:\\d+:\\d+ up(?: (\\d+) days?,)?(?:\\s+(\\d+):(\\d+)|\\s+?(\\d+)\\s+?min).*"
)

//Thx xavin
val linuxRamPattern: Pattern = Pattern.compile("([0-9]+$)")

fun getSystemUptime(): Long {
    return try {
        var uptime: Long = -1
        val os = System.getProperty("os.name").toLowerCase()
        if (os.contains("win")) {
            uptime = getWindowsUptime()
        } else if (os.contains("mac") || os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            uptime = getUnixUptime()
        }
        uptime
    } catch (e: Exception) {
        -1
    }
}

fun Calendar.isLeapYear(): Boolean {
    val cal = this
    return cal.getActualMaximum(Calendar.DAY_OF_YEAR) > 365
}


//EPIC CODE DO NOT TOUCH
fun <K, V> loadingCacheFrom(function: (K) -> CompletableFuture<V>): CacheLoader<K, CompletableFuture<V>> {
    return CacheLoader.from { k ->
        if (k == null) throw IllegalArgumentException("BRO CRINGE")
        function.invoke(k)
    }
}

fun commandFromContext(context: CommandContext): AbstractCommand = context.commandOrder.last()

fun getUnixUptime(): Long {
    val uptimeProc = Runtime.getRuntime().exec("uptime") //Parse time to groups if possible
    val `in` = BufferedReader(InputStreamReader(uptimeProc.inputStream))
    val line = `in`.readLine() ?: return -1
    val matcher = linuxUptimePattern.matcher(line)

    if (!matcher.find()) return -1 //Extract ints out of groups
    val days2 = matcher.group(1)
    val hours2 = matcher.group(2)
    val minutes2 = if (matcher.group(3) == null) {
        matcher.group(4)
    } else {
        matcher.group(3)
    }
    val days = if (days2 != null) Integer.parseInt(days2) else 0
    val hours = if (hours2 != null) Integer.parseInt(hours2) else 0
    val minutes = if (minutes2 != null) Integer.parseInt(minutes2) else 0
    return (minutes * 60000 + hours * 60000 * 60 + days * 60000 * 60 * 24).toLong()
}

fun getUnixRam(): Int {
    val uptimeProc = Runtime.getRuntime().exec("free -m") //Parse time to groups if possible
    val `in` = BufferedReader(InputStreamReader(uptimeProc.inputStream))
    `in`.readLine() ?: return -1
    val lineTwo = `in`.readLine() ?: return -1

    val matcher = linuxRamPattern.matcher(lineTwo)

    if (!matcher.find()) return -1 //Extract ints out of groups
    val group = matcher.group(1)
    return group.toInt()
}


fun getWindowsUptime(): Long {
    val uptimeProc = Runtime.getRuntime().exec("net stats workstation")
    val `in` = BufferedReader(InputStreamReader(uptimeProc.inputStream))
    for (line in `in`.readLines()) {
        if (line.startsWith("Statistieken vanaf")) {
            val format = SimpleDateFormat("'Statistieken vanaf' dd/MM/yyyy hh:mm:ss") //Dutch windows version
            val bootTime = format.parse(line.remove("?"))
            return System.currentTimeMillis() - bootTime.time

        } else if (line.startsWith("Statistics since")) {
            val format = SimpleDateFormat("'Statistics since' MM/dd/yyyy hh:mm:ss") //English windows version
            val bootTime = format.parse(line.remove("?"))
            return System.currentTimeMillis() - bootTime.time

        }
    }
    return -1
}

fun Color.toHex(): String {
    val redHex = Integer.toHexString(red)
    val greenHex = Integer.toHexString(green)
    val blueHex = Integer.toHexString(blue)
    return "#$redHex$greenHex$blueHex"
}


inline fun <reified T : Enum<*>> enumValueOrNull(name: String): T? =
    T::class.java.enumConstants.firstOrNull {
        it.name.equals(name, true)
    }

suspend inline fun <reified T : Enum<*>> getEnumFromArgNMessage(context: CommandContext, index: Int, path: String): T? {
    if (argSizeCheckFailed(context, index)) return null
    val enumName = context.args[index]
    val enum = T::class.java.enumConstants.firstOrNull {
        it.name.equals(enumName, true)
    }
    if (enum == null) {
        val msg = context.getTranslation(path)
            .replace(PLACEHOLDER_ARG, enumName)
        sendMsg(context, msg)
    }
    return enum
}

suspend inline fun <T> getObjectFromArgNMessage(context: CommandContext, index: Int, mapper: (String) -> T?, path: String): T? {
    val newObj = getObjectFromArgN(context, index, mapper)
    if (newObj == null) {
        val msg = context.getTranslation(path)
            .replace(PLACEHOLDER_ARG, context.args[index])
        sendMsg(context, msg)
    }
    return newObj
}

suspend inline fun <T> getObjectFromArgN(context: CommandContext, index: Int, mapper: (String) -> T?): T? {
    if (argSizeCheckFailed(context, index)) return null
    val arg = context.args[index]
    return mapper(arg)
}


suspend inline fun <reified T : Enum<*>> getEnumFromArgN(context: CommandContext, index: Int): T? {
    if (argSizeCheckFailed(context, index, true)) return null
    val enumName = context.args[index]
    return T::class.java.enumConstants.firstOrNull {
        it.name.equals(enumName, true)
    }
}

val ccTagPattern = Pattern.compile("cc.\\d+")
suspend fun getCommandIdsFromArgNMessage(context: CommandContext, index: Int): Set<String>? {
    val arg = context.args[index]
    val category: CommandCategory? = enumValueOrNull(arg)
    val matcher = ccTagPattern.matcher(arg)

    val commands = if (category == null) {
        if (arg == "*") {
            context.commandList
        } else {
            context.commandList
                .filter { command -> command.isCommandFor(arg) }
        }
    } else {
        context.commandList
            .filter { command -> command.commandCategory == category }
    }.map { cmd -> cmd.id.toString() }.toMutableSet()

    commands.removeIf { id -> id == "16" }

    if (matcher.matches()) {
        commands.add(arg)
    }

    if (commands.isEmpty()) {
        val language = context.getLanguage()
        val msg = i18n.getTranslation(language, "message.unknown.commandnode")
            .replace(PLACEHOLDER_ARG, arg)
        sendMsg(context, msg, null)
        return null
    }
    return commands
}

suspend fun getCommandsFromArgNMessage(context: CommandContext, index: Int): Set<AbstractCommand>? {
    val arg = context.args[index]
    val category: CommandCategory? = enumValueOrNull(arg)

    val commands = if (category == null) {
        context.commandList
            .filter { command -> command.isCommandFor(arg) }
    } else {
        context.commandList
            .filter { command -> command.commandCategory == category }
    }.toMutableSet()

    if (commands.isEmpty()) {
        val language = context.getLanguage()
        val msg = i18n.getTranslation(language, "message.unknown.commands")
            .replace(PLACEHOLDER_ARG, arg)
        sendMsg(context, msg, null)
        return null
    }
    return commands
}


suspend fun getLongFromArgNMessage(context: CommandContext, index: Int, min: Long = Long.MIN_VALUE, max: Long = Long.MAX_VALUE): Long? {
    val arg = context.args[index]
    val long = arg.toLongOrNull()
    val language = context.getLanguage()
    if (!arg.matches("\\d+".toRegex())) {
        val msg = i18n.getTranslation(language, "message.unknown.number")
            .replace(PLACEHOLDER_ARG, arg)
        sendMsg(context, msg, null)
    } else if (long == null) {
        val msg = i18n.getTranslation(language, "message.unknown.long")
            .replace(PLACEHOLDER_ARG, arg)
        sendMsg(context, msg, null)
    }
    if (long != null) {
        if (min > long || long > max) {
            val msg = i18n.getTranslation(language, "message.long.notingrange")
                .replace("%min%", min.toString())
                .replace("%max%", max.toString())
                .replace(PLACEHOLDER_ARG, arg)
            sendMsg(context, msg, null)
            return null
        }
    }
    return long
}


//Dayofyear, year
suspend fun getBirthdayByArgsNMessage(context: CommandContext, index: Int, format: MonthFormat = MonthFormat.DMY): Pair<Int, Int?>? {
    val list: List<Int> = context.args[0].split("/", "-")
        .map { value ->
            val newVal = value.toIntOrNull()
            if (newVal == null) {
                val msg = context.getTranslation("message.unknown.number")
                    .replace(PLACEHOLDER_ARG, value)
                sendMsg(context, msg)
                return null
            } else newVal
        }

    if (list.size >= 2) {
        var birthdayIndex = 0
        var monthIndex = 0
        var yearIndex = 0
        when (format) {
            MonthFormat.DMY -> {
                birthdayIndex = 0
                monthIndex = 1
                yearIndex = 2
            }
            MonthFormat.MDY -> {
                birthdayIndex = 1
                monthIndex = 0
                yearIndex = 2
            }
            MonthFormat.YMD -> {
                birthdayIndex = 2
                monthIndex = 1
                yearIndex = 0
            }
        }

        val birthday = list[birthdayIndex]
        if (birthday < 1 || birthday > 31) {
            val msg = context.getTranslation("message.number.notinrange")
                .replace(PLACEHOLDER_ARG, "$birthday")
                .replace("%start%", "1")
                .replace("%end%", "31")
            sendMsg(context, msg)
            return null
        }
        val birthMonth = list[monthIndex]
        if (birthMonth < 1 || birthMonth > 12) {
            val msg = context.getTranslation("message.number.notinrange")
                .replace(PLACEHOLDER_ARG, "$birthMonth")
                .replace("%start%", "1")
                .replace("%end%", "12")
            sendMsg(context, msg)
            return null
        }

        val birthYear = if (list.size > 2) list[yearIndex] else null
        if (birthYear != null && (birthYear < 1900 || birthYear > Year.now().value - 12)) {
            val msg = context.getTranslation("message.number.notinrange")
                .replace(PLACEHOLDER_ARG, "$birthYear")
                .replace("%start%", "1900")
                .replace("%end%", "2008")
            sendMsg(context, msg)
            return null
        }

        val localDate = LocalDate.of(2019, Month.of(birthMonth), birthday)
        return Pair(localDate.dayOfYear, birthYear)
    } else {
        val msg = context.getTranslation("message.unknwon.birthday")
            .replace(PLACEHOLDER_ARG, context.args[index])
        sendMsg(context, msg)
        return null
    }
}

fun isPremiumUser(context: CommandContext, user: User = context.author): Boolean {
    return context.daoManager.supporterWrapper.userSupporterIds.contains(user.idLong)
}

fun isPremiumGuild(context: CommandContext): Boolean {
    return context.daoManager.supporterWrapper.guildSupporterIds.contains(context.guildId)
}


fun getLongFromArgN(context: CommandContext, index: Int): Long? =
    if (context.args.size > index) context.args[index].toLongOrNull() else null

fun getIntegerFromArgN(context: CommandContext, index: Int, min: Int = Int.MIN_VALUE, max: Int = Int.MAX_VALUE): Int? {
    val number = (if (context.args.size > index) context.args[index].toIntOrNull() else null) ?: return null
    if (number > max || number < min) return null
    return number
}

fun Executor.launch(block: suspend CoroutineScope.() -> Unit): Job {
    return CoroutineScope(this.asCoroutineDispatcher()).launch {
        block.invoke(this)
    }
}

//UpperCamelCase
fun Enum<*>.toUCC(): String {
    return this
        .toUCSC()
        .remove(" ")
}

//UpperCamelSpaceCase
fun Enum<*>.toUCSC(): String {
    return toString()
        .replace("_", " ")
        .toUpperWordCase()
}

//lowerCamelCase
fun Enum<*>.toLCC(): String {
    val uCC = this.toUCC()
    return uCC[0].toLowerCase() + uCC.substring(1)
}

val numberRegex = "-?\\d+".toRegex()
val negativeNumberRegex = "-\\d+".toRegex()
val positiveNumberRegex = "\\d+".toRegex()
fun String.isNumber(): Boolean = this.matches(numberRegex)
fun String.isPositiveNumber(): Boolean = this.matches(positiveNumberRegex)
fun String.isNegativeNumber(): Boolean = this.matches(negativeNumberRegex)
fun <E : Any> MutableList<E>.addIfNotPresent(value: E) {
    if (!this.contains(value)) this.add(value)
}