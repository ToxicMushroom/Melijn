package me.melijn.melijnbot

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import me.melijn.llklient.io.jda.JDALavalink
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.enums.RoleUpdateCause
import me.melijn.melijnbot.internals.Settings
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.events.eventlisteners.EventWaiter
import me.melijn.melijnbot.internals.music.LavaManager
import me.melijn.melijnbot.internals.services.ServiceManager
import me.melijn.melijnbot.internals.utils.ModularPaginationInfo
import me.melijn.melijnbot.internals.utils.PaginationInfo
import me.melijn.melijnbot.internals.web.RestServer
import me.melijn.melijnbot.internals.web.WebManager
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val objectMapper = jacksonObjectMapper()

class Container {


    var voteReq: Boolean = true
    var logToDiscord: Boolean = true

    // userId, roleId, cause
    val roleAddedMap = mutableMapOf<Pair<Long, Long>, RoleUpdateCause>()
    val roleRemovedMap = mutableMapOf<Pair<Long, Long>, RoleUpdateCause>()

    //millis, info
    val paginationMap = mutableMapOf<Long, PaginationInfo>()
    val modularPaginationMap = mutableMapOf<Long, ModularPaginationInfo>()

    val eventWaiter = EventWaiter()

    var restServer: RestServer? = null
    var shuttingDown: Boolean = false
        set(value) {
            if (value) {
                serviceManager.stopServices()
                MelijnBot.shardManager.setActivity(Activity.playing("updating or maintenance"))
                MelijnBot.shardManager.setStatus(OnlineStatus.IDLE)
            }
            field = value
        }

    var startTime = System.currentTimeMillis()

    var settings: Settings = Settings.initSettings()

    //Used by events
    val daoManager = DaoManager(settings.database, settings.redis)
    val webManager = WebManager(settings)

    //enabled on event
    val serviceManager = ServiceManager(daoManager, webManager)

    var jdaLavaLink: JDALavalink? = null

    lateinit var lavaManager: LavaManager

    var commandMap = emptyMap<Int, AbstractCommand>()

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

    fun initLava(jdaLavaLink: JDALavalink?) {
        this.jdaLavaLink = jdaLavaLink
        this.lavaManager = LavaManager(settings.lavalink.enabled, daoManager, jdaLavaLink)
    }

    val uptimeMillis: Long
        get() = System.currentTimeMillis() - startTime
}
