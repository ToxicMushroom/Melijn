package me.melijn.melijnbot.internals.web.rest.voted

import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.LogUtils
import me.melijn.melijnbot.internals.web.RequestContext
import net.dv8tion.jda.api.utils.data.DataObject

object VotedResponseHandler {
    suspend fun handleVotedResponse(context: RequestContext) {
        if (context.call.request.header("Authorization") != context.restToken) {
            context.call.respondText(status = HttpStatusCode.Forbidden) { "bruh" }
            return
        }

        val body = DataObject.fromJson(context.call.receiveText())
        TaskManager.async {
            val credits = body.getLong("mel")
            val userId = body.getLong("user")
            val votes = body.getInt("votes")
            val streak = body.getInt("streak")
            val wrapper = context.daoManager.balanceWrapper

            val newBalance = wrapper.getBalance(userId) + credits
            wrapper.setBalance(userId, newBalance)

            LogUtils.sendReceivedVoteRewards(context.container, userId, newBalance, credits, streak, votes)
        }

        context.call.respondText { "success" }
    }
}