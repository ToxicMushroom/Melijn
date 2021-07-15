package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.internals.utils.getDurationString
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.utils.retrieveUserByArgsNMessage
import me.melijn.melijnbot.internals.utils.withVariable
import me.melijn.melijnbot.internals.web.rest.voted.BotList
import me.melijn.melijnbot.internals.web.rest.voted.getBotListTimeOut
import net.dv8tion.jda.api.utils.TimeFormat

class VoteInfoCommand : AbstractCommand("command.voteinfo") {

    init {
        id = 117
        name = "voteInfo"
        aliases = arrayOf("voteStats", "vi")
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: ICommandContext) {
        val voteWrapper = context.daoManager.voteWrapper
        val target = if (context.args.isEmpty()) {
            context.author
        } else {
            retrieveUserByArgsNMessage(context, 0) ?: return
        }

        val userVote = voteWrapper.getUserVote(target.idLong)
        val title = context.getTranslation("$root.embed.title")
            .withVariable(PLACEHOLDER_USER, target.asTag)
        val cMillis = System.currentTimeMillis()

        val readyOne = getBotListTimeOut(BotList.TOP_GG) + (userVote?.topggLastTime ?: 0) - cMillis
        val readyTwo = getBotListTimeOut(BotList.DISCORD_BOT_LIST_COM) + (userVote?.dblLastTime ?: 0) - cMillis
        val readyThree = getBotListTimeOut(BotList.BOTS_FOR_DISCORD_COM) + (userVote?.bfdLastTime ?: 0) - cMillis
        val readyFour = getBotListTimeOut(BotList.DISCORD_BOATS) + (userVote?.dboatsLastTime ?: 0) - cMillis

        val statusOne = if (readyOne <= 1000L) "ready" else getDurationString(readyOne)
        val statusTwo = if (readyTwo <= 1000L) "ready" else getDurationString(readyTwo)
        val statusThree = if (readyThree <= 1000L) "ready" else getDurationString(readyThree)
        val statusFour = if (readyFour <= 1000L) "ready" else getDurationString(readyFour)

        val lastTime = listOf(
            userVote?.bfdLastTime,
            userVote?.topggLastTime,
            userVote?.dblLastTime,
            userVote?.dboatsLastTime
        ).maxByOrNull {
            it ?: 0
        }

        val description = context.getTranslation("$root.embed.description")
            .withVariable("votes", userVote?.votes ?: 0)
            .withVariable("streak", userVote?.streak ?: 0)
            .withVariable("topgg", statusOne)
            .withVariable("dbl", statusTwo)
            .withVariable("bfd", statusThree)
            .withVariable("dboats", statusFour)
            .withVariable("lastTime", lastTime?.let { TimeFormat.DATE_TIME_SHORT.atTimestamp(it) } ?: "/")

        val eb = Embedder(context)
            .setThumbnail(target.effectiveAvatarUrl)
            .setTitle(title)
            .setDescription(description)
        sendEmbedRsp(context, eb.build())
    }
}