package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.database.ban.Ban
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.objects.translation.Translateable
import me.melijn.melijnbot.objects.utils.*
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import java.awt.Color

class BanCommand : AbstractCommand("command.ban") {

    init {
        id = 24
        name = "ban"
        aliases = arrayOf("permBan")
        commandCategory = CommandCategory.MODERATION
        discordPermissions = arrayOf(Permission.BAN_MEMBERS)
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context, syntax)
            return
        }
        val targetUser = getUserByArgsNMessage(context, 0) ?: return
        val member = context.getGuild().getMember(targetUser)
        if (member != null&&!context.getGuild().selfMember.canInteract(member)) {
                val msg = Translateable("$root.cannotban").string(context)
                        .replace(PLACEHOLDER_USER, targetUser.asTag)
                sendMsg(context, msg)
                return

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
        try {
            val privateChannel = targetUser.openPrivateChannel().await()
            val message = privateChannel.sendMessage(banning).await()
            continueBanning(context, targetUser, ban, activeBan, message)
        } catch (t: Throwable) {
            continueBanning(context, targetUser, ban, activeBan)
        }
    }

    private fun continueBanning(context: CommandContext, targetUser: User, ban: Ban, activeBan: Ban?, banningMessage: Message? = null) {
        val guild = context.getGuild()
        val author = context.getAuthor()
        val bannedMessageDm = getBanMessage(guild, targetUser, author, ban)
        val bannedMessageLc = getBanMessage(guild, targetUser, author, ban, true, targetUser.isBot, banningMessage != null)

        context.daoManager.banWrapper.setBan(ban)
        context.getGuild().ban(targetUser, 7).queue({
            banningMessage?.editMessage(
                    bannedMessageDm
            )?.override(true)?.queue()

            val logChannelWrapper = context.daoManager.logChannelWrapper
            val logChannelId = logChannelWrapper.logChannelCache.get(Pair(guild.idLong, LogChannelType.PERMANENT_BAN)).get()
            val logChannel = guild.getTextChannelById(logChannelId)
            logChannel?.let { it1 -> sendEmbed(context.daoManager.embedDisabledWrapper, it1, bannedMessageLc) }

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

fun getBanMessage(guild: Guild,
                  bannedUser: User,
                  banAuthor: User,
                  ban: Ban,
                  lc: Boolean = false,
                  isBot: Boolean = false,
                  received: Boolean = true
): MessageEmbed {
    val eb = EmbedBuilder()

    val banDuration = ban.endTime?.let { endTime ->
        getDurationString((endTime - ban.startTime))
    } ?: "infinite"

    val description = "```LDIF" +
            if (!lc) {
                "" +
                        "\nGuild: " + guild.name +
                        "\nGuildId: " + guild.id
            } else {
                ""
            } +
            "\nBan Author: " + (banAuthor.asTag) +
            "\nBan Author Id: " + ban.banAuthorId +
            "\nBanned: " + bannedUser.asTag +
            "\nBannedId: " + bannedUser.id +
            "\nReason: " + ban.reason +
            "\nDuration: " + banDuration +
            "\nStart of ban: " + (ban.startTime.asEpochMillisToDateTime()) +
            "\nEnd of ban: " + (ban.endTime?.asEpochMillisToDateTime() ?: "none")
    if (!received || isBot) {
        "\nExtra: " +
                if (isBot) {
                    "Target is a bot"
                } else {
                    "Target had dm's disabled"
                }
    } else {
        ""
    } + "```"

    eb.setAuthor("Banned by: " + banAuthor.asTag + " ".repeat(45).substring(0, 45 - banAuthor.name.length) + "\u200B", null, banAuthor.effectiveAvatarUrl)
    eb.setDescription(description)
    eb.setThumbnail(bannedUser.effectiveAvatarUrl)
    eb.setColor(Color.BLUE)
    return eb.build()
}