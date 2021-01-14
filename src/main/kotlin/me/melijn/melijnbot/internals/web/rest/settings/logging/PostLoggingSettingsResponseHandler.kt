package me.melijn.melijnbot.internals.web.rest.settings.logging

import io.ktor.request.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import me.melijn.melijnbot.MelijnBot
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.awaitOrNull
import me.melijn.melijnbot.internals.web.RequestContext
import me.melijn.melijnbot.internals.web.WebUtils.respondJson
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.utils.data.DataObject

object PostLoggingSettingsResponseHandler {
    suspend fun handleSetLoggingSettings(context: RequestContext) {
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

            val logChannels = settings.getArray("logchannels")
            val availableChannels = guild.textChannels.filter { it.canTalk() }.map { it.idLong }
            for (i in 0 until logChannels.length()) {
                val logchannel = logChannels.getObject(i)
                val type = LogChannelType.valueOf(logchannel.getString("type"))
                val value = logchannel.getString("value", null)?.toLongOrNull()
                if (value == null || availableChannels.contains(value)) {
                    jobs.add(TaskManager.async {
                        daoManager.logChannelWrapper.setChannel(guild.idLong, type, value ?: -1)
                    })
                }
            }

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