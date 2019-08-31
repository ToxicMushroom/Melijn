package me.melijn.melijnbot.objects.utils

import me.melijn.melijnbot.Container
import me.melijn.melijnbot.MelijnBot
import me.melijn.melijnbot.database.embed.EmbedDisabledWrapper
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.PREFIX_PLACE_HOLDER
import me.melijn.melijnbot.objects.translation.Translateable
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.*
import java.io.PrintWriter
import java.io.StringWriter
import java.time.format.DateTimeFormatter

fun Exception.sendInGuild(guild: Guild? = null, channel: MessageChannel? = null, author: User? = null, thread: Thread = Thread.currentThread()) {
    if (Container.instance.settings.unLoggedThreads.contains(thread.name)) return

    val channelId = Container.instance.settings.exceptionChannel
    val textChannel = MelijnBot.shardManager?.getTextChannelById(channelId) ?: return

    val sb = StringBuilder()
    if (guild != null) {
        sb.appendln("**Guild**: " + guild.name + " | " + guild.id)
    }
    if (channel != null) {
        sb.appendln("**" + channel.type.name.toUpperWordCase() + "Channel**: #" + channel.name + " | " + channel.id)
    }
    if (author != null) {
        sb.appendln("**User**: " + author.asTag + " | " + author.id)
    }
    sb.appendln("**Thread**: " + thread.name)
    val writer = StringWriter()
    val printWriter = PrintWriter(writer)
    this.printStackTrace(printWriter)
    val stacktrace = writer.toString()
            .replace("me.melijn.melijnbot", "**me.melijn.melijnbot**")
    sb.append(stacktrace)
    sendMsg(textChannel, sb.toString())
}

fun sendSyntax(context: CommandContext, translationPath: String) {
    val syntax = Translateable(translationPath)
    sendSyntax(context, syntax)
}

fun sendSyntax(context: CommandContext, syntax: Translateable) {
    var syntaxString = syntax.string(context)
    syntaxString = syntaxString.replacePrefix(context)
    sendMsg(context.getTextChannel(),
            Translateable("message.command.usage").string(context).replace("%syntax%", syntaxString)
    )
}

fun sendMsgCodeBlock(context: CommandContext, msg: String, lang: String) {
    if (context.isFromGuild) {
        val channel = context.getTextChannel()
        if (channel.canTalk()) {
            if (msg.length <= 2000) {
                channel.sendMessage(msg).queue()
            } else {
                val parts = StringUtils().splitMessage(msg, margin = 8 + lang.length);
                parts.forEachIndexed { index, msgPart ->
                    channel.sendMessage(when {
                        index == 0 -> "$msgPart```"
                        index + 1 == parts.size -> "```$lang\n$msgPart"
                        else -> "```$lang\n$msgPart```"
                    }).queue()
                }
            }
        }
    } else {
        val privateChannel = context.getPrivateChannel()
        if (msg.length <= 2000) {
            privateChannel.sendMessage(msg).queue()
        } else {
            val parts = StringUtils().splitMessage(msg, margin = 8 + lang.length);
            parts.forEachIndexed { index, msgPart ->
                privateChannel.sendMessage(when {
                    index == 0 -> "$msgPart```"
                    index + 1 == parts.size -> "```$lang\n$msgPart"
                    else -> "```$lang\n$msgPart```"
                }).queue()
            }
        }
    }
}

fun sendMsgCodeBlocks(
        context: CommandContext,
        msg: String,
        lang: String,
        success: ((message: Message) -> Unit)? = null,
        failed: ((ex: Throwable) -> Unit)? = null,
        multicallback: Boolean = false
) {
    if (context.isFromGuild) sendMsgCodeBlocks(context.getTextChannel(), msg, lang, success, failed, multicallback)
    else sendMsgCodeBlocks(context.getPrivateChannel(), msg, lang, success, failed, multicallback)
}

fun sendMsgCodeBlocks(
        channel: PrivateChannel,
        msg: String,
        lang: String,
        success: ((message: Message) -> Unit)? = null,
        failed: ((ex: Throwable) -> Unit)? = null,
        multicallback: Boolean = false
) {
    if (msg.length <= 2000) {
        channel.sendMessage(msg).queue(success, failed)
    } else {
        var executedOnce = false
        StringUtils().splitMessageWithCodeBlocks(msg, 1600, 20 + lang.length, lang).forEach {
            val future = channel.sendMessage(it)
            if (executedOnce && !multicallback) {
                future.queue()
            } else {
                future.queue(success, failed)
            }
            executedOnce = true
        }
    }
}

