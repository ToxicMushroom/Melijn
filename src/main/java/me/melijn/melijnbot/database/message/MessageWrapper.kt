package me.melijn.melijnbot.database.message

import me.melijn.melijnbot.objects.threading.TaskManager

class MessageWrapper(val taskManager: TaskManager, private val messageDao: MessageDao) {

    fun getMessageById(messageId: Long, message: (DaoMessage?) -> Unit) {
        messageDao.get(messageId) {
            message(it)
        }
    }

    suspend fun addMessage(daoMessage: DaoMessage) {
        messageDao.add(daoMessage)
    }
}