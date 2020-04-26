package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.sendSyntax

class BanMessageCommand : AbstractCommand("command.banmessage") {

    init {
        id = 162
        name = "banMessage"
        aliases = arrayOf("bm")
        children = arrayOf(
            LeaveMessageCommand.SetContentArg(root, MessageType.BANNED),
            LeaveMessageCommand.EmbedArg(root, MessageType.BANNED),
            LeaveMessageCommand.AttachmentsArg(root, MessageType.BANNED),
            LeaveMessageCommand.ViewArg(root, MessageType.BANNED)
        )
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context)
    }
}