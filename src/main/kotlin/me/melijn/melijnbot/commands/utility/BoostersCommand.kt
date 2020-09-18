package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.utils.asLongLongGMTString
import me.melijn.melijnbot.internals.utils.await
import me.melijn.melijnbot.internals.utils.message.sendRsp

class BoostersCommand : AbstractCommand("command.boosters") {

    init {
        id = 215
        name = "boosters"
        runConditions = arrayOf(RunCondition.VOTED)
        aliases = arrayOf("listBoosters", "boosterList", "boostersList")
    }

    override suspend fun execute(context: CommandContext) {
        val boosters = context.guild.findMembers { it.timeBoosted != null }.await()
        val msg = if (boosters.isEmpty()) {
            "no boosters"
        } else {
            "**Boosters List (${boosters.size})**" +
                boosters
                    .sortedBy { it.timeBoosted?.toInstant()?.toEpochMilli() ?: 0 }
                    .joinToString("\n") {
                        "${it.effectiveName} (${it.id}) [${it.timeBoosted?.asLongLongGMTString()}]"
                    }
        }
        sendRsp(context, msg)
    }
}