package me.melijn.melijnbot.objects.services.stats

import me.melijn.melijnbot.objects.events.eventutil.VoiceUtil
import me.melijn.melijnbot.objects.services.Service
import me.melijn.melijnbot.objects.threading.Task
import me.melijn.melijnbot.objects.web.cancer.BotListApi
import net.dv8tion.jda.api.sharding.ShardManager
import java.util.concurrent.TimeUnit

class StatsService(
    val shardManager: ShardManager,
    private val botListApi: BotListApi
) : Service("Stats", 2, 3, TimeUnit.MINUTES) {

    override val service = Task {
        val shards = shardManager.shardCache.size()
        val guildArray = shardManager.shardCache.map { shard -> shard.guildCache.size() }
        val guilds = shardManager.guildCache.size()
        val voice = VoiceUtil.getConnectedChannelsAmount(shardManager)

        botListApi.updateTopDotGG(guildArray) // 1s ratelimit
        botListApi.updateBotsOnDiscordXYZ(guilds) // 2min ratelimit
        botListApi.updateBotlistSpace(guildArray) // 15s ratelimit
        botListApi.updateDiscordBotListCom(guilds, voice) // no
        botListApi.updateDiscordBotsGG(guilds, shards) // 0.05s ratelimit
        botListApi.updateBotsForDiscordCom(guilds) // no
        botListApi.updateDiscordBoats(guilds) // 1s
    }
}