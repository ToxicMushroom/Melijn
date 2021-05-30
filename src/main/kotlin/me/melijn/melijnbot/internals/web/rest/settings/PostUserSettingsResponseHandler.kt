package me.melijn.melijnbot.internals.web.rest.settings

import io.ktor.request.*
import me.melijn.melijnbot.commands.utility.PREMIUM_PRIVATE_PREFIXES_LIMIT
import me.melijn.melijnbot.internals.models.TriState
import me.melijn.melijnbot.internals.utils.getBirthdayByArgsN
import me.melijn.melijnbot.internals.web.RequestContext
import me.melijn.melijnbot.internals.web.WebUtils.respondJson
import net.dv8tion.jda.api.utils.data.DataObject
import java.awt.Color
import java.util.*
import kotlin.math.min

object PostUserSettingsResponseHandler {

    suspend fun handleUserSettingsPost(context: RequestContext) {
        try {
            val userId = context.call.parameters["userId"]?.toLongOrNull() ?: return
            if (userId < 0) return

            val body = context.call.receiveText()
            val jsonBody = DataObject.fromJson(body)

            val settings = jsonBody.getObject("settings")

            val daoManager = context.daoManager
            val premium = daoManager.supporterWrapper.getUsers().contains(userId)

            if (premium) {
                val prefixArray = settings.getArray("prefixes")
                val prefixes = mutableListOf<String>()
                for (i in 0 until min(prefixArray.length(), PREMIUM_PRIVATE_PREFIXES_LIMIT)) {
                    prefixArray.getString(i)
                        .takeIf { it != "%SPLIT%" }
                        ?.let { prefixes.add(it) }
                }
                daoManager.userPrefixWrapper.setPrefixes(userId, prefixes)

                val color = Color.decode(settings.getString("embedColor"))
                if (context.container.settings.botInfo.embedColor != color.rgb) {
                    daoManager.userEmbedColorWrapper.setColor(userId, color.rgb)
                } else {
                    daoManager.userEmbedColorWrapper.removeColor(userId)
                }
            }

            try {
                TriState.valueOf(settings.getString("allowSpacePrefix"))
            } catch (e: Exception) {
                null
            }?.let {
                daoManager.allowSpacedPrefixWrapper.setUserState(userId, it)
            }

            val birthday = settings.getString("birthday")
            getBirthdayByArgsN(birthday)?.let { (birthday, birthyear) ->
                daoManager.birthdayWrapper.setBirthday(userId, birthday, birthyear)
            }

            val timeZone = settings.getString("timeZone")
            if (TimeZone.getAvailableIDs().toList().contains(timeZone)) {
                val tz = TimeZone.getTimeZone(timeZone)
                if (tz == TimeZone.getTimeZone("UTC")) {
                    daoManager.timeZoneWrapper.removeTimeZone(userId)
                } else {
                    daoManager.timeZoneWrapper.setTimeZone(userId, tz)
                }
            }

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