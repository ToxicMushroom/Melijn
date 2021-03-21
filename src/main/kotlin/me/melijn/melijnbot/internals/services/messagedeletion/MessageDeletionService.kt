package me.melijn.melijnbot.internals.services.messagedeletion

import me.melijn.melijnbot.internals.services.Service
import me.melijn.melijnbot.internals.threading.RunnableTask
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.sharding.ShardManager
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class MessageDeletionService(val shardManager: ShardManager) : Service("MessageDeletion", 5, 5, TimeUnit.SECONDS) {

    companion object {
        val messageQueue = ConcurrentHashMap<Long, Map<Long, List<Long>>>()

        fun queueMessageDeletes(textChannel: TextChannel, id: Long) {
            val guildId = textChannel.guild.idLong
            val channelId = textChannel.idLong
            val channelMsgList = messageQueue.getOrDefault(guildId, mutableMapOf()).toMutableMap()
            val msgs = channelMsgList.getOrDefault(channelId, mutableListOf()).toMutableList()
            msgs.add(id)
            channelMsgList[channelId] = msgs
            messageQueue[guildId] = channelMsgList
        }
    }

    override val service: RunnableTask = RunnableTask {
        val iterator = messageQueue.iterator()
        while (iterator.hasNext()) {
            val (guildId, textMap) = iterator.next()
            val guild = shardManager.getGuildById(guildId)
            if (guild == null) {
                val guildLeft = shardManager.shards.all {
                    it.status == JDA.Status.CONNECTED && it.unavailableGuilds.isEmpty()
                }
                if (guildLeft) {
                    iterator.remove()
                }
                continue
            } else {
                val iterator2 = textMap.iterator()
                while (iterator2.hasNext()) {
                    val (channelId, messages) = iterator2.next()
                    val text = guild.getTextChannelById(channelId)
                    text?.purgeMessagesById(messages.map { it.toString() })
                }
                iterator.remove()
            }
        }
    }
}