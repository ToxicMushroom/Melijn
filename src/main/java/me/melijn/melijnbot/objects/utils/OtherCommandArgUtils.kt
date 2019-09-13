package me.melijn.melijnbot.objects.utils

import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.objects.translation.Translateable

suspend fun getIntegerFromArgNMessage(context: CommandContext, index: Int): Int? {
    if (argSizeCheckFailed(context, index)) return null
    val arg = context.args[index]

    val int = arg.toIntOrNull()
    if (int == null) {
        val msg = Translateable("message.unknown.integer")
            .string(context)
            .replace(PLACEHOLDER_ARG, arg)
        sendMsg(context, msg)
    }

    return int
}
suspend fun getBooleanFromArgNMessage(context: CommandContext, index: Int): Boolean? {
    if (argSizeCheckFailed(context, index)) return null
    val arg = context.args[index]

    val bool = when (arg.toLowerCase()) {
        "true", "yes", "on", "enable", "positive", "+" -> true
        "false", "no", "off", "disable", "negative", "-" -> false
        else -> null
    }
    if (bool == null) {
        val msg = Translateable("message.unknown.boolean")
            .string(context)
            .replace(PLACEHOLDER_ARG, arg)
        sendMsg(context, msg)
    }

    return bool
}

fun argSizeCheckFailed(context: CommandContext, index: Int): Boolean {
    return if (context.args.size <= index) {
        sendSyntax(context, context.commandOrder.last().syntax)
        true
    } else false
}
