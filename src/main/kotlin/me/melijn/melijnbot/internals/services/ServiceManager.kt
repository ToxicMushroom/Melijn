package me.melijn.melijnbot.internals.services

import me.melijn.melijnbot.Container
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.internals.services.bans.BanService
import me.melijn.melijnbot.internals.services.bans.BotBanService
import me.melijn.melijnbot.internals.services.donator.DonatorService
import me.melijn.melijnbot.internals.services.games.RSPService
import me.melijn.melijnbot.internals.services.games.TTTService
import me.melijn.melijnbot.internals.services.message.MessageCleanerService
import me.melijn.melijnbot.internals.services.messagedeletion.MessageDeletionService
import me.melijn.melijnbot.internals.services.mutes.MuteService
import me.melijn.melijnbot.internals.services.ppexpiry.PPExpireService
import me.melijn.melijnbot.internals.services.ratelimits.RatelimitService
import me.melijn.melijnbot.internals.services.reddit.RedditAboutService
import me.melijn.melijnbot.internals.services.reddit.RedditService
import me.melijn.melijnbot.internals.services.reminders.ReminderService
import me.melijn.melijnbot.internals.services.roles.RolesService
import me.melijn.melijnbot.internals.services.votes.VoteReminderService
import me.melijn.melijnbot.internals.web.WebManager
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.MiscUtil

class ServiceManager(val daoManager: DaoManager, val webManager: WebManager) {

    var started = false
    var slowStarted = false
    var shardManager: ShardManager? = null

    val services = mutableListOf<Service>()
    private val slowServices = mutableListOf<Service>()

    // TODO make everything able to start as fast* services
    fun init(container: Container, shardManager: ShardManager) {
        this.shardManager = shardManager
        val podInfo = container.podInfo
        val proxiedHttpClient = webManager.proxiedHttpClient
        slowServices.add(BanService(shardManager, daoManager, podInfo, proxiedHttpClient))
        slowServices.add(MuteService(shardManager, daoManager, podInfo, proxiedHttpClient))
//        slowServices.add(BirthdayService(shardManager, webManager.proxiedHttpClient, daoManager))


        slowServices.add(RolesService(daoManager.tempRoleWrapper, shardManager))
        slowServices.add(BotBanService(shardManager, daoManager))
//        slowServices.add(EmoteCacheService(daoManager.emoteCache, shardManager))

        services.add(MessageDeletionService(shardManager))
        services.add(RatelimitService(shardManager))

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

    fun startSlowServices() {
        requireNotNull(shardManager) { "Init shardManager first!" }
        slowServices.startAll()
        slowStarted = true
    }

    fun startServices() {
        requireNotNull(shardManager) { "Init shardManager first!" }
        services.startAll()
        started = true
    }

    fun stopAllServices() {
        require(started) { "Never started!" }
        services.stopAll()
        slowServices.stopAll()
    }

    private fun List<Service>.stopAll() = this.forEach { it.stop() }
    private fun List<Service>.startAll() = this.forEach { it.start() }

}