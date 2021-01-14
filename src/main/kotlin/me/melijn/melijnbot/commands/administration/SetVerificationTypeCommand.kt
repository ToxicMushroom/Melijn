package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.enums.VerificationType
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.internals.utils.enumValueOrNull
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.toUCC
import me.melijn.melijnbot.internals.utils.withVariable

class SetVerificationTypeCommand : AbstractCommand("command.setverificationtype") {

    init {
        id = 43
        name = "setVerificationType"
        aliases = arrayOf("svt")
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: ICommandContext) {
        val wrapper = context.daoManager.verificationTypeWrapper
        if (context.args.isEmpty()) {
            val type = wrapper.getType(context.guildId)
            val part = if (type == VerificationType.NONE) "unset" else "set"
            val msg = context.getTranslation("$root.show.$part")
                .withVariable("type", type.toUCC())
            sendRsp(context, msg)
            return
        }


        val type = enumValueOrNull<VerificationType>(context.rawArg)
        val msg = if (context.rawArg == "null" || type == VerificationType.NONE) {
            wrapper.removeType(context.guildId)
            context.getTranslation("$root.unset")
        } else if (type == null) {
            context.getTranslation("message.unknown.verificationtype")
                .withVariable(PLACEHOLDER_ARG, context.rawArg)
        } else {
            wrapper.setType(context.guildId, type)
            context.getTranslation("$root.set")
                .withVariable(PLACEHOLDER_ARG, type.toUCC())
        }

        sendRsp(context, msg)
    }
}