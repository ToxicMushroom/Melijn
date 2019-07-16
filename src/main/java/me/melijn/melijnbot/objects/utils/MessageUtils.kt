package me.melijn.melijnbot.objects.utils

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageChannel

class MessageUtils {
    fun printException(currentThread: Thread, e: Exception, originGuild: Guild? = null, originChannel: MessageChannel? = null) {
        println("blub")
    }
}

fun String.toUpperWordCase(): String {
    var previous = 'a'
    var newString = ""
    this.toCharArray().forEach { c: Char ->
        newString += if (previous == ' ') c.toUpperCase() else c
        previous = c
    }
    return newString
}