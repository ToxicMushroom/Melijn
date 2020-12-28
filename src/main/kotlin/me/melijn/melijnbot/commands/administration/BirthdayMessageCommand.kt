package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.message.sendSyntax

class BirthdayMessageCommand : AbstractCommand("command.birthdaymessage") {

    init {
        id = 142
        name = "birthdayMessage"
        aliases = arrayOf("bm")
        children = arrayOf(
            LeaveMessageCommand.SetContentArg(root, MessageType.BIRTHDAY),
            LeaveMessageCommand.EmbedArg(root, MessageType.BIRTHDAY),
            LeaveMessageCommand.AttachmentsArg(root, MessageType.BIRTHDAY),
            LeaveMessageCommand.ViewArg(root, MessageType.BIRTHDAY),
            LeaveMessageCommand.SetPingableArg(root, MessageType.BIRTHDAY)
        )
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: ICommandContext) {
        sendSyntax(context)
    }
}