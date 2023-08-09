package me.melijn.melijnbot

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.internals.Settings
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.events.eventlisteners.EventWaiter
import me.melijn.melijnbot.internals.models.PodInfo
import me.melijn.melijnbot.internals.services.ServiceManager
import me.melijn.melijnbot.internals.utils.FetchingPaginationInfo
import me.melijn.melijnbot.internals.utils.ModularFetchingPaginationInfo
import me.melijn.melijnbot.internals.utils.ModularStoragePaginationInfo
import me.melijn.melijnbot.internals.utils.StoragePaginationInfo
import me.melijn.melijnbot.internals.web.ProbeServer
import me.melijn.melijnbot.internals.web.RestServer
import me.melijn.melijnbot.internals.web.WebManager
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val objectMapper: ObjectMapper = jacksonObjectMapper()
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

class Container {

    var voteReq: Boolean = true
    var logToDiscord: Boolean = true
    lateinit var podInfo: PodInfo

    //millis, info
    val fetcherPaginationMap = mutableMapOf<Long, FetchingPaginationInfo>()
    val modularFetcherPaginationMap = mutableMapOf<Long, ModularFetchingPaginationInfo>()
    val paginationMap = mutableMapOf<Long, StoragePaginationInfo>()
    val modularPaginationMap = mutableMapOf<Long, ModularStoragePaginationInfo>()
    val eventWaiter by lazy { EventWaiter() }

    val restServer: RestServer by lazy { RestServer(this) }
    val probeServer: ProbeServer by lazy { ProbeServer(this) }
    var shuttingDown: Boolean = false
        set(value) {
            if (value) {
                serviceManager.stopAllServices()
                MelijnBot.shardManager.setActivity(Activity.playing("updating or maintenance"))
                MelijnBot.shardManager.setStatus(OnlineStatus.IDLE)
            }
            field = value
        }

    var ratelimiting: Boolean = false


    var startTime = System.currentTimeMillis()

    var settings: Settings = Settings.initSettings()

    //Used by events
    val daoManager by lazy { DaoManager(settings.database, settings.redis, settings.tokens.hot) }
    val webManager by lazy { WebManager(settings) }

    //enabled on event
    val serviceManager by lazy { ServiceManager(daoManager, webManager) }

//    var jdaLavaLink: JDALavalink? = null
//
//    lateinit var lavaManager: LavaManager

    var commandMap = emptyMap<Int, AbstractCommand>()
    var commandSet = emptySet<AbstractCommand>()

    // safe for clustering
    //<messageId, <infoType (must_contain ect), info (wordList)>>
    val filteredMap = mutableMapOf<Long, Map<String, List<String>>>()

    //messageId, purgerId
    val purgedIds = mutableMapOf<Long, Long>()

    //messageId
    val botDeletedMessageIds = mutableSetOf<Long>()

    private val logger: Logger = LoggerFactory.getLogger(Container::class.java)

    init {
        logger.info("Using ${System.getenv("CONFIG_NAME") ?: "config"}.json as config")
        Embedder.defaultColor = settings.botInfo.embedColor
        instance = this
    }

    companion object {
        lateinit var instance: Container
    }

//    fun initLava(jdaLavaLink: JDALavalink?) {
//        this.jdaLavaLink = jdaLavaLink
//        this.lavaManager = LavaManager(settings.lavalink.enabled, daoManager, jdaLavaLink)
//    }

    val uptimeMillis: Long
        get() = System.currentTimeMillis() - startTime
}
