package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.translation.MESSAGE_UNKNOWN_TIMEZONE
import me.melijn.melijnbot.internals.utils.getObjectFromArgNMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.withVariable
import java.util.*

class SetTimeZoneCommand : AbstractCommand("command.settimezone") {

    init {
        id = 140
        name = "setTimeZone"
        aliases = arrayOf("stz", "setTZ")
        commandCategory = CommandCategory.ADMINISTRATION
    }

    suspend fun execute(context: ICommandContext) {
        if (context.args.isEmpty()) {
            sendTimeZone(context)
        } else {
            setTimeZone(context) { it.guildId }
        }
    }

    private suspend fun sendTimeZone(context: ICommandContext) {
        val dao = context.daoManager.timeZoneWrapper
        val id = dao.getTimeZone(context.guildId)

        val msg = context.getTranslation(
            if (id.isBlank()) {
                "$root.show.unset"
            } else {
                "$root.show.set"
            }
        ).withVariable("zone", id)

        sendRsp(context, msg)
    }

    companion object {
        suspend fun setTimeZone(context: ICommandContext, idParser: (ICommandContext) -> Long) {
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
                dao.removeTimeZone(idParser(context))
            } else {
                dao.setTimeZone(idParser(context), zone)
            }

            val possible = if (shouldUnset) {
                "un"
            } else {
                ""
            }

            val msg = context.getTranslation("${context.commandOrder.first().root}.${possible}set")
                .withVariable("zone", zone?.id ?: "")

            sendRsp(context, msg)
        }
    }
}