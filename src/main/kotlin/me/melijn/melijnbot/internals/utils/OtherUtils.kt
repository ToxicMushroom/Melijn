package me.melijn.melijnbot.internals.utils

import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.enums.DateFormat
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.PLACEHOLDER_PREFIX
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.internals.utils.message.sendRsp
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.utils.data.DataArray
import net.dv8tion.jda.api.utils.data.DataObject
import java.awt.Color
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Month
import java.time.MonthDay
import java.time.Year
import java.util.*
import java.util.regex.Pattern

val linuxUptimePattern: Pattern = Pattern.compile(
    "([0-9]+)(?:\\.[0-9]+)? [0-9]+(?:\\.[0-9]+)?" // 11105353.49 239988480.98
)

// Thx xavin
val linuxRamPattern: Pattern = Pattern.compile("Mem.*:\\s+([0-9]+) kB")

fun getSystemUptime(): Long {
    return try {
        var uptime: Long = -1
        val os = System.getProperty("os.name").lowercase()
        if (os.contains("win")) {
            uptime = getWindowsUptime()
        } else if (os.contains("web_binariesaa/mac") || os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            uptime = getUnixUptime()
        }
        uptime
    } catch (e: Exception) {
        -1
    }
}

fun Calendar.isLeapYear(): Boolean {
    return this.getActualMaximum(Calendar.DAY_OF_YEAR) > 365
}

fun getUnixUptime(): Long {
    val uptimeProc = Runtime.getRuntime().exec("cat /proc/uptime") // Parse time to groups if possible
    uptimeProc.inputStream.use { ins ->
        ins.bufferedReader().use { br ->
            val line = br.readLine() ?: return -1
            val matcher = linuxUptimePattern.matcher(line)

            if (!matcher.find()) return -1 // Extract ints out of groups
            return matcher.group(1).toLong()
        }
    }
}

fun getTotalMBUnixRam(): Long {
    val uptimeProc = Runtime.getRuntime().exec("cat /proc/meminfo")
    uptimeProc.inputStream.use { ins ->
        ins.bufferedReader().use { br ->
            val total = br.readLine() ?: return -1
            val matcher = linuxRamPattern.matcher(total)

            if (!matcher.find()) return -1 // Extract ints out of groups
            val group = matcher.group(1)
            return group.toLong() / 1024
        }
    }
}

fun getUsedMBUnixRam(): Long {
    val uptimeProc = Runtime.getRuntime().exec("cat /proc/meminfo")
    uptimeProc.inputStream.use { ins ->
        ins.bufferedReader().use { br ->
            val total = br.readLine() ?: return -1
            br.readLine() ?: return -1
            val available = br.readLine() ?: return -1

            val matcher1 = linuxRamPattern.matcher(total)
            val matcher2 = linuxRamPattern.matcher(available)

            if (!matcher1.find() || !matcher2.find()) return -1 // Extract ints out of groups
            val totalLong = matcher1.group(1).toLong()
            val availableLong = matcher2.group(1).toLong()
            return (totalLong - availableLong) / 1024
        }
    }
}

fun getWindowsUptime(): Long {
    val uptimeProc = Runtime.getRuntime().exec("net stats workstation")
    uptimeProc.inputStream.use { ins ->
        ins.bufferedReader().use { br ->
            for (line in br.readLines()) {
                if (line.startsWith("Statistieken vanaf")) {
                    val format = SimpleDateFormat("'Statistieken vanaf' dd/MM/yyyy hh:mm:ss") // Dutch windows version
                    val bootTime = format.parse(line.remove("?"))
                    return System.currentTimeMillis() - bootTime.time
                } else if (line.startsWith("Statistics since")) {
                    val format = SimpleDateFormat("'Statistics since' MM/dd/yyyy hh:mm:ss") // English windows version
                    val bootTime = format.parse(line.remove("?"))
                    return System.currentTimeMillis() - bootTime.time
                }
            }
        }
    }

    return -1
}

fun Color.toHex(): String {
    val redHex = Integer.toHexString(red).uppercase().padStart(2, '0')
    val greenHex = Integer.toHexString(green).uppercase().padStart(2, '0')
    val blueHex = Integer.toHexString(blue).uppercase().padStart(2, '0')
    return "#$redHex$greenHex$blueHex"
}

inline fun <reified T : Enum<*>> enumValueOrNull(name: String): T? =
    T::class.java.enumConstants.firstOrNull {
        it.name.equals(name, true)
    }

