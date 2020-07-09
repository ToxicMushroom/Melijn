package me.melijn.melijnbot

import com.fasterxml.jackson.databind.ObjectMapper
import me.melijn.llklient.io.jda.JDALavalink
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.enums.RoleUpdateCause
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.events.eventlisteners.EventWaiter
import me.melijn.melijnbot.internals.music.LavaManager
import me.melijn.melijnbot.internals.services.ServiceManager
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.ModularPaginationInfo
import me.melijn.melijnbot.internals.utils.PaginationInfo
import me.melijn.melijnbot.internals.web.RestServer
import me.melijn.melijnbot.internals.web.WebManager
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.sharding.ShardManager
import java.io.File

val objectMapper = ObjectMapper()

class Container {


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
                restServer?.stop()
                MelijnBot.shardManager.setActivity(Activity.playing("shutting down"))
                MelijnBot.shardManager.setStatus(OnlineStatus.DO_NOT_DISTURB)
            }
            field = value
        }

    var startTime = System.currentTimeMillis()

    var settings: Settings = objectMapper.readValue(File("${System.getenv("CONFIG_NAME") ?: "config"}.json"), Settings::class.java)
    val taskManager = TaskManager()

    //Used by events
    val daoManager = DaoManager(taskManager, settings.database)
    val webManager = WebManager(taskManager, settings)

    //enabled on event
    val serviceManager = ServiceManager(taskManager, daoManager, webManager)

    lateinit var lavaManager: LavaManager

    var commandMap = emptyMap<Int, AbstractCommand>()

    //<messageId, <infoType (must_contain ect), info (wordList)>>
    val filteredMap = mutableMapOf<Long, Map<String, List<String>>>()

    //messageId, purgerId
    val purgedIds = mutableMapOf<Long, Long>()

    //messageId
    val botDeletedMessageIds = mutableSetOf<Long>()

    var jdaLavaLink: JDALavalink? = null
    var premiumJdaLavaLink: JDALavalink? = null

    init {
        instance = this
    }

    companion object {
        lateinit var instance: Container
    }

    fun initShardManager(shardManager: ShardManager) {
        lavaManager = LavaManager(settings.lavalink.enabled, daoManager, shardManager, jdaLavaLink, premiumJdaLavaLink)
    }


    fun initLava(jdaLavaLink: JDALavalink?, premiumJdaLavaLink: JDALavalink?) {
        this.jdaLavaLink = jdaLavaLink
        this.premiumJdaLavaLink = premiumJdaLavaLink
    }

    val uptimeMillis: Long
        get() = System.currentTimeMillis() - startTime
}
