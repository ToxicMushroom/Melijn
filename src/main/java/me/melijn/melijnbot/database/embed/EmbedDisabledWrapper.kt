package me.melijn.melijnbot.database.embed

import me.melijn.melijnbot.objects.threading.TaskManager

class EmbedDisabledWrapper(val taskManager: TaskManager, private val embedDisabledDao: EmbedDisabledDao) {

    val embedDisabledCache = HashSet<Long>()

    init {
        taskManager.asyncAfter(2000) {
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