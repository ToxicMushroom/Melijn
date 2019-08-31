package me.melijn.melijnbot.objects.events.eventlisteners

import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.objects.events.AbstractListener
import me.melijn.melijnbot.objects.translation.Translateable
import me.melijn.melijnbot.objects.utils.asEpochMillisToDateTime
import me.melijn.melijnbot.objects.utils.asTag
import me.melijn.melijnbot.objects.utils.sendEmbed
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent
import java.awt.Color

class MessageReactionAddedListener(container: Container) : AbstractListener(container) {
    override fun onEvent(event: GenericEvent) {
        if (event is GuildMessageReactionAddEvent) onGuildMessageReactionAdd(event)
    }

    private fun onGuildMessageReactionAdd(event: GuildMessageReactionAddEvent) = runBlocking {
        val channelId = container.daoManager.logChannelWrapper.logChannelCache.get(Pair(event.guild.idLong, LogChannelType.REACTION)).await()
        if (channelId == -1L) return@runBlocking
        val channel = event.guild.getTextChannelById(channelId) ?: return@runBlocking
        postReactionAddedLog(event, channel, container)
    }

    private fun postReactionAddedLog(event: GuildMessageReactionAddEvent, logChannel: TextChannel, container: Container) {
        val embedBuilder = EmbedBuilder()
        val title = Translateable("listener.message.reaction.log.title")
                .string(container.daoManager, event.guild.idLong)
                .replace("%channel%", event.channel.asTag)

        val part = if (event.reactionEmote.isEmote) "emote" else "emoji"
        val description = Translateable("listener.message.reaction.$part.log.description")
                .string(container.daoManager, event.guild.idLong)
                .replace("%userId%", event.member.id)
                .replace("%messageId%", event.messageId)
                .replace("%emoteName%", event.reactionEmote.name)
                .replace("%emoteId%", if (event.reactionEmote.isEmote) event.reactionEmote.id else "no id")
                .replace("%moment%", System.currentTimeMillis().asEpochMillisToDateTime())
                .replace("%messageUrl%", "https://discordapp.com/channels/${event.guild.id}/${event.channel.id}/${event.messageId}")
                .replace("%emoteUrl%", event.reactionEmote.emote.imageUrl)

        embedBuilder.setTitle(title)
        embedBuilder.setDescription(description)
        embedBuilder.setThumbnail(event.reactionEmote.emote.imageUrl)
        val footer = Translateable("listener.message.reaction.log.footer")
                .string(container.daoManager, event.guild.idLong)
                .replace("%user%", event.member.asTag)
        embedBuilder.setFooter(footer, event.member.user.effectiveAvatarUrl)
        embedBuilder.setColor(Color.WHITE)

        sendEmbed(container.daoManager.embedDisabledWrapper, logChannel, embedBuilder.build())
    }
}