package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.commands.utility.SetPrivateEmbedColorCommand
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext

class SetEmbedColorCommand : AbstractCommand("command.setembedcolor") {

    init {
        id = 77
        name = "setEmbedColor"
        aliases = arrayOf("sec")
        commandCategory = CommandCategory.ADMINISTRATION
    }

    suspend fun execute(context: ICommandContext) {
        SetPrivateEmbedColorCommand.setEmbedColor(context) { it.guildId }
    }
}