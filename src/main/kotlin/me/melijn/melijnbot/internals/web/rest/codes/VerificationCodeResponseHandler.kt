package me.melijn.melijnbot.internals.web.rest.codes

import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import me.melijn.melijnbot.MelijnBot
import me.melijn.melijnbot.enums.RoleType
import me.melijn.melijnbot.internals.utils.VerificationUtils
import me.melijn.melijnbot.internals.utils.awaitOrNull
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyRoleByType
import me.melijn.melijnbot.internals.web.RequestContext
import me.melijn.melijnbot.internals.web.WebUtils.respondJson
import net.dv8tion.jda.api.utils.data.DataArray
import net.dv8tion.jda.api.utils.data.DataObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object VerificationCodeResponseHandler {

    val logger: Logger = LoggerFactory.getLogger(VerificationCodeResponseHandler::class.java)

    suspend fun handleUnverifiedGuilds(context: RequestContext) {
        if (context.call.request.header("Authorization") != context.restToken) {
            context.call.respondText(status = HttpStatusCode.Forbidden) { "bruh" }
            return
        }

        val body = context.call.receiveText().toLongOrNull() ?: return
        val unverifiedUsersWrapper = context.container.daoManager.unverifiedUsersWrapper
        val guilds: List<Long> = unverifiedUsersWrapper.getGuilds(body)

        val guildObjectArray = DataArray.empty()
        guilds.forEach { guildId ->
            val guild = MelijnBot.shardManager.getGuildById(guildId) ?: return@forEach
            val member = guild.retrieveMemberById(body).awaitOrNull()
            if (member == null) {
                unverifiedUsersWrapper.remove(guildId, body)
                return@forEach
            }
            val guildOjb = DataObject.empty()
            guildOjb.put("id", guild.id)
            guildOjb.put("icon", guild.iconId)
            guildOjb.put("name", guild.name)

            guildObjectArray.add(
                guildOjb
            )
        }

        context.call.respondJson(guildObjectArray, HttpStatusCode.OK)
    }

    suspend fun handleGuildVeriifcation(context: RequestContext) {
        if (context.call.request.header("Authorization") != context.restToken) {
            context.call.respondText(status = HttpStatusCode.Forbidden) { "bruh" }
            return
        }

        val body = DataObject.fromJson(context.call.receiveText())
        val guildId = body.getString("guildId").toLongOrNull() ?: return
        val userId = body.getString("userId").toLongOrNull() ?: return

        val guild = MelijnBot.shardManager.getGuildById(guildId) ?: return
        val unverifiedRole = guild.getAndVerifyRoleByType(context.daoManager, RoleType.UNVERIFIED) ?: return
        val member = guild.retrieveMemberById(userId).awaitOrNull() ?: return

        VerificationUtils.verify(context.daoManager, context.container.webManager.proxiedHttpClient,
            unverifiedRole, guild.selfMember.user, member)

        context.call.respondText { "verified" }
    }
}