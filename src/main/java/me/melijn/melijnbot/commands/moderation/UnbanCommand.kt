package me.melijn.melijnbot.commands.moderation

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.ban.Ban
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.*
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import java.awt.Color

class UnbanCommand : AbstractCommand("command.unban") {

    init {
        id = 25
        name = "unban"
        aliases = arrayOf("pardon")
        commandCategory = CommandCategory.MODERATION
        discordPermissions = arrayOf(Permission.BAN_MEMBERS)
    }

    override suspend fun execute(context: CommandContext) {
        val guild = context.getGuild()
        if (context.args.isEmpty()) {
            sendSyntax(context, syntax)
            return
        }
        val language = context.getLanguage()
        val targetUser = retrieveUserByArgsNMessage(context, 0) ?: return

        var unbanReason = context.rawArg
            .replaceFirst(context.args[0], "")
            .trim()
        if (unbanReason.isBlank()) unbanReason = "/"

        var reasonPreSpaceCount = 0
        for (c in unbanReason) {
            if (c == ' ') reasonPreSpaceCount++
            else break
        }
        unbanReason = unbanReason.substring(reasonPreSpaceCount)

        val activeBan: Ban? = context.daoManager.banWrapper.getActiveBan(context.getGuildId(), targetUser.idLong)
        val ban: Ban = activeBan
                ?: Ban(context.getGuildId(),
                        targetUser.idLong,
                        null,
                        "/"
                )
        ban.unbanAuthorId = context.authorId
        ban.unbanReason = unbanReason
        ban.endTime = System.currentTimeMillis()
        ban.active = false

        val banAuthor = ban.banAuthorId?.let { context.getShardManager()?.getUserById(it) }

        try {
            guild.retrieveBan(targetUser).await()
            try {
                guild.unban(targetUser).await()
                context.daoManager.banWrapper.setBan(ban)

                //Normal success path
                val msg = getUnbanMessage(context.getGuild(), targetUser, banAuthor, context.getAuthor(), ban)
                targetUser.openPrivateChannel().queue({ privateChannel ->
                    sendEmbed(privateChannel, msg, failed = null)
                }, null)

                val logChannelWrapper = context.daoManager.logChannelWrapper
                val logChannelId = logChannelWrapper.logChannelCache.get(Pair(context.getGuildId(), LogChannelType.UNBAN)).await()
                val logChannel = guild.getTextChannelById(logChannelId)
                logChannel?.let { it1 -> sendEmbed(context.daoManager.embedDisabledWrapper, it1, msg) }

                val success = i18n.getTranslation(language,"$root.success")
                        .replace(PLACEHOLDER_USER, targetUser.asTag)
                        .replace("%reason%", unbanReason)
                sendMsg(context, success)

            } catch (t: Throwable) {
                //Sum ting wrong
                val msg = i18n.getTranslation(language,"$root.failure")
                        .replace(PLACEHOLDER_USER, targetUser.asTag)
                        .replace("%cause%", t.message ?: "unknown (contact support for info)")
                sendMsg(context, msg)
            }
        } catch (t: Throwable) {
            //Not banned anymore

            val msg = i18n.getTranslation(language,"$root.notbanned")
                    .replace(PLACEHOLDER_USER, targetUser.asTag)
            sendMsg(context, msg)

            if (activeBan != null) {
                context.daoManager.banWrapper.setBan(ban)
            }
        }
    }
}

fun getUnbanMessage(guild: Guild, bannedUser: User, banAuthor: User?, unbanAuthor: User, ban: Ban): MessageEmbed {
    val eb = EmbedBuilder()
    eb.setAuthor("Unbanned by: " + unbanAuthor.asTag + " ".repeat(45).substring(0, 45 - unbanAuthor.name.length) + "\u200B", null, unbanAuthor.effectiveAvatarUrl)
    eb.setDescription("```LDIF" +
            "\nGuild: " + guild.name +
            "\nGuildId: " + guild.id +
            "\nBan Author: " + (banAuthor?.asTag ?: "deleted account") +
            "\nBan Author Id: " + ban.banAuthorId +
            "\nUnbanned: " + bannedUser.asTag +
            "\nUnbannedId: " + bannedUser.id +
            "\nBan Reason: " + ban.reason +
            "\nUnban Reason: " + ban.unbanReason +
            "\nStart of ban: " + (ban.startTime.asEpochMillisToDateTime()) +
            "\nEnd of ban: " + (ban.endTime?.asEpochMillisToDateTime() ?: "none") + "```")
    eb.setThumbnail(bannedUser.effectiveAvatarUrl)
    eb.setColor(Color.green)
    return eb.build()
}