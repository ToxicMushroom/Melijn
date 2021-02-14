package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.database.ban.Ban
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.enums.SpecialPermission
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.hasPermission
import me.melijn.melijnbot.internals.translation.MESSAGE_INTERACT_MEMBER_HIARCHYEXCEPTION
import me.melijn.melijnbot.internals.translation.MESSAGE_SELFINTERACT_MEMBER_HIARCHYEXCEPTION
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendEmbed
import me.melijn.melijnbot.internals.utils.message.sendMsgAwaitEL
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User

class TempBanCommand : AbstractCommand("command.tempban") {

    init {
        id = 23
        name = "tempBan"
        aliases = arrayOf("tBan", "temporaryBan")
        commandCategory = CommandCategory.MODERATION
        discordChannelPermissions = arrayOf(Permission.BAN_MEMBERS)
    }

    override suspend fun execute(context: ICommandContext) {
        if (context.args.size < 2) {
            sendSyntax(context)
            return
        }

        val guild = context.guild
        val targetUser = retrieveUserByArgsNMessage(context, 0) ?: return
        val member = guild.retrieveMember(targetUser).awaitOrNull()
        if (member != null) {
            if (!context.guild.selfMember.canInteract(member)) {
                val msg = context.getTranslation(MESSAGE_SELFINTERACT_MEMBER_HIARCHYEXCEPTION)
                    .withSafeVariable(PLACEHOLDER_USER, member.asTag)
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
                    .withSafeVariable(PLACEHOLDER_USER, member.asTag)
                sendRsp(context, msg)
                return
            }
        }

        // ban user <-t1> reason
        var deldays = 7
        var offset = 0
        if (context.args.size > 2){
            val firstArg = context.args[1]
            if (firstArg.matches(BanCommand.optionalDeldaysPattern)){
                offset = 1
                deldays = BanCommand.optionalDeldaysPattern.find(firstArg)?.groupValues?.get(1)?.toInt() ?: 0
            }
        }

        val durationArgs = context.args[offset+1].split(SPACE_PATTERN)
        val banDuration = (getDurationByArgsNMessage(context, 0, durationArgs.size, durationArgs) ?: return) * 1000

        var reason = context.getRawArgPart(offset+2)
        if (reason.isBlank()) reason = "/"

        val activeBan: Ban? = context.daoManager.banWrapper.getActiveBan(context.guildId, targetUser.idLong)
        val ban = Ban(
            context.guildId,
            targetUser.idLong,
            context.authorId,
            reason,
            null,
            endTime = System.currentTimeMillis() + banDuration
        )
        if (activeBan != null) {
            ban.banId = activeBan.banId
            ban.startTime = activeBan.startTime
        }

        val banning = context.getTranslation("message.banning")

        val privateChannel = if (context.guild.isMember(targetUser)) {
            targetUser.openPrivateChannel().awaitOrNull()
        } else {
            null
        }
        val message: Message? = privateChannel?.let {
            sendMsgAwaitEL(it, banning)
        }?.firstOrNull()

        continueBanning(context, targetUser, ban, activeBan, message, deldays)
    }

    private suspend fun continueBanning(
        context: ICommandContext,
        targetUser: User,
        ban: Ban,
        activeBan: Ban?,
        banningMessage: Message?,
        deldays: Int
    ) {
        val guild = context.guild
        val author = context.author
        val language = context.getLanguage()
        val daoManager = context.daoManager
        val zoneId = getZoneId(daoManager, guild.idLong)
        val privZoneId = getZoneId(daoManager, guild.idLong, targetUser.idLong)

        val bannedMessageDm = getBanMessage(language, privZoneId, guild, targetUser, author, ban)
        val bannedMessageLc = getBanMessage(
            language,
            zoneId,
            guild,
            targetUser,
            author,
            ban,
            true,
            targetUser.isBot,
            banningMessage != null
        )



        try {
            context.guild.ban(targetUser, deldays).reason("(tempBan) ${context.author.asTag}: " + ban.reason)
                .async { daoManager.banWrapper.setBan(ban) }
            banningMessage?.editMessage(
                bannedMessageDm
            )?.override(true)?.queue()

            val logChannelWrapper = daoManager.logChannelWrapper
            val logChannelId = logChannelWrapper.getChannelId(guild.idLong, LogChannelType.TEMP_BAN)
            val logChannel = guild.getTextChannelById(logChannelId)
            logChannel?.let { it1 -> sendEmbed(daoManager.embedDisabledWrapper, it1, bannedMessageLc) }

            val endTime = ban.endTime?.asEpochMillisToDateTime(zoneId)

            val msg = context.getTranslation("$root.success" + if (activeBan != null) ".updated" else "")
                .withSafeVariable(PLACEHOLDER_USER, targetUser.asTag)
                .withVariable("endTime", endTime ?: "none")
                .withSafeVariable("reason", ban.reason)
            sendRsp(context, msg)
        } catch (t: Throwable) {

            val failedMsg = context.getTranslation("message.banning.failed")
            banningMessage?.editMessage(failedMsg)?.queue()

            val msg = context.getTranslation("$root.failure")
                .withSafeVariable(PLACEHOLDER_USER, targetUser.asTag)
                .withSafeVariable("cause", t.message ?: "/")
            sendRsp(context, msg)
        }
    }
}