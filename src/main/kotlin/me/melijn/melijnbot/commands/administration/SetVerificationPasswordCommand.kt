package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.withVariable

class SetVerificationPasswordCommand : AbstractCommand("command.setverificationpassword") {

    init {
        id = 42
        name = "setVerificationPassword"
        aliases = arrayOf("svp")
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        val wrapper = context.daoManager.verificationPasswordWrapper
        if (context.args.isEmpty()) {
            val password = wrapper.getPassword(context.guildId)
            val part = if (password.isBlank()) {
                "unset"
            } else {
                "set"
            }

            val msg = context.getTranslation("$root.show.$part")
                .withVariable("password", password)
            sendRsp(context, msg)
            return
        }

        val msg = if (context.rawArg == "null") {
            wrapper.remove(context.guildId)
            context.getTranslation("$root.unset")
        } else {
            wrapper.set(context.guildId, context.rawArg)
            context.getTranslation("$root.set")
                .withVariable(PLACEHOLDER_ARG, context.rawArg)
        }

        sendRsp(context, msg)
    }
}