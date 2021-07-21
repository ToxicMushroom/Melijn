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
import java.time.ZoneId

class MassBanCommand : AbstractCommand("command.massban") {

    init {
        name = "massBan"
        aliases = arrayOf("mb")
        commandCategory = CommandCategory.MODERATION
        discordChannelPermissions = arrayOf(Permission.BAN_MEMBERS)
    }

    override suspend fun execute(context: ICommandContext) {
        if (context.args.size < 2) {
            sendSyntax(context)
            return
        }

        val deldays = 7
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
            if (member != null) {
                if (!context.guild.selfMember.canInteract(member)) {
                    val msg = context.getTranslation(MESSAGE_SELFINTERACT_MEMBER_HIARCHYEXCEPTION)
                        .withSafeVariable(PLACEHOLDER_USER, user.asTag)
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
                        .withSafeVariable(PLACEHOLDER_USER, user.asTag)
                    sendRsp(context, msg)
                    return
                }
            }
            users[user] = member
        }

        var reason = context.getRawArgPart(1 + offset)

        if (reason.isBlank()) reason = "/"
        reason = reason.trim()

        val banning = context.getTranslation("message.banning")
        var success = 0
        var updated = 0
        var failed = 0

        for ((targetUser, member) in users) {
            val (ban, updatedBan) = BanCommand.createBanFromActiveOrNew(context, targetUser, reason)
            updated += if (updatedBan) 1 else 0
            val privateChannel = if (users.size < 11 && member != null) {
                targetUser.openPrivateChannel().awaitOrNull()
            } else {
                null
            }
            val message: Message? = privateChannel?.let {
                sendMsgAwaitEL(it, banning)
            }?.firstOrNull()

            if (continueBanning(context, targetUser, ban, deldays, message)) success++
            else failed++
        }
        failed += failedRetrieves.size

        val msg = context.getTranslation("$root.banned.${if (failed == 0) "success" else "ok"}")
            .withVariable("success", success)
            .withVariable("failed", failed)
            .withSafeVarInCodeblock("reason", reason)

        val ban = Ban(
            context.guildId,
            -1,
            context.authorId,
            reason,
            null
        )

        val bannedMessageLc =
            getMassBanMessage(
                context.getLanguage(),
                context.getTimeZoneId(),
                context.guild,
                users.keys,
                context.author,
                ban
            )
        val doaManager = context.daoManager
        val logChannel = context.guild.getAndVerifyLogChannelByType(doaManager, LogChannelType.MASS_BAN)
        logChannel?.let {
            for (msgEb in bannedMessageLc)
                sendEmbed(doaManager.embedDisabledWrapper, it, msgEb)
        }
        sendRsp(context, msg)
    }

    private suspend fun continueBanning(
        context: ICommandContext,
        targetUser: User,
        ban: Ban,
        deldays: Int,
        banningMessage: Message? = null,
    ): Boolean {
        val guild = context.guild
        val author = context.author
        val language = context.getLanguage()
        val daoManager = context.daoManager
        val privZoneId = getZoneId(daoManager, guild.idLong, targetUser.idLong)
        val bannedMessageDm = getBanMessage(language, privZoneId, guild, targetUser, author, ban)

        return try {
            guild.ban(targetUser, deldays)
                .reason("(massBan) " + context.author.asTag + ": " + ban.reason)
                .async { daoManager.banWrapper.setBan(ban) }

            banningMessage?.editMessageEmbeds(
                bannedMessageDm
            )?.override(true)?.queue()
            true
        } catch (t: Throwable) {
            val failedMsg = context.getTranslation("message.banning.failed")
            banningMessage?.editMessage(failedMsg)?.queue()
            false
        }
    }

    private fun getMassBanMessage(
        language: String,
        zoneId: ZoneId,
        guild: Guild,
        bannedUsers: Set<User>,
        banAuthor: User,
        ban: Ban,
        lc: Boolean = false,
        isBot: Boolean = false,
        received: Boolean = true
    ): List<MessageEmbed> {
        val banDuration = ban.endTime?.let { endTime ->
            getDurationString((endTime - ban.startTime))
        } ?: i18n.getTranslation(language, "infinite")

        var description = "```LDIF\n"
        if (!lc) {
            description += i18n.getTranslation(language, "message.punishment.description.nlc")
                .withSafeVarInCodeblock("serverName", guild.name)
                .withVariable("serverId", guild.id)
        }

        val bannedList = bannedUsers.joinToString(separator = "\n- ", prefix = "\n- ") { "${it.id} - [${it.asTag}]" }

        description += i18n.getTranslation(language, "message.punishment.massban.description")
            .withSafeVarInCodeblock("banAuthor", banAuthor.asTag)
            .withVariable("banAuthorId", banAuthor.id)
            .withSafeVariable("bannedList", bannedList)
            .withSafeVarInCodeblock("reason", ban.reason)
            .withVariable("duration", banDuration)
            .withVariable("startTime", (ban.startTime.asEpochMillisToDateTime(zoneId)))
            .withVariable("endTime", (ban.endTime?.asEpochMillisToDateTime(zoneId) ?: "none"))
            .withVariable("banId", ban.banId)

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

        val author = i18n.getTranslation(language, "message.punishment.massban.author")
            .withVariable(PLACEHOLDER_USER, banAuthor.asTag)
            .withVariable("spaces", getAtLeastNCodePointsAfterName(banAuthor) + "\u200B")

        val hot = mutableListOf<MessageEmbed>()
        val parts = StringUtils.splitMessageWithCodeBlocks(description, MessageEmbed.DESCRIPTION_MAX_LENGTH, 64, "LDIF")
        val eb = EmbedBuilder()
            .setAuthor(author, null, banAuthor.effectiveAvatarUrl)
            .setColor(0xDA31FC)
        for ((index, part) in parts.withIndex()) {
            if (index != 0) eb.setAuthor(null)
            hot.add(
                eb
                    .setDescription(part)
                    .build()
            )
        }

        return hot
    }
}

