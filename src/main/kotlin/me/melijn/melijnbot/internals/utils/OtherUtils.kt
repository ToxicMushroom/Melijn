package me.melijn.melijnbot.internals.utils

import com.google.common.cache.CacheLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.enums.DateFormat
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.internals.utils.message.sendRsp
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

// Thx xavin
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


// EPIC CODE, DO NOT TOUCH
fun <K, V> loadingCacheFrom(function: (K) -> CompletableFuture<V>): CacheLoader<K, CompletableFuture<V>> {
    return CacheLoader.from { k ->
        if (k == null) throw IllegalArgumentException("BRO CRINGE")
        function.invoke(k)
    }
}

fun getUnixUptime(): Long {
    val uptimeProc = Runtime.getRuntime().exec("uptime") // Parse time to groups if possible
    val `in` = BufferedReader(InputStreamReader(uptimeProc.inputStream))
    val line = `in`.readLine() ?: return -1
    val matcher = linuxUptimePattern.matcher(line)

    if (!matcher.find()) return -1 // Extract ints out of groups
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
    val uptimeProc = Runtime.getRuntime().exec("free -m") // Parse time to groups if possible
    uptimeProc.inputStream.use { `is` ->
        `is`.bufferedReader().use { br ->
            br.readLine() ?: return -1
            val lineTwo = br.readLine() ?: return -1

            val matcher = linuxRamPattern.matcher(lineTwo)

            if (!matcher.find()) return -1 // Extract ints out of groups
            val group = matcher.group(1)
            return group.toInt()
        }
    }
}


fun getWindowsUptime(): Long {
    val uptimeProc = Runtime.getRuntime().exec("net stats workstation")
    uptimeProc.inputStream.use { `is` ->
        `is`.bufferedReader().use { br ->
            for (line in br.readLines()) {
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
        }
    }

    return -1
}

fun Color.toHex(): String {
    val redHex = Integer.toHexString(red).toUpperCase().padStart(2, '0')
    val greenHex = Integer.toHexString(green).toUpperCase().padStart(2, '0')
    val blueHex = Integer.toHexString(blue).toUpperCase().padStart(2, '0')
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
            .withVariable(PLACEHOLDER_ARG, enumName)
        sendRsp(context, msg)
    }
    return enum
}

