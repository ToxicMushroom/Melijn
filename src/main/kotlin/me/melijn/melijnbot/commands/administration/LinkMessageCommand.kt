package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.PLACEHOLDER_PREFIX
import me.melijn.melijnbot.internals.utils.getStringFromArgsNMessage
import me.melijn.melijnbot.internals.utils.isInside
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.withSafeVariable
import me.melijn.melijnbot.internals.utils.withVariable
import net.dv8tion.jda.api.entities.Guild

class LinkMessageCommand : AbstractCommand("command.linkmessage") {

    init {
        name = "linkMessage"
        aliases = arrayOf("lm", "linkMsg")
        commandCategory = CommandCategory.ADMINISTRATION
    }


    override suspend fun execute(context: ICommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }

        val matchingEnums: List<MessageType> = MessageType.getMatchingTypesFromNode(context.args[0])
        if (matchingEnums.isEmpty()) {
            sendSyntax(context)
            return
        }
        if (matchingEnums.size > 1) handleEnums(context, matchingEnums)
        else handleEnum(context, matchingEnums[0])

    }

    companion object {
        suspend fun Guild.getAndVerifyMsgName(daoManager: DaoManager, messageType: MessageType): String? {
            val msg = daoManager.linkedMessageWrapper.getMessage(this.idLong, messageType)
            val messages = daoManager.messageWrapper.getMessages(this.idLong)
            return if (msg == null || !messages.contains(msg)) {
                null
            } else {
                msg
            }
        }
    }

    private suspend fun handleEnum(context: ICommandContext, messageType: MessageType) {
        if (context.args.size > 1) {
            linkMessage(context, messageType)
        } else {
            displayLink(context, messageType)
        }
    }

    private suspend fun displayLink(context: ICommandContext, messageType: MessageType) {
        val daoManager = context.daoManager
        val msgName = context.guild.getAndVerifyMsgName(daoManager, messageType)

        val msg = (if (msgName != null) {
            context.getTranslation("$root.show.set.single")
                .withVariable("msgName", msgName)
        } else {
            context.getTranslation("$root.show.unset.single")
        }).withVariable("messageType", messageType.text)

        sendRsp(context, msg)
    }


    private suspend fun linkMessage(context: ICommandContext, messageType: MessageType) {
        if (context.args.size < 2) {
            sendSyntax(context)
            return
        }

        val daoWrapper = context.daoManager.linkedMessageWrapper
        val msg = if (context.args[1].equals("null", true)) {

            daoWrapper.removeMessage(context.guildId, messageType)
            context.getTranslation("$root.unset.single")
                .withVariable("messageType", messageType.text)
        } else {
            val msgName = getMsgNameFromArgsNMessage(context, 1) ?: return
            daoWrapper.setMessage(context.guildId, messageType, msgName)

            context.getTranslation("$root.set.single")
                .withVariable("messageType", messageType.text)
                .withVariable("msgName", msgName)

        }
        sendRsp(context, msg)
    }

    private suspend fun handleEnums(context: ICommandContext, logChannelTypes: List<MessageType>) {
        if (context.args.size > 1) {
            linkMessages(context, logChannelTypes)
        } else {
            displayLinks(context, logChannelTypes)
        }
    }

    private suspend fun displayLinks(context: ICommandContext, logChannelTypes: List<MessageType>) {
        val daoManager = context.daoManager
        val title = context.getTranslation("$root.show.multiple")
            .withVariable("messageCount", logChannelTypes.size.toString())
            .withSafeVariable("messageType", context.args[0])

        val lines = emptyList<String>().toMutableList()

        for (type in logChannelTypes) {
            val msgName = context.guild.getAndVerifyMsgName(daoManager, type)
            lines += "${type.text}: " + (msgName ?: "/")
        }

        val content = lines.joinToString(separator = "\n", prefix = "\n")
        val msg = title + content
        sendRsp(context, msg)
    }

    private suspend fun linkMessages(context: ICommandContext, messageTypes: List<MessageType>) {
        if (context.args.size < 2) {
            sendSyntax(context)
            return
        }

        val daoWrapper = context.daoManager.linkedMessageWrapper
        val msg = if (context.args[1].equals("null", true)) {
            daoWrapper.removeMessages(context.guildId, messageTypes)

            context.getTranslation("$root.unset.multiple")
                .withVariable("messageCount", messageTypes.size)
                .withSafeVariable("messageType", context.args[0])
        } else {
            val msgName = getMsgNameFromArgsNMessage(context, 1) ?: return
            daoWrapper.setMessages(context.guildId, messageTypes, msgName)


            context.getTranslation("$root.set.multiple")
                .withVariable("messageCount", messageTypes.size)
                .withSafeVariable("messageType", context.args[0])
                .withVariable("msgName", msgName)

        }
        sendRsp(context, msg)
    }
}

suspend fun getMsgNameFromArgsNMessage(context: ICommandContext, index: Int): String? {
    val msgName = getStringFromArgsNMessage(context, index, 1, 64) ?: return null
    val messages = context.daoManager.messageWrapper.getMessages(context.guildId)
    if (!msgName.isInside(messages, true)) {
        val msg = context.getTranslation("message.msgnoexist")
            .withSafeVariable("msg", msgName)
            .withSafeVariable(PLACEHOLDER_PREFIX, context.usedPrefix)
        sendRsp(context, msg)
        return null
    }
    return msgName
}