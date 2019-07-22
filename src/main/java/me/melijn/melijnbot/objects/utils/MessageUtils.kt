package me.melijn.melijnbot.objects.utils

import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.PREFIX_PLACE_HOLDER
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.*
import java.util.function.Consumer


fun printException(currentThread: Thread, e: Exception, originGuild: Guild? = null, originChannel: MessageChannel? = null) {
    println("blub")
}


fun sendEmbed(context: CommandContext, embed: MessageEmbed, success: Consumer<Message>? = null, failed: Consumer<Throwable>? = null) {
    if (context.isFromGuild()) {
        val channel = context.getTextChannel()
        if (channel.canTalk()) {
            if (channel.guild.selfMember.hasPermission(Permission.MESSAGE_EMBED_LINKS) &&
                    !context.daoManager.embedDisabledWrapper.embedDisabledCache.contains(context.getGuild().idLong)) {

            } else {
                sendEmbedAsMessage(context, embed, success, failed)
            }
        }
    } else context.getPrivateChannel().sendMessage(embed).queue(success, failed)
}

fun sendEmbed(privateChannel: PrivateChannel, embed: MessageEmbed, success: Consumer<Message>? = null, failed: Consumer<Throwable>? = null) {

}

fun sendEmbed(channel: TextChannel, embed: MessageEmbed, success: Consumer<Message>? = null, failed: Consumer<Throwable>? = null) {

}


fun sendMsg(context: CommandContext, msg: String, success: Consumer<Message>? = null) {
    if (context.isFromGuild()) sendMsg(context.getTextChannel(), msg, success)
    else sendMsg(context.getPrivateChannel(), msg, success)
}

fun sendMsg(privateChannel: PrivateChannel, msg: String, success: Consumer<Message>? = null) {
    if (msg.length <= 2000) {
        privateChannel.sendMessage(msg).queue()
    } else {
        StringUtils().splitMessage(msg).forEach {
            privateChannel.sendMessage(it).queue()
        }
    }
}

fun sendMsg(channel: TextChannel, msg: String, success: Consumer<Message>? = null) {
    if (channel.canTalk()) {
        if (msg.length <= 2000) {
            channel.sendMessage(msg).queue()
        } else {
            StringUtils().splitMessage(msg).forEach {
                channel.sendMessage(it).queue()
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