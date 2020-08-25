package me.melijn.melijnbot.internals.web.rest.info

import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import me.melijn.melijnbot.MelijnBot
import me.melijn.melijnbot.internals.utils.awaitOrNull
import me.melijn.melijnbot.internals.utils.isPositiveNumber
import me.melijn.melijnbot.internals.web.RequestContext
import me.melijn.melijnbot.internals.web.WebUtils.respondJson
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.utils.data.DataObject

object PostGuildResponseHandler {
    suspend fun handleGuildPostResponse(context: RequestContext) {
        val id = context.call.parameters["id"] ?: return
        if (!id.isPositiveNumber()) return

        val guild = MelijnBot.shardManager.getGuildById(id)

        val userId = context.call.receiveText()
        val member = guild?.retrieveMemberById(userId)?.awaitOrNull()
        if (member == null) {
            context.call.respondJson(DataObject.empty()
                .put("error", "guild invalidated"))
            return
        }

        val hasPerm = (member.hasPermission(Permission.ADMINISTRATOR) ||
            member.hasPermission(Permission.MANAGE_SERVER) ||
            member.isOwner)

        if (!hasPerm) {
            context.call.respondJson(DataObject.empty()
                .put("error", "guild invalidated"))
            return
        }

        val guildData = DataObject.empty()
            .put("id", id)
            .put("name", guild.name)
            .put("icon", guild.iconId)

        context.call.respondJson(guildData)
    }
}