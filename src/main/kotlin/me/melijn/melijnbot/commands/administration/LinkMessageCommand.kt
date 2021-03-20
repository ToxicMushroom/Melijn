package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.PLACEHOLDER_PREFIX
import me.melijn.melijnbot.internals.translation.MESSAGE_UNKNOWN_MESSAGETYPE
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendRsp

class LinkMessageCommand : AbstractCommand("command.linkmessage") {

    init {
        name = "linkMessage"
        aliases = arrayOf("lm", "linkMsg")
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: ICommandContext) {
        val type = getEnumFromArgNMessage<MessageType>(context, 0, MESSAGE_UNKNOWN_MESSAGETYPE) ?: return
        val msgName = getStringFromArgsNMessage(context, 1, 1, 64) ?: return
        val guildId = context.guildId
        val daoManager = context.daoManager
        val messages = daoManager.messageWrapper.getMessages(guildId)
        if (msgName.isInside(messages, true)) {
            daoManager.linkedMessageWrapper.setMessage(guildId, type, msgName)

            val msg = context.getTranslation("$root.linked")
                .withSafeVariable("msgType", type.toUCC())
                .withSafeVariable("msgName", msgName)
            sendRsp(context, msg)
        } else {
            val msg = context.getTranslation("${context.commandOrder.first().root}.msgnoexist")
                .withSafeVariable("msg", msgName)
                .withSafeVariable(PLACEHOLDER_PREFIX, context.usedPrefix)
            sendRsp(context, msg)
        }
    }
}