package me.melijn.melijnbot.internals.utils

object OSValidator {

    private val OS = System.getProperty("os.name").lowercase()

//    private val isWindows: Boolean
//        get() = OS.contains("win")
//
//    private val isMac: Boolean
//        get() = OS.contains("mac")

    val isUnix: Boolean
        get() = OS.contains("nix") || OS.contains("nux") || OS.contains("aix")

//    private val isSolaris: Boolean
//        get() = OS.contains("sunos")

//    val isUbuntu: Boolean
//        get() = OS.contains("ubuntu")
//
//    fun getOS(): String {
//        return when {
//            isWindows -> {
//                "win"
//            }
//            isMac -> {
//                "osx"
//            }
//            isUnix -> {
//                "uni"
//            }
//            isSolaris -> {
//                "sol"
//            }
//            else -> {
//                "err"
//            }
//        }
//    }
}