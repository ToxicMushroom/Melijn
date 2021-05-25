package me.melijn.melijnbot.internals.services.statesync

import me.melijn.melijnbot.database.statesync.EmoteCacheDao
import me.melijn.melijnbot.internals.services.Service
import me.melijn.melijnbot.internals.threading.RunnableTask
import net.dv8tion.jda.api.sharding.ShardManager
import java.util.concurrent.TimeUnit

class EmoteCacheService(
    private val cacheDao: EmoteCacheDao, val shardManager: ShardManager
) : Service("EmoteCache", 290, 290, TimeUnit.SECONDS) {

    override val service: RunnableTask = RunnableTask {
        for (emote in shardManager.emotes) {
            cacheDao.save(emote)
        }
    }
}