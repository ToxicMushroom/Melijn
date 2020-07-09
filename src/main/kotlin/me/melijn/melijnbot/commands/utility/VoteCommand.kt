package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.withVariable

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
            .withVariable("url", VOTE_URL)
        sendRsp(context, msg)
    }
}