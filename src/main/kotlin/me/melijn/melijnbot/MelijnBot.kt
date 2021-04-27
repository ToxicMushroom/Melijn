package me.melijn.melijnbot

import kotlinx.coroutines.runBlocking
import lol.up.pylon.gateway.client.GatewayGrpcClient
import lol.up.pylon.gateway.client.entity.event.GuildMemberAddEvent
import lol.up.pylon.gateway.client.event.EventSuppliers
import me.melijn.melijnbot.internals.Settings
import me.melijn.melijnbot.internals.events.EventManager
import me.melijn.melijnbot.internals.events.eventlisteners.JoinLeaveListener
import me.melijn.melijnbot.internals.models.PodInfo
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.web.RestServer
import org.slf4j.LoggerFactory
import java.net.InetAddress
import java.net.URI
import java.util.*


object MelijnBot {

    private val logger = LoggerFactory.getLogger(MelijnBot::class.java)
    private val client: GatewayGrpcClient? = null
    var eventManager: EventManager
    var hostName: String = "localhost"
    var podId: Int = 0

    init {
        Locale.setDefault(Locale.ENGLISH)
        System.setProperty(
            kotlinx.coroutines.DEBUG_PROPERTY_NAME,
            kotlinx.coroutines.DEBUG_PROPERTY_VALUE_ON
        )
        val container = Container()
        val settings = container.settings.botInfo
        val client = GatewayGrpcClient.builder(settings.id)
            .setRouterHost("ROUTER_HOST")
            .setRouterPort(6969)
            .build()
        // TODO: Replace grpc server implementation with worker groups
        /*
        client.registerEventSupplier(EventSuppliers.grpcWorkerGroupSupplier(
                env("WORKER_GROUP_AUTH_TOKEN"),
                env("WORKER_GROUP_CONSUMER_GROUP"),
                env("WORKER_GROUP_CONSUMER_ID")
        ));
        */

        client.eventDispatcher = eventManager
        client.registerEventSupplier(EventSuppliers.grpcServerEventSupplier(6969))
        client.registerReceiver(GuildMemberAddEvent::class.java, JoinLeaveListener())


        val podCount = settings.podCount
        val shardCount = settings.shardCount

        try {
            hostName = InetAddress.getLocalHost().hostName
            logger.info("[hostName] {}", hostName)
            podId = if (podCount == 1) 0
            else hostName.split("-").last().toInt()
        } catch (t: Throwable) {
            logger.warn("Cannot parse podId from hostname", t)
//            Thread.sleep(1000)
//            exitProcess(404)
        }
        logger.info("Starting $shardCount shards in $podCount pods")

        val podInfo = PodInfo(podId, podCount, shardCount)
        container.podInfo = podInfo
        logger.info("Shards: {}-{}", podInfo.minShardId, podInfo.maxShardId)
        logger.info("Launching shardManager with {} shards!", podInfo.shardsPerPod)

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
    MelijnBot
}