package me.melijn.melijnbot.commands.utility

import java.util.*
import kotlin.math.max


fun Date.toUniversalDateTimeFormat(): String {
    val cal = Calendar.getInstance()
    cal.time = this
    return "${goodFormat(cal, Calendar.YEAR, 4)}-${goodFormat(cal, Calendar.MONTH, 1)}-${
        goodFormat(
            cal,
            Calendar.DAY_OF_MONTH
        )
    } " +
        "${goodFormat(cal, Calendar.HOUR)}:${goodFormat(cal, Calendar.MINUTE)}"
}

fun goodFormat(cal: Calendar, field: Int, length: Int = 2): String {
    return cal.get(field).toString().prependZeros(length)
}

fun String.prependZeros(targetLength: Int): String {
    val zeros = max(targetLength - this.length, 0)
    val extraZeros = "0".repeat(zeros)
    return extraZeros + this
}

fun Date.toUniversalDateFormat(): String {
    val cal = Calendar.getInstance()
    cal.time = this
    return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}-${cal.get(Calendar.DAY_OF_MONTH)}"
}

//class KitsuCommand : AbstractCommand("command.kitsu") {
//
//    init {
//        id = 159
//        name = "kitsu"
//    }
//
//    suspend fun execute(context: ICommandContext) {
//
//    }
//}