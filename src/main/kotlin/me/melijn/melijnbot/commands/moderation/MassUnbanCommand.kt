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

class MassUnbanCommand : AbstractCommand("command.massunban") {

    init {
        name = "massUnban"
        aliases = arrayOf("mub")
        commandCategory = CommandCategory.MODERATION
        discordChannelPermissions = arrayOf(Permission.BAN_MEMBERS)
    }

    override suspend fun execute(context: ICommandContext) {
        if (context.args.size < 2) {
            sendSyntax(context)
            return
        }

        val guild = context.guild
        val daoManager = context.daoManager
        val language = context.getLanguage()


        var offset = 0
        val size = context.args.size
        val users = mutableListOf<User>()
        for (i in 0 until size) {
            if (context.args[i] == "-r") {
                break
            }
            offset++
            (retrieveUserByArgsNMessage(context, i) ?: return).let {
                users.add(i, it)
            }
        }

        var reason = context.getRawArgPart(1 + offset)

        if (reason.isBlank()) reason = "/"
        reason = reason.trim()


        var success = 0
        var failed = 0
        val zoneId = getZoneId(daoManager, guild.idLong)

        for (targetUser in users) {
            val activeBan: Ban? = daoManager.banWrapper.getActiveBan(context.guildId, targetUser.idLong)
            val ban: Ban = activeBan
                ?: Ban(
                    context.guildId,
                    targetUser.idLong,
                    null,
                    "/"
                )

            ban.unbanAuthorId = context.authorId
            ban.unbanReason = reason
            ban.endTime = System.currentTimeMillis()
            ban.active = false
            val banning = context.getTranslation("message.unbanning")
            val banAuthor = ban.banAuthorId?.let { context.shardManager.retrieveUserById(it).awaitOrNull() }

            try {
                guild.retrieveBan(targetUser).await()
                try {
                    guild
                        .unban(targetUser)
                        .reason("(massUnban) ${context.author.asTag}: " + reason)
                        .await()

                    daoManager.banWrapper.setBan(ban)


                    val msgLc =
                        getMassUnbanMessage(
                            language,
                            zoneId,
                            context.guild,
                            targetUser,
                            banAuthor,
                            context.author,
                            ban,
                            true
                        )


                    continueUnbanning(context, targetUser, ban, banAuthor, null)
                    success++


                } catch (t: Throwable) {
                    failed++

                    users.remove(targetUser)
                }


            } catch (t: Throwable) {
                //Not banned anymore
                users.remove(targetUser)

                failed++

                if (activeBan != null) {
                    context.daoManager.banWrapper.setBan(ban)
                }


            }
            

        }

        val msg = context.getTranslation("$root.unbanned.${if (failed == 0) "success" else "ok"}")
            .withVariable("success", success)
            .withVariable("failed", failed)
            .withSafeVarInCodeblock("reason", reason)

        sendRsp(context, msg)


        val activeBan: Ban? = daoManager.banWrapper.getActiveBan(context.guildId, users[0].idLong)
        val ban: Ban = activeBan
            ?: Ban(
                context.guildId,
                users[0].idLong,
                null,
                "/"
            )

        ban.unbanAuthorId = context.authorId
        ban.unbanReason = reason
        ban.endTime = System.currentTimeMillis()
        ban.active = false

        val messageEmbed = getMassLogUnbanMessage(language,zoneId, guild, users, context.author, ban)
        val logChannel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.MASS_UNBAN)
        logChannel?.let { it1 -> sendEmbed(daoManager.embedDisabledWrapper, it1, messageEmbed) }


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

        val success = context.getTranslation("$root.success")
            .withSafeVariable(PLACEHOLDER_USER, targetUser.asTag)
            .withSafeVarInCodeblock("reason", ban.unbanReason ?: "/")

    }





}

fun getMassUnbanMessage(
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

fun getMassLogUnbanMessage(
    language: String,
    zoneId: ZoneId,
    guild: Guild,
    bannedUsers: List<User>,
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

    val bannedList = bannedUsers.joinToString(separator = "\n- ", prefix = "\n- ") { "${it.id} - [${it.asTag}]" }
    val deletedAccount = i18n.getTranslation(language, "message.deleted.user")
    description += i18n.getTranslation(language, "message.punishment.massunban.description")
        .withVariable("unBanAuthorId", ban.unbanAuthorId.toString())
        .withVariable("unbannedList", bannedList)
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
        .setColor(Color.GREEN)
        .build()
}