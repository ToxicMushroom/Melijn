package me.melijn.melijnbot.database.giveaway


import com.fasterxml.jackson.module.kotlin.readValue
import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.objectMapper
import java.util.concurrent.CompletableFuture

class GiveawayWrapper(private val giveawayDao: GiveawayDao) {


    suspend fun getGiveaways(guildId: Long): List<Giveaway> {
        giveawayDao.getCacheEntry("$guildId", HIGHER_CACHE)?.let {
            return objectMapper.readValue(it)
        }

        val list = giveawayDao.getGiveaways(guildId)
        giveawayDao.setCacheEntry("$guildId", objectMapper.writeValueAsString(list), NORMAL_CACHE)
        return list
    }

    suspend fun setGiveaway(guildId: Long, giveaway: Giveaway) {
        val list = getGiveaways(guildId).toMutableList()
        list.removeIf { it.messageId == giveaway.messageId }
        list.add(giveaway)
        giveawayDao.insertOrUpdate(guildId, giveaway)
        giveawayDao.setCacheEntry("$guildId", objectMapper.writeValueAsString(list), NORMAL_CACHE)
    }

    suspend fun removeGiveaway(guildId: Long, giveaway: Giveaway) {
        val list = getGiveaways(guildId).toMutableList()
        list.removeIf { it.messageId == giveaway.messageId }
        giveawayDao.remove(guildId, giveaway.messageId)
        giveawayDao.setCacheEntry("$guildId", objectMapper.writeValueAsString(list), NORMAL_CACHE)
    }
}