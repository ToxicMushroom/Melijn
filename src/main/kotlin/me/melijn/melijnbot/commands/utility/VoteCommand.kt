package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.utils.LogUtils.VOTE_LINKS
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp

const val VOTE_URL: String = "https://top.gg/bot/melijn/vote"

class VoteCommand : AbstractCommand("command.vote") {

    init {
        id = 116
        name = "vote"
        aliases = arrayOf("v")
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: CommandContext) {
        val eb = Embedder(context)
            .setDescription(VOTE_LINKS)
        sendEmbedRsp(context, eb.build())
    }
}