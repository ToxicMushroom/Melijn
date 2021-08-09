package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.translation.MESSAGE_UNKNOWN_TIMEZONE
import me.melijn.melijnbot.internals.utils.getObjectFromArgNMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.withSafeVariable
import java.util.*

class SetPrivateTimeZoneCommand : AbstractCommand("command.setprivatetimezone") {

    init {
        id = 137
        name = "setPrivateTimeZone"
        aliases = arrayOf("sptz", "setPrivateTZ")
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: ICommandContext) {
        if (context.args.isEmpty()) {
            sendTimeZone(context)
        } else {
            setTimeZone(context)
        }
    }

    private suspend fun sendTimeZone(context: ICommandContext) {
        val dao = context.daoManager.timeZoneWrapper
        val id = dao.getTimeZone(context.authorId)

        val msg = context.getTranslation(
            if (id.isBlank()) {
                "$root.show.unset"
            } else {
                "$root.show.set"
            }
        ).withSafeVariable("zone", id)

        sendRsp(context, msg)
    }

    private suspend fun setTimeZone(context: ICommandContext) {
        val shouldUnset = "null".equals(context.commandParts[2], true)

        val zone = if (shouldUnset) {
            null
        } else {
            getObjectFromArgNMessage(context, 0, { s ->
                try {
                    TimeZone.getTimeZone(s)
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
            .withSafeVariable("zone", zone?.id ?: "")

        sendRsp(context, msg)
    }
}