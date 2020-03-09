package me.melijn.melijnbot.objects.services

import me.melijn.melijnbot.Container
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.objects.services.bans.BanService
import me.melijn.melijnbot.objects.services.birthday.BirthdayService
import me.melijn.melijnbot.objects.services.donator.DonatorService
import me.melijn.melijnbot.objects.services.music.SongCacheCleanerService
import me.melijn.melijnbot.objects.services.music.SpotifyService
import me.melijn.melijnbot.objects.services.mutes.MuteService
import me.melijn.melijnbot.objects.services.stats.StatsService
import me.melijn.melijnbot.objects.services.voice.VoiceScoutService
import me.melijn.melijnbot.objects.services.voice.VoiceService
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.web.WebManager
import net.dv8tion.jda.api.sharding.ShardManager

class ServiceManager(val taskManager: TaskManager, val daoManager: DaoManager, val webManager: WebManager) {

    var started = false
    var shardManager: ShardManager? = null
    val services = mutableListOf<Service>()

    fun init(container: Container, shardManager: ShardManager) {
        this.shardManager = shardManager
        services.add(BanService(shardManager, daoManager))
        services.add(MuteService(shardManager, daoManager))
        services.add(StatsService(shardManager, webManager))
        services.add(BirthdayService(shardManager, daoManager))
        services.add(SpotifyService(webManager))
        services.add(SongCacheCleanerService(daoManager.songCacheWrapper))
        services.add(VoiceService(container, shardManager))
        services.add(VoiceScoutService(container, shardManager))
        services.add(DonatorService(container, shardManager))
        //services.add(BrokenService(container, shardManager))
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