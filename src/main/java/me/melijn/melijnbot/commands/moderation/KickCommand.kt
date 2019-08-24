package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.database.kick.Kick
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.Translateable
import me.melijn.melijnbot.objects.utils.*
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.*
import java.awt.Color

class KickCommand : AbstractCommand("command.kick") {

    init {
        id = 30
        name = "kick"
        commandCategory = CommandCategory.MODERATION
    }

    override fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context, syntax)
            return
        }
        val targetMember = getMemberByArgsNMessage(context, 0) ?: return
        if (!context.getGuild().selfMember.canInteract(targetMember)) {
            val msg = Translateable("$root.cannotkick").string(context)
                    .replace("%user%", targetMember.asTag)
            sendMsg(context, msg)
            return
        }

        var reason = context.rawArg.replaceFirst((context.args[0] + "($:\\s+)?").toRegex(), "")
        if (reason.isBlank()) reason = "/"

        var reasonPreSpaceCount = 0
        for (c in reason) {
            if (c == ' ') reasonPreSpaceCount++
            else break
        }
        reason = reason.substring(reasonPreSpaceCount)

        val kick = Kick(
                context.getGuildId(),
                targetMember.idLong,
                context.authorId,
                reason
        )


        val kicking = Translateable("message.kicking").string(context)
        targetMember.user.openPrivateChannel().queue({ privateChannel ->
            privateChannel.sendMessage(kicking).queue({ message ->
                continueKicking(context, targetMember, kick, message)
            }, {
                continueKicking(context, targetMember, kick)
            })
        }, {
            continueKicking(context, targetMember, kick)
        })
    }

    private fun continueKicking(context: CommandContext, targetMember: Member, kick: Kick, kickingMessage: Message? = null) {
        val kickedMessage = getKickMessage(context.getGuild(), targetMember.user, context.getAuthor(), kick)
        context.daoManager.kickWrapper.addKick(kick)
        context.getGuild().kick(targetMember, kick.kickReason).queue({
            kickingMessage?.editMessage(
                    kickedMessage
            )?.override(true)?.queue()

            val logChannelWrapper = context.daoManager.logChannelWrapper
            val logChannelId = logChannelWrapper.logChannelCache.get(Pair(context.getGuildId(), LogChannelType.KICK)).get()
            val logChannel = context.getGuild().getTextChannelById(logChannelId)
            logChannel?.let { it1 -> sendEmbed(context.daoManager.embedDisabledWrapper, it1, kickedMessage) }

            val msg = Translateable("$root.success").string(context)
                    .replace("%user%", targetMember.asTag)
                    .replace("%reason%", kick.kickReason)
            sendMsg(context, msg)
        }, {
            kickingMessage?.editMessage("failed to kick")?.queue()
            val msg = Translateable("$root.failure").string(context)
                    .replace("%user%", targetMember.asTag)
                    .replace("%cause%", it.message ?: "unknown (contact support for info)")
            sendMsg(context, msg)
        })
    }
}

fun getKickMessage(guild: Guild, kickedUser: User, kickAuthor: User, kick: Kick): MessageEmbed {
    val eb = EmbedBuilder()
    eb.setAuthor("Kicked by: " + kickAuthor.asTag + " ".repeat(45).substring(0, 45 - kickAuthor.name.length) + "\u200B", null, kickAuthor.effectiveAvatarUrl)
    eb.setDescription("```LDIF" +
            "\nGuild: " + guild.name +
            "\nGuildId: " + guild.id +
            "\nKick Author: " + (kickAuthor.asTag) +
            "\nKick Author Id: " + kick.kickAuthorId +
            "\nKicked: " + kickedUser.asTag +
            "\nKickedId: " + kickedUser.id +
            "\nReason: " + kick.kickReason +
            "\nMoment of kick: " + (kick.kickMoment.asEpochMillisToDateTime()) + "```")
    eb.setThumbnail(kickedUser.effectiveAvatarUrl)
    eb.setColor(Color.BLUE)
    return eb.build()
}