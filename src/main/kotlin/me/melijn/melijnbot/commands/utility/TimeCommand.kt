package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.utils.asAsLongTimeAndInvisOffset
import me.melijn.melijnbot.internals.utils.asLongLongGMTString
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.utils.retrieveUserByArgsNMessage
import java.time.Instant
import java.time.ZoneId
import java.util.*

class TimeCommand : AbstractCommand("command.time") {

    init {
        id = 245
        name = "time"
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: ICommandContext) {
        val now = Instant.now()

        val targetZone = try {
            ZoneId.of(context.args[0])
        } catch (e: Exception) {
            null
        }
        val user = if (context.args.isNotEmpty() && targetZone == null) {
            retrieveUserByArgsNMessage(context, 0) ?: return
        } else context.author

        val guildTimezone = context.guildN?.idLong?.let {
            val zoneId = context.daoManager.timeZoneWrapper.getTimeZone(it)
            if (zoneId.isBlank()) null
            else TimeZone.getTimeZone(zoneId).toZoneId()
        }

        val userZoneIdStr = context.daoManager.timeZoneWrapper.getTimeZone(user.idLong)
        val userTimezone = if (userZoneIdStr.isBlank()) null
        else TimeZone.getTimeZone(userZoneIdStr).toZoneId()

        val gmtTimezone = ZoneId.of("GMT")

        val eb = Embedder(context)
            .setTitle("Time information")

        eb.addField(
            "`" + user.asTag + "`" + (userTimezone?.let { " ($it)" } ?: ""),
            userTimezone?.let { now.atZone(it).asLongLongGMTString() } ?: "not set",
            true)

        if (context.isFromGuild) {
            eb.addField(
                "`" + context.guild.name + "`" + (guildTimezone?.let { " ($it)" } ?: ""),
                guildTimezone?.let { now.atZone(it).asLongLongGMTString() } ?: "not set",
                true)
        }

        targetZone?.let {
            eb.addField("Provided ($it)", now.atZone(it).asLongLongGMTString(), false)
        }

        val gmtNow = gmtTimezone.let { now.atZone(it) }
        eb.addField("Bot Time (GMT)", gmtNow.asLongLongGMTString(), targetZone != null)
        eb.addField("Bot Time (Millis)", System.currentTimeMillis().toString(), true)
        sendEmbedRsp(context, eb.build())
    }
}