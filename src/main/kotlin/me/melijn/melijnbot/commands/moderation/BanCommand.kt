package me.melijn.melijnbot.commands.moderation

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.ban.Ban
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.enums.SpecialPermission
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.command.hasPermission
import me.melijn.melijnbot.internals.translation.MESSAGE_INTERACT_MEMBER_HIARCHYEXCEPTION
import me.melijn.melijnbot.internals.translation.MESSAGE_SELFINTERACT_MEMBER_HIARCHYEXCEPTION
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.internals.translation.i18n
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendEmbed
import me.melijn.melijnbot.internals.utils.message.sendMsgAwaitEL
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import java.awt.Color
import java.time.ZoneId

class BanCommand : AbstractCommand("command.ban") {

    init {
        id = 24
        name = "ban"
        aliases = arrayOf("permBan", "hackBan")
        commandCategory = CommandCategory.MODERATION
        discordChannelPermissions = arrayOf(Permission.BAN_MEMBERS)
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }

        val targetUser = retrieveUserByArgsNMessage(context, 0) ?: return
        val member = context.guild.retrieveMember(targetUser).awaitOrNull()
        if (member != null) {
            if (!context.guild.selfMember.canInteract(member)) {
                val msg = context.getTranslation(MESSAGE_SELFINTERACT_MEMBER_HIARCHYEXCEPTION)
                    .withVariable(PLACEHOLDER_USER, targetUser.asTag)
                sendRsp(context, msg)
                return
            }
            if (!context.member.canInteract(member) && !hasPermission(context, SpecialPermission.PUNISH_BYPASS_HIGHER.node, true)) {
                val msg = context.getTranslation(MESSAGE_INTERACT_MEMBER_HIARCHYEXCEPTION)
                    .withVariable(PLACEHOLDER_USER, targetUser.asTag)
                sendRsp(context, msg)
                return
            }
        }


        var reason = context.getRawArgPart(1)

        if (reason.isBlank()) reason = "/"
        reason = reason.trim()

        val activeBan: Ban? = context.daoManager.banWrapper.getActiveBan(context.guildId, targetUser.idLong)
        val ban = Ban(
            context.guildId,
            targetUser.idLong,
            context.authorId,
            reason,
            null
        )
        if (activeBan != null) {
            ban.banId = activeBan.banId
            ban.startTime = activeBan.startTime
        }

        val banning = context.getTranslation("message.banning")
        val privateChannel = if (context.guild.isMember(targetUser)) {
            targetUser.openPrivateChannel().awaitOrNull()
        } else {
            null
        }
        val message: Message? = privateChannel?.let {
            sendMsgAwaitEL(it, banning)
        }?.firstOrNull()

        continueBanning(context, targetUser, ban, activeBan, message)
    }

    private suspend fun continueBanning(context: CommandContext, targetUser: User, ban: Ban, activeBan: Ban?, banningMessage: Message? = null) {
        val guild = context.guild
        val author = context.author
        val language = context.getLanguage()
        val daoManager = context.daoManager
        val zoneId = getZoneId(daoManager, guild.idLong)
        val privZoneId = getZoneId(daoManager, guild.idLong, targetUser.idLong)
        val bannedMessageDm = getBanMessage(language, privZoneId, guild, targetUser, author, ban)
        val bannedMessageLc = getBanMessage(language, zoneId, guild, targetUser, author, ban, true, targetUser.isBot, banningMessage != null)

        context.daoManager.banWrapper.setBan(ban)

        val msg = try {
            context.guild.ban(targetUser, 7).await()
            banningMessage?.editMessage(
                bannedMessageDm
            )?.override(true)?.queue()

            val logChannelWrapper = daoManager.logChannelWrapper
            val logChannelId = logChannelWrapper.logChannelCache.get(Pair(guild.idLong, LogChannelType.PERMANENT_BAN)).await()
            val logChannel = guild.getTextChannelById(logChannelId)
            logChannel?.let { it1 -> sendEmbed(daoManager.embedDisabledWrapper, it1, bannedMessageLc) }

            context.getTranslation("$root.success" + if (activeBan != null) ".updated" else "")
                .withVariable(PLACEHOLDER_USER, targetUser.asTag)
                .withVariable("reason", ban.reason)

        } catch (t: Throwable) {
            val failedMsg = context.getTranslation("message.banning.failed")
            banningMessage?.editMessage(failedMsg)?.queue()

            context.getTranslation("$root.failure")
                .withVariable(PLACEHOLDER_USER, targetUser.asTag)
                .withVariable("cause", t.message ?: "/")
        }

        sendRsp(context, msg)
    }
}

fun getBanMessage(
    language: String,
    zoneId: ZoneId,
    guild: Guild,
    bannedUser: User,
    banAuthor: User,
    ban: Ban,
    lc: Boolean = false,
    isBot: Boolean = false,
    received: Boolean = true
): MessageEmbed {
    val banDuration = ban.endTime?.let { endTime ->
        getDurationString((endTime - ban.startTime))
    } ?: i18n.getTranslation(language, "infinite")

    var description = "```LDIF\n"
    if (!lc) {
        description += i18n.getTranslation(language, "message.punishment.description.nlc")
            .withVariable("serverName", guild.name)
            .withVariable("serverId", guild.id)
    }

    description += i18n.getTranslation(language, "message.punishment.ban.description")
        .withVariable("banAuthor", banAuthor.asTag)
        .withVariable("banAuthorId", banAuthor.id)
        .withVariable("banned", bannedUser.asTag)
        .withVariable("bannedId", bannedUser.id)
        .withVariable("reason", ban.reason)
        .withVariable("duration", banDuration)
        .withVariable("startTime", (ban.startTime.asEpochMillisToDateTime(zoneId)))
        .withVariable("endTime", (ban.endTime?.asEpochMillisToDateTime(zoneId) ?: "none"))
        .withVariable("banId", ban.banId)

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
        .withVariable(PLACEHOLDER_USER, banAuthor.asTag)
        .withVariable("spaces", " ".repeat(45).substring(0, 45 - banAuthor.name.length) + "\u200B")

    return EmbedBuilder()
        .setAuthor(author, null, banAuthor.effectiveAvatarUrl)
        .setThumbnail(bannedUser.effectiveAvatarUrl)
        .setColor(Color.RED)
        .setDescription(description)
        .build()
}