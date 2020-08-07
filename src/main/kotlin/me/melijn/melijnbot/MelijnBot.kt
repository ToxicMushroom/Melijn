package me.melijn.melijnbot

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory
import kotlinx.coroutines.runBlocking
import me.melijn.llklient.io.jda.JDALavalink
import me.melijn.melijnbot.internals.Settings
import me.melijn.melijnbot.internals.events.EventManager
import net.dv8tion.jda.api.GatewayEncoding
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.ChunkingFilter
import net.dv8tion.jda.api.utils.cache.CacheFlag
import net.dv8tion.jda.internal.requests.restaction.MessageActionImpl
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
        MessageActionImpl.setDefaultMentions(emptyList())

        val container = Container()

        val nodeMap = mutableMapOf<String, Array<Settings.Lavalink.Node>>()
        nodeMap["normal"] = container.settings.lavalink.verified_nodes
        nodeMap["http"] = container.settings.lavalink.http_nodes
        val jdaLavaLink = runBlocking { generateJdaLinkFromNodes(container, nodeMap) }

        container.initLava(jdaLavaLink)

        val eventManager = EventManager(container)

        val defaultShardManagerBuilder = DefaultShardManagerBuilder
            .create(
                GatewayIntent.DIRECT_MESSAGES,
                GatewayIntent.DIRECT_MESSAGE_REACTIONS,
                GatewayIntent.GUILD_BANS,
                GatewayIntent.GUILD_EMOJIS,
                GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.GUILD_MESSAGE_REACTIONS,
                GatewayIntent.GUILD_VOICE_STATES
            )
            .setShardsTotal(container.settings.shardCount)
            .setToken(container.settings.tokens.discord)
            .setActivity(Activity.listening("commands | ${container.settings.prefix}help"))
            .setAutoReconnect(true)
            .disableCache(CacheFlag.CLIENT_STATUS, CacheFlag.ACTIVITY)
            .setChunkingFilter(ChunkingFilter.NONE)
            .setEventManagerProvider { eventManager }
            .setGatewayEncoding(GatewayEncoding.ETF)

        if (!container.settings.lavalink.enabled) {
            defaultShardManagerBuilder.setAudioSendFactory(NativeAudioSendFactory())
        } else if (jdaLavaLink != null) {
            defaultShardManagerBuilder.setVoiceDispatchInterceptor(jdaLavaLink.voiceInterceptor)
        }

        eventManager.start()
        shardManager = defaultShardManagerBuilder.build()
    }

    private suspend fun generateJdaLinkFromNodes(container: Container, nodeMap: Map<String, Array<Settings.Lavalink.Node>>): JDALavalink? {
        return if (container.settings.lavalink.enabled) {
            val linkBuilder = JDALavalink(
                container.settings.id,
                container.settings.shardCount
            ) { id ->
                shardManager.getShardById(id)
            }

            linkBuilder.autoReconnect = true
            linkBuilder.defaultGroupId = "normal"

            for ((groupId, nodeList) in nodeMap) {
                for ((host, password) in nodeList) {
                    linkBuilder.addNode(groupId, URI.create("ws://${host}"), password)
                }
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