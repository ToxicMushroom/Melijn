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
import net.dv8tion.jda.api.entities.*
import java.awt.Color

class MuteCommand : AbstractCommand("command.mute") {

    init {
        id = 28
        name = "mute"
        aliases = arrayOf("m")
        commandCategory = CommandCategory.MODERATION
        discordPermissions = arrayOf(Permission.MANAGE_ROLES)
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.size < 2) {
            sendSyntax(context)
            return
        }
        val targetUser = getUserByArgsNMessage(context, 0) ?: return
        val member = context.getGuild().getMember(targetUser)
        if (member != null && !context.getGuild().selfMember.canInteract(member)) {
            val language = context.getLanguage()
            val msg = i18n.getTranslation(language, "$root.cannotmute")
                .replace(PLACEHOLDER_USER, targetUser.asTag)
            sendMsg(context, msg)
            return
        }

        var reason = context.rawArg
            .replaceFirst(context.args[0], "")
            .trim()
        if (reason.isBlank()) reason = "/"

        reason = reason.trim()

        val roleId = context.daoManager.roleWrapper.roleCache.get(Pair(context.getGuildId(), RoleType.MUTE)).await()
        val muteRole: Role? = context.getGuild().getRoleById(roleId)
        if (muteRole == null) {
            val language = context.getLanguage()
            val msg = i18n.getTranslation(language, "$root.creatingmuterole")
            sendMsg(context, msg)

            try {
                val role = context.getGuild().createRole()
                    .setName("muted")
                    .setMentionable(false)
                    .setHoisted(false)
                    .setPermissions(Permission.MESSAGE_READ)
                    .await()
                muteRoleAquired(context, targetUser, reason, role)
            } catch (t: Throwable) {
                val msgFailed = i18n.getTranslation(language, "$root.failed.creatingmuterole")
                    .replace("%cause%", t.message ?: "unknown (contact support for info)")
                sendMsg(context, msgFailed)
            }

            return
        } else {
            muteRoleAquired(context, targetUser, reason, muteRole)
        }


    }

    private suspend fun muteRoleAquired(context: CommandContext, targetUser: User, reason: String, muteRole: Role) {
        val activeMute: Mute? = context.daoManager.muteWrapper.getActiveMute(context.getGuildId(), targetUser.idLong)
        val mute = Mute(
            context.getGuildId(),
            targetUser.idLong,
            context.authorId,
            reason,
            null,
            endTime = null
        )

        if (activeMute != null) mute.startTime = activeMute.startTime

        val language = context.getLanguage()
        val muting = i18n.getTranslation(language, "message.muting")

        try {
            val privateChannel = targetUser.openPrivateChannel().await()
            val message = privateChannel.sendMessage(muting).await()
            continueMuting(context, muteRole, targetUser, mute, activeMute, message)
        } catch (t: Throwable) {
            continueMuting(context, muteRole, targetUser, mute, activeMute)
        }
    }

    private suspend fun continueMuting(context: CommandContext, muteRole: Role, targetUser: User, mute: Mute, activeMute: Mute?, mutingMessage: Message? = null) {
        val guild = context.getGuild()
        val author = context.getAuthor()
        val language = context.getLanguage()
        val mutedMessageDm = getMuteMessage(language, guild, targetUser, author, mute)
        val mutedMessageLc = getMuteMessage(language, guild, targetUser, author, mute, true, targetUser.isBot, mutingMessage != null)

        context.daoManager.muteWrapper.setMute(mute)
        val targetMember = guild.getMember(targetUser) ?: return

        val msg = try {
            guild.addRoleToMember(targetMember, muteRole).await()
            mutingMessage?.editMessage(
                mutedMessageDm
            )?.override(true)?.queue()

            val logChannelWrapper = context.daoManager.logChannelWrapper
            val logChannelId = logChannelWrapper.logChannelCache.get(Pair(guild.idLong, LogChannelType.PERMANENT_MUTE)).await()
            val logChannel = guild.getTextChannelById(logChannelId)
            logChannel?.let { it1 -> sendEmbed(context.daoManager.embedDisabledWrapper, it1, mutedMessageLc) }


            i18n.getTranslation(language, "$root.success" + if (activeMute != null) ".updated" else "")
                .replace(PLACEHOLDER_USER, targetUser.asTag)
                .replace("%reason%", mute.reason)
        } catch (t: Throwable) {
            mutingMessage?.editMessage("failed to mute")?.queue()

            i18n.getTranslation(language, "$root.failure")
                .replace(PLACEHOLDER_USER, targetUser.asTag)
                .replace("%cause%", t.message ?: "unknown (contact support for info)")
        }
        sendMsg(context, msg)
    }
}

fun getMuteMessage(
    language: String,
    guild: Guild,
    mutedUser: User,
    muteAuthor: User,
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

    description += i18n.getTranslation(language, "message.punishment.mute.description")
        .replace("%muteAuthor%", muteAuthor.asTag)
        .replace("%muteAuthorId%", muteAuthor.id)
        .replace("%muted%", mutedUser.asTag)
        .replace("%mutedId%", mutedUser.id)
        .replace("%reason%", mute.reason)
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

    val author = i18n.getTranslation(language, "message.punishment.mute.author")
        .replace(PLACEHOLDER_USER, muteAuthor.asTag)
        .replace("%spaces%", " ".repeat(45).substring(0, 45 - muteAuthor.name.length) + "\u200B")

    eb.setAuthor(author, null, muteAuthor.effectiveAvatarUrl)
    eb.setDescription(description)
    eb.setThumbnail(mutedUser.effectiveAvatarUrl)
    eb.setColor(Color.BLUE)
    return eb.build()
}