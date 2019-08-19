package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.getDurationByArgsNMessage
import me.melijn.melijnbot.objects.utils.getUserByArgsNMessage
import me.melijn.melijnbot.objects.utils.sendMsg
import net.dv8tion.jda.api.Permission

class TempBanCommand : AbstractCommand("command.tempban") {

    init {
        id = 23
        name = "tempBan"
        aliases = arrayOf("temporaryBan")
        commandCategory = CommandCategory.MODERATION
        discordPermissions = arrayOf(Permission.BAN_MEMBERS)
    }

    override fun execute(context: CommandContext) {
        val user = getUserByArgsNMessage(context, 0) ?: return
        val noReasonArgs = context.rawArg.split(">")[0].split("\\s+".toRegex())
        val banDuration = getDurationByArgsNMessage(context, noReasonArgs, 1, noReasonArgs.size) ?: return

        var reason = context.rawArg.substring(context.rawArg.indexOfFirst { s -> s == '>' }, context.rawArg.length)
        var reasonPreSpaceCount = 0
        for (c in reason) {
            if (c == ' ') reasonPreSpaceCount++
            else break
        }
        reason = reason.substring(reasonPreSpaceCount)


        sendMsg(context, "${user.asTag} <> $banDuration <> $reason")
    }
}