package me.melijn.melijnbot.database.join

class InactiveJMWrapper(private val inactiveJMDao: InactiveJMDao) {

    fun new(guildId: Long, userId: Long, msgId: Pair<Long, Long>, duration: Long) {
        inactiveJMDao.new(guildId, userId, msgId, duration)
    }

    suspend fun getMsg(guildId: Long, userId: Long): Pair<Long, Long>? {
        return inactiveJMDao.getMsg(guildId, userId)
    }

    fun delete(guildId: Long, userId: Long) {
        inactiveJMDao.delete(guildId, userId)
    }
}