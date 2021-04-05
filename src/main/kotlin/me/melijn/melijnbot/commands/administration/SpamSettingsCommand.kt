package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
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

    suspend fun execute(context: ICommandContext) {
        sendSyntax(context)
    }
}