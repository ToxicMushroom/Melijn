package me.melijn.melijnbot.internals.web.rest.info

import me.melijn.melijnbot.MelijnBot
import me.melijn.melijnbot.internals.translation.MISSING_IMAGE_URL
import me.melijn.melijnbot.internals.utils.isPositiveNumber
import me.melijn.melijnbot.internals.web.RequestContext
import me.melijn.melijnbot.internals.web.WebUtils.respondJson
import net.dv8tion.jda.api.utils.data.DataArray
import net.dv8tion.jda.api.utils.data.DataObject

object GetGuildResponseHandler {
    suspend fun handeGuildGetResponse(context: RequestContext) {
        val id = context.call.parameters["id"] ?: return
        if (!id.isPositiveNumber()) return
        val guild = MelijnBot.shardManager.getGuildById(id)
        if (guild == null) {
            context.call.respondJson(DataObject.empty()
                .put("isBotMember", false))
            return
        }

        val voiceChannels = DataArray.empty()
        val textChannels = DataArray.empty()
        val roles = DataArray.empty()

        guild.voiceChannelCache.forEach { voiceChannel ->
            voiceChannels.add(DataObject.empty()
                .put("position", voiceChannel.position)
                .put("id", voiceChannel.idLong)
                .put("name", voiceChannel.name)
            )
        }

        guild.textChannelCache.forEach { textChannel ->
            textChannels.add(DataObject.empty()
                .put("position", textChannel.position)
                .put("id", textChannel.idLong)
                .put("name", textChannel.name)
            )
        }

        guild.roleCache.forEach { role ->
            roles.add(DataObject.empty()
                .put("id", role.idLong)
                .put("name", role.name)
            )
        }

        context.call.respondJson(DataObject.empty()
            .put("name", guild.name)
            .put("iconUrl", if (guild.iconUrl == null) MISSING_IMAGE_URL else guild.iconUrl)
            .put("memberCount", guild.memberCount)
            .put("ownerId", guild.ownerId)
            .put("isBotMember", true)
            .put("voiceChannels", voiceChannels)
            .put("textChannels", textChannels)
            .put("roles", roles))
    }
}