package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.utils.message.sendSyntax

class SpamSettingsCommand : AbstractCommand("command.spamgroup") {

    init {
        id = 150
        name = "spamSettings"
        aliases = arrayOf("ss", "spamConfig", "spamInfo")
        commandCategory = CommandCategory.ADMINISTRATION
        children = arrayOf(

        )
    }

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context)
    }
}