package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.asEpochMillisToDateTimeMillis
import me.melijn.melijnbot.internals.utils.getLongFromArgNMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp

class IDInfoCommand : AbstractCommand("command.idinfo") {

    init {
        id = 208
        name = "idInfo"
        aliases = arrayOf("ii", "idi")
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: ICommandContext) {
        val id = getLongFromArgNMessage(context, 0, 0) ?: return
        val sentTime = (id shr 22) + 1420070400000

        sendRsp(context, "Snowflake created at `${sentTime.asEpochMillisToDateTimeMillis(context.getTimeZoneId())}`")
    }
}