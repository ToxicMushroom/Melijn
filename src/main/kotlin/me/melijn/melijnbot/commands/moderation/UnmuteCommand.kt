package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.database.mute.Mute
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.enums.RoleType
import me.melijn.melijnbot.enums.SpecialPermission
import me.melijn.melijnbot.internals.command.*
import me.melijn.melijnbot.internals.translation.MESSAGE_INTERACT_MEMBER_HIARCHYEXCEPTION
import me.melijn.melijnbot.internals.translation.MESSAGE_SELFINTERACT_MEMBER_HIARCHYEXCEPTION
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.internals.translation.i18n
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyLogChannelByType
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyRoleByType
import me.melijn.melijnbot.internals.utils.message.sendEmbed
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
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
        discordChannelPermissions = arrayOf(Permission.MANAGE_ROLES)
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

        val muteAuthor = mute.muteAuthorId?.let { context.shardManager.retrieveUserById(it).awaitOrNull() }
        val targetMember = guild.retrieveMember(targetUser).awaitOrNull()

        val muteRole = guild.getAndVerifyRoleByType(daoManager, RoleType.MUTE)
        if (muteRole == null) {
            val msg = context.getTranslation("$root.nomuterole")
                .withVariable(PLACEHOLDER_PREFIX, context.usedPrefix)
            sendRsp(context, msg)
            return
        }

        if (targetMember != null && targetMember.roles.contains(muteRole)) {

            if (!context.guild.selfMember.canInteract(targetMember)) {
                val msg = context.getTranslation(MESSAGE_SELFINTERACT_MEMBER_HIARCHYEXCEPTION)
                    .withVariable(PLACEHOLDER_USER, targetMember.asTag)
                sendRsp(context, msg)
                return
            }
            if (!context.member.canInteract(targetMember) && !hasPermission(context, SpecialPermission.PUNISH_BYPASS_HIGHER.node, true)) {
                val msg = context.getTranslation(MESSAGE_INTERACT_MEMBER_HIARCHYEXCEPTION)
                    .withVariable(PLACEHOLDER_USER, targetMember.asTag)
                sendRsp(context, msg)
                return
            }


            val exception = guild.removeRoleFromMember(targetMember, muteRole)
                .reason("(unmute) ${context.author.asTag}: " + unmuteReason)
                .awaitEX()

            if (exception == null) {
                sendUnmuteLogs(context, targetUser, muteAuthor, mute, unmuteReason)
            } else {
                val msg = context.getTranslation("$root.failure")
                    .withVariable(PLACEHOLDER_USER, targetUser.asTag)
                    .withVariable("cause", exception.message ?: "/")
                sendRsp(context, msg)
            }

        } else if (targetMember == null) {
            sendUnmuteLogs(context, targetUser, muteAuthor, mute, unmuteReason)

        } else if (!targetMember.roles.contains(muteRole)) {
            val msg = context.getTranslation("$root.notmuted")
                .withVariable(PLACEHOLDER_USER, targetUser.asTag)

            sendRsp(context, msg)
        }

        if (activeMute != null) {
            daoManager.muteWrapper.setMute(mute)
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
        val privateChannel = if (context.guild.isMember(targetUser)) {
            targetUser.openPrivateChannel().awaitOrNull()
        } else {
            null
        }

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
            .withVariable(PLACEHOLDER_USER, targetUser.asTag)
            .withVariable("reason", unmuteReason)

        sendRsp(context, successMsg)
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
    val muteDuration = mute.endTime?.let { endTime ->
        getDurationString((endTime - mute.startTime))
    } ?: i18n.getTranslation(language, "infinite")

    var description = "```LDIF\n"
    if (!lc) {
        description += i18n.getTranslation(language, "message.punishment.description.nlc")
            .withVariable("serverName", guild.name)
            .withVariable("serverId", guild.id)
    }

    val deletedAccount = i18n.getTranslation(language, "message.deleted.user")
    description += i18n.getTranslation(language, "message.punishment.unmute.description")
        .withVariable("muteAuthor", muteAuthor?.asTag ?: deletedAccount)
        .withVariable("muteAuthorId", mute.muteAuthorId.toString())
        .withVariable("unMuteAuthorId", mute.unmuteAuthorId.toString())
        .withVariable("unMuted", mutedUser?.asTag ?: deletedAccount)
        .withVariable("unMutedId", mute.mutedId.toString())
        .withVariable("muteReason", mute.reason)
        .withVariable("unmuteReason", mute.unmuteReason ?: "/")
        .withVariable("duration", muteDuration)
        .withVariable("startTime", (mute.startTime.asEpochMillisToDateTime(zoneId)))
        .withVariable("endTime", (mute.endTime?.asEpochMillisToDateTime(zoneId) ?: "none"))
        .withVariable("muteId", mute.muteId)

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
        ).withVariable("cause", failedCause)
    }

    description += extraDesc
    description += "```"

    val author = i18n.getTranslation(language, "message.punishment.unmute.author")
        .withVariable(PLACEHOLDER_USER, unmuteAuthor.asTag)
        .withVariable("spaces", " ".repeat(45).substring(0, 45 - unmuteAuthor.name.length) + "\u200B")

    return EmbedBuilder()
        .setAuthor(author, null, unmuteAuthor.effectiveAvatarUrl)
        .setDescription(description)
        .setThumbnail(mutedUser?.effectiveAvatarUrl)
        .setColor(Color.GREEN)
        .build()
}