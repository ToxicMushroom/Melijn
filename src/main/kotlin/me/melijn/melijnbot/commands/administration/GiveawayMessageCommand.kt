package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.sendSyntax

class GiveawayMessageCommand : AbstractCommand("command.giveawaymessage") {

    init {
        id = 181
        name = "giveawayMessage"
        aliases = arrayOf("gm")
        children = arrayOf(
            LeaveMessageCommand.SetContentArg(root, MessageType.GIVEAWAY),
            LeaveMessageCommand.EmbedArg(root, MessageType.GIVEAWAY),
            LeaveMessageCommand.AttachmentsArg(root, MessageType.GIVEAWAY),
            LeaveMessageCommand.ViewArg(root, MessageType.GIVEAWAY)
        )
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context)
    }
}