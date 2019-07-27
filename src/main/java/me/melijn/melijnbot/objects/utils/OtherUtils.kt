package me.melijn.melijnbot.objects.utils

import net.dv8tion.jda.api.entities.Member
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.regex.Pattern

val linuxUptimePattern: Pattern = Pattern.compile(
        "(?:\\s+)?\\d+:\\d+:\\d+ up(?: (\\d+) days?,)?(?:\\s+(\\d+):(\\d+)|\\s+?(\\d+)\\s+?min).*"
)

fun getSystemUptime(): Long {
    try {
        var uptime: Long = -1
        val os = System.getProperty("os.name").toLowerCase()
        if (os.contains("win")) {
            val uptimeProc = Runtime.getRuntime().exec("net stats workstation")
            val `in` = BufferedReader(InputStreamReader(uptimeProc.inputStream))
            for (line in `in`.readLines()) {
                if (line.startsWith("Statistieken vanaf")) {
                    val format = SimpleDateFormat("'Statistieken vanaf' dd/MM/yyyy hh:mm:ss") //Dutch windows version
                    val bootTime = format.parse(line.replace("?", ""))
                    uptime = System.currentTimeMillis() - bootTime.time
                    break
                } else if (line.startsWith("Statistics since")) {
                    val format = SimpleDateFormat("'Statistics since' MM/dd/yyyy hh:mm:ss") //English windows version
                    val bootTime = format.parse(line.replace("?", ""))
                    uptime = System.currentTimeMillis() - bootTime.time
                    break
                }
            }
        } else if (os.contains("mac") || os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            val uptimeProc = Runtime.getRuntime().exec("uptime") //Parse time to groups if possible
            val `in` = BufferedReader(InputStreamReader(uptimeProc.inputStream))
            val line = `in`.readLine() ?: return uptime
            val matcher = linuxUptimePattern.matcher(line)

            if (!matcher.find()) return uptime //Extract ints out of groups
            val days2 = matcher.group(1)
            val hours2 = matcher.group(2)
            val minutes2 = if (matcher.group(3) == null) matcher.group(4) else matcher.group(3)
            val days = if (days2 != null) Integer.parseInt(days2) else 0
            val hours = if (hours2 != null) Integer.parseInt(hours2) else 0
            val minutes = if (minutes2 != null) Integer.parseInt(minutes2) else 0
            uptime = (minutes * 60000 + hours * 60000 * 60 + days * 60000 * 60 * 24).toLong()
        }
        return uptime
    } catch (e: Exception) {
        return -1
    }
}


fun Member.getAsTag(): String {
    return this.user.asTag
}