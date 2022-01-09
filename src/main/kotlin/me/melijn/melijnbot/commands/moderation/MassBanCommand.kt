package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.commandutil.moderation.ModUtil
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.database.ban.Ban
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.jagtag.MassPunishJagTagParserArgs
import me.melijn.melijnbot.internals.jagtag.MassPunishmentJagTagParser
import me.melijn.melijnbot.internals.models.EmbedEditor
import me.melijn.melijnbot.internals.models.ModularMessage
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyLogChannelByType
import me.melijn.melijnbot.internals.utils.message.MessageSplitter
import me.melijn.melijnbot.internals.utils.message.sendMsg
import me.melijn.melijnbot.internals.utils.message.sendRsp
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import java.awt.Color

class MassBanCommand : AbstractCommand("command.massban") {

    init {
        name = "massBan"
        aliases = arrayOf("mb")
        commandCategory = CommandCategory.MODERATION
        discordChannelPermissions = arrayOf(Permission.BAN_MEMBERS)
    }

    override suspend fun execute(context: ICommandContext) {
        if (argSizeCheckFailed(context, 1)) return

        val delDays = 7
        var offset = 0
        val size = context.args.size
        val failedRetrieves = mutableListOf<Int>() // arg indexes of failed arguments
        val users = mutableMapOf<User, Member?>()
        for (i in 0 until size) {
            if (context.args[i] == "-r") {
                break
            }
            offset++
            val user = retrieveUserByArgsNMessage(context, i)
            if (user == null) {
                failedRetrieves.add(i)
                if (failedRetrieves.size > 3 && failedRetrieves.size > users.size) {
                    sendRsp(
                        context,
                        "Failed to retrieve: ```${
                            failedRetrieves.joinToString(", ") { context.args[it] }.escapeCodeblockMarkdown()
                        }```\n" +
                            "Stopping massban because more then half of the checked users have failed to retrieve, please remove these users from the command and try again."
                    )
                    return
                }

                continue
            }

            val member = context.guild.retrieveMember(user).awaitOrNull()
            if (member != null) if (ModUtil.cantPunishAndReply(context, member)) return

            users[user] = member
        }

        var reason = context.getRawArgPart(1 + offset)

        if (reason.isBlank()) reason = "/"
        reason = reason.trim()

        var success = 0
        var updated = 0
        var failed = 0

        for ((targetUser, member) in users) {
            val (ban, updatedBan) = BanCommand.createBanFromActiveOrNew(context, targetUser, reason)
            updated += if (updatedBan) 1 else 0

            val message = if (users.size < 11 && member != null)
                TempBanCommand.sendBanningDM(context, member.user)
            else null

            if (continueBanning(context, targetUser, ban, delDays, message)) success++
            else failed++
        }
        failed += failedRetrieves.size

        val msg = context.getTranslation("$root.banned.${if (failed == 0) "success" else "ok"}")
            .withVariable("success", success)
            .withVariable("failed", failed)
            .withSafeVarInCodeblock("reason", reason)

        val ban = Ban(context.guildId, -1, context.authorId, reason, null)

        val bannedMessageLc =
            getMassBanMessage(
                context.daoManager,
                context.guild,
                users.keys,
                context.author,
                ban
            )
        val doaManager = context.daoManager
        val logChannel = context.guild.getAndVerifyLogChannelByType(doaManager, LogChannelType.MASS_BAN)
        logChannel?.let {
            for (msgEb in bannedMessageLc)
                sendMsg(it, context.webManager.proxiedHttpClient, msgEb)
        }
        sendRsp(context, msg)
    }

    private suspend fun continueBanning(
        context: ICommandContext,
        targetUser: User,
        ban: Ban,
        delDays: Int,
        banningMessage: Message? = null,
    ): Boolean {
        val guild = context.guild
        val author = context.author
        val language = context.getLanguage()
        val daoManager = context.daoManager
        val bannedMessageDm = getTempPunishMessage(
            language, daoManager, guild, targetUser, author, ban,
            msgType = MessageType.BAN
        )

        return try {
            guild.ban(targetUser, delDays)
                .reason("(massBan) " + context.author.asTag + ": " + ban.reason)
                .async { daoManager.banWrapper.setBan(ban) }

            banningMessage?.editMessage(bannedMessageDm)?.override(true)?.queue()
            true
        } catch (t: Throwable) {
            val failedMsg = context.getTranslation("message.banning.failed")
            banningMessage?.editMessage(failedMsg)?.queue()
            false
        }
    }

    private suspend fun getMassBanMessage(
        daoManager: DaoManager,
        guild: Guild,
        bannedUsers: Set<User>,
        banAuthor: User,
        ban: Ban
    ): List<ModularMessage> {
        val banDm = daoManager.linkedMessageWrapper.getMessage(guild.idLong, MessageType.MASS_BAN_LOG)?.let {
            daoManager.messageWrapper.getMessage(guild.idLong, it)
        } ?: getDefaultMessage()

        val zoneId = getZoneId(daoManager, guild.idLong, null)
        val args = MassPunishJagTagParserArgs(
            banAuthor, bannedUsers, null, daoManager, ban.reason, ban.unbanReason,
            ban.startTime, ban.endTime, ban.banId, zoneId, guild
        )

        val message = banDm.mapAllStringFieldsSafeSplitting(messageSplitter = MessageSplitter.EmbedLdif) {
            if (it != null) MassPunishmentJagTagParser.parseJagTag(args, it)
            else null
        }

        return message
    }


    companion object {
        fun getDefaultMessage(): ModularMessage {
            val embed = EmbedEditor().apply {
                setAuthor("{punishAuthorTag}{titleSpaces:{punishAuthorTag}}", null, "{punishAuthorAvatarUrl}")
                setColor(Color(0xDA31FC))
                setDescription("```LDIF\n")
                appendDescription(
                    """
            Ban Author: {punishAuthorTag}
            Ban Author Id: {punishAuthorId}
            BannedList: {punishList}
            Reason: {reason}
            Duration: {timeDuration}
            Start of ban: {startTime:null}
            End of ban: {endTime:null}
            Case Id: {punishId}{if:{extraLcInfo}|=|null|then:|else:{extraLcInfo}}
        """.trimIndent()
                )
                appendDescription("```")
            }.build()
            return ModularMessage(null, embed)
        }
    }
}


