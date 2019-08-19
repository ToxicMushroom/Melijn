package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.database.ban.Ban
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.Translateable
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
        val targetUser = getUserByArgsNMessage(context, 0) ?: return
        val member = context.getGuild().getMember(targetUser)
        if (member != null) {
            if (!context.getGuild().selfMember.canInteract(member)) {
                val msg = Translateable("$root.cannotban").string(context)
                        .replace("%user%", targetUser.asTag)
                sendMsg(context, msg)
                return
            }
        }

        val noUserArg = context.rawArg.replaceFirst((context.args[0] + "($:\\s+)?").toRegex(), "")
        val noReasonArgs = noUserArg.split(">")[0].split("\\s+".toRegex())
        val banDuration = getDurationByArgsNMessage(context, noReasonArgs, 1, noReasonArgs.size) ?: return

        var reason = noUserArg.substring(noUserArg.indexOfFirst { s -> s == '>' } + 1, noUserArg.length)
        var reasonPreSpaceCount = 0
        for (c in reason) {
            if (c == ' ') reasonPreSpaceCount++
            else break
        }
        reason = reason.substring(reasonPreSpaceCount)


        val ban = Ban(
                context.getGuildId(),
                targetUser.idLong,
                context.authorId,
                reason,
                null,
                endTime = System.currentTimeMillis() + banDuration
        )

        context.getGuild().ban(targetUser, 7).queue({
            context.daoManager.banWrapper.setBan(ban)
        }, {
            val msg = Translateable("$root.failure").string(context)
                    .replace("%cause%", it.message ?: "unknown (contact support for info)")
            sendMsg(context, msg)
        })
    }
}