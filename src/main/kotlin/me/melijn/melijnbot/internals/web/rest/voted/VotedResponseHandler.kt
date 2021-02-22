package me.melijn.melijnbot.internals.web.rest.voted

import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import me.melijn.melijnbot.database.settings.VoteReminderOption
import me.melijn.melijnbot.database.votes.UserVote
import me.melijn.melijnbot.database.votes.VoteReminder
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.LogUtils
import me.melijn.melijnbot.internals.web.RequestContext
import net.dv8tion.jda.api.utils.data.DataObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.math.ln
import kotlin.math.max

object VotedResponseHandler {

    val logger: Logger = LoggerFactory.getLogger(VotedResponseHandler::class.java)
    suspend fun handleVotedResponse(context: RequestContext) {
        if (context.call.request.header("Authorization") != context.restToken) {
            context.call.respondText(status = HttpStatusCode.Forbidden) { "bruh" }
            return
        }

        val body = DataObject.fromJson(context.call.receiveText())

        val botId = body.getString("bot").toLong()
        if (botId != context.container.settings.botInfo.id) {
            context.call.respondText(status = HttpStatusCode.Forbidden) { "bruh" }
            return
        }

        val baseMel = context.container.settings.economy.baseMel
        val userId = body.getString("user", null)?.toLongOrNull()
        if (userId == null) {
            context.call.respondText(status = HttpStatusCode.BadRequest) { "please provide the user as string in json body" }
            return
        }

        logger.info(body.toString())
        val isWeekend = body.getBoolean("isWeekend", false)
        val botlist = body.getString("src", "")
        val daoManager = context.daoManager
        val balanceWrapper = daoManager.balanceWrapper
        val voteWrapper = daoManager.voteWrapper
        val voteType = body.getString("type", "")

        val oldUserVote = voteWrapper.getUserVote(userId)
            ?: UserVote(userId, 0, 0, 0, 0, 0, 0)

        val lastAnyVoteTime = listOf(
            oldUserVote.topggLastTime,
            oldUserVote.bfdLastTime,
            oldUserVote.dblLastTime,
            oldUserVote.dboatsLastTime
        ).maxOrNull()
            ?: return
        val millisSinceVoteReset = max(0, (System.currentTimeMillis() - lastAnyVoteTime) - 43200000)
        val maxMillisToKeepStreak = context.container.settings.economy.streakExpireHours * 3600_000
        val loseStreak = maxMillisToKeepStreak - millisSinceVoteReset < 0

        val oldStreak = if (loseStreak) 0 else oldUserVote.streak
        val (votes, streak) = if (isWeekend) {
            Pair(oldUserVote.votes + 2, oldStreak + 2)
        } else {
            Pair(oldUserVote.votes + 1, oldStreak + 1)
        }


        val speedMultiplier = max(
            0.0,
            ((maxMillisToKeepStreak - millisSinceVoteReset).toDouble() / maxMillisToKeepStreak.toDouble())
        ) + 1.0
        val premiumMultiplier = context.container.settings.economy.premiumMultiplier

        TaskManager.async {
            if (voteType == "test") {
                LogUtils.sendReceivedVoteTest(context.container, userId, botlist)
            } else {

                val multiplier = (if (daoManager.supporterWrapper.getUsers().contains(userId)) {
                    premiumMultiplier
                } else {
                    1f
                }) * speedMultiplier * (if (isWeekend) 2f else 1f)

                val extraMel = (ln(votes.toDouble()) + ln(streak.toDouble())) * 10
                val totalMel = ((baseMel + extraMel) * multiplier).toLong()

                val newUserVote = newVoteFromOldAndBotList(userId, votes, streak, oldUserVote, botlist)
                voteWrapper.setVote(newUserVote)

                val newBalance = (balanceWrapper.getBalance(userId) + totalMel)
                balanceWrapper.setBalance(userId, newBalance)

                LogUtils.sendReceivedVoteRewards(
                    context.container,
                    userId,
                    newBalance,
                    baseMel,
                    totalMel,
                    streak,
                    votes,
                    botlist
                )

                val activeReminders = daoManager.voteReminderWrapper.getReminder(userId)
                val botListOption: VoteReminderOption = getBotListOptionFromBotList(botlist)

                val voteReminderStates = daoManager.voteReminderStatesWrapper.contains(userId)
                if (voteReminderStates[VoteReminderOption.GLOBAL] == true) {
                    val newRemindTime = getBotListTimeOut(BotList.fromString(botlist)) + System.currentTimeMillis()

                    val lastGlobalReminderTime = activeReminders.firstOrNull { rem ->
                        rem.flag == VoteReminderOption.GLOBAL.number
                    }?.remindAt ?: 0
                    if (lastGlobalReminderTime < newRemindTime) {
                        daoManager.voteReminderWrapper.addReminder(
                            VoteReminder(userId, VoteReminderOption.GLOBAL.number, newRemindTime)
                        )
                    }
                }
                if (voteReminderStates[botListOption] == true) {
                    val newRemindTime = getBotListTimeOut(BotList.fromString(botlist)) + System.currentTimeMillis()

                    val lastReminderTime = activeReminders.firstOrNull { rem ->
                        rem.flag == botListOption.number
                    }?.remindAt ?: 0
                    if (lastReminderTime < newRemindTime) {
                        daoManager.voteReminderWrapper.addReminder(
                            VoteReminder(userId, botListOption.number, newRemindTime)
                        )
                    }
                }
            }
        }

        context.call.respondText(ContentType.Application.Json) { DataObject.empty().put("status", "OK").toString() }
    }

