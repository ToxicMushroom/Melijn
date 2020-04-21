package me.melijn.melijnbot.objects.services.stats

import me.melijn.melijnbot.objects.services.Service
import me.melijn.melijnbot.objects.threading.Task
import me.melijn.melijnbot.objects.web.WebManager
import net.dv8tion.jda.api.sharding.ShardManager
import java.util.concurrent.TimeUnit

class StatsService(
    val shardManager: ShardManager,
    val webManager: WebManager
) : Service("Stats", 2, 3, TimeUnit.MINUTES) {

    override val service = Task {
        val shards = shardManager.shardCache.size()
        val guildArray = shardManager.shardCache.map { shard -> shard.guildCache.size() }
        val guilds = shardManager.guildCache.size()

        webManager.updateTopDotGG(guildArray) // 1s ratelimit
        webManager.updateBotsOnDiscordXYZ(guilds) // 2min ratelimit
        webManager.updateBotlistSpace(guildArray) // 15s ratelimit
        webManager.updateDiscordBotListCom(guilds) // no
        webManager.updateDiscordBotsGG(guilds, shards) // 0.05s ratelimit
        webManager.updateBotsForDiscordCom(guilds) // no
        webManager.updateDiscordBoats(guilds) // 1s
    }
}