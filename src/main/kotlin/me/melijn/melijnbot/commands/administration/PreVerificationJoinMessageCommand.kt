package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.sendSyntax

class PreVerificationJoinMessageCommand : AbstractCommand("command.preverificationjoinmessage") {

    init {
        id = 147
        name = "preVerificationJoinMessage"
        aliases = arrayOf("pvjm")
        children = arrayOf(
            LeaveMessageCommand.SetContentArg(root, MessageType.PRE_VERIFICATION_JOIN_MESSAGE),
            LeaveMessageCommand.EmbedArg(root, MessageType.PRE_VERIFICATION_JOIN_MESSAGE),
            LeaveMessageCommand.AttachmentsArg(root, MessageType.PRE_VERIFICATION_JOIN_MESSAGE),
            LeaveMessageCommand.ViewArg(root, MessageType.PRE_VERIFICATION_JOIN_MESSAGE),
            LeaveMessageCommand.SetPingableArg(root, MessageType.PRE_VERIFICATION_JOIN_MESSAGE)
        )
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context)
    }
}