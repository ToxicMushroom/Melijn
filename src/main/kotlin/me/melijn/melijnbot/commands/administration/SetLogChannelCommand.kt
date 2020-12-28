package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_CHANNEL
import me.melijn.melijnbot.internals.utils.asTag
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyLogChannelByType
import me.melijn.melijnbot.internals.utils.getTextChannelByArgsNMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.withVariable

class SetLogChannelCommand : AbstractCommand("command.setlogchannel") {

    init {
        id = 21
        name = "setLogChannel"
        aliases = arrayOf("slc")
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: ICommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }

        val matchingEnums: List<LogChannelType> = LogChannelType.getMatchingTypesFromNode(context.args[0])
        if (matchingEnums.isEmpty()) {
            sendSyntax(context)
            return
        }
        if (matchingEnums.size > 1)
            handleEnums(context, matchingEnums)
        else handleEnum(context, matchingEnums[0])

    }

    private suspend fun handleEnum(context: ICommandContext, logChannelType: LogChannelType) {
        if (context.args.size > 1) {
            setChannel(context, logChannelType)
        } else {
            displayChannel(context, logChannelType)
        }
    }

    private suspend fun displayChannel(context: ICommandContext, logChannelType: LogChannelType) {
        val daoManager = context.daoManager
        val channel = context.guild.getAndVerifyLogChannelByType(daoManager, logChannelType)

        val msg = (if (channel != null) {
            context.getTranslation("$root.show.set.single")
                .withVariable(PLACEHOLDER_CHANNEL, channel.asTag)
        } else {
            context.getTranslation("$root.show.unset.single")
        }).withVariable("logChannelType", logChannelType.text)

        sendRsp(context, msg)
    }


    private suspend fun setChannel(context: ICommandContext, logChannelType: LogChannelType) {
        if (context.args.size < 2) {
            sendSyntax(context)
            return
        }

        val daoWrapper = context.daoManager.logChannelWrapper
        val msg = if (context.args[1].equals("null", true)) {

            daoWrapper.removeChannel(context.guildId, logChannelType)
            context.getTranslation("$root.unset.single")
                .withVariable("logChannelType", logChannelType.text)
        } else {
            val channel = getTextChannelByArgsNMessage(context, 1) ?: return
            daoWrapper.setChannel(context.guildId, logChannelType, channel.idLong)

            context.getTranslation("$root.set.single")
                .withVariable("logChannelType", logChannelType.text)
                .withVariable(PLACEHOLDER_CHANNEL, channel.asTag)

        }
        sendRsp(context, msg)
    }

    private suspend fun handleEnums(context: ICommandContext, logChannelTypes: List<LogChannelType>) {
        if (context.args.size > 1) {
            setChannels(context, logChannelTypes)
        } else {
            displayChannels(context, logChannelTypes)
        }
    }

    private suspend fun displayChannels(context: ICommandContext, logChannelTypes: List<LogChannelType>) {
        val daoManager = context.daoManager
        val title = context.getTranslation("$root.show.multiple")
            .withVariable("channelCount", logChannelTypes.size.toString())
            .withVariable("logChannelTypeNode", context.args[0])

        val lines = emptyList<String>().toMutableList()

        for (type in logChannelTypes) {
            val channel = context.guild.getAndVerifyLogChannelByType(daoManager, type)
            lines += "${type.text}: " + (channel?.asMention ?: "/")
        }

        val content = lines.joinToString(separator = "\n", prefix = "\n")
        val msg = title + content
        sendRsp(context, msg)
    }

    private suspend fun setChannels(context: ICommandContext, logChannelTypes: List<LogChannelType>) {
        if (context.args.size < 2) {
            sendSyntax(context)
            return
        }

        val daoWrapper = context.daoManager.logChannelWrapper
        val msg = if (context.args[1].equals("null", true)) {

            daoWrapper.removeChannels(context.guildId, logChannelTypes)

            context.getTranslation("$root.unset.multiple")
                .withVariable("channelCount", logChannelTypes.size.toString())
                .withVariable("logChannelTypeNode", context.args[0])
        } else {
            val channel = getTextChannelByArgsNMessage(context, 1) ?: return
            daoWrapper.setChannels(context.guildId, logChannelTypes, channel.idLong)


            context.getTranslation("$root.set.multiple")
                .withVariable("channelCount", logChannelTypes.size.toString())
                .withVariable("logChannelTypeNode", context.args[0])
                .withVariable(PLACEHOLDER_CHANNEL, channel.asTag)

        }
        sendRsp(context, msg)
    }
}