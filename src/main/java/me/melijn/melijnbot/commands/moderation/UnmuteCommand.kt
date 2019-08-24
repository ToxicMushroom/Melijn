package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.database.mute.Mute
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.enums.RoleType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.objects.translation.Translateable
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

    override fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context, syntax)
            return
        }
        val targetUser = getUserByArgsNMessage(context, 0) ?: return

        var unmuteReason = context.rawArg.replaceFirst((context.args[0] + "($:\\s+)?").toRegex(), "")
        if (unmuteReason.isBlank()) unmuteReason = "/"

        var reasonPreSpaceCount = 0
        for (c in unmuteReason) {
            if (c == ' ') reasonPreSpaceCount++
            else break
        }
        unmuteReason = unmuteReason.substring(reasonPreSpaceCount)

        val activeMute: Mute? = context.daoManager.muteWrapper.getActiveMute(context.getGuildId(), targetUser.idLong)
        val mute: Mute = activeMute
                ?: Mute(context.getGuildId(),
                        targetUser.idLong,
                        null,
                        "/"
                )
        mute.unmuteAuthorId = context.authorId
        mute.unmuteReason = unmuteReason
        mute.endTime = System.currentTimeMillis()
        mute.active = false

        val muteAuthor = mute.muteAuthorId?.let { context.getShardManager()?.getUserById(it) }
        val targetMember = context.getGuild().getMember(targetUser)

        val muteRoleId = context.daoManager.roleWrapper.roleCache.get(Pair(context.getGuildId(), RoleType.MUTE)).get()
        val muteRole = context.getGuild().getRoleById(muteRoleId)
        if (muteRole == null) {
            val msg = Translateable("$root.nomuterole").string(context)
                    .replace("%prefix%", context.usedPrefix)
            sendMsg(context, msg)
            return
        }

        if (targetMember != null && targetMember.roles.contains(muteRole)) {
            context.getGuild().removeRoleFromMember(targetMember, muteRole).queue({
                sendUnmuteLogs(context, targetUser, muteAuthor, mute, unmuteReason)
            }, { failure ->
                //Sum ting wrong
                val msg = Translateable("$root.failure").string(context)
                        .replace(PLACEHOLDER_USER, targetUser.asTag)
                        .replace("%cause%", failure.message ?: "unknown (contact support for info)")
                sendMsg(context, msg)
            })
        } else if (targetMember == null) {
            sendUnmuteLogs(context, targetUser, muteAuthor, mute, unmuteReason)
        } else if (!targetMember.roles.contains(muteRole)) {
            //Not muted anymore
            val msg = Translateable("$root.notmuted").string(context)
                    .replace(PLACEHOLDER_USER, targetUser.asTag)
            sendMsg(context, msg)

            if (activeMute != null) {
                context.daoManager.muteWrapper.setMute(mute)
            }
        }
    }

    private fun sendUnmuteLogs(context: CommandContext, targetUser: User, muteAuthor: User?, mute: Mute, unmuteReason: String) {
        context.daoManager.muteWrapper.setMute(mute)

        //Normal success path
        val msg = getUnmuteMessage(context.getGuild(), targetUser, muteAuthor, context.getAuthor(), mute)
        targetUser.openPrivateChannel().queue({ privateChannel ->
            sendEmbed(privateChannel, msg, failed = null)
        }, null)

        val logChannelWrapper = context.daoManager.logChannelWrapper
        val logChannelId = logChannelWrapper.logChannelCache.get(Pair(context.getGuildId(), LogChannelType.UNMUTE)).get()
        val logChannel = context.getGuild().getTextChannelById(logChannelId)
        logChannel?.let { it1 -> sendEmbed(context.daoManager.embedDisabledWrapper, it1, msg) }

        val successMsg = Translateable("$root.success").string(context)
                .replace(PLACEHOLDER_USER, targetUser.asTag)
                .replace("%reason%", unmuteReason)
        sendMsg(context, successMsg)
    }
}


fun getUnmuteMessage(guild: Guild, mutedUser: User?, muteAuthor: User?, unmuteAuthor: User, mute: Mute): MessageEmbed {
    val eb = EmbedBuilder()
    eb.setAuthor("Unmuted by: " + unmuteAuthor.asTag + " ".repeat(45).substring(0, 45 - unmuteAuthor.name.length) + "\u200B", null, unmuteAuthor.effectiveAvatarUrl)
    eb.setDescription("```LDIF" +
            "\nGuild: " + guild.name +
            "\nGuildId: " + guild.id +
            "\nMute Author: " + (muteAuthor?.asTag ?: "deleted account") +
            "\nMute Author Id: " + mute.muteAuthorId +
            "\nUnmuted: " + (mutedUser?.asTag ?: "deleted account") +
            "\nUnmutedId: " + mute.mutedId +
            "\nMute Reason: " + mute.reason +
            "\nUnmute Reason: " + mute.unmuteReason +
            "\nStart of mute: " + (mute.startTime.asEpochMillisToDateTime()) +
            "\nEnd of mute: " + (mute.endTime?.asEpochMillisToDateTime() ?: "none") + "```")
    eb.setThumbnail(mutedUser?.effectiveAvatarUrl)
    eb.setColor(Color.green)
    return eb.build()
}