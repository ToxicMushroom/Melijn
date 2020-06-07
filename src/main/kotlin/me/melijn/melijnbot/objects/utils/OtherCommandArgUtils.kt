package me.melijn.melijnbot.objects.utils

import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.objects.translation.i18n
import java.time.Instant
import java.util.*

suspend fun getIntegerFromArgNMessage(context: CommandContext, index: Int, start: Int = Integer.MIN_VALUE, end: Int = Integer.MAX_VALUE): Int? {
    if (argSizeCheckFailed(context, index)) return null
    val arg = context.args[index]


    val int = arg.toIntOrNull()
    val language = context.getLanguage()
    when {
        int == null -> {
            val msg = i18n.getTranslation(language, "message.unknown.integer")
                .replace(PLACEHOLDER_ARG, arg)
            sendMsg(context, msg)
        }
        int < start -> {
            val msg = i18n.getTranslation(language, "message.tosmall.integer")
                .replace(PLACEHOLDER_ARG, arg)
                .replace("%min%", start.toString())
            sendMsg(context, msg)
            return null
        }
        int > end -> {
            val msg = i18n.getTranslation(language, "message.tobig.integer")
                .replace(PLACEHOLDER_ARG, arg)
                .replace("%max%", end.toString())
            sendMsg(context, msg)
            return null
        }
    }

    return int
}

suspend fun getFloatFromArgNMessage(context: CommandContext, index: Int, start: Float = Float.MIN_VALUE, end: Float = Float.MAX_VALUE): Float? {
    if (argSizeCheckFailed(context, index)) return null
    val arg = context.args[index]

    val float = arg.toFloatOrNull()
    val language = context.getLanguage()
    when {
        float == null -> {
            val msg = i18n.getTranslation(language, "message.unknown.float")
                .replace(PLACEHOLDER_ARG, arg)
            sendMsg(context, msg)
        }
        float < start -> {
            val msg = i18n.getTranslation(language, "message.tosmall.float")
                .replace(PLACEHOLDER_ARG, arg)
                .replace("%min%", start.toString())
            sendMsg(context, msg)
        }
        float > end -> {
            val msg = i18n.getTranslation(language, "message.tobig.float")
                .replace(PLACEHOLDER_ARG, arg)
                .replace("%max%", end.toString())
            sendMsg(context, msg)
        }
    }

    return float
}

suspend fun getBooleanFromArgN(context: CommandContext, index: Int): Boolean? {
    if (argSizeCheckFailed(context, index, true)) return null
    val arg = context.args[index]

    return when (arg.toLowerCase()) {
        "true", "yes", "on", "enable", "enabled", "positive", "+" -> true
        "false", "no", "off", "disable", "disabled", "negative", "-" -> false
        else -> null
    }
}

suspend fun getBooleanFromArgNMessage(context: CommandContext, index: Int): Boolean? {
    if (argSizeCheckFailed(context, index)) return null
    val arg = context.args[index]

    val bool = getBooleanFromArgN(context, index)
    if (bool == null) {
        val language = context.getLanguage()
        val msg = i18n.getTranslation(language, "message.unknown.boolean")
            .replace(PLACEHOLDER_ARG, arg)
        sendMsg(context, msg)
    }

    return bool
}

// Returns in epoch millis at UTC+0
suspend fun getDateTimeFromArgNMessage(context: CommandContext, index: Int): Long? {
    if (argSizeCheckFailed(context, index)) return null
    val arg = context.args[index]

    if (arg.equals("current", true)) {
        return Instant.now().toEpochMilli()
    }

    val dateTime = getEpochMillisFromArgN(context, index)
    if (dateTime == null) {
        val language = context.getLanguage()
        val msg = i18n.getTranslation(language, "message.unknown.datetime")
            .replace(PLACEHOLDER_ARG, arg)
        sendMsg(context, msg)
    }

    return dateTime
}


fun getEpochMillisFromArgN(context: CommandContext, index: Int): Long? {
    val arg = context.args[index]
    return try {
        (simpleDateTimeFormatter.parse(arg) as Date).time
    } catch (e: Exception) {
        null
    }
}

suspend fun argSizeCheckFailed(context: CommandContext, index: Int, silent: Boolean = false): Boolean {
    return if (context.args.size <= index) {
        if (!silent) sendSyntax(context, context.commandOrder.last().syntax)
        true
    } else {
        false
    }
}
