package me.melijn.melijnbot.database.socialmedia

import me.melijn.melijnbot.internals.models.PodInfo

class TwitterWrapper(private val twitterDao: TwitterDao) {

    fun store(twitterWebhook: TwitterWebhook) {
        twitterDao.store(twitterWebhook)
    }

    suspend fun getAll(podInfo: PodInfo): List<TwitterWebhook> {
        return twitterDao.getAll(podInfo)
    }

    suspend fun getAll(guildId: Long): List<TwitterWebhook> {
        return twitterDao.getAll(guildId)
    }

    fun delete(guildId: Long, handle: String) {
        twitterDao.delete(guildId, handle)
    }
}