package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.utils.message.sendSyntax

class PreVerificationLeaveMessageCommand : AbstractCommand("command.preverificationleavemessage") {

    init {
        id = 156
        name = "preVerificationLeaveMessage"
        aliases = arrayOf("pvlm")
        children = arrayOf(
            LeaveMessageCommand.SetContentArg(root, MessageType.PRE_VERIFICATION_LEAVE_MESSAGE),
            LeaveMessageCommand.EmbedArg(root, MessageType.PRE_VERIFICATION_LEAVE_MESSAGE),
            LeaveMessageCommand.AttachmentsArg(root, MessageType.PRE_VERIFICATION_LEAVE_MESSAGE),
            LeaveMessageCommand.ViewArg(root, MessageType.PRE_VERIFICATION_LEAVE_MESSAGE),
            LeaveMessageCommand.SetPingableArg(root, MessageType.PRE_VERIFICATION_LEAVE_MESSAGE)
        )
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context)
    }
}