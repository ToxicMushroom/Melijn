package me.melijn.melijnbot.commands.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.RunCondition
import me.melijn.melijnbot.objects.utils.*

class SetRemoveResponsesCommand : AbstractCommand("command.setremoveresponses") {

    init {
        id = 187
        name = "setRemoveResponses"
        aliases = arrayOf("srr")
        runConditions = arrayOf(RunCondition.GUILD_SUPPORTER)
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }

        val channel = getTextChannelByArgsNMessage(context, 0) ?: return
        val wrapper = context.daoManager.removeResponseWrapper

        if (context.args.size == 1) {
            val map = wrapper.removeResponseCache.get(context.guildId).await()
            val msg = if (map.containsKey(channel.idLong)) {
                context.getTranslation("$root.show.set")
                    .withVariable("channel", channel.asTag)
                    .withVariable("seconds", map[channel.idLong] ?: 1)
            } else {
                context.getTranslation("$root.show.empty")
                    .withVariable("channel", channel.asTag)
            }
            sendMsg(context, msg)

        } else {
            if (context.args[1] == "null") {
                wrapper.remove(context.guildId, channel.idLong)

                val msg = context.getTranslation("$root.unset")
                    .withVariable("channel", channel.asTag)
                sendMsg(context, msg)
            } else {
                val seconds = getIntegerFromArgNMessage(context, 1, 1, 300) ?: return
                wrapper.set(context.guildId, channel.idLong, seconds)

                val msg = context.getTranslation("$root.set")
                    .withVariable("channel", channel.asTag)
                    .withVariable("seconds", seconds)
                sendMsg(context, msg)
            }

        }
    }
}