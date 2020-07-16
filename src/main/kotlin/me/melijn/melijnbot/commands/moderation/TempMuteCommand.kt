package me.melijn.melijnbot.commands.moderation

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.mute.Mute
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.enums.RoleType
import me.melijn.melijnbot.enums.SpecialPermission
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.command.hasPermission
import me.melijn.melijnbot.internals.translation.MESSAGE_INTERACT_MEMBER_HIARCHYEXCEPTION
import me.melijn.melijnbot.internals.translation.MESSAGE_SELFINTERACT_MEMBER_HIARCHYEXCEPTION
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyLogChannelByType
import me.melijn.melijnbot.internals.utils.message.sendEmbed
import me.melijn.melijnbot.internals.utils.message.sendMsgAwaitEL
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
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

    override suspend fun execute(context: CommandContext) {
        if (context.args.size < 2) {
            sendSyntax(context)
            return
        }

        val targetUser = retrieveUserByArgsNMessage(context, 0) ?: return
        val member = context.guild.retrieveMember(targetUser).awaitOrNull()
        if (member != null) {
            if (!context.guild.selfMember.canInteract(member)) {
                val msg = context.getTranslation(MESSAGE_SELFINTERACT_MEMBER_HIARCHYEXCEPTION)
                    .withVariable(PLACEHOLDER_USER, member.asTag)
                sendRsp(context, msg)
                return
            }
            if (!context.member.canInteract(member) && !hasPermission(context, SpecialPermission.PUNISH_BYPASS_HIGHER.node, true)) {
                val msg = context.getTranslation(MESSAGE_INTERACT_MEMBER_HIARCHYEXCEPTION)
                    .withVariable(PLACEHOLDER_USER, member.asTag)
                sendRsp(context, msg)
                return
            }
        }

        val durationArgs = context.args[1].split(SPACE_PATTERN)
        val muteDuration = (getDurationByArgsNMessage(context, 0, durationArgs.size, durationArgs) ?: return) * 1000

        var reason = context.getRawArgPart(2)
        if (reason.isBlank()) reason = "/"


        reason = reason.trim()

        val roleWrapper = context.daoManager.roleWrapper
        val roleId = roleWrapper.roleCache.get(Pair(context.guildId, RoleType.MUTE)).await()
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
                    .await()

                roleWrapper.setRole(context.guildId, RoleType.MUTE, muteRole.idLong)
            } catch (t: Throwable) {
                val msgFailed = context.getTranslation("message.creatingmuterole.failed")
                    .withVariable("cause", t.message ?: "/")
                sendRsp(context, msgFailed)
            }

            if (muteRole == null) return
        }

        muteRoleAcquired(context, targetUser, reason, muteRole, muteDuration)
    }

    private suspend fun muteRoleAcquired(context: CommandContext, targetUser: User, reason: String, muteRole: Role, muteDuration: Long) {
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

        val privateChannel = targetUser.openPrivateChannel().awaitOrNull()
        val message: Message? = privateChannel?.let {
            sendMsgAwaitEL(it, muting)
        }?.firstOrNull()

        continueMuting(context, muteRole, targetUser, mute, activeMute, message)
    }

    private suspend fun continueMuting(context: CommandContext, muteRole: Role, targetUser: User, mute: Mute, activeMute: Mute?, mutingMessage: Message?) {
        val guild = context.guild
        val author = context.author
        val language = context.getLanguage()
        val daoManager = context.daoManager
        val zoneId = getZoneId(daoManager, guild.idLong)
        val privZoneId = getZoneId(daoManager, guild.idLong, targetUser.idLong)
        val mutedMessageDm = getMuteMessage(language, privZoneId, guild, targetUser, author, mute)
        val mutedMessageLc = getMuteMessage(language, zoneId, guild, targetUser, author, mute, true, targetUser.isBot, mutingMessage != null)
        daoManager.muteWrapper.setMute(mute)
        val targetMember = guild.retrieveMember(targetUser).awaitOrNull()


        if (targetMember == null) {
            death(mutingMessage, mutedMessageDm, context, mutedMessageLc, activeMute, mute, targetUser)
            return
        }

        try {
            guild.addRoleToMember(targetMember, muteRole)
                .reason("muted")
                .await()
            death(mutingMessage, mutedMessageDm, context, mutedMessageLc, activeMute, mute, targetUser)

        } catch (t: Throwable) {
            val failedMsg = context.getTranslation("message.muting.failed")
            mutingMessage?.editMessage(failedMsg)?.queue()

            val msg = context.getTranslation("$root.failure")
                .withVariable(PLACEHOLDER_USER, targetUser.asTag)
                .withVariable("cause", t.message ?: "/")
            sendRsp(context, msg)
        }
    }

    private suspend fun death(mutingMessage: Message?, mutedMessageDm: MessageEmbed, context: CommandContext, mutedMessageLc: MessageEmbed, activeMute: Mute?, mute: Mute, targetUser: User) {
        mutingMessage?.editMessage(
            mutedMessageDm
        )?.override(true)?.queue()
        val daoManager = context.daoManager
        val logChannel = context.guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.TEMP_MUTE)
        logChannel?.let { it1 -> sendEmbed(daoManager.embedDisabledWrapper, it1, mutedMessageLc) }

        val msg = context.getTranslation("$root.success" + if (activeMute != null) ".updated" else "")
            .withVariable(PLACEHOLDER_USER, targetUser.asTag)
            .withVariable("endTime", mute.endTime?.asEpochMillisToDateTime(context.getTimeZoneId()) ?: "none")
            .withVariable("reason", mute.reason)
        sendRsp(context, msg)
    }
}

