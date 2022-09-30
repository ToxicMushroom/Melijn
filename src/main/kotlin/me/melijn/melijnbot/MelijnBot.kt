package me.melijn.melijnbot

import io.sentry.Sentry
import me.melijn.melijnbot.enums.Environment
import me.melijn.melijnbot.internals.events.EventManager
import me.melijn.melijnbot.internals.jda.MelijnSessionController
import me.melijn.melijnbot.internals.models.PodInfo
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.toLCC
import me.melijn.melijnbot.internals.web.rest.KillerInterceptor
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.requests.restaction.MessageAction
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.ChunkingFilter
import net.dv8tion.jda.api.utils.cache.CacheFlag
import okhttp3.OkHttpClient
import org.slf4j.LoggerFactory
import java.net.InetAddress
import java.util.*
import java.util.concurrent.Executors
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

        // Exception catcher 9000
        initSentry(container)

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
            .setGatewayPoolProvider { TaskManager.gatewayExecutorPool }
            .setEventPoolProvider { TaskManager.eventExecutorPool }
            .setRateLimitPoolProvider { TaskManager.rateLimitExecutorPool }
            .setCallbackPoolProvider { TaskManager.callbackExecutorPool }
            .setAudioPool(Executors.newSingleThreadScheduledExecutor())
            .setBulkDeleteSplittingEnabled(false)
            .setChunkingFilter(ChunkingFilter.NONE)
            .setEventManagerProvider { eventManager }
            .setHttpClientBuilder(OkHttpClient.Builder().addInterceptor(KillerInterceptor()))

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

    private fun initSentry(container: Container) {
        Sentry.init { options ->
            options.dsn = container.settings.sentry.url
            options.environment = container.settings.environment.toLCC()
            options.release = container.settings.botInfo.version
            // Set traces_sample_rate to 1.0 to capture 100% of transactions for performance monitoring.
            // We recommend adjusting this value in production.
            options.tracesSampleRate = 0.1
            // When first trying Sentry it's good to see what the SDK is doing:
            // options.debug = true
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
}

fun main() {
    MelijnBot
}