package me.melijn.melijnbot.internals.utils

import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import java.time.Instant
import java.util.*

suspend fun getIntegerFromArgNMessage(context: CommandContext, index: Int, start: Int = Integer.MIN_VALUE, end: Int = Integer.MAX_VALUE): Int? {
    if (argSizeCheckFailed(context, index)) return null
    val arg = context.args[index]

    val int = arg.toIntOrNull()
    when {
        int == null -> {
            val msg = context.getTranslation("message.unknown.integer")
                .withVariable(PLACEHOLDER_ARG, arg)
            sendRsp(context, msg)
        }
        int < start -> {
            val msg = context.getTranslation("message.tosmall.integer")
                .withVariable(PLACEHOLDER_ARG, arg)
                .withVariable("min", start)
            sendRsp(context, msg)
            return null
        }
        int > end -> {
            val msg = context.getTranslation("message.tobig.integer")
                .withVariable(PLACEHOLDER_ARG, arg)
                .withVariable("max", end)
            sendRsp(context, msg)
            return null
        }
    }

    return int
}

suspend fun getFloatFromArgNMessage(context: CommandContext, index: Int, start: Float = Float.MIN_VALUE, end: Float = Float.MAX_VALUE): Float? {
    if (argSizeCheckFailed(context, index)) return null
    val arg = context.args[index]

    val float = arg.toFloatOrNull()
    when {
        float == null -> {
            val msg = context.getTranslation("message.unknown.float")
                .withVariable(PLACEHOLDER_ARG, arg)
            sendRsp(context, msg)
        }
        float < start -> {
            val msg = context.getTranslation("message.tosmall.float")
                .withVariable(PLACEHOLDER_ARG, arg)
                .withVariable("min", start)
            sendRsp(context, msg)
        }
        float > end -> {
            val msg = context.getTranslation("message.tobig.float")
                .withVariable(PLACEHOLDER_ARG, arg)
                .withVariable("max", end)
            sendRsp(context, msg)
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
        val msg = context.getTranslation("message.unknown.boolean")
            .withVariable(PLACEHOLDER_ARG, arg)
        sendRsp(context, msg)
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
        val msg = context.getTranslation("message.unknown.datetime")
            .withVariable(PLACEHOLDER_ARG, arg)
        sendRsp(context, msg)
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
