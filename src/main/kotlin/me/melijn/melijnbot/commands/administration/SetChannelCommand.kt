package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.enums.ChannelType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.MESSAGE_UNKNOWN_CHANNELTYPE
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_CHANNEL
import me.melijn.melijnbot.objects.utils.*
import me.melijn.melijnbot.objects.utils.checks.getAndVerifyChannelByType

class SetChannelCommand : AbstractCommand("command.setchannel") {

    init {
        id = 33
        name = "setChannel"
        aliases = arrayOf("sc")
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }

        val type: ChannelType = getEnumFromArgNMessage(context, 0, MESSAGE_UNKNOWN_CHANNELTYPE) ?: return

        if (context.args.size > 1) {
            setChannel(context, type)
        } else {
            showChannel(context, type)
        }
    }

    private suspend fun showChannel(context: CommandContext, type: ChannelType) {
        val channel = context.guild.getAndVerifyChannelByType(context.daoManager, type)
        if (channel == null) {
            val msg = context.getTranslation("$root.show.unset")
                .replace("%channelType%", type.toString().toUpperWordCase())

            sendMsg(context, msg)
            return
        }

        val msg = context.getTranslation("$root.show.set")
            .replace("%channelType%", type.toString().toUpperWordCase())
            .replace(PLACEHOLDER_CHANNEL, channel.asTag)

        sendMsg(context, msg)
    }

    private suspend fun setChannel(context: CommandContext, type: ChannelType) {
        val channel = if (context.args[1].equals("null", true)) {
            null
        } else {
            getTextChannelByArgsNMessage(context, 1) ?: return
        }

        val channelWrapper = context.daoManager.channelWrapper


        val msg = if (channel == null) {
            channelWrapper.removeChannel(context.guildId, type)
            context.getTranslation("$root.unset")

        } else {
            channelWrapper.setChannel(context.guildId, type, channel.idLong)
            context.getTranslation("$root.set")
                .replace(PLACEHOLDER_CHANNEL, channel.asTag)
        }.replace("%channelType%", type.toString().toUpperWordCase())

        sendMsg(context, msg)
    }
}