package me.melijn.melijnbot.database.message

import me.melijn.melijnbot.objects.threading.TaskManager

class MessageWrapper(val taskManager: TaskManager, private val messageDao: MessageDao) {

    suspend fun getMessageById(messageId: Long): DaoMessage? {
        return messageDao.get(messageId)
    }

    fun addMessage(daoMessage: DaoMessage) {
        messageDao.add(daoMessage)
    }

    fun setMessage(daoMessage: DaoMessage) {
        messageDao.set(daoMessage)
    }
}