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
import me.melijn.melijnbot.internals.utils.message.sendEmbedAwaitEL
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import java.awt.Color
import java.time.ZoneId

class UnbanCommand : AbstractCommand("command.unban") {

    init {
        id = 25
        name = "unban"
        aliases = arrayOf("pardon")
        commandCategory = CommandCategory.MODERATION
        discordChannelPermissions = arrayOf(Permission.BAN_MEMBERS)
    }

    override suspend fun execute(context: ICommandContext) {
        val guild = context.guild
        val daoManager = context.daoManager
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }
        val language = context.getLanguage()
        val targetUser = retrieveUserByArgsNMessage(context, 0) ?: return

        var unbanReason = context.rawArg
            .removeFirst(context.args[0])
            .trim()
        if (unbanReason.isBlank()) unbanReason = "/"

        unbanReason = unbanReason.trim()

        val activeBan: Ban? = daoManager.banWrapper.getActiveBan(context.guildId, targetUser.idLong)
        val ban: Ban = activeBan
            ?: Ban(
                context.guildId,
                targetUser.idLong,
                null,
                "/"
            )
        ban.unbanAuthorId = context.authorId
        ban.unbanReason = unbanReason
        ban.endTime = System.currentTimeMillis()
        ban.active = false

        val banAuthor = ban.banAuthorId?.let { context.shardManager.retrieveUserById(it).awaitOrNull() }

        try {
            guild.retrieveBan(targetUser).await()
            try {
                guild
                    .unban(targetUser)
                    .reason("(unban) ${context.author.asTag}: " + unbanReason)
                    .await()

                daoManager.banWrapper.setBan(ban)
                val zoneId = getZoneId(daoManager, guild.idLong)

                //Normal success path
                val msgLc =
                    getUnbanMessage(language, zoneId, context.guild, targetUser, banAuthor, context.author, ban, true)

                val privateChannel = if (context.guild.isMember(targetUser)) {
                    targetUser.openPrivateChannel().awaitOrNull()
                } else {
                    null
                }
                privateChannel?.let {
                    try {
                        val msg = sendEmbedAwaitEL(it, msgLc)
                        continueUnbanning(context, targetUser, ban, banAuthor, msg[0])
                    } catch (t: Throwable) {
                        continueUnbanning(context, targetUser, ban, banAuthor, null)
                    }
                } ?: continueUnbanning(context, targetUser, ban, banAuthor, null)

            } catch (t: Throwable) {
                //Sum ting wrong
                val msg = context.getTranslation("$root.failure")
                    .withSafeVariable(PLACEHOLDER_USER, targetUser.asTag)
                    .withSafeVariable("cause", t.message ?: "/")
                sendRsp(context, msg)
            }
        } catch (t: Throwable) {
            //Not banned anymore

            val msg = context.getTranslation("$root.notbanned")
                .withSafeVariable(PLACEHOLDER_USER, targetUser.asTag)
            sendRsp(context, msg)

            if (activeBan != null) {
                context.daoManager.banWrapper.setBan(ban)
            }
        }
    }

    private suspend fun continueUnbanning(
        context: ICommandContext,
        targetUser: User,
        ban: Ban,
        banAuthor: User?,
        unbanningMessage: Message? = null
    ) {
        val guild = context.guild
        val unbanAuthor = context.author
        val daoManager = context.daoManager
        val language = context.getLanguage()
        val isBot = targetUser.isBot
        val received = unbanningMessage != null
        val privZoneId = getZoneId(daoManager, guild.idLong, targetUser.idLong)
        val lcMsg = getUnbanMessage(
            language, privZoneId, guild, targetUser, banAuthor, unbanAuthor, ban, true, isBot, received
        )

        val logChannel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.UNBAN)
        logChannel?.let { it1 -> sendEmbed(daoManager.embedDisabledWrapper, it1, lcMsg) }

        val success = context.getTranslation("$root.success")
            .withSafeVariable(PLACEHOLDER_USER, targetUser.asTag)
            .withSafeVarInCodeblock("reason", ban.unbanReason ?: "/")
        sendRsp(context, success)
    }
}

fun getUnbanMessage(
    language: String,
    zoneId: ZoneId,
    guild: Guild,
    bannedUser: User,
    banAuthor: User?,
    unbanAuthor: User,
    ban: Ban,
    lc: Boolean = false,
    isBot: Boolean = false,
    received: Boolean = true,
    failedCause: String? = null
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

    val deletedAccount = i18n.getTranslation(language, "message.deleted.user")
    description += i18n.getTranslation(language, "message.punishment.unban.description")
        .withSafeVarInCodeblock("banAuthor", banAuthor?.asTag ?: deletedAccount)
        .withVariable("banAuthorId", ban.banAuthorId.toString())
        .withVariable("unBanAuthorId", ban.unbanAuthorId.toString())
        .withSafeVarInCodeblock("unBanned", bannedUser.asTag)
        .withVariable("unBannedId", ban.bannedId.toString())
        .withSafeVarInCodeblock("banReason", ban.reason)
        .withSafeVarInCodeblock("unbanReason", ban.unbanReason ?: "/")
        .withVariable("duration", banDuration)
        .withVariable("startTime", (ban.startTime.asEpochMillisToDateTime(zoneId)))
        .withVariable("endTime", (ban.endTime?.asEpochMillisToDateTime(zoneId) ?: "none"))
        .withVariable("banId", ban.banId)

    var extraDesc: String = if (!received || isBot) {
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

    if (failedCause != null) {
        extraDesc += i18n.getTranslation(
            language,
            "message.punishment.extra.failed"
        ).withSafeVarInCodeblock("cause", failedCause)
    }

    description += extraDesc
    description += "```"

    val author = i18n.getTranslation(language, "message.punishment.unban.author")
        .withVariable(PLACEHOLDER_USER, unbanAuthor.asTag)
        .withVariable("spaces", getAtLeastNCodePointsAfterName(unbanAuthor) + "\u200B")

    return EmbedBuilder()
        .setAuthor(author, null, unbanAuthor.effectiveAvatarUrl)
        .setDescription(description)
        .setThumbnail(bannedUser.effectiveAvatarUrl)
        .setColor(Color.GREEN)
        .build()
}