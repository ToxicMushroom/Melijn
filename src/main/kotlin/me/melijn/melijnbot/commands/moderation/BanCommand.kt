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
        if (member != null && !context.getGuild().selfMember.canInteract(member)) {
            val language = context.getLanguage()
            val msg = i18n.getTranslation(language, "$root.cannotban")
                .replace(PLACEHOLDER_USER, targetUser.asTag)
            sendMsg(context, msg)
            return

        }

        var reason = context.rawArg
            .replaceFirst(context.args[0], "")
            .trim()

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

        val language = context.getLanguage()
        val banning = i18n.getTranslation(language, "message.banning")
        try {
            val privateChannel = targetUser.openPrivateChannel().await()
            val message = privateChannel.sendMessage(banning).await()
            continueBanning(context, targetUser, ban, activeBan, message)
        } catch (t: Throwable) {
            continueBanning(context, targetUser, ban, activeBan)
        }
    }

    private suspend fun continueBanning(context: CommandContext, targetUser: User, ban: Ban, activeBan: Ban?, banningMessage: Message? = null) {
        val guild = context.getGuild()
        val author = context.getAuthor()
        val language = context.getLanguage()
        val bannedMessageDm = getBanMessage(language, guild, targetUser, author, ban)
        val bannedMessageLc = getBanMessage(language, guild, targetUser, author, ban, true, targetUser.isBot, banningMessage != null)

        context.daoManager.banWrapper.setBan(ban)

        val msg = try {
            context.getGuild().ban(targetUser, 7).await()
            banningMessage?.editMessage(
                bannedMessageDm
            )?.override(true)?.queue()

            val logChannelWrapper = context.daoManager.logChannelWrapper
            val logChannelId = logChannelWrapper.logChannelCache.get(Pair(guild.idLong, LogChannelType.PERMANENT_BAN)).await()
            val logChannel = guild.getTextChannelById(logChannelId)
            logChannel?.let { it1 -> sendEmbed(context.daoManager.embedDisabledWrapper, it1, bannedMessageLc) }

            i18n.getTranslation(language, "$root.success" + if (activeBan != null) ".updated" else "")
                .replace(PLACEHOLDER_USER, targetUser.asTag)
                .replace("%reason%", ban.reason)

        } catch (t: Throwable) {
            banningMessage?.editMessage("failed to ban")?.queue()

            i18n.getTranslation(language, "$root.failure")
                .replace(PLACEHOLDER_USER, targetUser.asTag)
                .replace("%cause%", t.message ?: "unknown (contact support for info)")
        }
        sendMsg(context, msg)
    }
}

fun getBanMessage(
    language: String,
    guild: Guild,
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
    } ?: i18n.getTranslation(language, "infinite")

    var description = "```LDIF"
    if (!lc) {
        description += i18n.getTranslation(language, "message.punishment.ban.nlc")
            .replace("%guildName%", guild.name)
            .replace("%guildId%", guild.name)
    }

    description += i18n.getTranslation(language, "message.punishment.ban.description")

        .replace("%banAuthor%", guild.name)
        .replace("%banAuthorId%", guild.name)
        .replace("%banned%", guild.name)
        .replace("%bannedId%", guild.name)
        .replace("%reason%", ban.reason)
        .replace("%duration%", banDuration)
        .replace("%startTime%", (ban.startTime.asEpochMillisToDateTime()))
        .replace("%endTime%", (ban.endTime?.asEpochMillisToDateTime() ?: "none"))

    val extraDesc: String = if (!received || isBot) {
        i18n.getTranslation(language,
            if (isBot) {
                "message.punishment.ban.extra.bot"
            } else {
                "message.punishment.ban.extra.dm"
            }
        )
    } else {
        ""
    }
    description += extraDesc
    description += "```"

    val author = i18n.getTranslation(language, "message.punishment.ban.author")
        .replace(PLACEHOLDER_USER, banAuthor.asTag)
        .replace("%spaces%", " ".repeat(45).substring(0, 45 - banAuthor.name.length) + "\u200B")

    eb.setAuthor(author, null, banAuthor.effectiveAvatarUrl)
    eb.setDescription(description)
    eb.setThumbnail(bannedUser.effectiveAvatarUrl)
    eb.setColor(Color.BLUE)
    return eb.build()
}