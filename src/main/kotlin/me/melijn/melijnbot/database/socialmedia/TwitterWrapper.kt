package me.melijn.melijnbot.database.socialmedia

class TwitterWrapper(private val twitterDao: TwitterDao) {

    fun store(twitterWebhook: TwitterWebhook) {
        twitterDao.store(twitterWebhook)
    }

    suspend fun getAll(): List<TwitterWebhook> {
        return twitterDao.getAll()
    }

    suspend fun getAll(guildId: Long): List<TwitterWebhook> {
        return twitterDao.getAll(guildId)
    }

    fun delete(guildId: Long, handle: String) {
        twitterDao.delete(guildId, handle)
    }
}