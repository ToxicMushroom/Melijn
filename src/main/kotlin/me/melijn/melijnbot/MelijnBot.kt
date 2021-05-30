package me.melijn.melijnbot

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory
import kotlinx.coroutines.runBlocking
import me.melijn.llklient.io.jda.JDALavalink
import me.melijn.melijnbot.enums.Environment
import me.melijn.melijnbot.internals.Settings
import me.melijn.melijnbot.internals.events.EventManager
import me.melijn.melijnbot.internals.jda.MelijnSessionController
import me.melijn.melijnbot.internals.models.PodInfo
import me.melijn.melijnbot.internals.threading.TaskManager
import net.dv8tion.jda.api.GatewayEncoding
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.requests.restaction.MessageAction
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.ChunkingFilter
import net.dv8tion.jda.api.utils.cache.CacheFlag
import org.slf4j.LoggerFactory
import java.net.InetAddress
import java.net.URI
import java.util.*
import kotlin.system.exitProcess

object MelijnBot {

    private val logger = LoggerFactory.getLogger(MelijnBot::class.java)
    var shardManager: ShardManager
    var eventManager: EventManager
    var hostName: String = "localhost-0"

    init {
        Locale.setDefault(Locale.ENGLISH)
        System.setProperty(
            kotlinx.coroutines.DEBUG_PROPERTY_NAME,
            kotlinx.coroutines.DEBUG_PROPERTY_VALUE_ON
        )
        MessageAction.setDefaultMentions(emptyList())
        val container = Container()
        val settings = container.settings.botInfo
        val podCount = settings.podCount
        val shardCount = settings.shardCount
        val podId = fetchPodIdFromHostname(
            podCount,
            container.settings.environment == Environment.PRODUCTION
        )
        PodInfo.init(podCount, shardCount, podId)

        logger.info("Starting $shardCount shards in $podCount pods")

        container.podInfo = PodInfo
        logger.info("Shards: {}-{}", PodInfo.minShardId, PodInfo.maxShardId)
        logger.info("Launching shardManager with {} shards!", PodInfo.shardsPerPod)

        val nodeMap = mutableMapOf<String, Array<Settings.Lavalink.LLNode>>()
        nodeMap["normal"] = container.settings.lavalink.verified_nodes
        if (container.settings.lavalink.enabled_http_nodes) {
            nodeMap["http"] = container.settings.lavalink.http_nodes
        }

        logger.info("Connecting to lavalink")
        val jdaLavaLink = runBlocking {
            try {
                generateJdaLinkFromNodes(container, nodeMap)
            } catch (t: Throwable) {
                null
            }
        }

        container.initLava(jdaLavaLink)

        eventManager = EventManager(container)

        logger.info("Building JDA Shardmanager")
        val defaultShardManagerBuilder = DefaultShardManagerBuilder
            .create(
                GatewayIntent.DIRECT_MESSAGES,
                GatewayIntent.DIRECT_MESSAGE_REACTIONS,
                GatewayIntent.GUILD_BANS,
                GatewayIntent.GUILD_EMOJIS,
                GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.GUILD_MESSAGE_REACTIONS,
                GatewayIntent.GUILD_VOICE_STATES,
            )
            .setRawEventsEnabled(true)
            .setShardsTotal(shardCount)
            .setShards(PodInfo.minShardId, PodInfo.maxShardId)
            .setToken(container.settings.tokens.discord)
            .setActivity(Activity.playing("Starting.."))
            .setStatus(OnlineStatus.IDLE)
            .setAutoReconnect(true)
            .disableCache(CacheFlag.CLIENT_STATUS, CacheFlag.ACTIVITY, CacheFlag.ONLINE_STATUS)
            .setSessionController(MelijnSessionController(container.daoManager.rateLimitWrapper))
            .setBulkDeleteSplittingEnabled(false)
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
            container.restServer.start()
            logger.info("Started rest-server")
        }
        TaskManager.async {
            logger.info("Starting probe-server..")
            container.probeServer.start()
            logger.info("Started probe-server")
        }
    }

    private fun fetchPodIdFromHostname(podCount: Int, dynamic: Boolean) = try {
        if (dynamic) hostName = InetAddress.getLocalHost().hostName
        logger.info("[hostName] {}", hostName)

        if (podCount == 1) 0
        else hostName.split("-").last().toInt()
    } catch (t: Throwable) {
        logger.warn("Cannot parse podId from hostname", t)
        if (podCount == 1) 0
        else {
            Thread.sleep(1000)
            exitProcess(404)
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
    MelijnBot
}