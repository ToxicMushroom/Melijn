package me.melijn.melijnbot.database.scripts

import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.objectMapper

class ScriptWrapper(val scriptDao: ScriptDao) {

    fun addScript(entityId: Long, script: Script) {
        scriptDao.removeCacheEntry(entityId)

        scriptDao.addScript(
            entityId,
            script.usePrefix,
            script.trigger,
            objectMapper.writeValueAsString(script.commands),
            script.enabled
        )
    }

    fun removeScript(entityId: Long, trigger: String) {
        scriptDao.removeScript(entityId, trigger)
    }

    suspend fun getScripts(entityId: Long): List<Script> {
        val cached = scriptDao.getValueFromCache<List<Script>>(entityId, HIGHER_CACHE)
        if (cached != null) return cached

        val result = scriptDao.getScripts(entityId)
        scriptDao.setCacheEntry(entityId, result, NORMAL_CACHE)
        return result
    }

}