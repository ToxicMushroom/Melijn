package me.melijn.melijnbot.internals.web.rest.member

import io.ktor.application.*
import io.ktor.response.*
import me.melijn.melijnbot.MelijnBot
import me.melijn.melijnbot.internals.utils.awaitOrNull
import me.melijn.melijnbot.internals.utils.isPositiveNumber
import me.melijn.melijnbot.internals.web.RequestContext
import me.melijn.melijnbot.internals.web.WebUtils.respondJson
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.utils.data.DataObject

object MemberInfoResponseHandler {
    suspend fun handleMemberInfoResponse(context: RequestContext) {
        val shardManager = MelijnBot.shardManager
        val guild = context.call.parameters["guildId"]?.let { guildId ->
            if (guildId.isPositiveNumber()) shardManager.getGuildById(guildId) else null
        }

        if (guild == null) {
            context.call.respondJson(DataObject.empty()
                .put("error", "Invalid guildId")
                .put("isMember", false))
            return
        }


        val user = context.call.parameters["userId"]?.let { userId ->
            if (userId.isPositiveNumber()) shardManager.retrieveUserById(userId).awaitOrNull() else null
        }

        val member = user?.let {
            guild.retrieveMember(it).awaitOrNull()
        }
        if (member == null) {
            context.call.respondJson(DataObject.empty()
                .put("error", "Member not found")
                .put("isMember", false))
            return
        }


        context.call.respondJson(DataObject.empty()
            .put("isMember", true)
            .put("isAdmin", member.hasPermission(Permission.ADMINISTRATOR) || member.isOwner))
    }
}