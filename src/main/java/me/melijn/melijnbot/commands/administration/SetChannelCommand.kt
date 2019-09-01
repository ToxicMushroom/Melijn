package me.melijn.melijnbot.commands.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.enums.ChannelType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.objects.translation.Translateable
import me.melijn.melijnbot.objects.utils.*

class SetChannelCommand : AbstractCommand("command.setchannel") {

    init {
        id = 33
        name = "setChannel"
        aliases = arrayOf("sc")
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context, syntax)
            return
        }

        val type = enumValueOrNull<ChannelType>(context.args[0])
        if (type == null) {
            val msg = Translateable("message.unknown.channeltype").string(context)
                    .replace(PLACEHOLDER_ARG, context.args[0])
            sendMsg(context, msg)
            return
        }

        if (context.args.size > 1) {
            setChannel(context, type)
        } else {
            showChannel(context, type)
        }
    }

    private suspend fun showChannel(context: CommandContext, type: ChannelType) {
        val channelWrapper = context.daoManager.channelWrapper
        val channelId = channelWrapper.logChannelCache.get(Pair(context.getGuildId(), type)).await()
        val channel = if (channelId == -1L) null else context.getGuild().getTextChannelById(channelId)
        if (channel == null) {
            val msg = Translateable("$root.show.unset")
                    .string(context)
                    .replace("%channelType%", type.toString())

            sendMsg(context, msg)
            return
        }
        val msg = Translateable("$root.show.set")
                .string(context)
                .replace("%channelType%", type.toString())
                .replace("%channel%", channel.asTag)

        sendMsg(context, msg)
    }

    private suspend fun setChannel(context: CommandContext, type: ChannelType) {
        val channel = getTextChannelByArgsNMessage(context, 2) ?: return
        val channelWrapper = context.daoManager.channelWrapper
        channelWrapper.setChannel(context.getGuildId(), type, channel.idLong)

        val msg = Translateable("$root.set")
                .string(context)
                .replace("%channel%", channel.asTag)
        sendMsg(context, msg)
    }
}