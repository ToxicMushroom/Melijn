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
import java.time.ZoneId

class MuteCommand : AbstractCommand("command.mute") {

    init {
        id = 28
        name = "mute"
        aliases = arrayOf("m")
        commandCategory = CommandCategory.MODERATION
        discordChannelPermissions = arrayOf(Permission.MANAGE_ROLES)
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }
        val targetUser = getUserByArgsNMessage(context, 0) ?: return
        val member = context.guild.getMember(targetUser)
        if (member != null && !context.guild.selfMember.canInteract(member)) {
            val msg = context.getTranslation("message.interact.member.hierarchyexception")
                .replace(PLACEHOLDER_USER, targetUser.asTag)
            sendMsg(context, msg)
            return
        }

        var reason = context.rawArg
            .removeFirst(context.args[0])
            .trim()
        if (reason.isBlank()) reason = "/"

        reason = reason.trim()

        val roleId = context.daoManager.roleWrapper.roleCache.get(Pair(context.guildId, RoleType.MUTE)).await()
        val muteRole: Role? = context.guild.getRoleById(roleId)
        if (muteRole == null) {
            val msg = context.getTranslation("message.creatingmuterole")
            sendMsg(context, msg)

            try {
                val role = context.guild.createRole()
                    .setName("muted")
                    .setMentionable(false)
                    .setHoisted(false)
                    .setPermissions(Permission.MESSAGE_READ)
                    .await()
                muteRoleAquired(context, targetUser, reason, role)
            } catch (t: Throwable) {
                val msgFailed = context.getTranslation("message.creatingmuterole.failed")
                    .replace("%cause%", t.message ?: "/")
                sendMsg(context, msgFailed)
            }

            return
        } else {
            muteRoleAquired(context, targetUser, reason, muteRole)
        }
    }

    private suspend fun muteRoleAquired(context: CommandContext, targetUser: User, reason: String, muteRole: Role) {
        val activeMute: Mute? = context.daoManager.muteWrapper.getActiveMute(context.guildId, targetUser.idLong)
        val mute = Mute(
            context.guildId,
            targetUser.idLong,
            context.authorId,
            reason,
            null,
            endTime = null
        )

        if (activeMute != null) mute.startTime = activeMute.startTime

        val muting = context.getTranslation("message.muting")

        val privateChannel = targetUser.openPrivateChannel().awaitOrNull()
        val message: Message? = privateChannel?.let {
            sendMsgEL(it, muting)
        }?.firstOrNull()

        continueMuting(context, muteRole, targetUser, mute, activeMute, message)
    }

    private suspend fun continueMuting(context: CommandContext, muteRole: Role, targetUser: User, mute: Mute, activeMute: Mute?, mutingMessage: Message?) {
        val guild = context.guild
        val author = context.author
        val language = context.getLanguage()
        val daoManager = context.daoManager
        val zoneId = getZoneId(daoManager, guild.idLong)
        val privZoneId = getZoneId(daoManager, guild.idLong, targetUser.idLong)
        val mutedMessageDm = getMuteMessage(language, privZoneId, guild, targetUser, author, mute)
        val mutedMessageLc = getMuteMessage(language, zoneId, guild, targetUser, author, mute, true, targetUser.isBot, mutingMessage != null)

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


            context.getTranslation("$root.success" + if (activeMute != null) ".updated" else "")
                .replace(PLACEHOLDER_USER, targetUser.asTag)
                .replace("%reason%", mute.reason)
        } catch (t: Throwable) {
            val failedMsg = context.getTranslation("message.muting.failed")
            mutingMessage?.editMessage(failedMsg)?.queue()

            context.getTranslation("$root.failure")
                .replace(PLACEHOLDER_USER, targetUser.asTag)
                .replace("%cause%", t.message ?: "/")
        }
        sendMsg(context, msg)
    }
}

fun getMuteMessage(
    language: String,
    zoneId: ZoneId,
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
        .replace("%startTime%", (mute.startTime.asEpochMillisToDateTime(zoneId)))
        .replace("%endTime%", (mute.endTime?.asEpochMillisToDateTime(zoneId) ?: "none"))
        .replace("%muteId%", mute.muteId)

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