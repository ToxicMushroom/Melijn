package me.melijn.melijnbot.objects.utils

import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.PREFIX_PLACE_HOLDER
import me.melijn.melijnbot.objects.translation.Translateable
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.*
import java.time.format.DateTimeFormatter
import java.util.function.Consumer


fun printException(currentThread: Thread, e: Exception, originGuild: Guild? = null, originChannel: MessageChannel? = null) {
    println("blub")
}

fun sendSyntax(context: CommandContext, translationPath: String) {
    val syntax = Translateable(translationPath)
    sendSyntax(context, syntax)
}

fun sendSyntax(context: CommandContext, syntax: Translateable) {
    var syntaxString = syntax.string(context)
    syntaxString = syntaxString.replace(PREFIX_PLACE_HOLDER, context.commandParts[0])
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

fun sendEmbed(context: CommandContext, embed: MessageEmbed, success: Consumer<Message>? = null, failed: Consumer<Throwable>? = null) {
    if (context.isFromGuild) {
        val channel = context.getTextChannel()
        if (channel.canTalk()) {
            if (channel.guild.selfMember.hasPermission(Permission.MESSAGE_EMBED_LINKS) &&
                    !context.daoManager.embedDisabledWrapper.embedDisabledCache.contains(context.getGuild().idLong)) {
                context.getTextChannel().sendMessage(embed).queue(success, failed)
            } else {
                sendEmbedAsMessage(context, embed, success, failed)
            }
        }
    } else
        context.getPrivateChannel().sendMessage(embed).queue(success, failed)
}

fun sendEmbedAsMessage(context: CommandContext, embed: MessageEmbed, success: Consumer<Message>?, failed: Consumer<Throwable>?) {
    val sb = StringBuilder()
    if (embed.author != null) {
        sb.append("**").append(embed.author!!.name).appendln("**")
    }
    if (embed.title != null) {
        sb.appendln(embed.title)
    }
    if (embed.description != null) {
        sb.append(embed.description!!.replace(Regex("\\[(.+)]\\((.+)\\)"), "$1 (Link: $2)")).append("\n\n")
    }
    if (embed.fields.isNotEmpty()) {
        for (field in embed.fields) {
            sb.append("__").append(field.name).append("__\n")
                    .append(field.value!!.replace(Regex("\\[(.+)]\\((.+)\\)"), "$1 (Link: $2)")).append("\n\n")
        }
    }
    if (embed.footer != null) {
        sb.append(embed.footer!!.text)
        if (embed.timestamp != null)
            sb.append(" | ")
    }
    if (embed.timestamp != null) {
        sb.append(embed.timestamp!!.format(DateTimeFormatter.ISO_DATE_TIME))
    }
    sendMsg(context.getTextChannel(), sb.toString(), success)
}

fun sendMsg(context: CommandContext, msg: String, success: Consumer<Message>? = null) {
    if (context.isFromGuild) sendMsg(context.getTextChannel(), msg, success)
    else sendMsg(context.getPrivateChannel(), msg, success)
}

fun sendMsg(privateChannel: PrivateChannel, msg: String, success: Consumer<Message>? = null) {
    if (msg.length <= 2000) {
        privateChannel.sendMessage(msg).queue(success)
    } else {
        StringUtils().splitMessage(msg).forEach {
            privateChannel.sendMessage(it).queue(success)
        }
    }
}

fun sendMsg(channel: TextChannel, msg: String, success: Consumer<Message>? = null) {
    if (channel.canTalk()) {
        if (msg.length <= 2000) {
            channel.sendMessage(msg).queue(success)
        } else {
            StringUtils().splitMessage(msg).forEach {
                channel.sendMessage(it).queue(success)
            }
        }
    }
}

fun String.toUpperWordCase(): String {
    var previous = 'a'
    var newString = ""
    this.toCharArray().forEach { c: Char ->
        newString += if (previous == ' ') c.toUpperCase() else c
        previous = c
    }
    return newString
}

fun String.replacePrefix(context: CommandContext): String {
    return this.replace(PREFIX_PLACE_HOLDER, context.commandParts[0])
}