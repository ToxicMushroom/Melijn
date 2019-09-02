package me.melijn.melijnbot.objects.events.eventlisteners

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.enums.ChannelType
import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.objects.events.AbstractListener
import me.melijn.melijnbot.objects.utils.checks.getAndVerifyChannelById
import me.melijn.melijnbot.objects.utils.sendMsg
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberLeaveEvent

class JoinLeaveListener(container: Container) : AbstractListener(container) {

    override fun onEvent(event: GenericEvent) {
        if (event is GuildMemberJoinEvent) onGuildMemberJoin(event)
        else if (event is GuildMemberLeaveEvent) onGuildMemberLeave(event)
    }

    private fun onGuildMemberJoin(event: GuildMemberJoinEvent) = CoroutineScope(Dispatchers.Default).launch {
        postWelcomeMessage(event.guild, container, ChannelType.JOIN, MessageType.JOIN)
    }

    private fun onGuildMemberLeave(event: GuildMemberLeaveEvent) = CoroutineScope(Dispatchers.Default).launch {
        postWelcomeMessage(event.guild, container, ChannelType.LEAVE, MessageType.LEAVE)
    }

    private suspend fun postWelcomeMessage(guild: Guild, container: Container, channelType: ChannelType, messageType: MessageType) {
        val channelWrapper = container.daoManager.channelWrapper
        val channelId = channelWrapper.channelCache.get(Pair(guild.idLong, channelType)).await()
        val channel = guild.getAndVerifyChannelById(channelType, channelId, channelWrapper) ?: return

        val messageWrapper = container.daoManager.messageWrapper
        val message = messageWrapper.messageCache.get(Pair(guild.idLong, messageType)).await()
        if (message.isNotBlank()) {
            sendMsg(channel, message)
        }
    }
}