package me.melijn.melijnbot.internals.web.rest.settings.general

import io.ktor.request.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import me.melijn.melijnbot.MelijnBot
import me.melijn.melijnbot.commands.administration.PREFIXES_LIMIT
import me.melijn.melijnbot.commands.administration.PREMIUM_PREFIXES_LIMIT
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.awaitOrNull
import me.melijn.melijnbot.internals.utils.isPositiveNumber
import me.melijn.melijnbot.internals.utils.toHexString
import me.melijn.melijnbot.internals.web.RequestContext
import me.melijn.melijnbot.internals.web.WebUtils.respondJson
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.utils.data.DataArray
import net.dv8tion.jda.api.utils.data.DataObject
import java.util.*

object GetGeneralSettingsResponseHandler {
    suspend fun handleGeneralSettingsGet(context: RequestContext) {
        val id = context.call.parameters["guildId"] ?: return
        if (!id.isPositiveNumber()) return

        val guild = MelijnBot.shardManager.getGuildById(id)

        val userId = context.call.receiveText()
        val member = guild?.retrieveMemberById(userId)?.awaitOrNull()
        if (member == null) {
            context.call.respondJson(
                DataObject.empty()
                    .put("error", "guild invalidated")
            )
            return
        }

        val idLong = guild.idLong

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
            val prefixes = DataArray.fromCollection(daoManager.guildPrefixWrapper.getPrefixes(idLong))
            settings.put("prefixes", prefixes)
        })

        jobs.add(TaskManager.async {
            val allowed = daoManager.allowSpacedPrefixWrapper.getGuildState(idLong)
            settings.put("allowSpacePrefix", allowed)
        })

        jobs.add(TaskManager.async {
            var ec = daoManager.embedColorWrapper.getColor(idLong)
            if (ec == 0) ec = context.container.settings.botInfo.embedColor
            settings.put("embedColor", ec.toHexString())
        })

        jobs.add(TaskManager.async {
            val tz = daoManager.timeZoneWrapper.getTimeZone(idLong)
            settings.put("timeZone", tz)
        })

        jobs.add(TaskManager.async {
            val language = daoManager.guildLanguageWrapper.getLanguage(idLong)
            settings.put("language", language)
        })

        val disabled = daoManager.embedDisabledWrapper.embedDisabledCache.contains(idLong)
        settings.put("embedsDisabled", disabled)

        val provided = DataObject.empty()
            .put("timezones", DataArray.fromCollection(TimeZone.getAvailableIDs().toList()))
            .put("prefixLimit", if (premiumGuild) PREMIUM_PREFIXES_LIMIT else PREFIXES_LIMIT)

        jobs.joinAll()

        context.call.respondJson(
            DataObject.empty()
                .put("guild", guildData)
                .put("settings", settings)
                .put("provided", provided)
        )
    }
}