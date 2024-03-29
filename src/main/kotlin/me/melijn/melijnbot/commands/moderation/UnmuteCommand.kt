package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.commandutil.moderation.ModUtil
import me.melijn.melijnbot.database.mute.Mute
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.enums.RoleType
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.PLACEHOLDER_PREFIX
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.internals.translation.i18n
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyLogChannelByType
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyRoleByType
import me.melijn.melijnbot.internals.utils.message.sendEmbed
import me.melijn.melijnbot.internals.utils.message.sendRsp
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

    override suspend fun execute(context: ICommandContext) {
        if (argSizeCheckFailed(context, 0)) return

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
            ?: Mute(
                context.guildId,
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
                .withSafeVariable(PLACEHOLDER_PREFIX, context.usedPrefix)
            sendRsp(context, msg)
            return
        }

        if (targetMember != null && targetMember.roles.contains(muteRole)) {
            if (ModUtil.cantPunishAndReply(context, targetMember)) return

            val exception = guild.removeRoleFromMember(targetMember, muteRole)
                .reason("(unmute) ${context.author.asTag}: " + unmuteReason)
                .awaitEX()

            if (exception == null) {
                sendUnmuteLogs(context, targetUser, muteAuthor, mute, unmuteReason)
            } else {
                val msg = context.getTranslation("$root.failure")
                    .withSafeVariable(PLACEHOLDER_USER, targetUser.asTag)
                    .withSafeVarInCodeblock("cause", exception.message ?: "/")
                sendRsp(context, msg)
            }

        } else if (targetMember == null) {
            sendUnmuteLogs(context, targetUser, muteAuthor, mute, unmuteReason)

        } else if (!targetMember.roles.contains(muteRole)) {
            val msg = context.getTranslation("$root.notmuted")
                .withSafeVariable(PLACEHOLDER_USER, targetUser.asTag)

            sendRsp(context, msg)
        }

        if (activeMute != null) {
            daoManager.muteWrapper.setMute(mute)
        }
    }

    private suspend fun sendUnmuteLogs(
        context: ICommandContext,
        targetUser: User,
        muteAuthor: User?,
        mute: Mute,
        unmuteReason: String
    ) {
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

        val msgLc = getUnmuteMessage(
            language,
            privZoneId,
            guild,
            targetUser,
            muteAuthor,
            context.author,
            mute,
            true,
            targetUser.isBot,
            success
        )

        val logChannel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.UNMUTE)
        logChannel?.let { it1 -> sendEmbed(daoManager.embedDisabledWrapper, it1, msgLc) }

        val successMsg = context.getTranslation("$root.success")
            .withSafeVariable(PLACEHOLDER_USER, targetUser.asTag)
            .withSafeVarInCodeblock("reason", unmuteReason)

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
            .withSafeVarInCodeblock("serverName", guild.name)
            .withVariable("serverId", guild.id)
    }

    val deletedAccount = i18n.getTranslation(language, "message.deleted.user")
    description += i18n.getTranslation(language, "message.punishment.unmute.description")
        .withSafeVarInCodeblock("muteAuthor", muteAuthor?.asTag ?: deletedAccount)
        .withVariable("muteAuthorId", mute.muteAuthorId.toString())
        .withVariable("unMuteAuthorId", mute.unmuteAuthorId.toString())
        .withSafeVarInCodeblock("unMuted", mutedUser?.asTag ?: deletedAccount)
        .withVariable("unMutedId", mute.mutedId.toString())
        .withSafeVarInCodeblock("muteReason", mute.reason)
        .withSafeVarInCodeblock("unmuteReason", mute.unmuteReason ?: "/")
        .withVariable("duration", muteDuration)
        .withVariable("startTime", (mute.startTime.asEpochMillisToDateTime(zoneId)))
        .withVariable("endTime", (mute.endTime?.asEpochMillisToDateTime(zoneId) ?: "none"))
        .withVariable("muteId", mute.muteId)

    var extraDesc: String = if (!received || isBot) {
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
    if (failedCause != null) {
        extraDesc += i18n.getTranslation(
            language,
            "message.punishment.extra.failed"
        ).withSafeVarInCodeblock("cause", failedCause)
    }

    description += extraDesc
    description += "```"

    val author = i18n.getTranslation(language, "message.punishment.unmute.author")
        .withSafeVariable(PLACEHOLDER_USER, unmuteAuthor.asTag)
        .withSafeVariable("spaces", getAtLeastNCodePointsAfterName(unmuteAuthor) + "\u200B")

    return EmbedBuilder()
        .setAuthor(author, null, unmuteAuthor.effectiveAvatarUrl)
        .setDescription(description)
        .setThumbnail(mutedUser?.effectiveAvatarUrl)
        .setColor(Color.GREEN)
        .build()
}