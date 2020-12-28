package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.message.sendSyntax

class KickedMessageCommand : AbstractCommand("command.kickedmessage") {

    init {
        id = 163
        name = "kickedMessage"
        aliases = arrayOf("km")
        children = arrayOf(
            LeaveMessageCommand.SetContentArg(root, MessageType.KICKED),
            LeaveMessageCommand.EmbedArg(root, MessageType.KICKED),
            LeaveMessageCommand.AttachmentsArg(root, MessageType.KICKED),
            LeaveMessageCommand.ViewArg(root, MessageType.KICKED),
            LeaveMessageCommand.SetPingableArg(root, MessageType.KICKED)
        )
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: ICommandContext) {
        sendSyntax(context)
    }
}