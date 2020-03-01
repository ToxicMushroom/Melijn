package me.melijn.melijnbot.commands.utility

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.MESSAGE_UNKNOWN_TIMEZONE
import me.melijn.melijnbot.objects.utils.getObjectFromArgNMessage
import me.melijn.melijnbot.objects.utils.sendMsg
import java.time.ZoneId
import java.util.*

class SetPrivateTimeZoneCommand : AbstractCommand("command.setprivatetimezone") {

    init {
        id = 137
        name = "setPrivateTimeZone"
        aliases = arrayOf("sptz", "setPrivateTZ")
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendTimeZone(context)
        } else {
            setTimeZone(context)
        }
    }

    private suspend fun sendTimeZone(context: CommandContext) {
        val dao = context.daoManager.timeZoneWrapper
        val id = dao.timeZoneCache.get(context.authorId).await()

        val msg = context.getTranslation(
            if (id.isBlank()) {
                "$root.show.unset"
            } else {
                "$root.show.set"
            }
        ).replace("%zone%", id)

        sendMsg(context, msg)
    }

    private suspend fun setTimeZone(context: CommandContext) {
        val shouldUnset = "null".equals(context.commandParts[2], true)

        val zone = if (shouldUnset) {
            null
        } else {
            getObjectFromArgNMessage(context, 0, { s ->
                try {
                    TimeZone.getTimeZone(ZoneId.of(s))
                } catch (t: Throwable) {
                    null
                }
            }, MESSAGE_UNKNOWN_TIMEZONE) ?: return
        }


        val dao = context.daoManager.timeZoneWrapper
        if (zone == null) {
            dao.removeTimeZone(context.authorId)
        } else {
            dao.setTimeZone(context.authorId, zone)
        }

        val possible = if (shouldUnset) {
            "un"
        } else {
            ""
        }

        val msg = context.getTranslation("$root.${possible}set")
            .replace("%zone%", zone?.id ?: "")

        sendMsg(context, msg)
    }
}