package me.melijn.melijnbot.database.audio

import com.google.common.cache.CacheBuilder
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.loadingCacheFrom
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class Music247Wrapper(val music247Dao: Music247Dao) {

    val music247Cache = CacheBuilder.newBuilder()
        .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Long, Boolean> { guildId ->
            is247Mode(guildId)
        })

    private fun is247Mode(guildId: Long): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()

       TaskManager.async {
            val contains = music247Dao.contains(guildId)
            future.complete(contains)
        }

        return future
    }

    suspend fun add(guildId: Long) {
        music247Dao.add(guildId)
        music247Cache.put(guildId, CompletableFuture.completedFuture(true))
    }

    suspend fun remove(guildId: Long) {
        music247Dao.remove(guildId)
        music247Cache.put(guildId, CompletableFuture.completedFuture(false))
    }

}