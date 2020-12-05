package me.melijn.melijnbot

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory
import kotlinx.coroutines.runBlocking
import me.melijn.llklient.io.jda.JDALavalink
import me.melijn.melijnbot.internals.Settings
import me.melijn.melijnbot.internals.events.EventManager
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.web.RestServer
import net.dv8tion.jda.api.GatewayEncoding
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.ChunkingFilter
import net.dv8tion.jda.api.utils.cache.CacheFlag
import net.dv8tion.jda.internal.requests.restaction.MessageActionImpl
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.*


class MelijnBot {

    private val logger = LoggerFactory.getLogger(MelijnBot::class.java)

    companion object {
        lateinit var instance: MelijnBot
        lateinit var shardManager: ShardManager
        lateinit var eventManager: EventManager
    }

    init {
        instance = this
        Locale.setDefault(Locale.ENGLISH)
        MessageActionImpl.setDefaultMentions(emptyList())

        val container = Container()

        val nodeMap = mutableMapOf<String, Array<Settings.Lavalink.LLNode>>()
        nodeMap["normal"] = container.settings.lavalink.verified_nodes
        nodeMap["http"] = container.settings.lavalink.http_nodes

        logger.info("Connecting to lavalink")
        val jdaLavaLink = runBlocking {
            TaskManager.taskValueAsync { generateJdaLinkFromNodes(container, nodeMap) }.await()
        }

        container.initLava(jdaLavaLink)

        eventManager = EventManager(container)

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
            .setShardsTotal(container.settings.botInfo.shardCount)
            .setToken(container.settings.tokens.discord)
            .setActivity(Activity.listening("commands | ${container.settings.botInfo.prefix}help"))
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

        container.startTime = System.currentTimeMillis()

        logger.info("Starting services..")
        container.serviceManager.init(container, shardManager)
        container.serviceManager.startServices()
        logger.info("Services ready")

        TaskManager.async {
            logger.info("Starting rest-server..")

            val restServer = RestServer(container)
            restServer.start()

            container.restServer = restServer
            logger.info("Started rest-server")
        }
    }

    private suspend fun generateJdaLinkFromNodes(
        container: Container,
        nodeMap: Map<String, Array<Settings.Lavalink.LLNode>>
    ): JDALavalink? {
        return if (container.settings.lavalink.enabled) {
            val linkBuilder = JDALavalink(
                container.settings.botInfo.id,
                container.settings.botInfo.shardCount
            ) { id ->
                shardManager.getShardById(id)
            }

            linkBuilder.autoReconnect = true
            linkBuilder.defaultGroupId = "normal"

            for ((groupId, nodeList) in nodeMap) {
                for ((_, host, pass) in nodeList) {
                    linkBuilder.addNode(groupId, URI.create("ws://${host}"), pass)
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