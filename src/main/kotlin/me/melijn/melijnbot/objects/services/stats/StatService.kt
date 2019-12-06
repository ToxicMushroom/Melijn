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
            val serversArray = shardManager.shardCache.map { shard -> shard.userCache.size() }
            val servers = shardManager.guildCache.size()
            val users = shardManager.userCache.size()

            webManager.updateTopDotGG(serversArray) // 1s ratelimit
            webManager.updateBotsOnDiscordXYZ(servers) // 2min ratelimit
            webManager.updateBotlistSpace(serversArray) // 15s ratelimit
            webManager.updateDiscordBotListCom(servers, users) // no
            webManager.updateDivinedDiscordBots(servers, shards) // 1min ratelimit
            webManager.updateDiscordBotsGG(servers, shards) // 0.05s ratelimit
            webManager.updateBotsForDiscordCom(servers) // no
            webManager.updateDiscordBoats(servers) // 1s
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