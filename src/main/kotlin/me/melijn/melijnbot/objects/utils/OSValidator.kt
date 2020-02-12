package me.melijn.melijnbot.objects.utils

object OSValidator {

    private val OS = System.getProperty("os.name").toLowerCase()

    val isWindows: Boolean
        get() = OS.contains("win")

    val isMac: Boolean
        get() = OS.contains("mac")

    val isUnix: Boolean
        get() = OS.contains("nix") || OS.contains("nux") || OS.contains("aix")

    val isSolaris: Boolean
        get() = OS.contains("sunos")

    val isUbuntu: Boolean
        get() = OS.contains("ubuntu")

    fun getOS(): String {
        return when {
            isWindows -> {
                "win"
            }
            isMac -> {
                "osx"
            }
            isUnix -> {
                "uni"
            }
            isSolaris -> {
                "sol"
            }
            else -> {
                "err"
            }
        }
    }
}