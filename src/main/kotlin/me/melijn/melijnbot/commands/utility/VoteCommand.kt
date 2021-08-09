package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.database.settings.VoteReminderOption
import me.melijn.melijnbot.database.votes.UserVote
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.utils.LogUtils.VOTE_LINKS
import me.melijn.melijnbot.internals.utils.LogUtils.getVoteStatusForSite
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.utils.withVariable

const val VOTE_URL: String = "https://top.gg/bot/melijn/vote"

class VoteCommand : AbstractCommand("command.vote") {

    init {
        id = 116
        name = "vote"
        aliases = arrayOf("v")
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: ICommandContext) {
        val cMillis = System.currentTimeMillis()
        val userVote = context.daoManager.voteWrapper.getUserVote(context.authorId)
            ?: UserVote(context.authorId, 0, 0, 0, 0, 0, 0)

        val statusOne = getVoteStatusForSite(VoteReminderOption.TOPGG, userVote.topggLastTime - cMillis)
        val statusTwo = getVoteStatusForSite(VoteReminderOption.BFDCOM, userVote.topggLastTime - cMillis)
        val statusThree = getVoteStatusForSite(VoteReminderOption.DBLCOM, userVote.topggLastTime - cMillis)
        val statusFour = getVoteStatusForSite(VoteReminderOption.DBOATS, userVote.topggLastTime - cMillis)

        val eb = Embedder(context)
            .setTitle("Voting Sites")
            .setDescription(
                VOTE_LINKS
                    .withVariable("statusOne", statusOne)
                    .withVariable("statusTwo", statusTwo)
                    .withVariable("statusThree", statusThree)
                    .withVariable("statusFour", statusFour)
            )
        sendEmbedRsp(context, eb.build())
    }
}