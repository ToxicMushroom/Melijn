package me.melijn.melijnbot.internals.utils.message

import kotlinx.coroutines.delay
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.database.embed.EmbedDisabledWrapper
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.awaitOrNull
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.PrivateChannel
import net.dv8tion.jda.api.entities.TextChannel
import java.time.format.DateTimeFormatter


suspend fun sendEmbedRspAwaitEL(context: CommandContext, embed: MessageEmbed): List<Message> {
    val premiumGuild = context.isFromGuild && context.daoManager.supporterWrapper.getGuilds().contains(context.guildId)
    return if (premiumGuild) {
        sendEmbedRspAwaitEL(context.daoManager, context.textChannel, embed)
    } else {
        sendEmbedAwaitEL(context, embed)
    }
}

suspend fun sendEmbedRspAwaitEL(daoManager: DaoManager, textChannel: TextChannel, embed: MessageEmbed): List<Message> {
    val guild = textChannel.guild
    if (!textChannel.canTalk()) {
        throw IllegalArgumentException("No permission to talk in this channel")
    }

    return if (guild.selfMember.hasPermission(textChannel, Permission.MESSAGE_EMBED_LINKS) &&
        !daoManager.embedDisabledWrapper.embedDisabledCache.contains(guild.idLong)) {


        val message = textChannel.sendMessage(embed).awaitOrNull()
            ?: return emptyList()

        TaskManager.async(textChannel) {
            handleRspDelete(daoManager, message)
        }

        listOf(message)
    } else {
        sendEmbedAsResponseAwaitEL(textChannel, daoManager, embed)
    }
}

fun sendEmbed(context: CommandContext, embed: MessageEmbed) {
    if (context.isFromGuild) {
        sendEmbed(context.daoManager.embedDisabledWrapper, context.textChannel, embed)
    } else {
        sendEmbed(context.privateChannel, embed)
    }
}

suspend fun sendEmbedRsp(context: CommandContext, embed: MessageEmbed) {
    val premiumGuild = context.isFromGuild && context.daoManager.supporterWrapper.getGuilds().contains(context.guildId)
    if (premiumGuild) {
        sendEmbedRsp(context.daoManager, context.textChannel, embed)
    } else {
        sendEmbed(context, embed)
    }
}

fun sendEmbedRsp(daoManager: DaoManager, textChannel: TextChannel, embed: MessageEmbed) {
    val guild = textChannel.guild
    if (!textChannel.canTalk()) {
        throw IllegalArgumentException("No permission to talk in this channel")
    }
    if (guild.selfMember.hasPermission(textChannel, Permission.MESSAGE_EMBED_LINKS) &&
        !daoManager.embedDisabledWrapper.embedDisabledCache.contains(guild.idLong)) {
        TaskManager.async(textChannel) {
            val message = textChannel.sendMessage(embed).awaitOrNull() ?: return@async

            val timeMap = daoManager.removeResponseWrapper.getMap(textChannel.guild.idLong)
            val seconds = timeMap[textChannel.idLong] ?: return@async

            delay(seconds * 1000L)
            Container.instance.botDeletedMessageIds.add(message.idLong)

            message.delete().queue(null, { Container.instance.botDeletedMessageIds.remove(message.idLong) })
        }

    } else {
        sendEmbedAsMessageRsp(daoManager, textChannel, embed)
    }
}

suspend fun sendEmbedAwaitEL(context: CommandContext, embed: MessageEmbed): List<Message> {
    return if (context.isFromGuild) {
        sendEmbedAwaitEL(context.daoManager.embedDisabledWrapper, context.textChannel, embed)
    } else {
        sendEmbedAwaitEL(context.privateChannel, embed)
    }
}

fun sendEmbed(privateChannel: PrivateChannel, embed: MessageEmbed) {
    if (privateChannel.user.isBot) return
    privateChannel.sendMessage(embed).queue()
}

suspend fun sendEmbedAwaitEL(privateChannel: PrivateChannel, embed: MessageEmbed): List<Message> {
    if (privateChannel.user.isBot) {
        return emptyList()
    }
    val msg = privateChannel.sendMessage(embed).awaitOrNull()
    return msg?.let { listOf(it) } ?: emptyList()
}


suspend fun sendEmbedAwaitEL(embedDisabledWrapper: EmbedDisabledWrapper, textChannel: TextChannel, embed: MessageEmbed): List<Message> {
    val guild = textChannel.guild
    if (!textChannel.canTalk()) {
        throw IllegalArgumentException("No permission to talk in this channel")
    }

    return if (guild.selfMember.hasPermission(textChannel, Permission.MESSAGE_EMBED_LINKS) &&
        !embedDisabledWrapper.embedDisabledCache.contains(guild.idLong)) {

        val msg = textChannel.sendMessage(embed).awaitOrNull()
        msg?.let { listOf(it) } ?: emptyList()

    } else {
        sendEmbedAsMessageAwaitEL(textChannel, embed)
    }
}

fun sendEmbed(embedDisabledWrapper: EmbedDisabledWrapper, textChannel: TextChannel, embed: MessageEmbed) {
    val guild = textChannel.guild
    if (!textChannel.canTalk()) {
        throw IllegalArgumentException("No permission to talk in this channel")
    }
    if (guild.selfMember.hasPermission(textChannel, Permission.MESSAGE_EMBED_LINKS) &&
        !embedDisabledWrapper.embedDisabledCache.contains(guild.idLong)) {
        textChannel.sendMessage(embed).queue()
    } else {
        sendEmbedAsMessage(textChannel, embed)
    }
}

fun MessageEmbed.toMessage(): String {
    val sb = StringBuilder()

    if (this.author != null) {
        sb.append("***").append(this.author?.name).appendln("***")
    }

    if (this.title != null) {
        sb.appendln("__${this.title}__\n")
    }

    if (this.description != null) {
        sb.append(this.description?.replace(Regex("\\[(.+)]\\((.+)\\)"), "$1 (Link: $2)")).append("\n\n")
    }

    if (this.image != null) {
        sb.append(this.image?.url).append("\n\n")
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

fun sendEmbedAsMessageRsp(daoManager: DaoManager, textChannel: TextChannel, embed: MessageEmbed) {
    sendRsp(textChannel, daoManager, embed.toMessage())
}

fun sendEmbedAsMessage(textChannel: TextChannel, embed: MessageEmbed) {
    sendMsg(textChannel, embed.toMessage())
}

suspend fun sendEmbedAsMessageAwaitEL(textChannel: TextChannel, embed: MessageEmbed): List<Message> {
    return sendMsgAwaitEL(textChannel, embed.toMessage())
}

suspend fun sendEmbedAsResponseAwaitEL(textChannel: TextChannel, daoManager: DaoManager, embed: MessageEmbed): List<Message> {
    return sendRspAwaitEL(textChannel, daoManager, embed.toMessage())
}