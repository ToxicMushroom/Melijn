package me.melijn.melijnbot.commands.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.objects.utils.message.sendRsp
import me.melijn.melijnbot.objects.utils.withVariable

class SetStreamUrlCommand : AbstractCommand("command.setstreamurl") {

    init {
        id = 120
        name = "setStreamUrl"
        aliases = arrayOf("ssu")
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        val wrapper = context.daoManager.streamUrlWrapper
        if (context.args.isEmpty()) {
            val url = wrapper.streamUrlCache.get(context.guildId).await()
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