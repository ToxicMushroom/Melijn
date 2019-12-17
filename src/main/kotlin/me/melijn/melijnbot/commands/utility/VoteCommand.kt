package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.sendMsg

const val VOTE_URL: String = "https://top.gg/bot/melijn/vote"

class VoteCommand : AbstractCommand("command.vote") {

    init {
        id = 116
        name = "vote"
        aliases = arrayOf("v")
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: CommandContext) {
        val msg = context.getTranslation("$root.success")
            .replace("%url%", VOTE_URL)
        sendMsg(context, msg)
    }
}