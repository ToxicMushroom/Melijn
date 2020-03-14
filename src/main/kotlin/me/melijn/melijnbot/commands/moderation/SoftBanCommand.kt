package me.melijn.melijnbot.commands.moderation

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.ban.SoftBan
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
        val targetUser = getUserByArgsNMessage(context, 0) ?: return
        val member = context.guild.retrieveMember(targetUser).awaitOrNull()
        if (member != null && !context.guild.selfMember.canInteract(member)) {
            val msg = context.getTranslation("message.interact.member.hierarchyexception")
                .replace(PLACEHOLDER_USER, targetUser.asTag)
            sendMsg(context, msg)
            return
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

        val hasActiveSoftBan: Boolean = context.daoManager.banWrapper.getActiveBan(context.guildId, targetUser.idLong) != null
        val ban = SoftBan(
            context.guildId,
            targetUser.idLong,
            context.authorId,
            reason)

        val softBanning = context.getTranslation("message.softbanning")

        val privateChannel = targetUser.openPrivateChannel().awaitOrNull()
        val message: Message? = privateChannel?.let {
            sendMsgEL(it, softBanning)
        }?.firstOrNull()

        continueBanning(context, targetUser, ban, hasActiveSoftBan, clearDays ?: 7, message)
    }

    private suspend fun continueBanning(context: CommandContext, targetUser: User, softBan: SoftBan, hasActiveBan: Boolean, clearDays: Int, softBanningMessage: Message? = null) {
        val guild = context.guild
        val author = context.author
        val language = context.getLanguage()
        val daoManager = context.daoManager
        val zoneId = getZoneId(daoManager, guild.idLong)
        val privZoneId = getZoneId(daoManager, guild.idLong, targetUser.idLong)
        val softBannedMessageDm = getSoftBanMessage(language, privZoneId, guild, targetUser, author, softBan)
        val softBannedMessageLc = getSoftBanMessage(language, zoneId, guild, targetUser, author, softBan, true, targetUser.isBot, softBanningMessage != null)

        daoManager.softBanWrapper.addSoftBan(softBan)

        val msg = try {
            guild.ban(targetUser, clearDays).reason("softbanned").await()
            softBanningMessage?.editMessage(
                softBannedMessageDm
            )?.override(true)?.queue()

            val logChannelWrapper = daoManager.logChannelWrapper
            val logChannelId = logChannelWrapper.logChannelCache.get(Pair(guild.idLong, LogChannelType.SOFT_BAN)).await()
            val logChannel = guild.getTextChannelById(logChannelId)
            logChannel?.let { it1 -> sendEmbed(daoManager.embedDisabledWrapper, it1, softBannedMessageLc) }

            if (!hasActiveBan) {
                guild.unban(targetUser).reason("softbanned").await()
            }

            context.getTranslation("$root.success")
                .replace(PLACEHOLDER_USER, targetUser.asTag)
                .replace("%reason%", softBan.reason)

        } catch (t: Throwable) {
            val failedMsg = context.getTranslation("message.softbanning.failed")
            softBanningMessage?.editMessage(failedMsg)?.queue()

            context.getTranslation("$root.failure")
                .replace(PLACEHOLDER_USER, targetUser.asTag)
                .replace("%cause%", t.message ?: "/")
        }
        sendMsg(context, msg)
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
    val eb = EmbedBuilder()
    var description = "```LDIF\n"

    if (!lc) {
        description += i18n.getTranslation(language, "message.punishment.description.nlc")
            .replace("%guildName%", guild.name)
            .replace("%guildId%", guild.id)
    }

    description += i18n.getTranslation(language, "message.punishment.softban.description")
        .replace("%softBanAuthor%", softBanAuthor.asTag)
        .replace("%softBanAuthorId%", softBanAuthor.id)
        .replace("%softBanned%", softBannedUser.asTag)
        .replace("%softBannedId%", softBannedUser.id)
        .replace("%reason%", softBan.reason)
        .replace("%moment%", (softBan.moment.asEpochMillisToDateTime(zoneId)))
        .replace("%softBanId%", softBan.softBanId)

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

    val author = i18n.getTranslation(language, "message.punishment.softban.author")
        .replace(PLACEHOLDER_USER, softBanAuthor.asTag)
        .replace("%spaces%", " ".repeat(45).substring(0, 45 - softBanAuthor.name.length) + "\u200B")

    eb.setAuthor(author, null, softBanAuthor.effectiveAvatarUrl)
    eb.setDescription(description)
    eb.setThumbnail(softBannedUser.effectiveAvatarUrl)
    eb.setColor(Color.RED)
    return eb.build()
}