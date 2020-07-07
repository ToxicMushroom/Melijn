package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.message.sendRsp

class SupportCommand : AbstractCommand("command.support") {

    init {
        id = 148
        name = "support"
        aliases = arrayOf("supportServer")
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: CommandContext) {
        val msg = context.getTranslation("$root.server")
        sendRsp(context, msg)
    }
}