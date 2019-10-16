package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.sendSyntax

class JoinMessageCommand : AbstractCommand("command.joinmessage") {

    init {
        id = 34
        name = "joinMessage"
        aliases = arrayOf("jm")
        commandCategory = CommandCategory.ADMINISTRATION
        children = arrayOf(
            LeaveMessageCommand.SetContentArg(root, MessageType.JOIN),
            LeaveMessageCommand.EmbedArg(root, MessageType.JOIN),
            LeaveMessageCommand.AttachmentsArg(root, MessageType.JOIN)
        )
    }

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context, syntax)
    }
}