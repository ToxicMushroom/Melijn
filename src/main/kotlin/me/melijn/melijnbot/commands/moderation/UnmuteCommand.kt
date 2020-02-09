package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.database.mute.Mute
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.enums.RoleType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.*
import me.melijn.melijnbot.objects.utils.checks.getAndVerifyLogChannelByType
import me.melijn.melijnbot.objects.utils.checks.getAndVerifyRoleByType
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import java.awt.Color
import java.time.ZoneId

class UnmuteCommand : AbstractCommand("command.unmute") {

    init {
        id = 26
        name = "unmute"
        commandCategory = CommandCategory.MODERATION
        discordPermissions = arrayOf(Permission.MANAGE_ROLES)
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }
        val guild = context.guild
        val daoManager = context.daoManager
        val targetUser = retrieveUserByArgsNMessage(context, 0) ?: return

        var unmuteReason = context.rawArg
            .removeFirst(context.args[0])
            .trim()

        if (unmuteReason.isBlank()) {
            unmuteReason = "/"
        }

        val activeMute: Mute? = daoManager.muteWrapper.getActiveMute(context.guildId, targetUser.idLong)
        val mute: Mute = activeMute
            ?: Mute(context.guildId,
                targetUser.idLong,
                null,
                "/"
            )

        mute.unmuteAuthorId = context.authorId
        mute.unmuteReason = unmuteReason
        mute.endTime = System.currentTimeMillis()
        mute.active = false

        val muteAuthor = mute.muteAuthorId?.let { context.shardManager.getUserById(it) }
        val targetMember = guild.getMember(targetUser)

        val muteRole = guild.getAndVerifyRoleByType(daoManager, RoleType.MUTE)
        if (muteRole == null) {
            val msg = context.getTranslation("$root.nomuterole")
                .replace("%prefix%", context.usedPrefix)
            sendMsg(context, msg)
            return
        }

        if (targetMember != null && targetMember.roles.contains(muteRole)) {
            val exception = guild.removeRoleFromMember(targetMember, muteRole).awaitEX()
            if (exception == null) {
                sendUnmuteLogs(context, targetUser, muteAuthor, mute, unmuteReason)
            } else {
                val msg = context.getTranslation("$root.failure")
                    .replace(PLACEHOLDER_USER, targetUser.asTag)
                    .replace("%cause%", exception.message ?: "/")
                sendMsg(context, msg)
            }

        } else if (targetMember == null) {
            sendUnmuteLogs(context, targetUser, muteAuthor, mute, unmuteReason)

        } else if (!targetMember.roles.contains(muteRole)) {
            val msg = context.getTranslation("$root.notmuted")
                .replace(PLACEHOLDER_USER, targetUser.asTag)

            sendMsg(context, msg)

            if (activeMute != null) {
                daoManager.muteWrapper.setMute(mute)
            }
        }
    }

    private suspend fun sendUnmuteLogs(context: CommandContext, targetUser: User, muteAuthor: User?, mute: Mute, unmuteReason: String) {
        val guild = context.guild
        val daoManager = context.daoManager
        val language = context.getLanguage()
        daoManager.muteWrapper.setMute(mute)
        val zoneId = getZoneId(daoManager, guild.idLong)
        val privZoneId = getZoneId(daoManager, guild.idLong, targetUser.idLong)

        //Normal success path
        val msg = getUnmuteMessage(language, zoneId, guild, targetUser, muteAuthor, context.author, mute)
        val privateChannel = targetUser.openPrivateChannel().awaitOrNull()

        val success = try {
            privateChannel?.let {
                sendEmbed(it, msg)
                true
            } ?: false
        } catch (t: Throwable) {
            false
        }

        val msgLc = getUnmuteMessage(language, privZoneId, guild, targetUser, muteAuthor, context.author, mute, true, targetUser.isBot, success)

        val logChannel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.UNMUTE)
        logChannel?.let { it1 -> sendEmbed(daoManager.embedDisabledWrapper, it1, msgLc) }

        val successMsg = context.getTranslation("$root.success")
            .replace(PLACEHOLDER_USER, targetUser.asTag)
            .replace("%reason%", unmuteReason)

        sendMsg(context, successMsg)
    }
}


fun getUnmuteMessage(
    language: String,
    zoneId: ZoneId,
    guild: Guild,
    mutedUser: User?,
    muteAuthor: User?,
    unmuteAuthor: User,
    mute: Mute,
    lc: Boolean = false,
    isBot: Boolean = false,
    received: Boolean = true,
    failedCause: String? = null
): MessageEmbed {

    val eb = EmbedBuilder()

    val muteDuration = mute.endTime?.let { endTime ->
        getDurationString((endTime - mute.startTime))
    } ?: i18n.getTranslation(language, "infinite")

    var description = "```LDIF\n"
    if (!lc) {
        description += i18n.getTranslation(language, "message.punishment.description.nlc")
            .replace("%guildName%", guild.name)
            .replace("%guildId%", guild.id)
    }

    val deletedAccount = i18n.getTranslation(language, "message.deletedaccount")
    description += i18n.getTranslation(language, "message.punishment.unmute.description")
        .replace("%muteAuthor%", muteAuthor?.asTag ?: deletedAccount)
        .replace("%muteAuthorId%", mute.muteAuthorId.toString())
        .replace("%unMuteAuthorId%", mute.unmuteAuthorId.toString())
        .replace("%unMuted%", mutedUser?.asTag ?: deletedAccount)
        .replace("%unMutedId%", mute.mutedId.toString())
        .replace("%muteReason%", mute.reason)
        .replace("%unmuteReason%", mute.unmuteReason ?: "/")
        .replace("%duration%", muteDuration)
        .replace("%startTime%", (mute.startTime.asEpochMillisToDateTime(zoneId)))
        .replace("%endTime%", (mute.endTime?.asEpochMillisToDateTime(zoneId) ?: "none"))

    var extraDesc: String = if (!received || isBot) {
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
    if (failedCause != null) {
        extraDesc += i18n.getTranslation(language,
            "message.punishment.extra.failed"
        ).replace("%cause%", failedCause)
    }

    description += extraDesc
    description += "```"

    val author = i18n.getTranslation(language, "message.punishment.unmute.author")
        .replace(PLACEHOLDER_USER, unmuteAuthor.asTag)
        .replace("%spaces%", " ".repeat(45).substring(0, 45 - unmuteAuthor.name.length) + "\u200B")

    eb.setAuthor(author, null, unmuteAuthor.effectiveAvatarUrl)
    eb.setDescription(description)
    eb.setThumbnail(mutedUser?.effectiveAvatarUrl)
    eb.setColor(Color.GREEN)
    return eb.build()
}