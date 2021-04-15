package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.database.kick.Kick
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
import me.melijn.melijnbot.internals.utils.message.sendEmbed
import me.melijn.melijnbot.internals.utils.message.sendMsgAwaitEL
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.*
import java.awt.Color
import java.time.ZoneId

class MassKickCommand : AbstractCommand("command.masskick") {

    init {
        name = "massKick"
        aliases = arrayOf("mk")
        commandCategory = CommandCategory.MODERATION
        discordChannelPermissions = arrayOf(Permission.KICK_MEMBERS)
    }

    override suspend fun execute(context: ICommandContext) {
        if (context.args.size < 2) {
            sendSyntax(context)
            return
        }

        var offset = 0
        val size = context.args.size
        val users = mutableListOf<Member>()
        for (i in 0 until size) {
            if (context.args[i] == "-r") {
                break
            }
            offset++
            val user = retrieveMemberByArgsNMessage(context, i) ?: return


            if (user != null) {
                if (!context.guild.selfMember.canInteract(user)) {
                    val msg = context.getTranslation(MESSAGE_SELFINTERACT_MEMBER_HIARCHYEXCEPTION)
                        .withSafeVariable(PLACEHOLDER_USER, user.asTag)
                    sendRsp(context, msg)
                    return
                }
                if (!context.member.canInteract(user) && !hasPermission(
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
            users.add(i, user)
        }

        var reason = context.getRawArgPart(1 + offset)

        if (reason.isBlank()) reason = "/"
        reason = reason.trim()


        var success = 0
        var failed = 0

        for (targetMember in users) {

            val kick = Kick(
                context.guildId,
                targetMember.idLong,
                context.authorId,
                reason
            )

            if (users.size < 11) {
                val kicking = context.getTranslation("message.kicking")
                val privateChannel = targetMember.user.openPrivateChannel().awaitOrNull()
                val message: Message? = privateChannel?.let {
                    sendMsgAwaitEL(it, kicking)
                }?.firstOrNull()
            }
            if (continueKicking(context, targetMember, kick)) success++
            else failed++
        }


        val kick = Kick(
            context.guildId,
            users[0].idLong,
            context.authorId,
            reason
        )

        val warnedMessageLc = getMassKickMessage(
            context.getLanguage(),
            context.getTimeZoneId(),
            context.guild,
            users,
            context.author,
            kick,
        )

        val logChannelWrapper = context.daoManager.logChannelWrapper
        val logChannelId = logChannelWrapper.getChannelId(context.guild.idLong, LogChannelType.MASS_KICK)
        val logChannel = context.guild.getTextChannelById(logChannelId)
        logChannel?.let { it1 -> sendEmbed(context.daoManager.embedDisabledWrapper, it1, warnedMessageLc) }

        val msg = context.getTranslation("$root.kicked.${if (failed == 0) "success" else "ok"}")
            .withVariable("success", success)
            .withVariable("failed", failed)
            .withSafeVarInCodeblock("reason", reason)

        sendRsp(context, msg)
    }


    private suspend fun continueKicking(
        context: ICommandContext,
        targetMember: Member,
        kick: Kick,
        kickingMessage: Message? = null
    ): Boolean {
        val guild = context.guild
        val author = context.author
        val language = context.getLanguage()
        val daoManager = context.daoManager
        val zoneId = getZoneId(daoManager, guild.idLong)
        val privZoneId = getZoneId(daoManager, guild.idLong, targetMember.idLong)

        val kickedMessageDm = getKickMessage(language, privZoneId, guild, targetMember.user, author, kick)
        val warnedMessageLc = getKickMessage(
            language,
            zoneId,
            guild,
            targetMember.user,
            author,
            kick,
            true,
            targetMember.user.isBot,
            kickingMessage != null
        )

        context.daoManager.kickWrapper.addKick(kick)
        return try {
            context.guild
                .kick(targetMember)
                .reason("(massKick) " + context.author.asTag + ": " + kick.reason)
                .await()

            kickingMessage?.editMessage(
                kickedMessageDm
            )?.override(true)?.queue()



            context.getTranslation("$root.success")
                .withSafeVariable(PLACEHOLDER_USER, targetMember.asTag)
                .withSafeVarInCodeblock("reason", kick.reason)
            true

        } catch (t: Throwable) {
            val failedMsg = context.getTranslation("message.kicking.failed")
            kickingMessage?.editMessage(failedMsg)?.queue()

            context.getTranslation("$root.failure")
                .withSafeVariable(PLACEHOLDER_USER, targetMember.asTag)
                .withSafeVarInCodeblock("cause", t.message ?: "/")
            false
        }
    }
}

fun getMassKickMessage(
    language: String,
    zoneId: ZoneId,
    guild: Guild,
    kickedUsers: List<Member>,
    kickAuthor: User,
    kick: Kick,
    lc: Boolean = false,
    isBot: Boolean = false,
    received: Boolean = true
): MessageEmbed {
    var description = "```LDIF\n"
    if (!lc) {
        description += i18n.getTranslation(language, "message.punishment.description.nlc")
            .withSafeVarInCodeblock("serverName", guild.name)
            .withVariable("serverId", guild.id)
    }
    val users = mutableListOf<User>()
    for ((i, member) in kickedUsers.withIndex()){
        users.add(i, member.user)
    }
    val bannedList = users.joinToString(separator = "\n- ", prefix = "\n- ") { "${it.id} - [${it.asTag}]" }

    description += i18n.getTranslation(language, "message.punishment.masskick.description")
        .withSafeVarInCodeblock("kickAuthor", kickAuthor.asTag)
        .withVariable("kickAuthorId", kickAuthor.id)
        .withSafeVarInCodeblock("kickedList", bannedList)
        .withSafeVarInCodeblock("reason", kick.reason)
        .withVariable("moment", (kick.moment.asEpochMillisToDateTime(zoneId)))
        .withVariable("kickId", kick.kickId)

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

    val author = i18n.getTranslation(language, "message.punishment.kick.author")
        .withSafeVariable(PLACEHOLDER_USER, kickAuthor.asTag)
        .withVariable("spaces", getAtLeastNCodePointsAfterName(kickAuthor) + "\u200B")

    return EmbedBuilder()
        .setAuthor(author, null, kickAuthor.effectiveAvatarUrl)
        .setDescription(description)
        .setColor(Color.ORANGE)
        .build()
}