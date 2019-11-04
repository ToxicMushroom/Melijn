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
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import java.awt.Color

class UnmuteCommand : AbstractCommand("command.unmute") {

    init {
        id = 26
        name = "unmute"
        commandCategory = CommandCategory.MODERATION
        discordPermissions = arrayOf(Permission.MANAGE_ROLES)
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }
        val guild = context.guild
        val targetUser = retrieveUserByArgsNMessage(context, 0) ?: return

        var unmuteReason = context.rawArg
            .replaceFirst(context.args[0], "")
            .trim()
        if (unmuteReason.isBlank()) unmuteReason = "/"

        unmuteReason = unmuteReason.trim()

        val activeMute: Mute? = context.daoManager.muteWrapper.getActiveMute(context.guildId, targetUser.idLong)
        val mute: Mute = activeMute
            ?: Mute(context.guildId,
                targetUser.idLong,
                null,
                "/"
            )
        mute.unmuteAuthorId = context.authorId
        mute.unmuteReason = unmuteReason
        mute.endTime = System.currentTimeMillis()
        mute.active = false

        val muteAuthor = mute.muteAuthorId?.let { context.shardManager.getUserById(it) }
        val targetMember = guild.getMember(targetUser)

        val muteRoleId = context.daoManager.roleWrapper.roleCache.get(Pair(context.guildId, RoleType.MUTE)).await()
        val muteRole = guild.getRoleById(muteRoleId)

        val language = context.getLanguage()
        if (muteRole == null) {

            val msg = i18n.getTranslation(language, "$root.nomuterole")
                .replace("%prefix%", context.usedPrefix)
            sendMsg(context, msg)
            return
        }

        if (targetMember != null && targetMember.roles.contains(muteRole)) {
            try {
                guild.removeRoleFromMember(targetMember, muteRole).await()
                sendUnmuteLogs(context, targetUser, muteAuthor, mute, unmuteReason)
            } catch (t: Throwable) {
                //Sum ting wrong
                val msg = i18n.getTranslation(language, "$root.failure")
                    .replace(PLACEHOLDER_USER, targetUser.asTag)
                    .replace("%cause%", t.message ?: "/")
                sendMsg(context, msg)
            }
        } else if (targetMember == null) {
            sendUnmuteLogs(context, targetUser, muteAuthor, mute, unmuteReason)
        } else if (!targetMember.roles.contains(muteRole)) {
            //Not muted anymore
            val msg = i18n.getTranslation(language, "$root.notmuted")

                .replace(PLACEHOLDER_USER, targetUser.asTag)

            sendMsg(context, msg)

            if (activeMute != null) {
                context.daoManager.muteWrapper.setMute(mute)
            }
        }
    }

    private suspend fun sendUnmuteLogs(context: CommandContext, targetUser: User, muteAuthor: User?, mute: Mute, unmuteReason: String) {
        context.daoManager.muteWrapper.setMute(mute)
        val guild = context.guild
        val language = context.getLanguage()

        //Normal success path
        val msg = getUnmuteMessage(language, guild, targetUser, muteAuthor, context.author, mute)
        val privateChannel = targetUser.openPrivateChannel().await()

        val success = try {
            sendEmbed(privateChannel, msg)
            true
        } catch (t: Throwable) {
            false
        }
        val msgLc = getUnmuteMessage(language, guild, targetUser, muteAuthor, context.author, mute, true, targetUser.isBot, success)


        val logChannelWrapper = context.daoManager.logChannelWrapper
        val logChannelId = logChannelWrapper.logChannelCache.get(Pair(guild.idLong, LogChannelType.UNMUTE)).await()
        val logChannel = guild.getTextChannelById(logChannelId)
        logChannel?.let { it1 -> sendEmbed(context.daoManager.embedDisabledWrapper, it1, msgLc) }


        val successMsg = i18n.getTranslation(language, "$root.success")
            .replace(PLACEHOLDER_USER, targetUser.asTag)
            .replace("%reason%", unmuteReason)

        sendMsg(context, successMsg)
    }
}


fun getUnmuteMessage(
    language: String,
    guild: Guild,
    mutedUser: User?,
    muteAuthor: User?,
    unmuteAuthor: User,
    mute: Mute,
    lc: Boolean = false,
    isBot: Boolean = false,
    received: Boolean = true
): MessageEmbed {

    val eb = EmbedBuilder()

    val muteDuration = mute.endTime?.let { endTime ->
        getDurationString((endTime - mute.startTime))
    } ?: i18n.getTranslation(language, "infinite")

    var description = "```LDIF\n"
    if (!lc) {
        description += i18n.getTranslation(language, "message.punishment.description.nlc")
            .replace("%guildName%", guild.name)
            .replace("%guildId%", guild.id)
    }

    val deletedAccount = i18n.getTranslation(language, "message.deletedaccount")
    description += i18n.getTranslation(language, "message.punishment.unmute.description")
        .replace("%muteAuthor%", muteAuthor?.asTag ?: deletedAccount)
        .replace("%muteAuthorId%", mute.muteAuthorId.toString())
        .replace("%unMuteAuthorId%", mute.unmuteAuthorId.toString())
        .replace("%unMuted%", mutedUser?.asTag ?: deletedAccount)
        .replace("%unMutedId%", mute.mutedId.toString())
        .replace("%muteReason%", mute.reason)
        .replace("%unmuteReason%", mute.unmuteReason ?: "/")
        .replace("%duration%", muteDuration)
        .replace("%startTime%", (mute.startTime.asEpochMillisToDateTime()))
        .replace("%endTime%", (mute.endTime?.asEpochMillisToDateTime() ?: "none"))

    val extraDesc: String = if (!received || isBot) {
        i18n.getTranslation(language,
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

    val author = i18n.getTranslation(language, "message.punishment.unmute.author")
        .replace(PLACEHOLDER_USER, unmuteAuthor.asTag)
        .replace("%spaces%", " ".repeat(45).substring(0, 45 - unmuteAuthor.name.length) + "\u200B")

    eb.setAuthor(author, null, unmuteAuthor.effectiveAvatarUrl)
    eb.setDescription(description)
    eb.setThumbnail(mutedUser?.effectiveAvatarUrl)
    eb.setColor(Color.GREEN)
    return eb.build()
}