package me.melijn.melijnbot.internals.web.rest.settings.starboard

import io.ktor.request.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import me.melijn.melijnbot.MelijnBot
import me.melijn.melijnbot.enums.ChannelType
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.awaitOrNull
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyChannelByType
import me.melijn.melijnbot.internals.utils.isPositiveNumber
import me.melijn.melijnbot.internals.web.RequestContext
import me.melijn.melijnbot.internals.web.WebUtils.respondJson
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.utils.data.DataArray
import net.dv8tion.jda.api.utils.data.DataObject

object GetStarboardSettingsResponseHandler {
    suspend fun handleGetStarboardSettings(context: RequestContext) {
        val id = context.call.parameters["guildId"] ?: return
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

        val daoManager = context.daoManager
        val premiumGuild = daoManager.supporterWrapper.getGuilds().contains(guild.idLong)
        val guildData = DataObject.empty()
            .put("id", id)
            .put("name", guild.name)
            .put("icon", guild.iconId)
            .put("premium", premiumGuild)

        val jobs = mutableListOf<Job>()
        val settings = DataObject.empty()

        jobs.add(TaskManager.async {
            val channel = guild.getAndVerifyChannelByType(
                daoManager,
                ChannelType.STARBOARD,
                Permission.MESSAGE_WRITE, Permission.MESSAGE_HISTORY
            )?.idLong
            settings.put("starboardChannel", channel)
        })

        jobs.add(TaskManager.async {
            val sbSettings = daoManager.starboardSettingsWrapper.getStarboardSettings(guild.idLong)

            settings
                .put("minstarcount", sbSettings.minStars)
                .put("excludedChannels", sbSettings.excludedChannelIds)
        })

        val channelStructure = mutableMapOf<String, DataArray>()
        guild.textChannels.filter {
            it.canTalk()
        }.forEach { channel ->
            channelStructure[channel.parent?.name ?: ""] = (channelStructure[channel.parent?.name] ?: DataArray.empty())
                .add(
                    DataObject.empty()
                        .put("name", channel.name)
                        .put("id", channel.id)
                )
        }

        val provided = DataObject.empty()
            .put("channelStructure", DataArray.fromCollection(channelStructure.map { (category, channels) ->
                DataObject.empty()
                    .put("category", category)
                    .put("channels", channels)
            }))

        jobs.joinAll()

        context.call.respondJson(
            DataObject.empty()
                .put("guild", guildData)
                .put("settings", settings)
                .put("provided", provided))
    }
}