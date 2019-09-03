package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.commandutil.administration.MessageCommandUtil
import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext

class JoinMessageCommand : AbstractCommand("command.join") {

    init {
        id = 34
        name = "joinMessage"
        aliases = arrayOf("jm")
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            MessageCommandUtil.showMessageContent(this, context, MessageType.JOIN)
        } else {
            MessageCommandUtil.setMessageContent(this, context, MessageType.JOIN)
        }
    }


}