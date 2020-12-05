package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.database.ban.SoftBan
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

class SoftBanCommand : AbstractCommand("command.softban") {

    init {
        id = 111
        name = "softBan"
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
                    .withSafeVariable(PLACEHOLDER_USER, member.asTag)
                sendRsp(context, msg)
                return
            }
            if (!context.member.canInteract(member) && !hasPermission(
                    context,
                    SpecialPermission.PUNISH_BYPASS_HIGHER.node,
                    true
                )
            ) {
                val msg = context.getTranslation(MESSAGE_INTERACT_MEMBER_HIARCHYEXCEPTION)
                    .withSafeVariable(PLACEHOLDER_USER, member.asTag)
                sendRsp(context, msg)
                return
            }
        }

        val clearDays = getIntegerFromArgN(context, 1, 1, 7)

        var reason = context.rawArg
            .removeFirst(context.args[0])
            .trim()

        if (context.args.size > 1 && clearDays != null) {
            reason = reason
                .removeFirst(context.args[1])
                .trim()
        }

        if (reason.isBlank()) reason = "/"
        reason = reason.trim()

        val hasActiveSoftBan: Boolean =
            context.daoManager.banWrapper.getActiveBan(context.guildId, targetUser.idLong) != null
        val ban = SoftBan(
            context.guildId,
            targetUser.idLong,
            context.authorId,
            reason
        )

        val softBanning = context.getTranslation("message.softbanning")

        val privateChannel = if (context.guild.isMember(targetUser)) {
            targetUser.openPrivateChannel().awaitOrNull()
        } else {
            null
        }
        val message: Message? = privateChannel?.let {
            sendMsgAwaitEL(it, softBanning)
        }?.firstOrNull()

        continueBanning(context, targetUser, ban, hasActiveSoftBan, clearDays ?: 7, message)
    }

    private suspend fun continueBanning(
        context: CommandContext,
        targetUser: User,
        softBan: SoftBan,
        hasActiveBan: Boolean,
        clearDays: Int,
        softBanningMessage: Message? = null
    ) {
        val guild = context.guild
        val author = context.author
        val language = context.getLanguage()
        val daoManager = context.daoManager
        val zoneId = getZoneId(daoManager, guild.idLong)
        val privZoneId = getZoneId(daoManager, guild.idLong, targetUser.idLong)
        val softBannedMessageDm = getSoftBanMessage(language, privZoneId, guild, targetUser, author, softBan)
        val softBannedMessageLc = getSoftBanMessage(
            language,
            zoneId,
            guild,
            targetUser,
            author,
            softBan,
            true,
            targetUser.isBot,
            softBanningMessage != null
        )

        daoManager.softBanWrapper.addSoftBan(softBan)

        val msg = try {
            guild
                .ban(targetUser, clearDays)
                .reason("(softban) ${context.author.asTag}: ${softBan.reason}")
                .await()

            softBanningMessage?.editMessage(
                softBannedMessageDm
            )?.override(true)?.queue()

            val logChannelWrapper = daoManager.logChannelWrapper
            val logChannelId = logChannelWrapper.getChannelId(guild.idLong, LogChannelType.SOFT_BAN)
            val logChannel = guild.getTextChannelById(logChannelId)
            logChannel?.let { it1 -> sendEmbed(daoManager.embedDisabledWrapper, it1, softBannedMessageLc) }

            if (!hasActiveBan) {
                guild.unban(targetUser)
                    .reason("(softban) ${context.author.asTag}: ${softBan.reason}").await()
            }

            context.getTranslation("$root.success")
                .withSafeVariable(PLACEHOLDER_USER, targetUser.asTag)
                .withSafeVariable("reason", softBan.reason)

        } catch (t: Throwable) {
            val failedMsg = context.getTranslation("message.softbanning.failed")
            softBanningMessage?.editMessage(failedMsg)?.queue()

            context.getTranslation("$root.failure")
                .withSafeVariable(PLACEHOLDER_USER, targetUser.asTag)
                .withSafeVariable("cause", t.message ?: "/")
        }
        sendRsp(context, msg)
    }
}


fun getSoftBanMessage(
    language: String,
    zoneId: ZoneId,
    guild: Guild,
    softBannedUser: User,
    softBanAuthor: User,
    softBan: SoftBan,
    lc: Boolean = false,
    isBot: Boolean = false,
    received: Boolean = true
): MessageEmbed {
    var description = "```LDIF\n"

    if (!lc) {
        description += i18n.getTranslation(language, "message.punishment.description.nlc")
            .withVariable("serverName", guild.name)
            .withVariable("serverId", guild.id)
    }

    description += i18n.getTranslation(language, "message.punishment.softban.description")
        .withSafeVariable("softBanAuthor", softBanAuthor.asTag)
        .withVariable("softBanAuthorId", softBanAuthor.id)
        .withSafeVariable("softBanned", softBannedUser.asTag)
        .withVariable("softBannedId", softBannedUser.id)
        .withSafeVariable("reason", softBan.reason)
        .withVariable("moment", (softBan.moment.asEpochMillisToDateTime(zoneId)))
        .withVariable("softBanId", softBan.softBanId)

    val extraDesc: String = if (!received || isBot) {
        i18n.getTranslation(
            language,
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

    val author = i18n.getTranslation(language, "message.punishment.softban.author")
        .withSafeVariable(PLACEHOLDER_USER, softBanAuthor.asTag)
        .withVariable("spaces", " ".repeat(45).substring(0, 45 - softBanAuthor.name.length) + "\u200B")

    return EmbedBuilder()
        .setAuthor(author, null, softBanAuthor.effectiveAvatarUrl)
        .setDescription(description)
        .setThumbnail(softBannedUser.effectiveAvatarUrl)
        .setColor(Color.RED)
        .build()
}