package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.getLongFromArgNMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp
import net.dv8tion.jda.api.utils.TimeFormat

class IDInfoCommand : AbstractCommand("command.idinfo") {

    init {
        id = 208
        name = "idInfo"
        aliases = arrayOf("ii", "idi", "snowflake")
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: ICommandContext) {
        val id = getLongFromArgNMessage(context, 0, 0) ?: return
        val sentTime = snowflakeToEpochMillis(id)

        sendRsp(
            context,
            "Snowflake created at ${TimeFormat.DATE_TIME_SHORT.atTimestamp(sentTime)}\n" +
                "Millis: `$sentTime`"
        )
    }
}

fun snowflakeToEpochMillis(snowflake: Long): Long {
    return (snowflake shr 22) + 1420070400000
}