    private fun getBotListOptionFromBotList(botlist: String): VoteReminderOption {
        return when (botlist) {
            "topgg" -> VoteReminderOption.TOPGG
            "dbl" -> VoteReminderOption.DBLCOM
            "bfd" -> VoteReminderOption.BFDCOM
            "dboats" -> VoteReminderOption.DBOATS
            else -> throw IllegalStateException("unknown vote site")
        }
    }

    private fun newVoteFromOldAndBotList(
        userId: Long,
        votes: Long,
        streak: Long,
        oldVote: UserVote,
        botlist: String
    ): UserVote {
        val ctime = System.currentTimeMillis()

        return when (botlist) {
            "topgg" -> UserVote(
                userId,
                votes,
                streak,
                ctime,
                oldVote.dblLastTime,
                oldVote.dblLastTime,
                oldVote.dboatsLastTime
            )
            "dbl" -> UserVote(
                userId,
                votes,
                streak,
                oldVote.topggLastTime,
                ctime,
                oldVote.bfdLastTime,
                oldVote.dboatsLastTime
            )
            "bfd" -> UserVote(
                userId,
                votes,
                streak,
                oldVote.topggLastTime,
                oldVote.dblLastTime,
                ctime,
                oldVote.dboatsLastTime
            )
            "dboats" -> UserVote(
                userId,
                votes,
                streak,
                oldVote.topggLastTime,
                oldVote.dblLastTime,
                oldVote.bfdLastTime,
                ctime
            )
            else -> UserVote(
                userId,
                votes,
                streak,
                oldVote.topggLastTime,
                oldVote.dblLastTime,
                oldVote.bfdLastTime,
                oldVote.dboatsLastTime
            )
        }
    }
}

fun getBotListTimeOut(botList: BotList): Long {
    return when (botList) {
        BotList.TOP_GG -> 12 * 3600_000
        BotList.DISCORD_BOT_LIST_COM -> 12 * 3600_000
        BotList.DISCORD_BOATS -> 12 * 3600_000
        BotList.BOTS_FOR_DISCORD_COM -> {
            val cDate = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            cDate.timeInMillis = System.currentTimeMillis()
            val nextMidnightDate = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            nextMidnightDate.set(
                cDate.get(Calendar.YEAR),
                cDate.get(Calendar.MONTH),
                cDate.get(Calendar.DAY_OF_MONTH),
                23,
                59,
                59
            )
            val nextMidnightMillis = nextMidnightDate.time.time + 1000
            nextMidnightMillis - System.currentTimeMillis()
        }
    }
}

enum class BotList(val text: String) {
    TOP_GG("topgg"),
    DISCORD_BOT_LIST_COM("dbl"),
    DISCORD_BOATS("dboats"),
    BOTS_FOR_DISCORD_COM("bfd");

    companion object {
        fun fromString(s: String): BotList {
            for (botList in values()) {
                if (s.equals(botList.text, true)) return botList
            }
            throw IllegalArgumentException("Invalid BotList: $s")
        }
    }
}