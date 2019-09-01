package me.melijn.melijnbot.commands.moderation

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.kick.Kick
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_USER
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

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context, syntax)
            return
        }
        val targetMember = getMemberByArgsNMessage(context, 0) ?: return
        if (!context.getGuild().selfMember.canInteract(targetMember)) {
            val msg = Translateable("$root.cannotkick").string(context)
                    .replace(PLACEHOLDER_USER, targetMember.asTag)
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
        try {
            val privateChannel = targetMember.user.openPrivateChannel().await()
            val message = privateChannel.sendMessage(kicking).await()

            continueKicking(context, targetMember, kick, message)
        } catch (t: Throwable) {
            continueKicking(context, targetMember, kick)
        }
    }

    private suspend fun continueKicking(context: CommandContext, targetMember: Member, kick: Kick, kickingMessage: Message? = null) {
        val guild = context.getGuild()
        val author = context.getAuthor()
        val kickedMessageDm = getKickMessage(guild, targetMember.user, author, kick)
        val warnedMessageLc = getKickMessage(guild, targetMember.user, author, kick, true, targetMember.user.isBot, kickingMessage != null)

        context.daoManager.kickWrapper.addKick(kick)
        try {
            context.getGuild().kick(targetMember, kick.kickReason).await()
            kickingMessage?.editMessage(
                    kickedMessageDm
            )?.override(true)?.queue()

            val logChannelWrapper = context.daoManager.logChannelWrapper
            val logChannelId = logChannelWrapper.logChannelCache.get(Pair(guild.idLong, LogChannelType.KICK)).await()
            val logChannel = guild.getTextChannelById(logChannelId)
            logChannel?.let { it1 -> sendEmbed(context.daoManager.embedDisabledWrapper, it1, warnedMessageLc) }

            val msg = Translateable("$root.success").string(context)
                    .replace(PLACEHOLDER_USER, targetMember.asTag)
                    .replace("%reason%", kick.kickReason)
            sendMsg(context, msg)
        } catch (t: Throwable) {
            kickingMessage?.editMessage("failed to kick")?.queue()
            val msg = Translateable("$root.failure").string(context)
                    .replace(PLACEHOLDER_USER, targetMember.asTag)
                    .replace("%cause%", t.message ?: "unknown (contact support for info)")
            sendMsg(context, msg)
        }
    }
}

fun getKickMessage(guild: Guild,
                   kickedUser: User,
                   kickAuthor: User,
                   kick: Kick,
                   lc: Boolean = false,
                   isBot: Boolean = false,
                   received: Boolean = true
): MessageEmbed {
    val eb = EmbedBuilder()
    eb.setAuthor("Kicked by: " + kickAuthor.asTag + " ".repeat(45).substring(0, 45 - kickAuthor.name.length) + "\u200B", null, kickAuthor.effectiveAvatarUrl)
    val description = "```LDIF" +
            if (!lc) {
                "" +
                        "\nGuild: " + guild.name +
                        "\nGuildId: " + guild.id
            } else {
                ""
            } +
            "\nKick Author: " + (kickAuthor.asTag) +
            "\nKick Author Id: " + kick.kickAuthorId +
            "\nKicked: " + kickedUser.asTag +
            "\nKickedId: " + kickedUser.id +
            "\nReason: " + kick.kickReason +
            "\nMoment of kick: " + (kick.kickMoment.asEpochMillisToDateTime()) +
            if (!received || isBot) {
                "\nExtra: " +
                        if (isBot) {
                            "Target is a bot"
                        } else {
                            "Target had dm's disabled"
                        }
            } else {
                ""
            } + "```"
    eb.setDescription(description)
    eb.setThumbnail(kickedUser.effectiveAvatarUrl)
    eb.setColor(Color.BLUE)
    return eb.build()
}