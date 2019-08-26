package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.database.warn.Warn
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

class WarnCommand : AbstractCommand("command.warn") {

    init {
        id = 32
        name = "warn"
        commandCategory = CommandCategory.MODERATION
    }

    override fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context, syntax)
            return
        }
        val targetMember = getMemberByArgsNMessage(context, 0) ?: return
        if (!context.getGuild().selfMember.canInteract(targetMember)) {
            val msg = Translateable("$root.cannotwarn").string(context)
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

        val warn = Warn(
                context.getGuildId(),
                targetMember.idLong,
                context.authorId,
                reason
        )


        val warning = Translateable("message.warning..").string(context)
        targetMember.user.openPrivateChannel().queue({ privateChannel ->
            privateChannel.sendMessage(warning).queue({ message ->
                continueWarning(context, targetMember, warn, message)
            }, {
                continueWarning(context, targetMember, warn)
            })
        }, {
            continueWarning(context, targetMember, warn)
        })
    }

    private fun continueWarning(context: CommandContext, targetMember: Member, warn: Warn, warningMessage: Message? = null) {
        val guild = context.getGuild()
        val author = context.getAuthor()
        val warnedMessageDm = getWarnMessage(guild, targetMember.user, author, warn)
        val warnedMessageLc = getWarnMessage(guild, targetMember.user, author, warn, true, targetMember.user.isBot, warningMessage != null)

        context.daoManager.warnWrapper.addWarn(warn)

        warningMessage?.editMessage(
                warnedMessageDm
        )?.override(true)?.queue()

        val logChannelWrapper = context.daoManager.logChannelWrapper
        val logChannelId = logChannelWrapper.logChannelCache.get(Pair(guild.idLong, LogChannelType.WARN)).get()
        val logChannel = guild.getTextChannelById(logChannelId)
        logChannel?.let { it1 ->
            sendEmbed(context.daoManager.embedDisabledWrapper, it1, warnedMessageLc)
        }

        val msg = Translateable("$root.success").string(context)
                .replace(PLACEHOLDER_USER, targetMember.asTag)
                .replace("%reason%", warn.warnReason)
        sendMsg(context, msg)
    }
}

fun getWarnMessage(guild: Guild,
                   warnedUser: User,
                   warnAuthor: User,
                   warn: Warn,
                   lc: Boolean = false,
                   isBot: Boolean = false,
                   received: Boolean = true
): MessageEmbed {
    val eb = EmbedBuilder()
    eb.setAuthor("Warned by: " + warnAuthor.asTag + " ".repeat(45).substring(0, 45 - warnAuthor.name.length) + "\u200B", null, warnAuthor.effectiveAvatarUrl)
    val description = "```LDIF" +
            if (!lc) {
                "" +
                        "\nGuild: " + guild.name +
                        "\nGuildId: " + guild.id
            } else {
                ""
            } +
            "\nWarn Author: " + (warnAuthor.asTag) +
            "\nWarn Author Id: " + warn.warnAuthorId +
            "\nWarned: " + warnedUser.asTag +
            "\nWarnedId: " + warnedUser.id +
            "\nReason: " + warn.warnReason +
            "\nMoment of warn: " + (warn.warnMoment.asEpochMillisToDateTime()) +
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
    eb.setThumbnail(warnedUser.effectiveAvatarUrl)
    eb.setColor(Color.YELLOW)
    return eb.build()
}