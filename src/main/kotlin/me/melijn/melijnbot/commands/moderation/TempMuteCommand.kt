package me.melijn.melijnbot.commands.moderation

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.mute.Mute
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.enums.RoleType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.objects.utils.*
import me.melijn.melijnbot.objects.utils.checks.getAndVerifyLogChannelByType
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User

class TempMuteCommand : AbstractCommand("command.tempmute") {

    init {
        id = 27
        name = "tempmute"
        aliases = arrayOf("tm")
        commandCategory = CommandCategory.MODERATION
        discordChannelPermissions = arrayOf(Permission.MANAGE_ROLES)
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.size < 2) {
            sendSyntax(context)
            return
        }

        val targetUser = retrieveUserByArgsNMessage(context, 0) ?: return
        val member = context.guild.getMember(targetUser)
        if (member != null && !context.guild.selfMember.canInteract(member)) {

            val msg = context.getTranslation("message.interact.member.hierarchyexception")
                .replace(PLACEHOLDER_USER, targetUser.asTag)
            sendMsg(context, msg)
            return
        }

        val noUserArg = context.rawArg
            .removeFirst(context.args[0])
            .trim()
        val noReasonArgs = noUserArg.split(">")[0].trim().split("\\s+".toRegex())
        val muteDuration = (getDurationByArgsNMessage(context, 0, noReasonArgs.size, noReasonArgs) ?: return) * 1000

        var reason = if (noUserArg.contains(">"))
            noUserArg.substring(noUserArg.indexOfFirst { s -> s == '>' } + 1, noUserArg.length)
        else
            "/"


        reason = reason.trim()

        val roleWrapper = context.daoManager.roleWrapper
        val roleId = roleWrapper.roleCache.get(Pair(context.guildId, RoleType.MUTE)).await()
        var muteRole: Role? = context.guild.getRoleById(roleId)
        if (muteRole == null) {
            val msg = context.getTranslation("message.creatingmuterole")
            sendMsg(context, msg)

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
                    .replace("%cause%", t.message ?: "/")
                sendMsg(context, msgFailed)
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
        if (activeMute != null) mute.startTime = activeMute.startTime

        val muting = context.getTranslation("message.muting")

        val privateChannel = targetUser.openPrivateChannel().awaitOrNull()
        val message: Message? = privateChannel?.let {
            sendMsgEL(it, muting)
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
        val targetMember = guild.getMember(targetUser)


        if (targetMember == null) {
            death(mutingMessage, mutedMessageDm, context, mutedMessageLc, activeMute, mute, targetUser)
            return
        }

        try {
            guild.addRoleToMember(targetMember, muteRole).await()
            death(mutingMessage, mutedMessageDm, context, mutedMessageLc, activeMute, mute, targetUser)

        } catch (t: Throwable) {
            val failedMsg = context.getTranslation("message.muting.failed")
            mutingMessage?.editMessage(failedMsg)?.queue()

            val msg = context.getTranslation("$root.failure")
                .replace(PLACEHOLDER_USER, targetUser.asTag)
                .replace("%cause%", t.message ?: "/")
            sendMsg(context, msg)
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
            .replace(PLACEHOLDER_USER, targetUser.asTag)
            .replace("%endTime%", mute.endTime?.asEpochMillisToDateTime(context.getTimeZoneId()) ?: "none")
            .replace("%reason%", mute.reason)
        sendMsg(context, msg)
    }
}

