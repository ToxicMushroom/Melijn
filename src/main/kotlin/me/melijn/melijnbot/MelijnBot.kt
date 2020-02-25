package me.melijn.melijnbot

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory
import lavalink.client.io.jda.JdaLavalink
import me.melijn.melijnbot.objects.events.EventManager
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.ChunkingFilter
import net.dv8tion.jda.api.utils.cache.CacheFlag
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

        val jdaLavaLink = generateJdaLinkFromNodes(container, container.settings.lavalink.verified_nodes)
        val premiumJdaLavaLink = generateJdaLinkFromNodes(container, container.settings.lavalink.http_nodes)

        container.initLava(jdaLavaLink, premiumJdaLavaLink)

        val eventManager = EventManager(container)

        val defaultShardManagerBuilder = DefaultShardManagerBuilder()
            .setShardsTotal(container.settings.shardCount)
            .setToken(container.settings.tokens.discord)
            .setActivity(Activity.listening("commands | ${container.settings.prefix}help"))
            .setAutoReconnect(true)
            .setDisabledCacheFlags(EnumSet.of(CacheFlag.CLIENT_STATUS, CacheFlag.ACTIVITY))
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

    private fun generateJdaLinkFromNodes(container: Container, httpNodes: Array<Settings.Lavalink.Node>): JdaLavalink? {
        return if (container.settings.lavalink.enabled) {
            val linkBuilder = JdaLavalink(
                container.settings.id.toString(),
                container.settings.shardCount
            ) { id ->
                shardManager.getShardById(id)
            }

            linkBuilder.autoReconnect = true

            for (node in httpNodes) {
                linkBuilder.addNode(URI.create("ws://${node.host}"), node.password)
            }
            linkBuilder
        } else {
            null
        }
    }
}

fun main() {
    MelijnBot()
}