package me.melijn.melijnbot.database.embed

import me.melijn.melijnbot.database.DaoManager

class EmbedDisabledWrapper(private val embedDisabledDao: EmbedDisabledDao) {

    val embedDisabledCache = HashSet<Long>()

    init {
        DaoManager.afterTableFunctions.add {
            embedDisabledDao.getSet {
                embedDisabledCache.addAll(it)
            }
        }
    }

    suspend fun setDisabled(guildId: Long, disabledState: Boolean) {
        if (disabledState && !embedDisabledCache.contains(guildId)) {
            embedDisabledCache.add(guildId)
            embedDisabledDao.add(guildId)
        } else {
            embedDisabledCache.remove(guildId)
            embedDisabledDao.remove(guildId)
        }
    }
}