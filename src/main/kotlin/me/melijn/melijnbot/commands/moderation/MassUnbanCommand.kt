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

        var firstBan: Ban? = null
        for (targetUser in users) {
            val activeBan: Ban? = daoManager.banWrapper.getActiveBan(context.guildId, targetUser.idLong)
            val ban = (activeBan ?: Ban(
                context.guildId,
                targetUser.idLong,
                null,
                "/"
            )).apply {
                unbanAuthorId = context.authorId
                unbanReason = reason
                endTime = System.currentTimeMillis()
                active = false
            }

            if (firstBan == null) firstBan = ban

            val isBanned = guild.retrieveBan(targetUser).awaitBool()
            if (isBanned) {
                val successUnban = guild
                    .unban(targetUser)
                    .reason("(massUnban) ${context.author.asTag}: " + reason)
                    .awaitBool()

                if (successUnban) {
                    success++
                    daoManager.banWrapper.setBan(ban)
                } else {
                    failed++
                    users.remove(targetUser)
                }
            } else {
                failed++
                users.remove(targetUser)

                if (activeBan != null) {
                    daoManager.banWrapper.setBan(ban)
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

        val messageEmbed = getMassLogUnbanMessage(language, zoneId, guild, users, context.author, ban)
        val logChannel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.MASS_UNBAN)
        logChannel?.let { it1 -> sendEmbed(daoManager.embedDisabledWrapper, it1, messageEmbed) }
    }
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
    var description = "```LDIF\n"
    if (!lc) {
        description += i18n.getTranslation(language, "message.punishment.description.nlc")
            .withSafeVarInCodeblock("serverName", guild.name)
            .withVariable("serverId", guild.id)
    }

    val bannedList = bannedUsers.joinToString(separator = "\n- ", prefix = "\n- ") { "${it.id} - [${it.asTag}]" }
    description += i18n.getTranslation(language, "message.punishment.massunban.description")
        .withVariable("unBanAuthorId", ban.unbanAuthorId.toString())
        .withVariable("unbannedList", bannedList)
        .withSafeVarInCodeblock("unbanReason", ban.unbanReason ?: "/")
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