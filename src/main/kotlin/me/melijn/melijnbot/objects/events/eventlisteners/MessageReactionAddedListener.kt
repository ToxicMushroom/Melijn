package me.melijn.melijnbot.objects.events.eventlisteners

import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.objects.events.AbstractListener
import me.melijn.melijnbot.objects.events.eventutil.SelfRoleUtil
import me.melijn.melijnbot.objects.translation.getLanguage
import me.melijn.melijnbot.objects.translation.i18n
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
        selfRoleHandler(event)
        val channelId = container.daoManager.logChannelWrapper.logChannelCache.get(Pair(event.guild.idLong, LogChannelType.REACTION)).await()
        if (channelId == -1L) return@runBlocking
        val channel = event.guild.getTextChannelById(channelId) ?: return@runBlocking
        postReactionAddedLog(event, channel, container)
    }

    private suspend fun postReactionAddedLog(event: GuildMessageReactionAddEvent, logChannel: TextChannel, container: Container) {
        val embedBuilder = EmbedBuilder()
        val language = getLanguage(container.daoManager, -1, event.guild.idLong)
        val title = i18n.getTranslation(language, "listener.message.reaction.log.title")
            .replace("%channel%", event.channel.asTag)

        val part = if (event.reactionEmote.isEmote) "emote" else "emoji"
        val description = i18n.getTranslation(language, "listener.message.reaction.$part.log.description")
            .replace("%userId%", event.member.id)
            .replace("%messageId%", event.messageId)
            .replace("%emoteName%", event.reactionEmote.name)
            .replace("%emoteId%", if (event.reactionEmote.isEmote) event.reactionEmote.id else "/")
            .replace("%moment%", System.currentTimeMillis().asEpochMillisToDateTime())
            .replace("%messageUrl%", "https://discordapp.com/channels/${event.guild.id}/${event.channel.id}/${event.messageId}")
            .replace("%emoteUrl%", if (event.reactionEmote.isEmote) event.reactionEmote.emote.imageUrl else "/")

        embedBuilder.setTitle(title)
        embedBuilder.setDescription(description)
        embedBuilder.setThumbnail(event.reactionEmote.emote.imageUrl)
        val footer = i18n.getTranslation(language, "listener.message.reaction.log.footer")
            .replace("%user%", event.member.asTag)
        embedBuilder.setFooter(footer, event.member.user.effectiveAvatarUrl)
        embedBuilder.setColor(Color.WHITE)

        sendEmbed(container.daoManager.embedDisabledWrapper, logChannel, embedBuilder.build())
    }

    private suspend fun selfRoleHandler(event: GuildMessageReactionAddEvent) {
        val guild = event.guild
        val member = event.member
        val role = SelfRoleUtil.getSelectedSelfRoleNByReactionEvent(event, container) ?: return

        if (!member.roles.contains(role)) {
            guild.addRoleToMember(member, role).queue()
        }
    }
}