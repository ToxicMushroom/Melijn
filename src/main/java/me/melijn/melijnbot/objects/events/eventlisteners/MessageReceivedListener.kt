package me.melijn.melijnbot.objects.events.eventlisteners

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.database.message.DaoMessage
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.objects.events.AbstractListener
import me.melijn.melijnbot.objects.utils.toMessage
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent

class MessageReceivedListener(container: Container) : AbstractListener(container) {

    override fun onEvent(event: GenericEvent) {
        if (event is GuildMessageReceivedEvent) {
            onGuildMessageReceived(event)
        }
    }

    private fun onGuildMessageReceived(event: GuildMessageReceivedEvent) = runBlocking {
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
}