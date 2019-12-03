package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext

class PunishmentPointGroupCommand : AbstractCommand("command.punishmentpointgroup") {

    init {
        id = 124
        name = "punishmentPointGroup"
        aliases = arrayOf("ppg", "PPGroup", "punishmentPG", "punishPG")
        commandCategory = CommandCategory.DEVELOPER
    }

    override suspend fun execute(context: CommandContext) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}