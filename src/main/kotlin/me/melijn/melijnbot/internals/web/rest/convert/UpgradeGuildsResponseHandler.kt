package me.melijn.melijnbot.internals.web.rest.convert

import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import me.melijn.melijnbot.MelijnBot
import me.melijn.melijnbot.internals.web.RequestContext
import me.melijn.melijnbot.internals.web.WebUtils.respondJson
import net.dv8tion.jda.api.utils.data.DataArray
import net.dv8tion.jda.api.utils.data.DataObject

object UpgradeGuildsResponseHandler {
    suspend fun handleUpgradeGuildsResponse(context: RequestContext) {
        if (context.call.request.header("Authorization") != context.restToken) {
            context.call.respondText(status = HttpStatusCode.Forbidden) { "Invalid token\n" }
            return
        }
        val partialGuilds = DataArray.fromJson(context.call.receiveText())
        val shardManager = MelijnBot.shardManager

        val upgradedGuilds = DataArray.empty()

        for (i in 0 until partialGuilds.length()) {
            val upgradedGuild = DataObject.empty()
            val partialGuild = partialGuilds.getObject(i)
            val isOwner = partialGuild.getBoolean("owner")
            val name = partialGuild.getString("name")
            val icon = partialGuild.getString("icon", "null")
            val permissions = partialGuild.getInt("permissions")
            val id = partialGuild.getLong("id")

            // Owner, admin or manage_guild
            val hasPerms = isOwner || (permissions and 0x00000020) != 0 || (permissions and 0x00000008) != 0
            if (!hasPerms) continue

            val fullGuild = shardManager.getGuildById(id)
            val hasMelijn = fullGuild?.let { true } ?: false
            if (!hasMelijn) continue

            upgradedGuild.put("id", "$id")
            upgradedGuild.put("name", name)
            upgradedGuild.put("icon", icon)

            upgradedGuilds.add(upgradedGuild)
        }


        context.call.respondJson(upgradedGuilds)
    }
}