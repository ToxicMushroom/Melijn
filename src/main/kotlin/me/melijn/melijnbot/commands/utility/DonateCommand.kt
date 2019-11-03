package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.sendMsg

class DonateCommand : AbstractCommand("command.donate") {

    init {
        id = 97
        name = "donate"
        aliases = arrayOf("patreon", "patron", "sponsor")
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: CommandContext) {
        val msg = i18n.getTranslation(context, "$root.response")
            .replace("%url%", "https://patreon.com/")
        sendMsg(context, msg)
    }
}