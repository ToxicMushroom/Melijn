package me.melijn.melijnbot.objects.utils

import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.objects.translation.i18n

suspend fun getIntegerFromArgNMessage(context: CommandContext, index: Int): Int? {
    if (argSizeCheckFailed(context, index)) return null
    val arg = context.args[index]

    val int = arg.toIntOrNull()
    val language = context.getLanguage()
    if (int == null) {
        val msg = i18n.getTranslation(language, "message.unknown.integer")
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
        val language = context.getLanguage()
        val msg = i18n.getTranslation(language, "message.unknown.boolean")
            .replace(PLACEHOLDER_ARG, arg)
        sendMsg(context, msg)
    }

    return bool
}

suspend fun argSizeCheckFailed(context: CommandContext, index: Int): Boolean {
    return if (context.args.size <= index) {
        sendSyntax(context, context.commandOrder.last().syntax)
        true
    } else false
}
