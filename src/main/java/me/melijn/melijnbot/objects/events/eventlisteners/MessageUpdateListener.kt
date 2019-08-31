package me.melijn.melijnbot.objects.events.eventlisteners

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.database.message.DaoMessage
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.objects.events.AbstractListener
import me.melijn.melijnbot.objects.translation.Translateable
import me.melijn.melijnbot.objects.utils.*
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent
import java.awt.Color

class MessageUpdateListener(container: Container) : AbstractListener(container) {

    override fun onEvent(event: GenericEvent) {
        if (event is GuildMessageUpdateEvent) onMessageUpdate(event)
    }

    private fun onMessageUpdate(event: GuildMessageUpdateEvent) = runBlocking {
        val message = event.message
        val newContent = event.message.contentRaw
        val messageWrapper = container.daoManager.messageWrapper
        val deferredMessage = async { messageWrapper.getMessageById(event.messageIdLong) }
        val daoMessage = deferredMessage.await()
                ?: DaoMessage(
                        event.guild.idLong,
                        event.channel.idLong,
                        event.author.idLong,
                        message.idLong,
                        newContent,
                        message.timeCreated.toInstant().toEpochMilli()
                )
        val oldContent = daoMessage.content
        daoMessage.content = newContent
        GlobalScope.launch {
            messageWrapper.setMessage(daoMessage)
        }

        val channelId = container.daoManager.logChannelWrapper.logChannelCache.get(Pair(event.guild.idLong, LogChannelType.EDITED_MESSAGE)).await()
        if (channelId == -1L) return@runBlocking
        val channel = event.guild.getTextChannelById(channelId) ?: return@runBlocking

        postMessageUpdateLog(event, channel, daoMessage, oldContent)
    }

    private fun postMessageUpdateLog(event: GuildMessageUpdateEvent, logChannel: TextChannel, daoMessage: DaoMessage, oldContent: String) {
        val embedBuilder = EmbedBuilder()
        val title = Translateable("listener.message.update.log.title")
                .string(container.daoManager, event.guild.idLong)
                .replace("%channel%", event.channel.asTag)

        val description = Translateable("listener.message.update.log.description")
                .string(container.daoManager, event.guild.idLong)
                .replace("%oldContent%", escapeForLog(oldContent))
                .replace("%newContent%", escapeForLog(daoMessage.content))
                .replace("%user%", event.author.asTag)
                .replace("%userId%", event.author.id)
                .replace("%sentTime%", event.message.timeCreated.asLongLongGMTString())
                .replace("%editedTime%", System.currentTimeMillis().asEpochMillisToDateTime())
                .replace("%link%", "https://discordapp.com/channels/${event.guild.id}/${event.channel.id}/${event.message.id}")

        embedBuilder.setColor(Color.decode("#000001"))
        embedBuilder.setThumbnail(event.author.effectiveAvatarUrl)

        embedBuilder.setTitle(title)
        embedBuilder.setDescription(description)

        sendEmbed(container.daoManager.embedDisabledWrapper, logChannel, embedBuilder.build())
    }
}