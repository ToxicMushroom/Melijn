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
            sendSyntax(context)
            return
        }
        val targetUser = getUserByArgsNMessage(context, 0) ?: return
        val member = context.guild.getMember(targetUser)
        if (member != null && !context.guild.selfMember.canInteract(member)) {
            val msg = context.getTranslation("message.interact.member.hierarchyexception")
                .replace(PLACEHOLDER_USER, targetUser.asTag)
            sendMsg(context, msg)
            return
        }

        var reason = context.rawArg
            .removeFirst(context.args[0])
            .trim()

        if (reason.isBlank()) reason = "/"
        reason = reason.trim()

        val activeBan: Ban? = context.daoManager.banWrapper.getActiveBan(context.guildId, targetUser.idLong)
        val ban = Ban(
            context.guildId,
            targetUser.idLong,
            context.authorId,
            reason,
            null)
        if (activeBan != null) {
            ban.startTime = activeBan.startTime
        }

        val banning = context.getTranslation("message.banning")
        try {
            val privateChannel = targetUser.openPrivateChannel().await()
            val message = privateChannel.sendMessage(banning).await()
            continueBanning(context, targetUser, ban, activeBan, message)
        } catch (t: Throwable) {
            continueBanning(context, targetUser, ban, activeBan)
        }
    }

    private suspend fun continueBanning(context: CommandContext, targetUser: User, ban: Ban, activeBan: Ban?, banningMessage: Message? = null) {
        val guild = context.guild
        val author = context.author
        val language = context.getLanguage()
        val bannedMessageDm = getBanMessage(language, guild, targetUser, author, ban)
        val bannedMessageLc = getBanMessage(language, guild, targetUser, author, ban, true, targetUser.isBot, banningMessage != null)

        context.daoManager.banWrapper.setBan(ban)

        val msg = try {
            context.guild.ban(targetUser, 7).await()
            banningMessage?.editMessage(
                bannedMessageDm
            )?.override(true)?.queue()

            val logChannelWrapper = context.daoManager.logChannelWrapper
            val logChannelId = logChannelWrapper.logChannelCache.get(Pair(guild.idLong, LogChannelType.PERMANENT_BAN)).await()
            val logChannel = guild.getTextChannelById(logChannelId)
            logChannel?.let { it1 -> sendEmbed(context.daoManager.embedDisabledWrapper, it1, bannedMessageLc) }

            context.getTranslation("$root.success" + if (activeBan != null) ".updated" else "")
                .replace(PLACEHOLDER_USER, targetUser.asTag)
                .replace("%reason%", ban.reason)

        } catch (t: Throwable) {
            val failedMsg = context.getTranslation("message.banning.failed")
            banningMessage?.editMessage(failedMsg)?.queue()

            context.getTranslation("$root.failure")
                .replace(PLACEHOLDER_USER, targetUser.asTag)
                .replace("%cause%", t.message ?: "/")
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

    var description = "```LDIF\n"
    if (!lc) {
        description += i18n.getTranslation(language, "message.punishment.description.nlc")
            .replace("%guildName%", guild.name)
            .replace("%guildId%", guild.id)
    }

    description += i18n.getTranslation(language, "message.punishment.ban.description")
        .replace("%banAuthor%", banAuthor.asTag)
        .replace("%banAuthorId%", banAuthor.id)
        .replace("%banned%", bannedUser.asTag)
        .replace("%bannedId%", bannedUser.id)
        .replace("%reason%", ban.reason)
        .replace("%duration%", banDuration)
        .replace("%startTime%", (ban.startTime.asEpochMillisToDateTime()))
        .replace("%endTime%", (ban.endTime?.asEpochMillisToDateTime() ?: "none"))

    val extraDesc: String = if (!received || isBot) {
        i18n.getTranslation(language,
            if (isBot) {
                "message.punishment.extra.bot"
            } else {
                "message.punishment.extra.dm"
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
    eb.setThumbnail(bannedUser.effectiveAvatarUrl)
    eb.setColor(Color.RED)
    eb.setDescription(description)
    return eb.build()
}