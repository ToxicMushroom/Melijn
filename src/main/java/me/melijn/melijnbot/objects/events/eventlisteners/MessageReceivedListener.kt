package me.melijn.melijnbot.objects.events.eventlisteners

import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.database.message.DaoMessage
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.objects.events.AbstractListener
import me.melijn.melijnbot.objects.translation.Translateable
import me.melijn.melijnbot.objects.utils.asLongLongGMTString
import me.melijn.melijnbot.objects.utils.asTag
import me.melijn.melijnbot.objects.utils.sendEmbed
import me.melijn.melijnbot.objects.utils.toMessage
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import java.awt.Color

class MessageReceivedListener(container: Container) : AbstractListener(container) {

    override fun onEvent(event: GenericEvent) {
        if (event is GuildMessageReceivedEvent) {
            handleMessageReceivedStoring(event)
            handleAttachmentLog(event)
        }
    }

    private fun handleAttachmentLog(event: GuildMessageReceivedEvent) {
        if (event.message.attachments.isEmpty()) return
        val logChannelWrapper = container.daoManager.logChannelWrapper
        val logChannelCache = logChannelWrapper.logChannelCache
        CoroutineScope(Dispatchers.Default).launch {
            val attachmentLogChannelId = logChannelCache.get(Pair(event.guild.idLong, LogChannelType.ATTACHMENT)).await()
            if (attachmentLogChannelId == -1L) return@launch
            val logChannel = event.guild.getTextChannelById(attachmentLogChannelId) ?: return@launch
            for (attachment in event.message.attachments) {
                postAttachmentLog(event, logChannel, attachment)
            }
        }
    }

    private fun handleMessageReceivedStoring(event: GuildMessageReceivedEvent) = runBlocking {
        val timeOne = System.currentTimeMillis()
        val guildId = event.guild.idLong
        val logChannelWrapper = container.daoManager.logChannelWrapper
        val logChannelCache = logChannelWrapper.logChannelCache

        val odmId = logChannelCache.get(Pair(guildId, LogChannelType.OTHER_DELETED_MESSAGE))
        val sdmId = logChannelCache.get(Pair(guildId, LogChannelType.SELF_DELETED_MESSAGE))
        val pmId = logChannelCache.get(Pair(guildId, LogChannelType.PURGED_MESSAGE))
        val fmId = logChannelCache.get(Pair(guildId, LogChannelType.FILTERED_MESSAGE))
        if (odmId.await() == -1L && sdmId.await() == -1L && pmId.await() == -1L && fmId.await() == -1L) return@runBlocking

        val messageWrapper = container.daoManager.messageWrapper
        var content = event.message.contentRaw
        event.message.embeds.forEach { embed ->
            content += "\n${embed.toMessage()}"
        }

        GlobalScope.launch {
            messageWrapper.addMessage(DaoMessage(
                    guildId,
                    event.channel.idLong,
                    event.author.idLong,
                    event.messageIdLong,
                    event.message.contentRaw,
                    event.message.timeCreated.toInstant().toEpochMilli()
            ))
        }


        println("duration: " + (System.currentTimeMillis() - timeOne))
    }

    private fun postAttachmentLog(event: GuildMessageReceivedEvent, logChannel: TextChannel, attachment: Message.Attachment) {
        val embedBuilder = EmbedBuilder()
        val title = Translateable("listener.message.attachment.log.title")
                .string(container.daoManager, event.guild.idLong)
                .replace("%channel%", event.channel.asTag)

        val description = Translateable("listener.message.attachment.log.description")
                .string(container.daoManager, event.guild.idLong)
                .replace("%userId%", event.author.id)
                .replace("%messageId%", event.messageId)
                .replace("%messageUrl%", "https://discordapp.com/channels/${event.guild.id}/${event.channel.id}/${event.message.id}")
                .replace("%attachmentUrl%", attachment.url)
                .replace("%moment%", event.message.timeCreated.asLongLongGMTString())

        val footer = Translateable("listener.message.attachment.log.footer")
                .string(container.daoManager, event.guild.idLong)
                .replace("%user%", event.author.asTag)
        embedBuilder.setFooter(footer, event.author.effectiveAvatarUrl)

        embedBuilder.setColor(Color.decode("#DC143C"))
        embedBuilder.setImage(attachment.url)

        embedBuilder.setTitle(title)
        embedBuilder.setDescription(description)

        sendEmbed(container.daoManager.embedDisabledWrapper, logChannel, embedBuilder.build())
    }
}