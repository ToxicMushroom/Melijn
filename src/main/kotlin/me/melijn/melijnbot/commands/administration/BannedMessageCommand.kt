package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.message.sendSyntax

class BannedMessageCommand : AbstractCommand("command.bannedmessage") {

    init {
        id = 162
        name = "bannedMessage"
        aliases = arrayOf("bm")
        children = arrayOf(
            LeaveMessageCommand.SetContentArg(root, MessageType.BANNED),
            LeaveMessageCommand.EmbedArg(root, MessageType.BANNED),
            LeaveMessageCommand.AttachmentsArg(root, MessageType.BANNED),
            LeaveMessageCommand.ViewArg(root, MessageType.BANNED),
            LeaveMessageCommand.SetPingableArg(root, MessageType.BANNED)
        )
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context)
    }
}