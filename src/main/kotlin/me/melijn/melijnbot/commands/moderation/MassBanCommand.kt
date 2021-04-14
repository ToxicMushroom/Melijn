package me.melijn.melijnbot.commands.moderation


import me.melijn.melijnbot.database.ban.Ban
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
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
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import java.time.ZoneId

class MassBanCommand : AbstractCommand("command.massban") {

    init {
        name = "massban"
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
        val users = mutableListOf<User>()
        for (i in 0 until size) {
            if (context.args[i] == "-r") {
                break
            }
            offset++
            (retrieveUserByArgsNMessage(context, i) ?: return).let { users.add(i, it) }
        }


        var reason = context.getRawArgPart(1 + offset)

        if (reason.isBlank()) reason = "/"
        reason = reason.trim()

        val messages = mutableListOf<String>()
        var i = 0
        val banning = context.getTranslation("message.banning")
        for (targetUser in users) {
            val activeBan: Ban? = context.daoManager.banWrapper.getActiveBan(context.guildId, targetUser.idLong)
            val ban = Ban(
                context.guildId,
                targetUser.idLong,
                context.authorId,
                reason,
                null
            )
            if (activeBan != null) {
                ban.banId = activeBan.banId
                ban.startTime = activeBan.startTime
            }

            val privateChannel = if (users.size < 11 && context.guild.retrieveMember(targetUser).awaitBool()) {
                targetUser.openPrivateChannel().awaitOrNull()
            } else {
                null
            }
            val message: Message? = privateChannel?.let {
                sendMsgAwaitEL(it, banning)
            }?.firstOrNull()


            messages.add(i, continueBanning(context, targetUser, ban, activeBan, deldays, message))
            i++
        }
        val separator = "\n"
        val string = java.lang.String.join(separator, messages)

        val ban = Ban(
            context.guildId,
            users[0].idLong,
            context.authorId,
            reason,
            null
        )

        val bannedMessageLc = getBanMessage(context.getLanguage(), context.getTimeZoneId(), context.guild, users, context.author, ban)
        val doaManager = context.daoManager
        val logChannel = context.guild.getAndVerifyLogChannelByType(doaManager, LogChannelType.PERMANENT_BAN)
        logChannel?.let{
            sendEmbed(doaManager.embedDisabledWrapper, it, bannedMessageLc)
        }
        sendRsp(context, string)
    }

    private suspend fun continueBanning(
        context: ICommandContext,
        targetUser: User,
        ban: Ban,
        activeBan: Ban?,
        deldays: Int,
        banningMessage: Message? = null,
    ): String {
        val guild = context.guild
        val author = context.author
        val language = context.getLanguage()
        val daoManager = context.daoManager
        val privZoneId = getZoneId(daoManager, guild.idLong, targetUser.idLong)
        val bannedMessageDm = getBanMessage(language, privZoneId, guild, targetUser, author, ban)

        val msg = try {
            guild.ban(targetUser, deldays)
                .reason("(massBan) " + context.author.asTag + ": " + ban.reason)
                .async { daoManager.banWrapper.setBan(ban) }

            banningMessage?.editMessage(
                bannedMessageDm
            )?.override(true)?.queue()


            context.getTranslation("$root.success" + if (activeBan != null) ".updated" else "")
                .withSafeVariable(PLACEHOLDER_USER, targetUser.asTag)
                .withSafeVarInCodeblock("reason", ban.reason)

        } catch (t: Throwable) {
            val failedMsg = context.getTranslation("message.banning.failed")
            banningMessage?.editMessage(failedMsg)?.queue()

            context.getTranslation("$root.failure")
                .withSafeVariable(PLACEHOLDER_USER, targetUser.asTag)
                .withSafeVarInCodeblock("cause", t.message ?: "/")
        }
        return msg
    }

    fun getBanMessage(
        language: String,
        zoneId: ZoneId,
        guild: Guild,
        bannedUsers: List<User>,
        banAuthor: User,
        ban: Ban,
        lc: Boolean = false,
        isBot: Boolean = false,
        received: Boolean = true
    ): MessageEmbed {
        val banDuration = ban.endTime?.let { endTime ->
            getDurationString((endTime - ban.startTime))
        } ?: i18n.getTranslation(language, "infinite")

        var description = "```LDIF\n"
        if (!lc) {
            description += i18n.getTranslation(language, "message.punishment.description.nlc")
                .withSafeVarInCodeblock("serverName", guild.name)
                .withVariable("serverId", guild.id)
        }

        val bannedList = bannedUsers.joinToString(separator = "\n- ", prefix = "\n- ") {  "${it.id} - [${it.asTag}]" }

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

        return EmbedBuilder()
            .setAuthor(author, null, banAuthor.effectiveAvatarUrl)
            .setColor(0xDA31FC)
            .setDescription(description)
            .build()
    }
}

