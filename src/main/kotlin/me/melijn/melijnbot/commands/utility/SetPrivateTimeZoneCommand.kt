package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.commands.administration.SetTimeZoneCommand
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.withSafeVariable

class SetPrivateTimeZoneCommand : AbstractCommand("command.setprivatetimezone") {

    init {
        id = 137
        name = "setPrivateTimeZone"
        aliases = arrayOf("sptz", "setPrivateTZ")
        commandCategory = CommandCategory.UTILITY
    }

    suspend fun execute(context: ICommandContext) {
        if (context.args.isEmpty()) {
            sendTimeZone(context)
        } else {
            SetTimeZoneCommand.setTimeZone(context) { it.authorId }
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

}