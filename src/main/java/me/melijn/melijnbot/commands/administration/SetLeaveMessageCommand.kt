package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.commandutil.administration.MessageCommandUtil
import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext

class SetLeaveMessageCommand  : AbstractCommand("command.leave") {

    init {
        id = 34
        name = "setLeaveMessage"
        aliases = arrayOf("slm")
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            MessageCommandUtil.showMessage(this, context, MessageType.LEAVE)
        } else {
            MessageCommandUtil.setMessage(this, context, MessageType.LEAVE)
        }
    }
}