suspend inline fun <reified T : Enum<*>> getEnumFromArgNMessage(context: ICommandContext, index: Int, path: String): T? {
    if (argSizeCheckFailed(context, index)) return null
    val enumName = context.args[index]
    val enum = T::class.java.enumConstants.firstOrNull {
        it.name.equals(enumName, true)
    }
    if (enum == null) {
        val msg = context.getTranslation(path)
            .withSafeVariable(PLACEHOLDER_ARG, enumName)
            .withSafeVariable(PLACEHOLDER_PREFIX, context.usedPrefix)
        sendRsp(context, msg)
    }
    return enum
}

suspend inline fun <T> getObjectFromArgNMessage(
    context: ICommandContext,
    index: Int,
    mapper: (String) -> T?,
    path: String
): T? {
    if (argSizeCheckFailed(context, index)) return null
    val newObj = getObjectFromArgN(context, index, mapper)
    if (newObj == null) {
        val msg = context.getTranslation(path)
            .withSafeVariable(PLACEHOLDER_ARG, context.args[index])
            .withSafeVariable(PLACEHOLDER_PREFIX, context.usedPrefix)
        sendRsp(context, msg)
    }
    return newObj
}

suspend inline fun <T> getObjectFromArgN(context: ICommandContext, index: Int, mapper: (String) -> T?): T? {
    if (argSizeCheckFailed(context, index)) return null
    val arg = context.args[index]
    return mapper(arg)
}

suspend inline fun <reified T : Enum<*>> getEnumFromArgN(context: ICommandContext, index: Int): T? {
    if (argSizeCheckFailed(context, index, true)) return null
    val enumName = context.args[index]
    return T::class.java.enumConstants.firstOrNull {
        it.name.equals(enumName, true)
    }
}

val ccTagPattern = Regex("cc\\.(\\d+)")
suspend fun getCommandIdsFromArgNMessage(context: ICommandContext, index: Int): Set<String>? {
    if (argSizeCheckFailed(context, index)) return null
    val arg = context.args[index]
    val category: CommandCategory? = enumValueOrNull(arg)

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

    if (ccTagPattern.matches(arg)) {
        commands.add(arg)
    }

    if (commands.isEmpty()) {
        val msg = context.getTranslation("message.unknown.commandnode")
            .withSafeVariable(PLACEHOLDER_ARG, arg)
        sendRsp(context, msg)
        return null
    }
    return commands
}

suspend fun getCommandsFromArgNMessage(context: ICommandContext, index: Int): Set<AbstractCommand>? {
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
            .withSafeVariable(PLACEHOLDER_ARG, arg)
        sendRsp(context, msg)
        return null
    }
    return commands
}

suspend fun getLongFromArgNMessage(
    context: ICommandContext,
    index: Int,
    min: Long = Long.MIN_VALUE, // inclusive
    max: Long = Long.MAX_VALUE, // inclusive
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
            .withSafeVariable(PLACEHOLDER_ARG, arg)
        sendRsp(context, msg)
    } else if (long == null) {
        val msg = context.getTranslation("message.unknown.long")
            .withSafeVariable(PLACEHOLDER_ARG, arg)
        sendRsp(context, msg)
    }
    if (long != null) {
        if (min > long || long > max) {
            val msg = context.getTranslation("message.number.notinrange")
                .withVariable("start", min)
                .withVariable("end", max)
                .withSafeVariable(PLACEHOLDER_ARG, arg)
            sendRsp(context, msg)
            return null
        }
    }
    return long
}

