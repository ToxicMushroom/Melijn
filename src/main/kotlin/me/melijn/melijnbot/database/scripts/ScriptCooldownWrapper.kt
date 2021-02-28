package me.melijn.melijnbot.database.scripts

class ScriptCooldownWrapper(private val scriptCooldownDao: ScriptCooldownDao) {

    fun addCooldown(guildId: Long, invoke: String, seconds: Int) {
        scriptCooldownDao.addCooldown(guildId, invoke, seconds)
    }

    suspend fun isOnCooldown(guildId: Long, invoke: String): Boolean {
        return scriptCooldownDao.isOnCooldown(guildId, invoke)
    }
}