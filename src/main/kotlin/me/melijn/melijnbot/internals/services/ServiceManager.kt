package me.melijn.melijnbot.internals.services

import me.melijn.melijnbot.Container
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.internals.services.bans.BanService
import me.melijn.melijnbot.internals.services.birthday.BirthdayService
import me.melijn.melijnbot.internals.services.donator.DonatorService
import me.melijn.melijnbot.internals.services.message.MessageCleanerService
import me.melijn.melijnbot.internals.services.music.SongCacheCleanerService
import me.melijn.melijnbot.internals.services.music.SpotifyService
import me.melijn.melijnbot.internals.services.mutes.MuteService
import me.melijn.melijnbot.internals.services.roles.RolesService
import me.melijn.melijnbot.internals.services.stats.StatsService
import me.melijn.melijnbot.internals.services.voice.VoiceScoutService
import me.melijn.melijnbot.internals.services.voice.VoiceService
import me.melijn.melijnbot.internals.services.votes.VoteReminderService
import me.melijn.melijnbot.internals.web.WebManager
import net.dv8tion.jda.api.sharding.ShardManager

class ServiceManager(val daoManager: DaoManager, val webManager: WebManager) {

    var started = false
    var shardManager: ShardManager? = null
    val services = mutableListOf<Service>()

    fun init(container: Container, shardManager: ShardManager) {
        this.shardManager = shardManager
        services.add(BanService(shardManager, daoManager))
        services.add(MuteService(shardManager, daoManager))
        services.add(StatsService(shardManager, webManager.botListApi))
        services.add(BirthdayService(shardManager, daoManager))
        //services.add(MemSpammerService())
        webManager.spotifyApi?.let { spotifyApi ->
            services.add(SpotifyService(spotifyApi))
        }

        services.add(SongCacheCleanerService(daoManager.songCacheWrapper))
        services.add(MessageCleanerService(daoManager.messageHistoryWrapper))
        services.add(VoiceService(container, shardManager))
        services.add(VoiceScoutService(container, shardManager))
        services.add(DonatorService(container, shardManager))
        services.add(RolesService(daoManager.tempRoleWrapper, shardManager))
        services.add(VoteReminderService(daoManager))
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