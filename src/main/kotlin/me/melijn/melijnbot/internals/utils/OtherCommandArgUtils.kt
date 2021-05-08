package me.melijn.melijnbot.internals.utils

import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import java.time.Instant
import java.util.*


// warning the scanned numbers are decreased by one
suspend fun getIntegersFromArgsNMessage(context: ICommandContext, index: Int, start: Int, end: Int): IntArray? {

    val args = context.getRawArgPart(index).remove(" ").split(",")
    val ints = mutableListOf<Int>()
    try {
        for (arg in args) {
            if (arg.contains("-")) {

                val list: List<String> = arg.split("-")
                if (list.size == 2) {
                    val first = list[0]
                    val second = list[1]
                    if (first.isNumber() && second.isNumber()) {
                        val firstInt = first.toInt()
                        val secondInt = second.toInt()
                        for (i in firstInt..secondInt)
                            ints.addIfNotPresent(i - 1)
                    }
                } else {
                    val msg = context.getTranslation("message.unknown.numberornumberrange")
                        .withSafeVariable(PLACEHOLDER_ARG, arg)
                    sendRsp(context, msg)
                    return null
                }
            } else if (arg.isNumber()) {
                if (!ints.contains(arg.toInt() - 1)) {
                    ints.add(arg.toInt() - 1)
                }
            } else {
                val msg = context.getTranslation("message.unknown.numberornumberrange")
                    .withSafeVariable(PLACEHOLDER_ARG, arg)
                sendRsp(context, msg)
                return null
            }
        }

    } catch (e: NumberFormatException) {
        val msg = context.getTranslation("message.numbertobig")
            .withVariable(PLACEHOLDER_ARG, e.message ?: "/")
        sendRsp(context, msg)
        return null
    }
    for (i in ints) {
        if (i < (start - 1) || i > (end - 1)) {
            val msg = context.getTranslation("message.number.notinrange")
                .withSafeVariable(PLACEHOLDER_ARG, i + 1)
                .withVariable("start", start)
                .withVariable("end", end)
            sendRsp(context, msg)
            return null
        }
    }
    return ints.toIntArray()
}

suspend fun getIntegerFromArgNMessage(
    context: ICommandContext,
    index: Int,
    start: Int = Integer.MIN_VALUE,
    end: Int = Integer.MAX_VALUE
): Int? {
    if (argSizeCheckFailed(context, index)) return null
    val arg = context.args[index]

    val int = arg.toIntOrNull()
    when {
        int == null -> {
            val msg = context.getTranslation("message.unknown.integer")
                .withSafeVariable(PLACEHOLDER_ARG, arg)
            sendRsp(context, msg)
        }
        int < start -> {
            val msg = context.getTranslation("message.tosmall.integer")
                .withSafeVariable(PLACEHOLDER_ARG, arg)
                .withVariable("min", start)
            sendRsp(context, msg)
            return null
        }
        int > end -> {
            val msg = context.getTranslation("message.tobig.integer")
                .withSafeVariable(PLACEHOLDER_ARG, arg)
                .withVariable("max", end)
            sendRsp(context, msg)
            return null
        }
    }

    return int
}

suspend fun getFloatFromArgNMessage(
    context: ICommandContext,
    index: Int,
    start: Float = Float.MIN_VALUE,
    end: Float = Float.MAX_VALUE
): Float? {
    if (argSizeCheckFailed(context, index)) return null
    val arg = context.args[index]

    val float = arg.toFloatOrNull()
    when {
        float == null -> {
            val msg = context.getTranslation("message.unknown.float")
                .withSafeVariable(PLACEHOLDER_ARG, arg)
            sendRsp(context, msg)
        }
        float < start -> {
            val msg = context.getTranslation("message.tosmall.float")
                .withSafeVariable(PLACEHOLDER_ARG, arg)
                .withVariable("min", start)
            sendRsp(context, msg)
        }
        float > end -> {
            val msg = context.getTranslation("message.tobig.float")
                .withSafeVariable(PLACEHOLDER_ARG, arg)
                .withVariable("max", end)
            sendRsp(context, msg)
        }
    }

    return float
}

suspend fun getBooleanFromArgN(context: ICommandContext, index: Int): Boolean? {
    if (argSizeCheckFailed(context, index, true)) return null
    val arg = context.args[index]

    return when (arg.lowercase()) {
        "true", "yes", "on", "enable", "enabled", "positive", "+" -> true
        "false", "no", "off", "disable", "disabled", "negative", "-" -> false
        else -> null
    }
}

suspend fun getBooleanFromArgNMessage(context: ICommandContext, index: Int): Boolean? {
    if (argSizeCheckFailed(context, index)) return null
    val arg = context.args[index]

    val bool = getBooleanFromArgN(context, index)
    if (bool == null) {
        val msg = context.getTranslation("message.unknown.boolean")
            .withSafeVariable(PLACEHOLDER_ARG, arg)
        sendRsp(context, msg)
    }

    return bool
}

// Returns in epoch millis at UTC+0
suspend fun getDateTimeFromArgNMessage(context: ICommandContext, index: Int): Long? {
    if (argSizeCheckFailed(context, index)) return null
    val arg = context.args[index]

    if (arg.equals("current", true)) {
        return Instant.now().toEpochMilli()
    }

    val dateTime = getEpochMillisFromArgN(context, index)
    if (dateTime == null) {
        val msg = context.getTranslation("message.unknown.datetime")
            .withSafeVariable(PLACEHOLDER_ARG, arg)
        sendRsp(context, msg)
    }

    return dateTime
}


fun getEpochMillisFromArgN(context: ICommandContext, index: Int): Long? {
    val arg = context.args[index]
    return try {
        (simpleDateTimeFormatter.parse(arg) as Date).time
    } catch (e: Exception) {
        null
    }
}

suspend fun argSizeCheckFailed(context: ICommandContext, index: Int, silent: Boolean = false): Boolean {
    return if (context.args.size <= index) {
        if (!silent) sendSyntax(context, context.commandOrder.last().syntax)
        true
    } else {
        false
    }
}
