package me.melijn.melijnbot.commands.moderation

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.mute.Mute
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.enums.RoleType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.*
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
        discordPermissions = arrayOf(Permission.MANAGE_ROLES)
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.size < 2) {
            sendSyntax(context)
            return
        }

        val language = context.getLanguage()
        val targetUser = retrieveUserByArgsNMessage(context, 0) ?: return
        val member = context.guild.getMember(targetUser)
        if (member != null && !context.guild.selfMember.canInteract(member)) {

            val msg = i18n.getTranslation(language, "$root.cannotmute")
                .replace(PLACEHOLDER_USER, targetUser.asTag)
            sendMsg(context, msg)
            return
        }

        val noUserArg = context.rawArg
            .replaceFirst(context.args[0], "")
            .trim()
        val noReasonArgs = noUserArg.split(">")[0].trim().split("\\s+".toRegex())
        val muteDuration = (getDurationByArgsNMessage(context, noReasonArgs, 0, noReasonArgs.size) ?: return) * 1000

        var reason = if (noUserArg.contains(">"))
            noUserArg.substring(noUserArg.indexOfFirst { s -> s == '>' } + 1, noUserArg.length)
        else
            "/"


        reason = reason.trim()

        val roleWrapper = context.daoManager.roleWrapper
        val roleId = roleWrapper.roleCache.get(Pair(context.guildId, RoleType.MUTE)).await()
        var muteRole: Role? = context.guild.getRoleById(roleId)
        if (muteRole == null) {
            val msg = i18n.getTranslation(language, "$root.creatingmuterole")
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
                val msgFailed = i18n.getTranslation(language, "$root.failed.creatingmuterole")
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


        val language = context.getLanguage()
        val muting = i18n.getTranslation(language, "message.muting")
        try {
            val privateChannel = targetUser.openPrivateChannel().await()
            val message = privateChannel.sendMessage(muting).await()
            continueMuting(context, muteRole, targetUser, mute, activeMute, message)
        } catch (t: Throwable) {
            continueMuting(context, muteRole, targetUser, mute, activeMute)
        }
    }

    private suspend fun continueMuting(context: CommandContext, muteRole: Role, targetUser: User, mute: Mute, activeMute: Mute?, mutingMessage: Message? = null) {
        val guild = context.guild
        val author = context.author
        val language = context.getLanguage()
        val mutedMessageDm = getMuteMessage(language, guild, targetUser, author, mute)
        val mutedMessageLc = getMuteMessage(language, guild, targetUser, author, mute, true, targetUser.isBot, mutingMessage != null)
        context.daoManager.muteWrapper.setMute(mute)
        val targetMember = guild.getMember(targetUser)


        if (targetMember == null) {
            death(mutingMessage, mutedMessageDm, context, mutedMessageLc, activeMute, mute, targetUser)
            return
        }

        try {
            guild.addRoleToMember(targetMember, muteRole).await()
            death(mutingMessage, mutedMessageDm, context, mutedMessageLc, activeMute, mute, targetUser)
        } catch (t: Throwable) {
            val failedMsg = i18n.getTranslation(language, "message.muting.failed")
            mutingMessage?.editMessage(failedMsg)?.queue()

            val msg = i18n.getTranslation(language, "$root.failure")
                .replace(PLACEHOLDER_USER, targetUser.asTag)
                .replace("%cause%", t.message ?: "/")
            sendMsg(context, msg)
        }

    }

    private suspend fun death(mutingMessage: Message?, mutedMessageDm: MessageEmbed, context: CommandContext, mutedMessageLc: MessageEmbed, activeMute: Mute?, mute: Mute, targetUser: User) {
        mutingMessage?.editMessage(
            mutedMessageDm
        )?.override(true)?.queue()

        val logChannelWrapper = context.daoManager.logChannelWrapper
        val logChannelId = logChannelWrapper.logChannelCache.get(Pair(context.guildId, LogChannelType.TEMP_MUTE)).await()
        val logChannel = context.guild.getTextChannelById(logChannelId)
        logChannel?.let { it1 -> sendEmbed(context.daoManager.embedDisabledWrapper, it1, mutedMessageLc) }

        val language = context.getLanguage()
        val msg = i18n.getTranslation(language, "$root.success" + if (activeMute != null) ".updated" else "")
            .replace(PLACEHOLDER_USER, targetUser.asTag)
            .replace("%endTime%", mute.endTime?.asEpochMillisToDateTime() ?: "none")
            .replace("%reason%", mute.reason)
        sendMsg(context, msg)
    }
}

