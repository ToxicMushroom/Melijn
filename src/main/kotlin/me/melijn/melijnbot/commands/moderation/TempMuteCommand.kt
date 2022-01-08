package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.commandutil.moderation.ModUtil
import me.melijn.melijnbot.database.mute.Mute
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.enums.RoleType
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyLogChannelByType
import me.melijn.melijnbot.internals.utils.message.sendMsg
import me.melijn.melijnbot.internals.utils.message.sendMsgAwaitEL
import me.melijn.melijnbot.internals.utils.message.sendRsp
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User

class TempMuteCommand : AbstractCommand("command.tempmute") {

    init {
        id = 27
        name = "tempMute"
        aliases = arrayOf("tm", "temporaryMute")
        commandCategory = CommandCategory.MODERATION
        discordChannelPermissions = arrayOf(Permission.MANAGE_ROLES)
    }

    override suspend fun execute(context: ICommandContext) {
        if (argSizeCheckFailed(context, 1)) return

        val targetUser = retrieveUserByArgsNMessage(context, 0) ?: return
        val member = context.guild.retrieveMember(targetUser).awaitOrNull()
        if (member != null) if (ModUtil.cantPunishAndReply(context, member)) return

        val durationArgs = context.args[1].split(SPACE_PATTERN)
        val muteDuration = (getDurationByArgsNMessage(context, 0, durationArgs.size, durationArgs) ?: return) * 1000

        var reason = context.getRawArgPart(2)
        if (reason.isBlank()) reason = "/"

        reason = reason.trim()

        val roleWrapper = context.daoManager.roleWrapper
        val roleId = roleWrapper.getRoleId(context.guildId, RoleType.MUTE)
        var muteRole: Role? = context.guild.getRoleById(roleId)
        if (muteRole == null) {
            val msg = context.getTranslation("message.creatingmuterole")
            sendRsp(context, msg)

            try {
                muteRole = context.guild.createRole()
                    .setName("muted")
                    .setMentionable(false)
                    .setHoisted(false)
                    .setPermissions(Permission.MESSAGE_READ)
                    .reason("Missing mute role, creating one")
                    .await()

                roleWrapper.setRole(context.guildId, RoleType.MUTE, muteRole.idLong)
            } catch (t: Throwable) {
                val msgFailed = context.getTranslation("message.creatingmuterole.failed")
                    .withSafeVarInCodeblock("cause", t.message ?: "/")
                sendRsp(context, msgFailed)
            }

            if (muteRole == null) return
        }

        muteRoleAcquired(context, targetUser, reason, muteRole, muteDuration)
    }

    private suspend fun muteRoleAcquired(
        context: ICommandContext,
        targetUser: User,
        reason: String,
        muteRole: Role,
        muteDuration: Long
    ) {
        val activeMute: Mute? = context.daoManager.muteWrapper.getActiveMute(context.guildId, targetUser.idLong)
        val mute = Mute(
            context.guildId,
            targetUser.idLong,
            context.authorId,
            reason,
            null,
            endTime = System.currentTimeMillis() + muteDuration
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
        val mutedMessageDm =
            getTempPunishMessage(language, daoManager, guild, targetUser, author, mute, msgType = MessageType.MUTE)
        val mutedMessageLc = getTempPunishMessage(
            language, daoManager, guild, targetUser, author, mute,
            true, targetUser.isBot, MessageType.MUTE_LOG
        )

        val targetMember = guild.retrieveMember(targetUser).awaitOrNull()

        try {
            if (targetMember != null) {
                guild.addRoleToMember(targetMember, muteRole)
                    .reason("(tempMute) ${context.author.asTag}: " + mute.reason)
                    .async { daoManager.muteWrapper.setMute(mute) }
            } else daoManager.muteWrapper.setMute(mute)

            mutingMessage?.editMessage(mutedMessageDm)?.override(true)?.queue()
            val logChannel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.TEMP_MUTE)
            logChannel?.let { it1 -> sendMsg(it1, context.webManager.proxiedHttpClient, mutedMessageLc) }

            val msg = context.getTranslation("$root.success" + if (activeMute != null) ".updated" else "")
                .withSafeVariable(PLACEHOLDER_USER, targetUser.asTag)
                .withVariable("endTime", mute.endTime?.asEpochMillisToDateTime(context.getTimeZoneId()) ?: "none")
                .withSafeVarInCodeblock("reason", mute.reason)
            sendRsp(context, msg)
        } catch (t: Throwable) {
            val failedMsg = context.getTranslation("message.muting.failed")
            mutingMessage?.editMessage(failedMsg)?.queue()

            val msg = context.getTranslation("$root.failure")
                .withSafeVariable(PLACEHOLDER_USER, targetUser.asTag)
                .withSafeVarInCodeblock("cause", t.message ?: "/")
            sendRsp(context, msg)
        }
    }
}

