package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.message.sendSyntax

class JoinMessageCommand : AbstractCommand("command.joinmessage") {

    init {
        id = 34
        name = "joinMessage"
        aliases = arrayOf("jm")
        children = arrayOf(
            LeaveMessageCommand.SetContentArg(root, MessageType.JOIN),
            LeaveMessageCommand.EmbedArg(root, MessageType.JOIN),
            LeaveMessageCommand.AttachmentsArg(root, MessageType.JOIN),
            LeaveMessageCommand.ViewArg(root, MessageType.JOIN),
            LeaveMessageCommand.SetPingableArg(root, MessageType.JOIN)
        )
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: ICommandContext) {
        sendSyntax(context)
    }
}