package me.melijn.melijnbot.internals.web.rest.voted

import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import me.melijn.melijnbot.database.votes.UserVote
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
        logger.info(body.toString())

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

        val isWeekend = body.getBoolean("isWeekend", false)
        val botlist = body.getString("src", "")
        val daoManager = context.daoManager
        val balanceWrapper = daoManager.balanceWrapper
        val voteWrapper = daoManager.voteWrapper
        val voteType = body.getString("type", "")

        val oldUserVote = voteWrapper.getUserVote(userId)
            ?: UserVote(userId, 0, 0, 0)

        val millisSinceVoteReset = max(0, (System.currentTimeMillis() - oldUserVote.lastTime) - 43200000)
        val maxMillisToKeepStreak = context.container.settings.economy.streakExpireHours * 3600_000
        val loseStreak = maxMillisToKeepStreak - millisSinceVoteReset < 0

        val oldStreak = if (loseStreak) 0 else oldUserVote.streak
        val (_, votes, streak, cTime) = if (isWeekend) {
            UserVote(userId, oldUserVote.votes + 2, oldStreak + 2, System.currentTimeMillis())
        } else {
            UserVote(userId, oldUserVote.votes + 1, oldStreak + 1, System.currentTimeMillis())
        }


        val speedMultiplier = max(0.0, (maxMillisToKeepStreak - millisSinceVoteReset).toDouble() / maxMillisToKeepStreak.toDouble()) + 1.0
        val premiumMultiplier = context.container.settings.economy.premiumMultiplier

        TaskManager.async {
            if (voteType == "test") {
                LogUtils.sendReceivedVoteTest(context.container, userId, botlist)
            } else if (voteType == "vote") {

                val multiplier =
                    (if (daoManager.supporterWrapper.getUsers().contains(userId)) premiumMultiplier else 1f) * speedMultiplier * (if (isWeekend) 2f else 1f)

                val extraMel = (ln(votes.toDouble()) + ln(streak.toDouble())) * 10
                val totalMel = ((baseMel + extraMel) * multiplier).toLong()

                voteWrapper.setVote(userId, votes, streak, cTime)

                val newBalance = (balanceWrapper.getBalance(userId) + totalMel)
                balanceWrapper.setBalance(userId, newBalance)

                LogUtils.sendReceivedVoteRewards(context.container, userId, newBalance, baseMel, totalMel, streak, votes, botlist)

                val disabledVoteReminder = daoManager.denyVoteReminderWrapper.contains(userId)
                if (!disabledVoteReminder) {
                    val newRemindTime = when (botlist) {
                        "topgg" -> 12 * 3600_000
                        "dbl" -> 24 * 3600_000
                        "dboats" -> 24 * 3600_000
                        "bfd" -> {
                            val cDate = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                            cDate.timeInMillis = System.currentTimeMillis()
                            val nextMidnightDate = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                            nextMidnightDate.set(cDate.get(Calendar.YEAR), cDate.get(Calendar.MONTH), cDate.get(Calendar.DAY_OF_MONTH), 23, 59, 59)
                            val nextMidnightMillis = nextMidnightDate.time.time + 1000
                            nextMidnightMillis - System.currentTimeMillis()
                        }

                        else -> 0
                    } + System.currentTimeMillis()

                    val previousRemindMillis = daoManager.voteReminderWrapper.getReminder(userId) ?: 0
                    if (previousRemindMillis < newRemindTime) {
                        daoManager.voteReminderWrapper.addReminder(userId, newRemindTime)
                    }
                }
            } else {
                context.call.respondText { "Unknown type" }
            }
        }

        context.call.respondText(ContentType.Application.Json) { DataObject.empty().put("status", "OK").toString() }
    }
}