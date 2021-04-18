package me.melijn.melijnbot.internals.services

import me.melijn.melijnbot.Container
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.internals.services.bans.BanService
import me.melijn.melijnbot.internals.services.birthday.BirthdayService
import me.melijn.melijnbot.internals.services.donator.DonatorService
import me.melijn.melijnbot.internals.services.games.RSPService
import me.melijn.melijnbot.internals.services.games.TTTService
import me.melijn.melijnbot.internals.services.message.MessageCleanerService
import me.melijn.melijnbot.internals.services.messagedeletion.MessageDeletionService
import me.melijn.melijnbot.internals.services.music.SpotifyService
import me.melijn.melijnbot.internals.services.mutes.MuteService
import me.melijn.melijnbot.internals.services.ppexpiry.PPExpireService
import me.melijn.melijnbot.internals.services.reddit.RedditAboutService
import me.melijn.melijnbot.internals.services.reddit.RedditService
import me.melijn.melijnbot.internals.services.reminders.ReminderService
import me.melijn.melijnbot.internals.services.roles.RolesService
import me.melijn.melijnbot.internals.services.stats.StatsService
import me.melijn.melijnbot.internals.services.twitter.TwitterService
import me.melijn.melijnbot.internals.services.voice.VoiceScoutService
import me.melijn.melijnbot.internals.services.voice.VoiceService
import me.melijn.melijnbot.internals.services.votes.VoteReminderService
import me.melijn.melijnbot.internals.web.WebManager
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.MiscUtil

class ServiceManager(val daoManager: DaoManager, val webManager: WebManager) {

    var started = false
    var slowStarted = false
    var shardManager: ShardManager? = null

    val services = mutableListOf<Service>()
    val slowServices = mutableListOf<Service>()

    // TODO make everything able to start as fast* services
    fun init(container: Container, shardManager: ShardManager) {
        this.shardManager = shardManager
        val podInfo = container.podInfo
        slowServices.add(BanService(shardManager, daoManager, podInfo))
        slowServices.add(MuteService(shardManager, daoManager, podInfo))
        slowServices.add(StatsService(shardManager, webManager.botListApi))
        slowServices.add(BirthdayService(shardManager, webManager.proxiedHttpClient, daoManager))


        // TODO: create microservice for proper ratelimits
        webManager.spotifyApi?.let { spotifyApi ->
            services.add(SpotifyService(spotifyApi))
        }

        slowServices.add(VoiceService(container, shardManager))
        slowServices.add(VoiceScoutService(container, shardManager))
        slowServices.add(RolesService(daoManager.tempRoleWrapper, shardManager))
//        slowServices.add(SpamService(container, shardManager))

        services.add(MessageDeletionService(shardManager))
        slowServices.add(
            TwitterService(
                webManager.proxiedHttpClient,
                container.settings.api.twitter.bearerToken,
                daoManager.twitterWrapper,
                shardManager,
                podInfo
            )
        )

        // Some conditional services
        if (podInfo.minShardId == 0) {

            services.add(VoteReminderService(daoManager))
            services.add(MessageCleanerService(daoManager.messageHistoryWrapper))
            services.add(ReminderService(daoManager))
            services.add(PPExpireService(daoManager.autoPunishmentWrapper))
            services.add(RedditService(webManager.httpClient, daoManager.driverManager))
            services.add(RedditAboutService(webManager.httpClient, daoManager.driverManager))
            services.add(RSPService(shardManager, daoManager))
            services.add(TTTService(shardManager, daoManager))
        }


        val shards = podInfo.shardList
        val melijnGuildId = 340081887265685504L
        val guildShardId = MiscUtil.getShardForGuild(melijnGuildId, podInfo.shardCount)
        if (shards.contains(guildShardId)) {
            services.add(DonatorService(container, shardManager))
        }
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