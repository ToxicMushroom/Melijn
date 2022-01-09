package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.commandutil.moderation.ModUtil
import me.melijn.melijnbot.database.mute.Mute
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.enums.RoleType
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.models.EmbedEditor
import me.melijn.melijnbot.internals.models.ModularMessage
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
import java.awt.Color

class MuteCommand : AbstractCommand("command.mute") {

    init {
        id = 28
        name = "mute"
        aliases = arrayOf("m")
        commandCategory = CommandCategory.MODERATION
        discordChannelPermissions = arrayOf(Permission.MANAGE_ROLES)
    }

    companion object {
        /** Helper function to create a ban object based on active ban, or just create a new one.
         *  @returns created ban object AND boolean == based on the active ban?
         **/
        suspend fun createMuteFromActiveOrNew(
            context: ICommandContext,
            muted: User,
            reason: String
        ): Pair<Mute, Boolean> {
            val activeMute = context.daoManager.muteWrapper.getActiveMute(context.guildId, muted.idLong)
            val mute = Mute(context.guildId, muted.idLong, context.authorId, reason, null)
            if (activeMute != null) {
                mute.muteId = activeMute.muteId
                mute.startTime = activeMute.startTime
            }
            return mute to (activeMute != null)
        }

        fun getDefaultMessage(logChannel: Boolean): ModularMessage {
            val embed = EmbedEditor().apply {
                setAuthor("{punishAuthorTag}{titleSpaces:{punishAuthorTag}}", null, "{punishAuthorAvatarUrl}")
                setColor(Color.BLUE)
                setThumbnail("{punishedUserAvatarUrl}")
                setDescription("```LDIF\n")
                if (!logChannel) appendDescription("Server: {serverName}\nServer Id: {serverId}\n")
                appendDescription(
                    """
            Mute Author: {punishAuthorTag}
            Mute Author Id: {punishAuthorId}
            Muted: {punishedUserTag}
            Muted Id: {punishedUserId}
            Reason: {reason}
            Duration: {timeDuration}
            Start of mute: {startTime:${if (logChannel) "null" else "{punishedUserId}"}}
            End of mute: {endTime:${if (logChannel) "null" else "{punishedUserId}"}}
            Case Id: {punishId}{if:{extraLcInfo}|=|null|then:|else:{extraLcInfo}}
        """.trimIndent()
                )
                appendDescription("```")
            }.build()
            return ModularMessage(null, embed)
        }

    }

    override suspend fun execute(context: ICommandContext) {
        if (argSizeCheckFailed(context, 0)) return

        val targetUser = retrieveUserByArgsNMessage(context, 0) ?: return
        val member = context.guild.retrieveMember(targetUser).awaitOrNull()
        if (member != null) if (ModUtil.cantPunishAndReply(context, member)) return

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
        val (mute, isActive) = createMuteFromActiveOrNew(context, targetUser, reason)
        val muting = context.getTranslation("message.muting")

        val pc = if (context.guild.isMember(targetUser)) targetUser.openPrivateChannel().awaitOrNull()
        else null

        val message = pc?.let { sendMsgAwaitEL(it, muting) }?.firstOrNull()
        continueMuting(context, muteRole, targetUser, mute, isActive, message)
    }

    private suspend fun continueMuting(
        context: ICommandContext,
        muteRole: Role,
        targetUser: User,
        mute: Mute,
        isActive: Boolean,
        mutingMessage: Message?
    ) {
        val guild = context.guild
        val author = context.author
        val language = context.getLanguage()
        val daoManager = context.daoManager
        val mutedMessageDm = getTempPunishMessage(language, daoManager, guild, targetUser, author, mute, msgType = MessageType.MUTE)
        val mutedMessageLc = getTempPunishMessage(language, daoManager, guild, targetUser, author, mute,
            true, targetUser.isBot, MessageType.MUTE_LOG)

        val targetMember = guild.retrieveMember(targetUser).awaitOrNull()

        val msg = try {
            if (targetMember != null) {
                guild.addRoleToMember(targetMember, muteRole)
                    .reason("(mute) ${context.author.asTag}: " + mute.reason)
                    .async { daoManager.muteWrapper.setMute(mute) }
            } else daoManager.muteWrapper.setMute(mute)

            mutingMessage?.editMessage(mutedMessageDm)?.override(true)?.queue()

            val logChannel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.PERMANENT_MUTE)
            logChannel?.let { it1 -> sendMsg(it1, context.webManager.proxiedHttpClient, mutedMessageLc) }

            context.getTranslation("$root.success" + if (isActive) ".updated" else "")
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