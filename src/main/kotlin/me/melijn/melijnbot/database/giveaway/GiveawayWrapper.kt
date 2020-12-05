package me.melijn.melijnbot.database.giveaway

import com.google.common.cache.CacheBuilder
import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.loadingCacheFrom
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class GiveawayWrapper(val taskManager: TaskManager, val giveawayDao: GiveawayDao) {


    val giveawayCache = CacheBuilder.newBuilder()
        .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Long, List<Giveaway>> { key ->
            getGiveaways(key)
        })

    private fun getGiveaways(guildId: Long): CompletableFuture<List<Giveaway>> {
        val giveawayFuture = CompletableFuture<List<Giveaway>>()
        taskManager.async {
            val language = giveawayDao.getGiveaways(guildId)
            giveawayFuture.complete(language)
        }
        return giveawayFuture
    }

    suspend fun setGiveaway(guildId: Long, giveaway: Giveaway) {
        val currentList = giveawayCache.get(guildId).await().toMutableList()
        currentList.add(giveaway)
        giveawayCache.put(guildId, CompletableFuture.completedFuture(currentList))
        giveawayDao.insertOrUpdate(guildId, giveaway)
    }

    suspend fun removeGiveaway(guildId: Long, giveaway: Giveaway) {
        val currentList = giveawayCache.get(guildId).await().toMutableList()
        currentList.remove(giveaway)
        giveawayCache.put(guildId, CompletableFuture.completedFuture(currentList))

        giveawayDao.remove(guildId, giveaway.messageId)
    }
}