fun sendMsgCodeBlocks(
        channel: TextChannel,
        msg: String,
        lang: String,
        success: ((message: Message) -> Unit)? = null,
        failed: ((ex: Throwable) -> Unit)? = null,
        multicallback: Boolean = false
) {
    if (channel.canTalk()) {
        if (msg.length <= 2000) {
            channel.sendMessage(msg).queue(success, failed)
        } else {
            var executedOnce = false
            StringUtils().splitMessageWithCodeBlocks(msg, 1600, 20 + lang.length, lang).forEach {
                val future = channel.sendMessage(it)
                if (executedOnce && !multicallback) {
                    future.queue()
                } else {
                    future.queue(success, failed)
                }
                executedOnce = true
            }
        }
    }
}

fun escapeForLog(string: String): String {
    return string.replace("`", "´")
            .replace("\n", " ")
}

fun sendEmbed(context: CommandContext, embed: MessageEmbed, success: ((message: Message) -> Unit)? = null, failed: ((ex: Throwable) -> Unit)? = null) {
    if (context.isFromGuild) {
        sendEmbed(context.daoManager.embedDisabledWrapper, context.getTextChannel(), embed, success, failed)
    } else {
        sendEmbed(context.getPrivateChannel(), embed, success, failed)
    }
}

fun sendEmbed(privateChannel: PrivateChannel, embed: MessageEmbed, success: ((message: Message) -> Unit)? = null, failed: ((ex: Throwable) -> Unit)? = null) {
    privateChannel.sendMessage(embed).queue(success, failed)
}

fun sendEmbed(embedDisabledWrapper: EmbedDisabledWrapper, textChannel: TextChannel, embed: MessageEmbed, success: ((message: Message) -> Unit)? = null, failed: ((ex: Throwable) -> Unit)? = null) {
    val guild = textChannel.guild
    if (!textChannel.canTalk()) {
        failed?.invoke(IllegalArgumentException("No permission to talk in this channel"))
        return
    }
    if (guild.selfMember.hasPermission(textChannel, Permission.MESSAGE_EMBED_LINKS) &&
            !embedDisabledWrapper.embedDisabledCache.contains(guild.idLong)) {
        textChannel.sendMessage(embed).queue(success, failed)
    } else {
        sendEmbedAsMessage(textChannel, embed, success, failed)
    }
}

fun MessageEmbed.toMessage(): String {
    val sb = StringBuilder()
    if (this.author != null) {
        sb.append("***").append(this.author?.name).appendln("***")
    }
    if (this.title != null) {
        sb.appendln("**__${this.title}__**\n")
    }
    if (this.description != null) {
        sb.append(this.description?.replace(Regex("\\[(.+)]\\((.+)\\)"), "$1 (Link: $2)")).append("\n\n")
    }
    if (this.fields.isNotEmpty()) {
        for (field in this.fields) {
            sb.append("**").append(field.name).append("**\n")
                    .append(field.value?.replace(Regex("\\[(.+)]\\((.+)\\)"), "$1 (Link: $2)")).append("\n\n")
        }
    }
    if (this.footer != null) {
        sb.append("*${this.footer?.text}")
        if (this.timestamp != null)
            sb.append(" | ")
        else sb.append("*")
    }
    if (this.timestamp != null) {
        sb.append(this.timestamp?.format(DateTimeFormatter.ISO_DATE_TIME)).append("*")
    }
    return sb.toString()
}

fun sendEmbedAsMessage(textChannel: TextChannel, embed: MessageEmbed, success: ((message: Message) -> Unit)? = null, failed: ((ex: Throwable) -> Unit)? = null) {
    sendMsg(textChannel, embed.toMessage(), success, failed)
}

fun sendMsg(context: CommandContext, msg: String, success: ((message: Message) -> Unit)? = null, failed: ((ex: Throwable) -> Unit)? = null) {
    if (context.isFromGuild) sendMsg(context.getTextChannel(), msg, success, failed)
    else sendMsg(context.getPrivateChannel(), msg, success, failed)
}

fun sendMsg(privateChannel: PrivateChannel, msg: String, success: ((message: Message) -> Unit)? = null, failed: ((ex: Throwable) -> Unit)? = null) {
    if (msg.length <= 2000) {
        privateChannel.sendMessage(msg).queue(success, failed)
    } else {
        StringUtils().splitMessage(msg).forEach {
            privateChannel.sendMessage(it).queue(success, failed)
        }
    }
}

fun sendMsg(channel: TextChannel, msg: String, success: ((message: Message) -> Unit)? = null, failed: ((ex: Throwable) -> Unit)? = null) {
    if (channel.canTalk()) {
        if (msg.length <= 2000) {
            channel.sendMessage(msg).queue(success, failed)
        } else {
            StringUtils().splitMessage(msg).forEach {
                channel.sendMessage(it).queue(success, failed)
            }
        }
    }
}

fun String.toUpperWordCase(): String {
    var previous = ' '
    var newString = ""
    this.toCharArray().forEach { c: Char ->
        newString += if (previous == ' ') c.toUpperCase() else c.toLowerCase()
        previous = c
    }
    return newString
}

fun String.replacePrefix(context: CommandContext): String {
    return this.replace(PREFIX_PLACE_HOLDER, context.commandParts[0])
}