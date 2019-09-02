package me.melijn.melijnbot.database.message

import me.melijn.melijnbot.objects.threading.TaskManager

class MessageHistoryWrapper(val taskManager: TaskManager, private val messageHistoryDao: MessageHistoryDao) {

    suspend fun getMessageById(messageId: Long): DaoMessage? {
        return messageHistoryDao.get(messageId)
    }

    fun addMessage(daoMessage: DaoMessage) {
        messageHistoryDao.add(daoMessage)
    }

    fun setMessage(daoMessage: DaoMessage) {
        messageHistoryDao.set(daoMessage)
    }
}