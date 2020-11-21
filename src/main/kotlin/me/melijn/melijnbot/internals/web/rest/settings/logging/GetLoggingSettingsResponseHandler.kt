package me.melijn.melijnbot.internals.web.rest.settings.logging

import io.ktor.request.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import me.melijn.melijnbot.MelijnBot
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.awaitOrNull
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyLogChannelByType
import me.melijn.melijnbot.internals.utils.isPositiveNumber
import me.melijn.melijnbot.internals.web.RequestContext
import me.melijn.melijnbot.internals.web.WebUtils.respondJson
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.utils.data.DataArray
import net.dv8tion.jda.api.utils.data.DataObject

object GetLoggingSettingsResponseHandler {
    suspend fun handleGetLoggingSettings(context: RequestContext) {
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

        val typeGroupMap = mutableMapOf(
            "Moderation" to listOf(
                LogChannelType.KICK,
                LogChannelType.PERMANENT_BAN,
                LogChannelType.TEMP_BAN,
                LogChannelType.UNBAN,
                LogChannelType.SOFT_BAN,
                LogChannelType.PERMANENT_MUTE,
                LogChannelType.TEMP_MUTE,
                LogChannelType.UNMUTE,
                LogChannelType.WARN
            ),
            "Messages" to listOf(
                LogChannelType.EDITED_MESSAGE,
                LogChannelType.FILTERED_MESSAGE,
                LogChannelType.OTHER_DELETED_MESSAGE,
                LogChannelType.SELF_DELETED_MESSAGE,
                LogChannelType.ATTACHMENT,
                LogChannelType.REACTION,
            )
        )

        val otherTypes = mutableListOf<LogChannelType>()
        LogChannelType.values().forEach { type ->
            if (typeGroupMap.values.none { it.contains(type) }) {
                otherTypes.add(type)
            }
        }
        typeGroupMap["Others"] = otherTypes

        jobs.add(TaskManager.async {
            val groups = DataArray.empty()
            for ((groupName, types) in typeGroupMap) {
                val channels = DataArray.empty()
                for (type in types) {
                    val channel = DataObject.empty()
                        .put("type", type.toString())
                        .put("textType", type.text)
                    val textChannel = guild.getAndVerifyLogChannelByType(daoManager, type)
                    channel.put("value", textChannel?.id)
                    channels.add(channel)
                }
                val group = DataObject.empty()
                    .put("group", groupName)
                    .put("channels", channels)
                groups.add(group)
            }
            settings.put("logchannels", groups)
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