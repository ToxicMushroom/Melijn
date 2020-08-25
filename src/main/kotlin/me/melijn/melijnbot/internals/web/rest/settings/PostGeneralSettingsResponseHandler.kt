package me.melijn.melijnbot.internals.web.rest.settings

import io.ktor.request.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import me.melijn.melijnbot.MelijnBot
import me.melijn.melijnbot.internals.utils.awaitOrNull
import me.melijn.melijnbot.internals.web.RequestContext
import me.melijn.melijnbot.internals.web.WebUtils.respondJson
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.utils.data.DataObject
import java.awt.Color
import java.util.*

object PostGeneralSettingsResponseHandler {
    suspend fun handleGeneralSettingsPostResponse(context: RequestContext) {
        try {
            val guildId = context.call.parameters["guildId"]?.toLongOrNull() ?: return
            if (guildId < 0) return

            val guild = MelijnBot.shardManager.getGuildById(guildId)

            val body = context.call.receiveText()
            val jsonBody = DataObject.fromJson(body)
            val userId = jsonBody.getLong("userId")
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

            val settings = jsonBody.getObject("settings")

            val jobs = mutableListOf<Job>()
            val daoManager = context.daoManager


            val prefixArray = settings.getArray("prefixes")
            val prefixes = mutableListOf<String>()
            for (i in 0 until prefixArray.length()) {
                prefixArray.getString(i)
                    .takeIf { it != "%SPLIT%" }
                    ?.let { prefixes.add(it) }
            }

            daoManager.guildPrefixWrapper.setPrefixes(guildId, prefixes)


            val allowSpacedPrefix = settings.getBoolean("allowSpacePrefix")
            daoManager.allowSpacedPrefixWrapper.setGuildState(guildId, allowSpacedPrefix)


            val color = Color.decode(settings.getString("embedColor"))
            if (context.container.settings.botInfo.embedColor != color.rgb) {
                daoManager.embedColorWrapper.setColor(guildId, color.rgb)
            } else {
                daoManager.embedColorWrapper.removeColor(guildId)
            }


            val timeZone = settings.getString("timeZone")
            if (TimeZone.getAvailableIDs().toList().contains(timeZone)) {
                val tz = TimeZone.getTimeZone(timeZone)
                if (tz == TimeZone.getTimeZone("UTC")) {
                    daoManager.timeZoneWrapper.removeTimeZone(guildId)
                } else {
                    daoManager.timeZoneWrapper.setTimeZone(guildId, tz)
                }
            }


            val embedsDisabled = settings.getBoolean("embedsDisabled")
            daoManager.embedDisabledWrapper.setDisabled(guildId, embedsDisabled)


            jobs.joinAll()

            context.call.respondJson(DataObject.empty()
                .put("success", true)
            )
        } catch (t: Throwable) {
            context.call.respondJson(DataObject.empty()
                .put("success", false)
            )
        }
    }
}