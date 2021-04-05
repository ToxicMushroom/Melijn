package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.withVariable

class SetStreamUrlCommand : AbstractCommand("command.setstreamurl") {

    init {
        id = 120
        name = "setStreamUrl"
        aliases = arrayOf("ssu")
        commandCategory = CommandCategory.ADMINISTRATION
    }

    suspend fun execute(context: ICommandContext) {
        val wrapper = context.daoManager.streamUrlWrapper
        if (context.args.isEmpty()) {
            val url = wrapper.getUrl(context.guildId)
            val part = if (url.isBlank()) "unset" else "set"

            val msg = context.getTranslation("$root.show.$part")
                .withVariable("url", url)
            sendRsp(context, msg)
            return
        }

        val msg = if (context.rawArg == "null") {
            wrapper.removeUrl(context.guildId)
            context.getTranslation("$root.unset")
        } else {
            wrapper.setUrl(context.guildId, context.rawArg)
            context.getTranslation("$root.set")
                .withVariable(PLACEHOLDER_ARG, context.rawArg)
        }

        sendRsp(context, msg)
    }
}