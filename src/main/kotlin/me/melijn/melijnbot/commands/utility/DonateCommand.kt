package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.RunCondition
import me.melijn.melijnbot.objects.utils.sendMsg

class DonateCommand : AbstractCommand("command.donate") {

    init {
        id = 97
        name = "donate"
        aliases = arrayOf("patreon", "patron", "sponsor")
        children = arrayOf(LinkGuildArg(root))
        commandCategory = CommandCategory.UTILITY
    }

    class LinkGuildArg(parent: String) : AbstractCommand("$parent.linkGuild") {

        init {
            name = "linkGuild"
            aliases = arrayOf("lg", "linkServer", "ls")
            runConditions = arrayOf(RunCondition.USER_SUPPORTER)
        }

        override suspend fun execute(context: CommandContext) {
            val wrapper = context.daoManager.supporterWrapper
            wrapper.supporters.forEach { supporter ->
                supporter.startMillis
            }
        }
    }

    override suspend fun execute(context: CommandContext) {
        val msg = context.getTranslation("$root.response")
            .replace("%url%", "https://patreon.com/melijn")
        sendMsg(context, msg)
    }
}