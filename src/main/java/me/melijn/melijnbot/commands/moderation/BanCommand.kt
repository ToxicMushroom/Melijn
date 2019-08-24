package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.database.ban.Ban
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.objects.translation.Translateable
import me.melijn.melijnbot.objects.utils.getUserByArgsNMessage
import me.melijn.melijnbot.objects.utils.sendEmbed
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.sendSyntax
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User

class BanCommand : AbstractCommand("command.ban") {

    init {
        id = 24
        name = "ban"
        aliases = arrayOf("permBan")
        commandCategory = CommandCategory.MODERATION
        discordPermissions = arrayOf(Permission.BAN_MEMBERS)
    }

    override fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context, syntax)
            return
        }
        val targetUser = getUserByArgsNMessage(context, 0) ?: return
        val member = context.getGuild().getMember(targetUser)
        if (member != null) {
            if (!context.getGuild().selfMember.canInteract(member)) {
                val msg = Translateable("$root.cannotban").string(context)
                        .replace(PLACEHOLDER_USER, targetUser.asTag)
                sendMsg(context, msg)
                return
            }
        }

        var reason = context.rawArg.replaceFirst((context.args[0] + "($:\\s+)?").toRegex(), "")
        if (reason.isBlank()) reason = "/"

        var reasonPreSpaceCount = 0
        for (c in reason) {
            if (c == ' ') reasonPreSpaceCount++
            else break
        }
        reason = reason.substring(reasonPreSpaceCount)

        val activeBan: Ban? = context.daoManager.banWrapper.getActiveBan(context.getGuildId(), targetUser.idLong)
        val ban = Ban(
                context.getGuildId(),
                targetUser.idLong,
                context.authorId,
                reason,
                null)
        if (activeBan != null) ban.startTime = activeBan.startTime

        val banning = Translateable("message.banning").string(context)
        targetUser.openPrivateChannel().queue({ privateChannel ->
            privateChannel.sendMessage(banning).queue({ message ->
                continueBanning(context, targetUser, ban, activeBan, message)
            }, {
                continueBanning(context, targetUser, ban, activeBan)
            })
        }, {
            continueBanning(context, targetUser, ban, activeBan)
        })
    }

    private fun continueBanning(context: CommandContext, targetUser: User, ban: Ban, activeBan: Ban?, banningMessage: Message? = null) {
        val bannedMessage = getBanMessage(context.getGuild(), targetUser, context.getAuthor(), ban)
        context.daoManager.banWrapper.setBan(ban)
        context.getGuild().ban(targetUser, 7).queue({
            banningMessage?.editMessage(
                   bannedMessage
            )?.override(true)?.queue()

            val logChannelWrapper = context.daoManager.logChannelWrapper
            val logChannelId = logChannelWrapper.logChannelCache.get(Pair(context.getGuildId(), LogChannelType.PERMANENT_BAN)).get()
            val logChannel = context.getGuild().getTextChannelById(logChannelId)
            logChannel?.let { it1 -> sendEmbed(context.daoManager.embedDisabledWrapper, it1, bannedMessage) }

            val msg = Translateable("$root.success" + if (activeBan != null) ".updated" else "").string(context)
                    .replace(PLACEHOLDER_USER, targetUser.asTag)
                    .replace("%reason%", ban.reason)
            sendMsg(context, msg)
        }, {
            banningMessage?.editMessage("failed to ban")?.queue()
            val msg = Translateable("$root.failure").string(context)
                    .replace(PLACEHOLDER_USER, targetUser.asTag)
                    .replace("%cause%", it.message ?: "unknown (contact support for info)")
            sendMsg(context, msg)
        })
    }
}