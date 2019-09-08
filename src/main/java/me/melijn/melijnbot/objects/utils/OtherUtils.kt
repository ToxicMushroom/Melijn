package me.melijn.melijnbot.objects.utils

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.objects.translation.Translateable
import java.awt.Color
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.regex.Pattern

val linuxUptimePattern: Pattern = Pattern.compile(
        "(?:\\s+)?\\d+:\\d+:\\d+ up(?: (\\d+) days?,)?(?:\\s+(\\d+):(\\d+)|\\s+?(\\d+)\\s+?min).*"
)

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

fun getUnixUptime(): Long {
    val uptimeProc = Runtime.getRuntime().exec("uptime") //Parse time to groups if possible
    val `in` = BufferedReader(InputStreamReader(uptimeProc.inputStream))
    val line = `in`.readLine() ?: return -1
    val matcher = linuxUptimePattern.matcher(line)

    if (!matcher.find()) return -1 //Extract ints out of groups
    val days2 = matcher.group(1)
    val hours2 = matcher.group(2)
    val minutes2 = if (matcher.group(3) == null) matcher.group(4) else matcher.group(3)
    val days = if (days2 != null) Integer.parseInt(days2) else 0
    val hours = if (hours2 != null) Integer.parseInt(hours2) else 0
    val minutes = if (minutes2 != null) Integer.parseInt(minutes2) else 0
    return (minutes * 60000 + hours * 60000 * 60 + days * 60000 * 60 * 24).toLong()
}

fun getWindowsUptime(): Long {
    val uptimeProc = Runtime.getRuntime().exec("net stats workstation")
    val `in` = BufferedReader(InputStreamReader(uptimeProc.inputStream))
    for (line in `in`.readLines()) {
        if (line.startsWith("Statistieken vanaf")) {
            val format = SimpleDateFormat("'Statistieken vanaf' dd/MM/yyyy hh:mm:ss") //Dutch windows version
            val bootTime = format.parse(line.replace("?", ""))
            return System.currentTimeMillis() - bootTime.time

        } else if (line.startsWith("Statistics since")) {
            val format = SimpleDateFormat("'Statistics since' MM/dd/yyyy hh:mm:ss") //English windows version
            val bootTime = format.parse(line.replace("?", ""))
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

fun getCommandsFromArgNMessage(context: CommandContext, index: Int): Set<AbstractCommand>? {
    val arg = context.args[index]
    val category: CommandCategory? = enumValueOrNull(arg)

    val commands = (if (category == null) {
        if (arg == "*") {
            context.getCommands()
        } else context.getCommands().filter { command -> command.isCommandFor(arg) }.toSet()
    } else {
        context.getCommands().filter { command -> command.commandCategory == category }.toSet()
    }).toMutableSet()
    commands.removeIf { cmd -> cmd.id == 16 }

    if (commands.isEmpty()) {
        sendMsg(context, Translateable("message.unknown.commandnode").string(context)
                .replace(PLACEHOLDER_ARG, arg), null)
        return null
    }
    return commands
}

fun getLongFromArgNMessage(context: CommandContext, index: Int): Long? {
    val arg = context.args[index]
    val long = arg.toLongOrNull()
    if (!arg.matches("\\d+".toRegex())) {
        sendMsg(context, Translateable("message.unknown.number").string(context)
                .replace(PLACEHOLDER_ARG, arg), null)
    } else if (long == null) {
        sendMsg(context, Translateable("message.unknown.long").string(context)
                .replace(PLACEHOLDER_ARG, arg), null)
    }
    return long
}