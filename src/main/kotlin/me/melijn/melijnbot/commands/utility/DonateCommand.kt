package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.RunCondition
import me.melijn.melijnbot.objects.utils.getDurationString
import me.melijn.melijnbot.objects.utils.sendMsg

class DonateCommand : AbstractCommand("command.donate") {

    init {
        id = 97
        name = "donate"
        aliases = arrayOf("patreon", "patron", "sponsor")
        children = arrayOf(LinkGuildArg(root))
        commandCategory = CommandCategory.UTILITY
    }

    class LinkGuildArg(parent: String) : AbstractCommand("$parent.linkguild") {

        init {
            name = "linkGuild"
            aliases = arrayOf("lg", "linkServer", "ls")
            runConditions = arrayOf(RunCondition.GUILD, RunCondition.USER_SUPPORTER)
        }

        override suspend fun execute(context: CommandContext) {
            val wrapper = context.daoManager.supporterWrapper
            val supporter = wrapper.supporters.firstOrNull { s -> s.userId == context.authorId } ?: return
            if (supporter.lastServerPickTime <= System.currentTimeMillis() - 1_209_600_000) {
                wrapper.setGuild(context.authorId, context.guildId)

                val msg = context.getTranslation("$root.selected")
                    .replace("%guild%", context.guild.name)
                sendMsg(context, msg)
            } else {
                val msg = context.getTranslation("$root.oncooldown")
                    .replace("%timeLeft%", getDurationString(supporter.lastServerPickTime - (System.currentTimeMillis() - 1_209_600_000)))
                sendMsg(context, msg)
            }
        }
    }

    override suspend fun execute(context: CommandContext) {
        val msg = context.getTranslation("$root.response")
            .replace("%url%", "https://patreon.com/melijn")
        sendMsg(context, msg)
    }
}