package me.melijn.melijnbot.objects.services

import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.objects.services.bans.BanService
import me.melijn.melijnbot.objects.services.birthday.BirthdayService
import me.melijn.melijnbot.objects.services.mutes.MuteService
import me.melijn.melijnbot.objects.services.spotify.SpotifyService
import me.melijn.melijnbot.objects.services.stats.StatsService
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.web.WebManager
import net.dv8tion.jda.api.sharding.ShardManager

class ServiceManager(val taskManager: TaskManager, val daoManager: DaoManager, val webManager: WebManager) {

    var started = false
    var shardManager: ShardManager? = null
    private val services = mutableListOf<Service>()

    fun init(shardManager: ShardManager) {
        this.shardManager = shardManager
        services.add(BanService(shardManager, daoManager))
        services.add(MuteService(shardManager, daoManager))
        services.add(StatsService(shardManager, webManager))
        services.add(BirthdayService(shardManager, daoManager))
        services.add(SpotifyService(webManager))
    }

    fun startServices() {
        requireNotNull(shardManager) { "Init first!" }
        services.forEach { service ->
            service.start()
        }

        started = true
    }

    fun stopServices() {
        require(started) { "Never started!" }
        services.forEach { service ->
            service.stop()
        }
    }
}