package me.melijn.melijnbot.database.locking

import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.database.getValueFromCache

class LockExcludedWrapper(private val lockExcludedDao: LockExcludedDao) {

    suspend fun getExcluded(guildId: Long, entityType: EntityType): List<Long> {
        val cached = lockExcludedDao.getValueFromCache<List<Long>>("$guildId:$entityType", NORMAL_CACHE)
        if (cached != null) return cached

        val result = lockExcludedDao.getExcluded(guildId, entityType)
        lockExcludedDao.setCacheEntry("$guildId:$entityType", result, HIGHER_CACHE)
        return result
    }

    suspend fun exclude(guildId: Long, entityType: EntityType, entities: List<Long>) {
        val excluded = getExcluded(guildId, entityType).toMutableList()
        excluded.addAll(entities)

        if (entities.size > 1) lockExcludedDao.excludeBulk(guildId, entityType, entities)
        else lockExcludedDao.exclude(guildId, entityType, entities.first())
        lockExcludedDao.setCacheEntry("$guildId:$entityType", excluded, HIGHER_CACHE)
    }

    suspend fun include(guildId: Long, entityType: EntityType, entities: List<Long>) {
        val excluded = getExcluded(guildId, entityType).toMutableList()
        excluded.removeAll(entities)

        if (entities.size > 1) lockExcludedDao.includeBulk(guildId, entityType, entities)
        else lockExcludedDao.include(guildId, entityType, entities.first())
        lockExcludedDao.setCacheEntry("$guildId:$entityType", excluded, HIGHER_CACHE)
    }

}

enum class EntityType(val id: Byte) {
    GUILD(0),
    ROLE(1),
    USER(2),
    EMOTE(3),
    TEXT_CHANNEL(4),
    VOICE_CHANNEL(5),
    PRIV_CHANNEL(6),
    CATEGORY(7);

    companion object {
        fun from(id: Byte): EntityType? {
            for (entity in values()) {
                if (entity.id == id) return entity
            }
            return null
        }
    }
}