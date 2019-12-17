package me.melijn.melijnbot.objects.services.stats

import kotlinx.coroutines.runBlocking
import me.melijn.melijnbot.objects.services.Service
import me.melijn.melijnbot.objects.web.WebManager
import net.dv8tion.jda.api.sharding.ShardManager
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class StatService(val shardManager: ShardManager, val webManager: WebManager) : Service("stat") {

    private var scheduledFuture: ScheduledFuture<*>? = null

    private val statService = Runnable {
        runBlocking {
            val shards = shardManager.shardCache.size()
            val guildArray = shardManager.shardCache.map { shard -> shard.guildCache.size() }
            val usersArray = shardManager.shardCache.map { shard -> shard.userCache.size() }
            val guilds = shardManager.guildCache.size()
            val users = shardManager.userCache.size()

            webManager.updateTopDotGG(guildArray) // 1s ratelimit
            webManager.updateBotsOnDiscordXYZ(guilds) // 2min ratelimit
            webManager.updateBotlistSpace(guildArray) // 15s ratelimit
            webManager.updateDiscordBotListCom(guilds, users) // no
            webManager.updateDivinedDiscordBots(guilds, shards) // 1min ratelimit
            webManager.updateDiscordBotsGG(guilds, shards) // 0.05s ratelimit
            webManager.updateBotsForDiscordCom(guilds) // no
            webManager.updateDiscordBoats(guilds) // 1s
        }
    }

    override fun start() {
        logger.info("Started StatService")
        scheduledFuture = scheduledExecutor.scheduleWithFixedDelay(statService, 1, 3, TimeUnit.MINUTES)
    }

    override fun stop() {
        logger.info("Stopping StatService")
        scheduledFuture?.cancel(false)
    }
}