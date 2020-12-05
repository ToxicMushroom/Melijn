package me.melijn.melijnbot.internals.web.rest.settings

import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import me.melijn.melijnbot.MelijnBot
import me.melijn.melijnbot.commands.utility.PREMIUM_PRIVATE_PREFIXES_LIMIT
import me.melijn.melijnbot.commands.utility.PRIVATE_PREFIXES_LIMIT
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.awaitOrNull
import me.melijn.melijnbot.internals.utils.isPositiveNumber
import me.melijn.melijnbot.internals.utils.toHexString
import me.melijn.melijnbot.internals.web.RequestContext
import me.melijn.melijnbot.internals.web.WebUtils.respondJson
import net.dv8tion.jda.api.utils.data.DataArray
import net.dv8tion.jda.api.utils.data.DataObject
import java.time.LocalDate
import java.util.*

object GetUserSettingsResponseHandler {
    suspend fun handleUserSettingsGet(context: RequestContext) {
        val id = context.call.parameters["userId"] ?: return
        if (!id.isPositiveNumber()) return

        val user = MelijnBot.shardManager.retrieveUserById(id).awaitOrNull()
        if (user == null) {
            context.call.respondJson(
                DataObject.empty()
                    .put("error", "user invalidated")
            )
            return
        }

        val idLong = user.idLong
        val daoManager = context.daoManager
        val premiumUser = daoManager.supporterWrapper.getUsers().contains(idLong)
        val userData = DataObject.empty()
            .put("id", id)
            .put("tag", user.asTag)
            .put("defaultAvatarId", user.defaultAvatarId)
            .put("avatarId", user.avatarId)
            .put("premium", premiumUser)

        val jobs = mutableListOf<Job>()
        val settings = DataObject.empty()


        jobs.add(TaskManager.async {
            val prefixes = DataArray.fromCollection(daoManager.userPrefixWrapper.getPrefixes(idLong))
            settings.put("prefixes", prefixes)
        })

        jobs.add(TaskManager.async {
            val triState = daoManager.allowSpacedPrefixWrapper.getUserTriState(idLong)
            settings.put("allowSpacePrefix", triState)
        })

        jobs.add(TaskManager.async {
            var ec = daoManager.userEmbedColorWrapper.getColor(idLong)
            if (ec == 0) ec = context.container.settings.botInfo.embedColor
            settings.put("embedColor", ec.toHexString())
        })

        jobs.add(TaskManager.async {
            val tz = daoManager.timeZoneWrapper.getTimeZone(idLong)
            settings.put("timeZone", tz)
        })

        jobs.add(TaskManager.async {
            val language = daoManager.userLanguageWrapper.getLanguage(idLong)
            settings.put("language", language)
        })

        jobs.add(TaskManager.async {
            settings.put("birthday", daoManager.birthdayWrapper.getBirthday(idLong)?.let { (day, year) ->
                val localDate = LocalDate.ofYearDay(2019, day)
                val dayOfMonth = localDate.dayOfMonth
                val month = localDate.monthValue
                "${year}-${month}-${dayOfMonth}"
            } ?: "")
        })

        jobs.add(TaskManager.async {
            val aliasMap = daoManager.aliasWrapper.getAliases(idLong)
            val obj = DataObject.empty()
            for ((cmd, aliases) in aliasMap) {
                val arr = DataArray.empty()
                for (alias in aliases) {
                    arr.add(alias)
                }
                obj.put(cmd, arr)
            }
            settings.put("aliases", obj)
        })

        val provided = DataObject.empty()
            .put("timezones", DataArray.fromCollection(TimeZone.getAvailableIDs().toList()))
            .put("prefixLimit", if (premiumUser) PREMIUM_PRIVATE_PREFIXES_LIMIT else PRIVATE_PREFIXES_LIMIT)

        jobs.joinAll()

        context.call.respondJson(
            DataObject.empty()
                .put("user", userData)
                .put("settings", settings)
                .put("provided", provided)
        )
    }
}