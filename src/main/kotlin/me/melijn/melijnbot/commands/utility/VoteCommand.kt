package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.database.votes.UserVote
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.utils.LogUtils.VOTE_LINKS
import me.melijn.melijnbot.internals.utils.getDurationString
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.utils.withVariable
import me.melijn.melijnbot.internals.web.rest.voted.BotList
import me.melijn.melijnbot.internals.web.rest.voted.getBotListTimeOut

const val VOTE_URL: String = "https://top.gg/bot/melijn/vote"

class VoteCommand : AbstractCommand("command.vote") {

    init {
        id = 116
        name = "vote"
        aliases = arrayOf("v")
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: CommandContext) {
        val cMillis = System.currentTimeMillis()
        val userVote = context.daoManager.voteWrapper.getUserVote(context.authorId)
            ?: UserVote(context.authorId, 0, 0, 0, 0, 0, 0)

        val readyOne = getBotListTimeOut(BotList.TOP_GG) + userVote.topggLastTime - cMillis
        val readyTwo = getBotListTimeOut(BotList.DISCORD_BOT_LIST_COM) + userVote.dblLastTime - cMillis
        val readyThree = getBotListTimeOut(BotList.BOTS_FOR_DISCORD_COM) + userVote.bfdLastTime - cMillis
        val readyFour = getBotListTimeOut(BotList.DISCORD_BOATS) + userVote.dboatsLastTime - cMillis

        val ready = "**vote ready**"
        val l = 1000L
        val statusOne = if (readyOne <= l) ready else getDurationString(readyOne)
        val statusTwo = if (readyTwo <= l) ready else getDurationString(readyTwo)
        val statusThree = if (readyThree <= l) ready else getDurationString(readyThree)
        val statusFour = if (readyFour <= l) ready else getDurationString(readyFour)


        val eb = Embedder(context)
            .setTitle("Voting Sites")
            .setDescription(VOTE_LINKS
                .withVariable("statusOne", statusOne)
                .withVariable("statusTwo", statusTwo)
                .withVariable("statusThree", statusThree)
                .withVariable("statusFour", statusFour)
            )
        sendEmbedRsp(context, eb.build())
    }
}