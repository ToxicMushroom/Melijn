package me.melijn.melijnbot.commands.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.sendMsg

class SetStreamUrlCommand : AbstractCommand("command.setstreamurl") {

    init {
        id = 120
        name = "setStreamUrl"
        aliases = arrayOf("ssu")
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        val wrapper = context.daoManager.streamUrlWrapper
        val language = context.getLanguage()
        if (context.args.isEmpty()) {
            val url = wrapper.streamUrlCache.get(context.guildId).await()
            val part = if (url.isBlank()) "unset" else "set"
            val msg = i18n.getTranslation(language, "$root.show.$part")
                .replace("%url%", url)
            sendMsg(context, msg)
            return
        }

        val msg = if (context.rawArg == "null") {
            wrapper.removeUrl(context.guildId)
            i18n.getTranslation(language, "$root.unset")
        } else {
            wrapper.setUrl(context.guildId, context.rawArg)
            i18n.getTranslation(language, "$root.set")
                .replace(PLACEHOLDER_ARG, context.rawArg)
        }

        sendMsg(context, msg)
    }
}