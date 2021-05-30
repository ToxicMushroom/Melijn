package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.enums.ChannelType
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.translation.MESSAGE_UNKNOWN_CHANNELTYPE
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_CHANNEL
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyChannelByType
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax

class SetChannelCommand : AbstractCommand("command.setchannel") {

    init {
        id = 33
        name = "setChannel"
        aliases = arrayOf("sc")
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: ICommandContext) {
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

    private suspend fun showChannel(context: ICommandContext, type: ChannelType) {
        val channel = context.guild.getAndVerifyChannelByType(context.daoManager, type)
        if (channel == null) {
            val msg = context.getTranslation("$root.show.unset")
                .withVariable("channelType", type.toUCC())

            sendRsp(context, msg)
            return
        }

        val msg = context.getTranslation("$root.show.set")
            .withVariable("channelType", type.toUCC())
            .withVariable(PLACEHOLDER_CHANNEL, channel.asTag)

        sendRsp(context, msg)
    }

    private suspend fun setChannel(context: ICommandContext, type: ChannelType) {
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
                .withVariable(PLACEHOLDER_CHANNEL, channel.asTag)
        }.withVariable("channelType", type.toUCC())

        sendRsp(context, msg)
    }
}