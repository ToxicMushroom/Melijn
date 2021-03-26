package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.database.mute.Mute
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.enums.RoleType
import me.melijn.melijnbot.enums.SpecialPermission
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.hasPermission
import me.melijn.melijnbot.internals.translation.MESSAGE_INTERACT_MEMBER_HIARCHYEXCEPTION
import me.melijn.melijnbot.internals.translation.MESSAGE_SELFINTERACT_MEMBER_HIARCHYEXCEPTION
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.internals.translation.i18n
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyLogChannelByType
import me.melijn.melijnbot.internals.utils.message.sendEmbed
import me.melijn.melijnbot.internals.utils.message.sendMsgAwaitEL
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.*
import java.awt.Color
import java.time.ZoneId

class MuteCommand : AbstractCommand("command.mute") {

    init {
        id = 28
        name = "mute"
        aliases = arrayOf("m")
        commandCategory = CommandCategory.MODERATION
        discordChannelPermissions = arrayOf(Permission.MANAGE_ROLES)
    }

    override suspend fun execute(context: ICommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }

        val targetUser = retrieveUserByArgsNMessage(context, 0) ?: return
        val member = context.guild.retrieveMember(targetUser).awaitOrNull()
        if (member != null) {
            if (!context.guild.selfMember.canInteract(member)) {
                val msg = context.getTranslation(MESSAGE_SELFINTERACT_MEMBER_HIARCHYEXCEPTION)
                    .withSafeVariable(PLACEHOLDER_USER, targetUser.asTag)
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
                    .withSafeVariable(PLACEHOLDER_USER, targetUser.asTag)
                sendRsp(context, msg)
                return
            }
        }

        var reason = context.rawArg
            .removeFirst(context.args[0])
            .trim()
        if (reason.isBlank()) reason = "/"

        reason = reason.trim()

        val roleId = context.daoManager.roleWrapper.getRoleId(context.guildId, RoleType.MUTE)
        val muteRole: Role? = context.guild.getRoleById(roleId)
        if (muteRole == null) {
            val msg = context.getTranslation("message.creatingmuterole")
            sendRsp(context, msg)

            try {
                val role = context.guild.createRole()
                    .setName("muted")
                    .setMentionable(false)
                    .setHoisted(false)
                    .setPermissions(Permission.MESSAGE_READ)
                    .reason("mute role was unset, creating one")
                    .await()

                muteRoleAquired(context, targetUser, reason, role)
            } catch (t: Throwable) {
                val msgFailed = context.getTranslation("message.creatingmuterole.failed")
                    .withSafeVarInCodeblock("cause", t.message ?: "/")
                sendRsp(context, msgFailed)
            }

            return
        } else {
            muteRoleAquired(context, targetUser, reason, muteRole)
        }
    }

    private suspend fun muteRoleAquired(context: ICommandContext, targetUser: User, reason: String, muteRole: Role) {
        val activeMute: Mute? = context.daoManager.muteWrapper.getActiveMute(context.guildId, targetUser.idLong)
        val mute = Mute(
            context.guildId,
            targetUser.idLong,
            context.authorId,
            reason,
            null,
            endTime = null
        )

        if (activeMute != null) {
            mute.muteId = activeMute.muteId
            mute.startTime = activeMute.startTime
        }

        val muting = context.getTranslation("message.muting")

        val privateChannel = if (context.guild.isMember(targetUser)) {
            targetUser.openPrivateChannel().awaitOrNull()
        } else {
            null
        }

        val message: Message? = privateChannel?.let {
            sendMsgAwaitEL(it, muting)
        }?.firstOrNull()

        continueMuting(context, muteRole, targetUser, mute, activeMute, message)
    }

    private suspend fun continueMuting(
        context: ICommandContext,
        muteRole: Role,
        targetUser: User,
        mute: Mute,
        activeMute: Mute?,
        mutingMessage: Message?
    ) {
        val guild = context.guild
        val author = context.author
        val language = context.getLanguage()
        val daoManager = context.daoManager
        val zoneId = getZoneId(daoManager, guild.idLong)
        val privZoneId = getZoneId(daoManager, guild.idLong, targetUser.idLong)
        val mutedMessageDm = getMuteMessage(language, privZoneId, guild, targetUser, author, mute)
        val mutedMessageLc = getMuteMessage(
            language,
            zoneId,
            guild,
            targetUser,
            author,
            mute,
            true,
            targetUser.isBot,
            mutingMessage != null
        )


        val targetMember = guild.retrieveMember(targetUser).awaitOrNull() ?: return

        val msg = try {
            guild.addRoleToMember(targetMember, muteRole)
                .reason("(mute) ${context.author.asTag}: " + mute.reason)
                .await()

            mutingMessage?.editMessage(
                mutedMessageDm
            )?.override(true)?.async { context.daoManager.muteWrapper.setMute(mute) }

            val logChannel = guild.getAndVerifyLogChannelByType(context.daoManager, LogChannelType.PERMANENT_MUTE)
            logChannel?.let { it1 -> sendEmbed(context.daoManager.embedDisabledWrapper, it1, mutedMessageLc) }


            context.getTranslation("$root.success" + if (activeMute != null) ".updated" else "")
                .withSafeVariable(PLACEHOLDER_USER, targetUser.asTag)
                .withSafeVarInCodeblock("reason", mute.reason)
        } catch (t: Throwable) {

            val failedMsg = context.getTranslation("message.muting.failed")
            mutingMessage?.editMessage(failedMsg)?.queue()

            context.getTranslation("$root.failure")
                .withSafeVariable(PLACEHOLDER_USER, targetUser.asTag)
                .withSafeVarInCodeblock("cause", t.message ?: "/")
        }
        sendRsp(context, msg)
    }
}

fun getMuteMessage(
    language: String,
    zoneId: ZoneId,
    guild: Guild,
    mutedUser: User,
    muteAuthor: User,
    mute: Mute,
    lc: Boolean = false,
    isBot: Boolean = false,
    received: Boolean = true
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

    description += i18n.getTranslation(language, "message.punishment.mute.description")
        .withSafeVarInCodeblock("muteAuthor", muteAuthor.asTag)
        .withVariable("muteAuthorId", muteAuthor.id)
        .withSafeVarInCodeblock("muted", mutedUser.asTag)
        .withVariable("mutedId", mutedUser.id)
        .withSafeVarInCodeblock("reason", mute.reason)
        .withVariable("duration", muteDuration)
        .withVariable("startTime", (mute.startTime.asEpochMillisToDateTime(zoneId)))
        .withVariable("endTime", (mute.endTime?.asEpochMillisToDateTime(zoneId) ?: "none"))
        .withVariable("muteId", mute.muteId)

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

    val author = i18n.getTranslation(language, "message.punishment.mute.author")
        .withSafeVariable(PLACEHOLDER_USER, muteAuthor.asTag)
        .withVariable("spaces", getAtLeastNCodePointsAfterName(muteAuthor) + "\u200B")

    return EmbedBuilder()
        .setAuthor(author, null, muteAuthor.effectiveAvatarUrl)
        .setDescription(description)
        .setThumbnail(mutedUser.effectiveAvatarUrl)
        .setColor(Color.BLUE)
        .build()
}