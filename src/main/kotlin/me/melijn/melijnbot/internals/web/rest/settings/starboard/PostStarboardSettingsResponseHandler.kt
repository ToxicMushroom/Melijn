package me.melijn.melijnbot.internals.web.rest.settings.starboard

import io.ktor.request.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import me.melijn.melijnbot.MelijnBot
import me.melijn.melijnbot.database.starboard.StarboardSettings
import me.melijn.melijnbot.enums.ChannelType
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.awaitOrNull
import me.melijn.melijnbot.internals.web.RequestContext
import me.melijn.melijnbot.internals.web.WebUtils.respondJson
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.utils.data.DataObject
import kotlin.math.max
import kotlin.math.min

object PostStarboardSettingsResponseHandler {
    suspend fun handleSetStarboardSettings(context: RequestContext) {
        try {
            val guildId = context.call.parameters["guildId"]?.toLongOrNull() ?: return
            if (guildId < 0) return

            val guild = MelijnBot.shardManager.getGuildById(guildId)

            val body = context.call.receiveText()
            val jsonBody = DataObject.fromJson(body)
            val userId = jsonBody.getLong("userId")
            val member = guild?.retrieveMemberById(userId)?.awaitOrNull()
            if (member == null) {
                context.call.respondJson(
                    DataObject.empty()
                        .put("error", "guild invalidated")
                )
                return
            }

            val hasPerm = (member.hasPermission(Permission.ADMINISTRATOR) ||
                member.hasPermission(Permission.MANAGE_SERVER) ||
                member.isOwner)

            if (!hasPerm) {
                context.call.respondJson(
                    DataObject.empty()
                        .put("error", "guild invalidated")
                )
                return
            }

            val settings = jsonBody.getObject("settings")

            val jobs = mutableListOf<Job>()
            val daoManager = context.daoManager

            val allChannels = guild.textChannels.map { it.idLong }
            val availableChannels = guild.textChannels.filter { it.canTalk() }.map { it.idLong }
            val starboardChannelId = settings.getString("starboardchanel", null)?.toLongOrNull()
            if (starboardChannelId != null && availableChannels.contains(starboardChannelId)) {
                jobs.add(TaskManager.async {
                    daoManager.channelWrapper.setChannel(guild.idLong, ChannelType.STARBOARD, starboardChannelId)
                })
            } else {
                daoManager.channelWrapper.removeChannel(guild.idLong, ChannelType.STARBOARD)
            }

            val minStars = max(1, min(10, settings.getInt("minStars", 3)))
            val excludedChannelIds = settings.getString("excludedChannels", null)
                ?.split(",")
                ?.mapNotNull {
                    it.toLongOrNull()
                }?.filter {
                    allChannels.contains(it)
                } ?: emptyList()
            jobs.add(TaskManager.async {
                daoManager.starboardSettingsWrapper.setStarboardSettings(
                    guild.idLong,
                    StarboardSettings(minStars, excludedChannelIds.joinToString(","))
                )
            })

            jobs.joinAll()

            context.call.respondJson(
                DataObject.empty()
                    .put("success", true)
            )
        } catch (t: Throwable) {
            context.call.respondJson(
                DataObject.empty()
                    .put("success", false)
            )
        }
    }
}