suspend inline fun <T> getObjectFromArgNMessage(context: CommandContext, index: Int, mapper: (String) -> T?, path: String): T? {
    if (argSizeCheckFailed(context, index)) return null
    val newObj = getObjectFromArgN(context, index, mapper)
    if (newObj == null) {
        val msg = context.getTranslation(path)
            .withVariable(PLACEHOLDER_ARG, context.args[index])
        sendRsp(context, msg)
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

val ccTagPattern = Pattern.compile("cc\\.(\\d+)")
suspend fun getCommandIdsFromArgNMessage(context: CommandContext, index: Int): Set<String>? {
    if (argSizeCheckFailed(context, index)) return null
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

    commands.removeIf { id -> id == "16" || id == "0" }

    if (matcher.matches()) {
        commands.add(arg)
    }

    if (commands.isEmpty()) {
        val msg = context.getTranslation("message.unknown.commandnode")
            .withVariable(PLACEHOLDER_ARG, arg)
        sendRsp(context, msg)
        return null
    }
    return commands
}

suspend fun getCommandsFromArgNMessage(context: CommandContext, index: Int): Set<AbstractCommand>? {
    if (argSizeCheckFailed(context, index)) return null
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
        val msg = context.getTranslation("message.unknown.commands")
            .withVariable(PLACEHOLDER_ARG, arg)
        sendRsp(context, msg)
        return null
    }
    return commands
}


suspend fun getLongFromArgNMessage(
    context: CommandContext,
    index: Int,
    min: Long = Long.MIN_VALUE,
    max: Long = Long.MAX_VALUE,
    vararg ignore: String
): Long? {
    if (argSizeCheckFailed(context, index)) return null
    var arg = context.args[index]
    for (a in ignore) {
        arg = arg.remove(a)
    }
    val long = arg.toLongOrNull()
    if (!arg.isNumber()) {
        val msg = context.getTranslation("message.unknown.number")
            .withVariable(PLACEHOLDER_ARG, arg)
        sendRsp(context, msg)
    } else if (long == null) {
        val msg = context.getTranslation("message.unknown.long")
            .withVariable(PLACEHOLDER_ARG, arg)
        sendRsp(context, msg)
    }
    if (long != null) {
        if (min > long || long > max) {
            val msg = context.getTranslation("message.number.notingrange")
                .withVariable("min", min)
                .withVariable("max", max)
                .withVariable(PLACEHOLDER_ARG, arg)
            sendRsp(context, msg)
            return null
        }
    }
    return long
}


//Dayofyear, year
suspend fun getBirthdayByArgsNMessage(context: CommandContext, index: Int, format: DateFormat = DateFormat.DMY): Pair<Int, Int?>? {
    if (argSizeCheckFailed(context, index)) return null
    val list: List<Int> = context.args[index].split("/", "-")
        .map { value ->
            val newVal = value.toIntOrNull()
            if (newVal == null) {
                val msg = context.getTranslation("message.unknown.number")
                    .withVariable(PLACEHOLDER_ARG, value)
                sendRsp(context, msg)
                return null
            } else newVal
        }

    if (list.size >= 2) {
        var birthdayIndex = 0
        var monthIndex = 0
        var yearIndex = 0
        when (format) {
            DateFormat.DMY -> {
                birthdayIndex = 0
                monthIndex = 1
                yearIndex = 2
            }
            DateFormat.MDY -> {
                birthdayIndex = 1
                monthIndex = 0
                yearIndex = 2
            }
            DateFormat.YMD -> {
                birthdayIndex = 2
                monthIndex = 1
                yearIndex = 0
            }
        }

        val birthday = list[birthdayIndex]
        if (birthday < 1 || birthday > 31) {
            val msg = context.getTranslation("message.number.notinrange")
                .withVariable(PLACEHOLDER_ARG, "$birthday")
                .withVariable("start", "1")
                .withVariable("end", "31")
            sendRsp(context, msg)
            return null
        }
        val birthMonth = list[monthIndex]
        if (birthMonth < 1 || birthMonth > 12) {
            val msg = context.getTranslation("message.number.notinrange")
                .withVariable(PLACEHOLDER_ARG, "$birthMonth")
                .withVariable("start", "1")
                .withVariable("end", "12")
            sendRsp(context, msg)
            return null
        }

        val birthYear = if (list.size > 2) list[yearIndex] else null
        if (birthYear != null && (birthYear < 1900 || birthYear > Year.now().value - 12)) {
            val msg = context.getTranslation("message.number.notinrange")
                .withVariable(PLACEHOLDER_ARG, "$birthYear")
                .withVariable("start", "1900")
                .withVariable("end", "2008")
            sendRsp(context, msg)
            return null
        }

        val localDate = LocalDate.of(2019, Month.of(birthMonth), birthday)
        return Pair(localDate.dayOfYear, birthYear)
    } else {
        val msg = context.getTranslation("message.unknown.birthday")
            .withVariable(PLACEHOLDER_ARG, context.args[index])
        sendRsp(context, msg)
        return null
    }
}

//Dayofyear, year
fun getBirthdayByArgsN(arg: String): Pair<Int, Int?>? {
    val list: List<Int> = arg.split("/", "-")
        .map { value ->
            value.toIntOrNull() ?: return null
        }

    if (list.size < 2) {
        return null
    }

    val yearIndex = 0
    val monthIndex = 1
    val birthdayIndex = 2

    val birthday = list[birthdayIndex]
    if (birthday < 1 || birthday > 31) {
        return null
    }
    val birthMonth = list[monthIndex]
    if (birthMonth < 1 || birthMonth > 12) {
        return null
    }

    val birthYear = if (list.size > 2) list[yearIndex] else null
    if (birthYear != null && (birthYear < 1900 || birthYear > Year.now().value - 12)) {
        return null
    }

    val localDate = LocalDate.of(2019, Month.of(birthMonth), birthday)
    return Pair(localDate.dayOfYear, birthYear)
}

suspend fun isPremiumUser(context: CommandContext, user: User = context.author): Boolean {
    return context.daoManager.supporterWrapper.getUsers().contains(user.idLong) ||
        context.container.settings.botInfo.developerIds.contains(user.idLong)
}

suspend fun isPremiumGuild(context: CommandContext): Boolean {
    return isPremiumGuild(context.daoManager, context.guildId)
}

suspend fun isPremiumGuild(daoManager: DaoManager, guildId: Long): Boolean {
    return daoManager.supporterWrapper.getGuilds().contains(guildId)
}


fun getLongFromArgN(context: CommandContext, index: Int, min: Long = Long.MIN_VALUE, max: Long = Long.MAX_VALUE): Long? {
    val number = (if (context.args.size > index) context.args[index].toLongOrNull() else null) ?: return null
    if (number > max || number < min) return null
    return number
}

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

//lowerCase
fun Enum<*>.toLC(): String = this.toString().toLowerCase()


val numberRegex = "-?\\d+".toRegex()
val negativeNumberRegex = "-\\d+".toRegex()
val positiveNumberRegex = "\\d+".toRegex()
fun String.isNumber(): Boolean = this.matches(numberRegex)
fun String.isPositiveNumber(): Boolean = this.matches(positiveNumberRegex)
fun String.isNegativeNumber(): Boolean = this.matches(negativeNumberRegex)

fun <E : Any> MutableList<E>.addIfNotPresent(value: E): Boolean {
    if (!this.contains(value)) {
        this.add(value)
        return true
    }
    return false
}

fun MutableList<String>.addIfNotPresent(value: String, ignoreCase: Boolean): Boolean {
    if (this.none { it.equals(value, ignoreCase) }) {
        this.add(value)
        return true
    }
    return false
}


// Any space surrounded sequence of characters is considered a word
fun String.countWords(): Int {
    val splitted = this.split(SPACE_PATTERN)
    if (splitted.size == 1 && splitted[0].isBlank()) return 0
    return splitted.size
}

fun String.replace(oldValue: String, newValue: Int): String {
    return this.replace(oldValue, "$newValue")
}

fun String.replace(oldValue: String, newValue: Long): String {
    return this.replace(oldValue, "$newValue")
}