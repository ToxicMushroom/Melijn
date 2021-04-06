package me.melijn.melijnbot.internals.arguments

import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.remove
import me.melijn.melijnbot.internals.utils.withSafeVariable

abstract class CommandArgParser<T> {

    abstract suspend fun parse(context: ICommandContext, arg: String): T?

    suspend fun wrongArg(context: ICommandContext, arg: String) {
        val argType = this.javaClass.simpleName.remove("ArgParser").toLowerCase()
        val unknownMsg = context.getTranslation("message.unknown.$argType")
            .withSafeVariable(PLACEHOLDER_ARG, arg)
        sendRsp(context, unknownMsg)
    }
}