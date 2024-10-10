package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.asLongLongGMTString
import me.melijn.melijnbot.internals.utils.await
import me.melijn.melijnbot.internals.utils.message.sendRsp
import net.dv8tion.jda.api.utils.MarkdownSanitizer

class BoostersCommand : AbstractCommand("command.boosters") {

    init {
        id = 215
        name = "boosters"
        cooldown = 10_000
        runConditions = arrayOf()
        aliases = arrayOf("listBoosters", "boosterList", "boostersList")
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: ICommandContext) {
        context.initCooldown()
        val boosters = context.guild.findMembers { it.timeBoosted != null }.await()
        val msg = if (boosters.isEmpty()) {
            "This server has no boosters"
        } else {
            "**Boosters List (${boosters.size})**" +
                boosters
                    .sortedBy { it.timeBoosted?.toInstant()?.toEpochMilli() ?: 0 }
                    .joinToString {
                        "\n${MarkdownSanitizer.escape(it.effectiveName)} (${it.id}) [${it.timeBoosted?.asLongLongGMTString()}]"
                    }
        }
        sendRsp(context, msg)
    }
}