//Dayofyear, year
suspend fun getBirthdayByArgsNMessage(
    context: ICommandContext,
    index: Int,
    format: DateFormat = DateFormat.DMY
): Pair<Int, Int?>? {
    if (argSizeCheckFailed(context, index)) return null
    val list: List<Int> = context.args[index].split("/", "-")
        .map { value ->
            val newVal = value.toIntOrNull()
            if (newVal == null) {
                val msg = context.getTranslation("message.unknown.number")
                    .withSafeVariable(PLACEHOLDER_ARG, value)
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

        suspend fun <T> Int.keepWithin(low: Int, high: Int, map: (Int) -> T?): T? {
            if (this !in low..high) {
                sendRsp(
                    context,
                    context.getTranslation("message.number.notinrange")
                        .withSafeVariable(PLACEHOLDER_ARG, "$this")
                        .withVariable("start", low.toString())
                        .withVariable("end", high.toString())
                )
                return null
            }

            return map(this)
        }

        val birthYear = list.getOrNull(yearIndex)?.let {
            it.keepWithin(1900, Year.now().value - 12) { year ->
                Year.of(year)
            } ?: return null
        } ?: Year.now()

        val birthMonth = list[monthIndex].let {
            it.keepWithin(1, 12) { month ->
                Month.of(month)
            } ?: return null
        }

        val birthday = list[birthdayIndex].let {
            it.keepWithin(1, birthMonth.length(birthYear.isLeap)) { it } ?: return null
        }

        return birthYear.atMonthDay(MonthDay.of(birthMonth, birthday)).dayOfYear to birthYear.value
    } else {
        val msg = context.getTranslation("message.unknown.birthday")
            .withSafeVariable(PLACEHOLDER_ARG, context.args[index])
        sendRsp(context, msg)
        return null
    }
}

// Dayofyear, year
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

suspend fun isPremiumUser(context: ICommandContext, user: User = context.author): Boolean {
    return context.daoManager.supporterWrapper.getUsers().contains(user.idLong) ||
        context.container.settings.botInfo.developerIds.contains(user.idLong)
}

suspend fun isPremiumGuild(context: ICommandContext): Boolean {
    return isPremiumGuild(context.daoManager, context.guildId)
}

suspend fun isPremiumGuild(daoManager: DaoManager, guildId: Long): Boolean {
    return daoManager.supporterWrapper.getGuilds().contains(guildId)
}

suspend fun getBalanceNMessage(context: ICommandContext, index: Int): Long? {
    if (argSizeCheckFailed(context, index)) return null
    val bal = context.daoManager.balanceWrapper.getBalance(context.authorId)

    val arg = context.args[index]
    return if (arg.equals("all", true)) {
        bal
    } else {
        val amount = getLongFromArgNMessage(context, index, 1) ?: return null
        if (amount > bal) {
            val msg = context.getTranslation("message.amounttoobig")
                .withVariable("bal", bal)
                .withVariable("amount", amount)

            sendRsp(context, msg)
            return null
        }
        amount
    }
}

suspend fun getLongFromArgN(
    context: ICommandContext,
    index: Int,
    min: Long = Long.MIN_VALUE,
    max: Long = Long.MAX_VALUE,
    vararg ignore: String
): Long? {
    if (argSizeCheckFailed(context, index, true)) return null
    var arg = context.args[index]
    for (a in ignore) {
        arg = arg.remove(a)
    }
    val number = arg.toLongOrNull() ?: return null
    if (number > max || number < min) return null
    return number
}

fun getIntegerFromArgN(context: ICommandContext, index: Int, min: Int = Int.MIN_VALUE, max: Int = Int.MAX_VALUE): Int? {
    val number = (if (context.args.size > index) context.args[index].toIntOrNull() else null) ?: return null
    if (number > max || number < min) return null
    return number
}

// UpperCamelCase
fun Enum<*>.toUCC(): String {
    return this
        .toUCSC()
        .remove(" ")
}

// UpperCamelSpaceCase
fun Enum<*>.toUCSC(): String {
    return toString()
        .replace("_", " ")
        .toUpperWordCase()
}

// lowerCamelCase
fun Enum<*>.toLCC(): String {
    val uCC = this.toUCC()
    return uCC[0].lowercase() + uCC.substring(1)
}

// lowerCase
fun Enum<*>.toLC(): String = this.toString().lowercase()

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

// Returns amount of added values
fun <E : Any> MutableList<E>.addAllIfNotPresent(col: Collection<E>): Int {
    var count = 0
    for (value in col) {
        if (!this.contains(value)) {
            this.add(value)
            count++
        }
    }
    return count
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

operator fun DataObject.set(key: String, value: Any) = this.put(key, value)
fun DataObject.getObjectN(key: String): DataObject? {
    return if (this.hasKey(key)) getObject(key) else null
}
fun DataObject.getArrayN(key: String): DataArray? {
    return if (this.hasKey(key)) getArray(key) else null
}

suspend fun <K, V> Map<K, V>.sum(function: suspend (Map.Entry<K, V>) -> Int): Int {
    var count = 0
    for (entry in this) {
        count += function(entry)
    }
    return count
}

suspend fun <K, V> Map<K, V>.first(function: suspend (Map.Entry<K, V>) -> Boolean): Map.Entry<K, V> {
    for (entry in this) {
        if (function(entry)) return entry
    }
    throw IllegalStateException("Map#first failed to return, predicate was false for all of the entries")
}

suspend fun <T> ICommandContext.optional(index: Int, default: T, func: suspend (Int) -> T): T {
    return if (this.args.size > index) func(index)
    else default
}

operator fun Boolean.plus(i: Int): Int {
    return if (this) i + 1
    else i
}
