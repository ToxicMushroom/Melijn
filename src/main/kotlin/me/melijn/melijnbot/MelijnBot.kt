package me.melijn.melijnbot

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory
import lavalink.client.io.jda.JdaLavalink
import me.melijn.melijnbot.objects.events.EventManager
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.ChunkingFilter
import java.net.URI
import java.util.*


class MelijnBot {

    companion object {
        lateinit var instance: MelijnBot
        lateinit var shardManager: ShardManager
    }

    init {
        instance = this
        Locale.setDefault(Locale.ENGLISH)

        val container = Container()


        val jdaLavaLink = if (container.settings.lavalink.enabled) {
            val linkBuilder = JdaLavalink(
                container.settings.id.toString(),
                container.settings.shardCount
            ) { id ->
                shardManager.getShardById(id)
            }
            linkBuilder.autoReconnect = true

            for (node in container.settings.lavalink.nodes) {
                linkBuilder.addNode(URI.create("ws://${node.host}"), node.password)
            }
            linkBuilder
        } else {
            null
        }

        container.initLava(jdaLavaLink)

        val eventManager = EventManager(container)

        val defaultShardManagerBuilder = DefaultShardManagerBuilder()
            .setShardsTotal(container.settings.shardCount)
            .setToken(container.settings.tokens.discord)
            .setActivity(Activity.listening("commands | ${container.settings.prefix}help"))
            .setAutoReconnect(true)
            .setChunkingFilter(ChunkingFilter.NONE)
            .setEventManagerProvider { eventManager }

        if (!container.settings.lavalink.enabled) {
            defaultShardManagerBuilder.setAudioSendFactory(NativeAudioSendFactory())
        } else if (jdaLavaLink != null) {
            defaultShardManagerBuilder.setVoiceDispatchInterceptor(jdaLavaLink.voiceInterceptor)
        }

        shardManager = defaultShardManagerBuilder.build()
        container.initShardManager(shardManager)
        eventManager.start()
    }
}

fun main(args: Array<String>) {
    MelijnBot()
}