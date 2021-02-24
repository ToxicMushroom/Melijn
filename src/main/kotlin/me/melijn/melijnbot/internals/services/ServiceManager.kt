package me.melijn.melijnbot.internals.services

import me.melijn.melijnbot.Container
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.internals.services.bans.BanService
import me.melijn.melijnbot.internals.services.birthday.BirthdayService
import me.melijn.melijnbot.internals.services.donator.DonatorService
import me.melijn.melijnbot.internals.services.message.MessageCleanerService
import me.melijn.melijnbot.internals.services.music.SpotifyService
import me.melijn.melijnbot.internals.services.mutes.MuteService
import me.melijn.melijnbot.internals.services.ppexpiry.PPExpireService
import me.melijn.melijnbot.internals.services.reddit.RedditAboutService
import me.melijn.melijnbot.internals.services.reddit.RedditService
import me.melijn.melijnbot.internals.services.reminders.ReminderService
import me.melijn.melijnbot.internals.services.rockpaperscissors.RSPService
import me.melijn.melijnbot.internals.services.roles.RolesService
import me.melijn.melijnbot.internals.services.stats.StatsService
import me.melijn.melijnbot.internals.services.twitter.TwitterService
import me.melijn.melijnbot.internals.services.voice.VoiceScoutService
import me.melijn.melijnbot.internals.services.voice.VoiceService
import me.melijn.melijnbot.internals.services.votes.VoteReminderService
import me.melijn.melijnbot.internals.web.WebManager
import net.dv8tion.jda.api.sharding.ShardManager

class ServiceManager(val daoManager: DaoManager, val webManager: WebManager) {

    var started = false
    var slowStarted = false
    var shardManager: ShardManager? = null
    val services = mutableListOf<Service>()
    val slowServices = mutableListOf<Service>()

    // TODO make everything able to start as fast* services
    fun init(container: Container, shardManager: ShardManager) {
        this.shardManager = shardManager
        slowServices.add(BanService(shardManager, daoManager))
        slowServices.add(MuteService(shardManager, daoManager))
        slowServices.add(StatsService(shardManager, webManager.botListApi))
        slowServices.add(BirthdayService(shardManager, webManager.proxiedHttpClient, daoManager))
        slowServices.add(VoteReminderService(daoManager))
        webManager.spotifyApi?.let { spotifyApi ->
            services.add(SpotifyService(spotifyApi))
        }

        services.add(MessageCleanerService(daoManager.messageHistoryWrapper))
        slowServices.add(VoiceService(container, shardManager))
        slowServices.add(VoiceScoutService(container, shardManager))
        services.add(DonatorService(container, shardManager))
        slowServices.add(RolesService(daoManager.tempRoleWrapper, shardManager))
        slowServices.add(ReminderService(daoManager))
//        slowServices.add(SpamService(container, shardManager))
        services.add(RedditService(webManager.httpClient, daoManager.driverManager))
        services.add(RedditAboutService(webManager.httpClient, daoManager.driverManager))
        services.add(PPExpireService(daoManager.autoPunishmentWrapper))
        slowServices.add(
            TwitterService(
                webManager.proxiedHttpClient,
                container.settings.api.twitter.bearerToken,
                daoManager.twitterWrapper,
                shardManager
            )
        )
        services.add(RSPService(shardManager, daoManager))
    }

    fun startSlowservices() {
        requireNotNull(shardManager) { "Init first!" }
        slowServices.forEach { service ->
            service.start()
        }

        slowStarted = true
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